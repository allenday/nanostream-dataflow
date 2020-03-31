package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.config.GcpProject;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;
import static java.lang.String.format;

@Service
public class JobStopper {
    private String project;

    @Autowired
    public JobStopper(GcpProject gcpProject) {
        project = gcpProject.getId();
    }

    public String invoke(String location, String jobId) throws IOException {
        JSONObject jsonObj = makeParams();

        HttpURLConnection connection = sendStopDataflowJobRequest(jsonObj, location, jobId);

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

    private HttpURLConnection sendStopDataflowJobRequest(JSONObject jsonObj, String location, String jobId) throws IOException {
        URL url = getUrl(location, jobId);
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

    private URL getUrl(String location, String jobId) throws MalformedURLException {
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/locations/%s/jobs/%s",
                project, location, jobId));
    }
}