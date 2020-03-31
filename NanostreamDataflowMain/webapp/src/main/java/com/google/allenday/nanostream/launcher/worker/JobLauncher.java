package com.google.allenday.nanostream.launcher.worker;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.allenday.nanostream.launcher.data.PipelineEntity;
import com.google.allenday.nanostream.launcher.data.ReferenceDb;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
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
import static com.google.allenday.nanostream.launcher.util.JsonResponseParser.extractJobId;
import static com.google.allenday.nanostream.launcher.util.JsonResponseParser.parseRunningJobs;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;
import static com.google.common.base.Preconditions.checkNotNull;
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

    public List<String> launchById(String pipelineId) throws ExecutionException, InterruptedException, IOException {
        logger.info("Launch new job for pipeline: {}", pipelineId);
        DocumentSnapshot document = getDocumentById(pipelineId);
        List<String> jobIds = new ArrayList<>();
        tryProcessDocument(jobIds, document);

        return jobIds;
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
                Optional<String> newJobId = processDocument(document);
                newJobId.ifPresent(jobIds::add);
            } catch (AlreadyLockedException e) {
                // do nothing. Ignore already locked
            }
        }
    }

    private Optional<String> processDocument(DocumentSnapshot document) throws InterruptedException, ExecutionException, IOException {
        PipelineEntity pipelineEntity = lockPipeline(document);
        Optional<String> newJobId = Optional.empty();
        try {
            logger.info("Pipeline name: {}", pipelineEntity.getPipelineName());

            if (!runningJobsContains(pipelineEntity.getJobIds())) {
                newJobId = Optional.of(runNewJob(pipelineEntity));
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

    private PipelineEntity unlockPipeline(DocumentSnapshot document, Optional<String> newJobId) throws ExecutionException, InterruptedException {
        DocumentReference docRef = document.getReference();
        ApiFuture<PipelineEntity> futureTransaction = db.runTransaction(transaction -> {
            DocumentSnapshot snapshot = transaction.get(docRef).get();
            PipelineEntity pipelineEntity = mapToPipelineOptions(snapshot);
            if (newJobId.isPresent()) {
                pipelineEntity.getJobIds().add(newJobId.get());
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
        return parseRunningJobs(json);
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

        private JSONObject makeParams(PipelineEntity pipelineEntity) throws JsonProcessingException {
            JSONObject jsonObj = null;
            try {
                String jobName = pipelineEntity.getPipelineName() + '_' + makeTimestamp();
                
                JSONObject parameters = new JSONObject();
                parameters.put("outputCollectionNamePrefix", pipelineEntity.getOutputCollectionNamePrefix());
                parameters.put("inputDataSubscription", pipelineEntity.getInputDataSubscription());
                parameters.put("inputDir", pipelineEntity.getInputFolder());
                parameters.put("autoStopDelay", pipelineEntity.getAutoStopDelaySeconds().toString());
                parameters.put("refDataJsonString", makeRefDataJsonString(pipelineEntity.getReferenceDbs()));
                parameters.put("jobNameLabel", jobName);  // This parameter used in autostop logic to identify currently running pipeline. For unknown reason jobName not available in pipeline itself

                JSONObject environment = new JSONObject()
                        .put("tempLocation", bucket + "/tmp/")
                        .put("bypassTempDirValidation", false);


                jsonObj = new JSONObject()
                        .put("jobName", jobName)
                        .put("parameters", parameters)
                        .put("environment", environment);

                logger.info("Launch pipeline params: {}", jsonObj);
            } catch (JSONException e) {
                logger.error(e.getMessage(), e);
            }
            return jsonObj;
        }

        private String makeRefDataJsonString(List<ReferenceDb> referenceDbs) throws JsonProcessingException {
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.writeValueAsString(referenceDbs);
        }

        private HttpURLConnection sendLaunchDataflowJobFromTemplateRequest(JSONObject jsonObj, String templateName) throws IOException {
            return sendRequest("POST", getUrl(templateName), jsonObj);
        }

        private URL getUrl(String templateName) throws MalformedURLException {
            // See docs: https://cloud.google.com/dataflow/docs/reference/rest/v1b3/projects.templates/launch
            return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/templates:launch?gcs_path=%s/templates/%s",
                    project, bucket, templateName));
        }

    }
}
