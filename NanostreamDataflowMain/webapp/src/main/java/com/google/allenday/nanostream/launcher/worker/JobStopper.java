package com.google.allenday.nanostream.launcher.worker;

import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;
import static java.lang.String.format;

public class JobStopper {
    private String project;
    private String location;
    private String jobId;

    public JobStopper(HttpServletRequest request) {
        project = getProjectId();

        location = request.getParameter("location");
        jobId = request.getParameter("jobId");
    }

    public String invoke() throws IOException {
        JSONObject jsonObj = makeParams();

        HttpURLConnection connection = sendStopDataflowJobRequest(jsonObj);

        return getRequestOutput(connection);
    }

    private JSONObject makeParams() {
        JSONObject jsonObj = null;
        try {
            jsonObj = new JSONObject()
                    .put("requestedState", DATAFLOW_JOB_STATE_CANCELLED);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonObj;
    }

    private HttpURLConnection sendStopDataflowJobRequest(JSONObject jsonObj) throws IOException {
        URL url = getUrl();
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("PUT");
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        conn.setRequestProperty("Content-Type", "application/json");

        try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
            jsonObj.write(writer);
        }
        return conn;
    }

    private URL getUrl() throws MalformedURLException {
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/locations/%s/jobs/%s",
                project, location, jobId));
    }
}