package com.google.allenday.nanostream.launcher.worker;

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
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;


import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.lang.String.format;

@Service
public class JobStarter {

    private static final Logger logger = LoggerFactory.getLogger(JobStarter.class);

    private final JobListFetcher jobListFetcher;

    private String project;
    private String bucket;

    @Autowired
    public JobStarter(JobListFetcher jobListFetcher) {
        this.jobListFetcher = jobListFetcher;
        project = getProjectId();
        bucket = format("gs://%s-dataflow", project);
    }

    public String invoke(PipelineRequestParams pipelineRequestParams) throws IOException, ExecutionException, InterruptedException {
        String templateName = format("nanostream-%s", pipelineRequestParams.getProcessingMode());
        JSONObject jsonObj = makeParams(pipelineRequestParams);

//        if (!isRunningJob()) {
            logger.info("Starting job: {}", pipelineRequestParams.getPipelineName());
            HttpURLConnection connection = sendLaunchDataflowJobFromTemplateRequest(jsonObj, templateName);
        String requestOutput = getRequestOutput(connection);

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

        return requestOutput;
//        } else {
//            throw new BadRequestException("NO_MORE_RUNNING_JOBS_ALLOWED", "Only one running job allowed");
//        }
    }

    // TODO: check isRunningPipeline (by input backet & folder)
    private boolean isRunningJob() throws IOException {
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

        List<Map<String, Object>> jobList =
                parse(json).read("$.jobs[?]", filter(
                        where("currentState").in("JOB_STATE_RUNNING", "JOB_STATE_PENDING", "JOB_STATE_QUEUED")
                ));

        return jobList.size() > 0;
    }

    private JSONObject makeParams(PipelineRequestParams params) {
        JSONObject jsonObj = null;
        try {
            JSONObject parameters = new JSONObject();
            parameters.put("outputCollectionNamePrefix", params.getOutputDocumentNamePrefix());
            parameters.put("outputDocumentNamePrefix", params.getOutputDocumentNamePrefix());
            parameters.put("inputDataSubscription", params.getInputDataSubscription());

            JSONObject environment = new JSONObject()
                    .put("tempLocation", bucket + "/tmp/")
                    .put("bypassTempDirValidation", false);
            jsonObj = new JSONObject()
                    .put("jobName", params.getPipelineName())
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

    private URL getUrl(String templateName) throws MalformedURLException {
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/templates:launch?gcs_path=%s/templates/%s",
                project, bucket, templateName));
    }
}
