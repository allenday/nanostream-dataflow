package com.google.allenday.nanostream.launcher;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.apphosting.api.ApiProxy.Environment;
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

import static com.google.apphosting.api.ApiProxy.getCurrentEnvironment;
import static java.lang.String.format;
import static java.net.HttpURLConnection.HTTP_OK;

@WebServlet(name = "LaunchDataflow", value = "/launch")
public class LaunchDataflow extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new PipelineStarter(req, resp).invoke();
    }

    private class PipelineStarter {
        private final String templateName;
        private HttpServletResponse response;
        private String project;
        private String bucket;
        private LaunchParams params;

        public PipelineStarter(HttpServletRequest request, HttpServletResponse response) {
            this.response = response;
            params = new LaunchParams(request);

            Environment env = getCurrentEnvironment();
            project = env.getAppId();
            bucket = format("gs://%s-dataflow", project);
            templateName = format("nanostream-%s", params.processingMode);
        }

        public void invoke() throws IOException {
            JSONObject jsonObj = makeParams();

            HttpURLConnection connection = sendLaunchDataflowJobFromTemplateRequest(jsonObj);

            printOutput(connection);
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
            return new URL(format("https://dataflow.googleapis.com/v1b3/projects/%s/templates:launch?gcs_path=%s/templates/%s",
                            project, bucket, templateName));
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
