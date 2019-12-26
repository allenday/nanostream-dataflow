package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.exception.BadRequestException;
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

import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;
import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.lang.String.format;

@Service
public class Starter {

    private static final Logger logger = LoggerFactory.getLogger(Starter.class);

    private final ListFetcher listFetcher;

    private String project;
    private String bucket;

    @Autowired
    public Starter(ListFetcher listFetcher) {
        this.listFetcher = listFetcher;
        project = getProjectId();
        bucket = format("gs://%s-dataflow", project);
    }

    public String invoke(LaunchParams launchParams) throws IOException {
        String templateName = format("nanostream-%s", launchParams.processingMode);
        JSONObject jsonObj = makeParams(launchParams);

        if (!isRunningJob()) {
            logger.info("Starting pipeline: {}", launchParams.pipelineName);
            HttpURLConnection connection = sendLaunchDataflowJobFromTemplateRequest(jsonObj, templateName);
            return getRequestOutput(connection);
        } else {
            throw new BadRequestException("NO_MORE_RUNNING_JOBS_ALLOWED", "Only one running job allowed");
        }
    }

    private boolean isRunningJob() throws IOException {
        String json = listFetcher.invoke();

// Json sample:
//    {
//      "jobs": [
//        {
//          "id": "2019-12-12_05_14_07-11307687861672664813",
//          "projectId": "tas-nanostream-test1",
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

    private JSONObject makeParams(LaunchParams params) {
        JSONObject jsonObj = null;
        try {
            JSONObject parameters = new JSONObject();
            parameters.put("outputCollectionNamePrefix", params.outputCollectionNamePrefix);
            parameters.put("outputDocumentNamePrefix", params.outputDocumentNamePrefix);

            JSONObject environment = new JSONObject()
                    .put("tempLocation", bucket + "/tmp/")
                    .put("bypassTempDirValidation", false);
            jsonObj = new JSONObject()
                    .put("jobName", params.pipelineName)
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
