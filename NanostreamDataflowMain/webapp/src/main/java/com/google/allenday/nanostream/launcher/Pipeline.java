package com.google.allenday.nanostream.launcher;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

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

public class Pipeline {
    private static final String DATAFLOW_API_BASE_URI = "https://dataflow.googleapis.com/v1b3/";
    private static final String DATAFLOW_JOB_STATE_CANCELLED = "JOB_STATE_CANCELLED";

    static class Starter {
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
            URL url = getUrl();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
            conn.setRequestProperty("Content-Type", "application/json");

            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                jsonObj.write(writer);
            }
            return conn;
        }

        private URL getUrl() throws MalformedURLException {
            return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/templates:launch?gcs_path=%s/templates/%s",
                    project, bucket, templateName));
        }
    }

    static class Stopper {
        private HttpServletResponse response;
        private String project;
        private String location;
        private String jobId;

        public Stopper(HttpServletRequest request, HttpServletResponse response) {
            this.response = response;
            project = getProjectId();

            location = request.getParameter("location");
            jobId = request.getParameter("jobId");
        }

        public void invoke() throws IOException {
            JSONObject jsonObj = makeParams();

            HttpURLConnection connection = sendStopDataflowJobRequest(jsonObj);

            printOutput(connection, response);
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

    static class ListFetcher {
        private HttpServletResponse response;
        private String project;

        public ListFetcher(HttpServletRequest request, HttpServletResponse response) {
            this.response = response;
            project = getProjectId();
        }

        public void invoke() throws IOException {
            HttpURLConnection connection = sendListDataflowJobsRequest();

            printOutput(connection, response);
        }

        private HttpURLConnection sendListDataflowJobsRequest() throws IOException {
            URL url = getUrl();
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
            conn.setRequestProperty("Content-Type", "application/json");

            return conn;
        }

        private URL getUrl() throws MalformedURLException {
            return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/jobs", project));
        }
    }

    private static String getProjectId() {
        return System.getenv("GOOGLE_CLOUD_PROJECT");
    }

    private static String getAccessToken() {
        List<String> scopes = new ArrayList<>();
        scopes.add("https://www.googleapis.com/auth/cloud-platform");
        final AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
        return appIdentity.getAccessToken(scopes).getAccessToken();
    }

    private static void printOutput(HttpURLConnection conn, HttpServletResponse response) throws IOException {
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
}
