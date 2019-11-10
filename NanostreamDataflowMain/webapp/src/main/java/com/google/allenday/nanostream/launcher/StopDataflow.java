package com.google.allenday.nanostream.launcher;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;

@WebServlet(name = "StopDataflow", value = "/stop")
public class StopDataflow extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new PipelineStopper(req, resp).invoke();
    }

    private class PipelineStopper {
        private static final String DATAFLOW_JOB_STATE_CANCELLED = "JOB_STATE_CANCELLED";
        private HttpServletResponse response;
        private String project;
        private String location;
        private String jobId;

        public PipelineStopper(HttpServletRequest request, HttpServletResponse response) {
            this.response = response;
            project = System.getenv("GOOGLE_CLOUD_PROJECT");

            location = request.getParameter("location");
            jobId = request.getParameter("jobId");
        }

        public void invoke() throws IOException {
            JSONObject jsonObj = makeParams();

            HttpURLConnection connection = sendStopDataflowJobRequest(jsonObj);

            printOutput(connection);
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
            return new URL(format("https://dataflow.googleapis.com/v1b3/projects/%s/locations/%s/jobs/%s",
                            project, location, jobId));
        }

        private void printOutput(HttpURLConnection conn) throws IOException {
            int respCode = conn.getResponseCode();
            try (PrintWriter writer = response.getWriter()) {
                if (respCode == HTTP_OK) {
                    response.setContentType("application/json");
                    String line;
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                        while ((line = reader.readLine()) != null) {
                            writer.println(line);
                        }
                    }
                } else {
                    StringWriter w = new StringWriter();
                    IOUtils.copy(conn.getErrorStream(), w, "UTF-8");
                    writer.println(w.toString());
                }
            }
        }

        private String getAccessToken() {
            List<String> scopes = new ArrayList<>();
            scopes.add("https://www.googleapis.com/auth/cloud-platform");
            final AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
            return appIdentity.getAccessToken(scopes).getAccessToken();
        }
    }
}
