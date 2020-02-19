package com.google.allenday.nanostream.launcher.worker;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import com.jayway.jsonpath.DocumentContext;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.google.allenday.nanostream.launcher.util.DateTimeUtil.makeTimestamp;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.lang.String.format;

/**
 * Documentation reference:
 * firestore get data:
 * https://firebase.google.com/docs/firestore/query-data/get-data
 */
@Service
public class JobLauncher {

    private static final Logger logger = LoggerFactory.getLogger(JobLauncher.class);

    private final JobListFetcher jobListFetcher;

    private String project;
    private String bucket;
    private Firestore db;

    @Autowired
    public JobLauncher(JobListFetcher jobListFetcher) {
        this.jobListFetcher = jobListFetcher;
        project = getProjectId();
        bucket = format("gs://%s-dataflow", project);
        db = initFirestoreConnection();
    }

    public List<String> launchAutostarted(String targetInputFolder, String uploadBucketName) throws ExecutionException, InterruptedException, IOException {
        logger.info(format("Launch new job: targetInputFolder %s, uploadBucketName %s", targetInputFolder, uploadBucketName));

        List<QueryDocumentSnapshot> documents = getUnlockedDocumentsToStart(targetInputFolder, uploadBucketName);

        List<String> jobIds = new ArrayList<>();
        for (DocumentSnapshot document : documents) {
            tryProcessDocument(jobIds, document);
        }

        return jobIds;
    }

    public void launchById(String pipelineId) throws ExecutionException, InterruptedException, IOException {
        logger.info("Launch new job for pipeline: {}", pipelineId);
        DocumentSnapshot document = getDocumentById(pipelineId);
        List<String> jobIds = new ArrayList<>();
        tryProcessDocument(jobIds, document);
    }

    private List<QueryDocumentSnapshot> getUnlockedDocumentsToStart(String targetInputFolder, String uploadBucketName) throws InterruptedException, ExecutionException {
//        {"inputDataSubscription":"projects/nanostream-test1/subscriptions/nanostream-20200211t090136.895z",
//        "inputFolder":"dogbite","jobIds":[],"processingMode":"species","outputCollectionNamePrefix":"test89",
//        "name":"test89","outputDocumentNamePrefix":"","uploadBucketName":"nanostream-test1-upload-bucket",
//        "id":"a24d0756-2e78-45a0-a629-5e99374cbbc1","createdAt":"2020-02-11T09:01:42.131Z"}

        CollectionReference pipelines = db.collection(FIRESTORE_PIPELINES_COLLECTION);
        Query query = pipelines
                .whereEqualTo("inputFolder", targetInputFolder)
                .whereEqualTo("uploadBucketName", uploadBucketName)
                .whereEqualTo("lockStatus", "UNLOCKED")
                .whereEqualTo("pipelineAutoStart", true);
        ApiFuture<QuerySnapshot> querySnapshot = query.get();

        return querySnapshot.get().getDocuments();
    }

    private DocumentSnapshot getDocumentById(String pipelineId) throws InterruptedException, ExecutionException {
        return db.collection(FIRESTORE_PIPELINES_COLLECTION).document(pipelineId).get().get();
    }

    private void tryProcessDocument(List<String> jobIds, DocumentSnapshot document) throws InterruptedException, ExecutionException, IOException {
        if (document.exists()) {
            try {
                jobIds.add(processDocument(document));
            } catch (AlreadyLockedException e) {
                // do nothing. Ignore already locked
            }
        }
    }

    private String processDocument(DocumentSnapshot document) throws InterruptedException, ExecutionException, IOException {
        PipelineEntity pipelineEntity = lockPipeline(document);
        String newJobId = "";
        try {
            logger.info("Pipeline name: {}", pipelineEntity.getPipelineName());
//            logger.info("DocumentSnapshot id: " + document.getId());
//            logger.info("Subscription: " + pipelineEntity.getInputDataSubscription());
//            logger.info("Lock status: " + pipelineEntity.getLockStatus());

            if (!runningJobsContains(pipelineEntity.getJobIds())) {
                newJobId = runNewJob(pipelineEntity);
            } else {
                logger.info("Pipeline {} already has running jobs", pipelineEntity.getPipelineName());
            }
        } finally {
            unlockPipeline(document, newJobId);
        }
        return newJobId;
    }

    private boolean runningJobsContains(List<String> jobIds) throws IOException {
        Set<String> jobIdsSet = new HashSet<>(jobIds);  // use HashSet for performance reason
        List<Map<String, Object>> jobList = getRunningJobs();
        for (Map<String, Object> objectMap : jobList) {
            String runningJobId = (String) objectMap.get("id");
            if (jobIdsSet.contains(runningJobId)) {
                return true;
            }
        }
        return false;
    }

    private String runNewJob(PipelineEntity pipelineEntity) throws IOException {
        return new JobRunner(pipelineEntity).invoke();
    }

    private PipelineEntity lockPipeline(DocumentSnapshot document) throws InterruptedException, ExecutionException {
        DocumentReference docRef = document.getReference();
        ApiFuture<PipelineEntity> futureTransaction = db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef).get();
            if (!"LOCKED".equals(snapshot.get("lockStatus"))) {
                transaction.update(docRef, "lockStatus", "LOCKED");
                PipelineEntity pipelineEntity = mapToPipelineOptions(snapshot);
                pipelineEntity.setLockStatus("LOCKED");
                return pipelineEntity;
            } else {
                throw new AlreadyLockedException();
            }
        });
        return futureTransaction.get();
    }

    private PipelineEntity unlockPipeline(DocumentSnapshot document, String newJobId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = document.getReference();
        ApiFuture<PipelineEntity> futureTransaction = db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef).get();
            PipelineEntity pipelineEntity = mapToPipelineOptions(snapshot);
            if (newJobId != null) {
                pipelineEntity.getJobIds().add(newJobId);
                transaction.update(docRef, "jobIds", pipelineEntity.getJobIds());
            }
            pipelineEntity.setLockStatus("UNLOCKED");
            transaction.update(docRef, "lockStatus", "UNLOCKED");
            return pipelineEntity;
        });
        return futureTransaction.get();
    }

    private PipelineEntity mapToPipelineOptions(DocumentSnapshot snapshot) {
        PipelineEntity pipelineEntity = snapshot.toObject(PipelineEntity.class);
        checkNotNull(pipelineEntity);
        return pipelineEntity;
    }

    private List<Map<String, Object>> getRunningJobs() throws IOException { // TODO: add local cache to not call it frequently
        String json = jobListFetcher.invoke();

// Json sample:
//    {
//      "jobs": [
//        {
//          "id": "2019-12-12_05_14_07-11307687861672664813",
//          "projectId": "nanostream-test1",
//          "name": "template-32a4ca21-24d5-41fe-b531-f551f5179cdf",
//          "type": "JOB_TYPE_STREAMING",
//          "currentState": "JOB_STATE_CANCELLING",
//          "currentStateTime": "2019-12-12T13:19:24.867468Z",
//          "requestedState": "JOB_STATE_CANCELLED",
//          "createTime": "2019-12-12T13:14:08.566549Z",
//          "location": "us-central1",
//          "jobMetadata": {
//            "sdkVersion": {
//              "version": "2.16.0",
//              "versionDisplayName": "Apache Beam SDK for Java",
//              "sdkSupportStatus": "SUPPORTED"
//            }
//          },
//          "startTime": "2019-12-12T13:14:08.566549Z"
//        }
//      ]
//    }

        // JsonPath docs: https://github.com/json-path/JsonPath
        // TODO: fix filter error when job list is empty
        return parse(json).read("$.jobs[?]", filter(
                where("currentState").in("JOB_STATE_RUNNING", "JOB_STATE_PENDING", "JOB_STATE_QUEUED")
        ));
    }

    private class AlreadyLockedException extends RuntimeException {
    }

    private class JobRunner {
        private PipelineEntity pipelineEntity;

        public JobRunner(PipelineEntity pipelineEntity) {
            this.pipelineEntity = pipelineEntity;
        }

        public String invoke() throws IOException {
            String templateName = getTemplateName(pipelineEntity);
            JSONObject jsonObj = makeParams(pipelineEntity);

            logger.info("Starting job for pipeline: {}", pipelineEntity.getPipelineName());
            HttpURLConnection connection = sendLaunchDataflowJobFromTemplateRequest(jsonObj, templateName);
            String requestOutput = getRequestOutput(connection);

            return extractJobId(requestOutput);
        }

        private String getTemplateName(PipelineEntity pipelineEntity) {
            return format("nanostream-%s", pipelineEntity.getProcessingMode());
        }

        private JSONObject makeParams(PipelineEntity params) {
            JSONObject jsonObj = null;
            try {
                JSONObject parameters = new JSONObject();
                parameters.put("outputCollectionNamePrefix", params.getOutputCollectionNamePrefix());
    //            parameters.put("outputDocumentNamePrefix", params.getOutputDocumentNamePrefix());
                parameters.put("inputDataSubscription", params.getInputDataSubscription());

                JSONObject environment = new JSONObject()
                        .put("tempLocation", bucket + "/tmp/")
                        .put("bypassTempDirValidation", false);
                jsonObj = new JSONObject()
                        .put("jobName", params.getPipelineName() + "_" + makeTimestamp())
                        .put("parameters", parameters)
                        .put("environment", environment);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return jsonObj;
        }

        private HttpURLConnection sendLaunchDataflowJobFromTemplateRequest(JSONObject jsonObj, String templateName) throws IOException {
            return sendRequest("POST", getUrl(templateName), jsonObj);
        }

        private String extractJobId(String json) {
    // Request output sample:
    //        {
    //          "job": {
    //            "id": "2020-02-11_03_16_40-8884773552833626077",
    //            "projectId": "nanostream-test1",
    //            "name": "id123467",
    //            "type": "JOB_TYPE_STREAMING",
    //            "currentStateTime": "1970-01-01T00:00:00Z",
    //            "createTime": "2020-02-11T11:16:41.405546Z",
    //            "location": "us-central1",
    //            "startTime": "2020-02-11T11:16:41.405546Z"
    //          }
    //        }
            DocumentContext document = parse(json); // TODO: move parser to a separate class
            String jobId = document.read("$.job.id");
            logger.info("new job id: " + jobId);
            return jobId;
        }

        private URL getUrl(String templateName) throws MalformedURLException {
            // See docs: https://cloud.google.com/dataflow/docs/reference/rest/v1b3/projects.templates/launch
            return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/templates:launch?gcs_path=%s/templates/%s",
                    project, bucket, templateName));
        }

    }
}
