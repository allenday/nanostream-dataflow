package com.google.allenday.nanostream.launcher.worker;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;
import static java.lang.String.format;

public class Starter {
    private final String templateName;
    private HttpServletResponse response;
    private String project;
    private String bucket;
    private LaunchParams params;

    public Starter(HttpServletRequest request, HttpServletResponse response) {
        this.response = response;
        params = new LaunchParams(request);

        project = getProjectId();
        bucket = format("gs://%s-dataflow", project);
        templateName = format("nanostream-%s", params.processingMode);
    }

    public void invoke() throws IOException {
        JSONObject jsonObj = makeParams();

        HttpURLConnection connection = sendLaunchDataflowJobFromTemplateRequest(jsonObj);
        // TODO: save metadata about created job
        printOutput(connection, response);
    }

    private JSONObject makeParams() {
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

    private HttpURLConnection sendLaunchDataflowJobFromTemplateRequest(JSONObject jsonObj) throws IOException {
        return sendRequest("POST", getUrl(), jsonObj);
    }

    private URL getUrl() throws MalformedURLException {
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/templates:launch?gcs_path=%s/templates/%s",
                project, bucket, templateName));
    }
}
