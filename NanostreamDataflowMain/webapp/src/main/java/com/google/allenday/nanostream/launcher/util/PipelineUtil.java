package com.google.allenday.nanostream.launcher.util;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import com.google.apphosting.api.ApiProxy;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static com.google.apphosting.api.ApiProxy.getCurrentEnvironment;
import static java.net.HttpURLConnection.HTTP_OK;

public final class PipelineUtil {
    public static final String DATAFLOW_API_BASE_URI = "https://dataflow.googleapis.com/v1b3/";
    public static final String PUBSUB_API_BASE_URI = "https://pubsub.googleapis.com/v1/";
    public static final String DATAFLOW_JOB_STATE_CANCELLED = "JOB_STATE_CANCELLED";
    public static final String FIRESTORE_PIPELINES_COLLECTION = "_pipelines";


    private static final Logger logger = LoggerFactory.getLogger(PipelineUtil.class);

    public static String getProjectId() { // TODO: convert to a bean to run the code once on startup
        String projectId;
        projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (projectId != null && !projectId.isEmpty()) {
            logger.info("ProjectId from env var: " + projectId);
            return projectId;
        } else { // useful for local environment where GOOGLE_CLOUD_PROJECT not set
            ApiProxy.Environment env = getCurrentEnvironment();
            String appId = env.getAppId();
            // According to docs (https://cloud.google.com/appengine/docs/standard/java/appidentity/)
            // appId should be the same as projectId.
            // In reality appId is prefixed by "s~" chars
            projectId = appId.replaceAll("^s~", "");
            logger.info("ProjectId from api proxy: " + projectId);
            return projectId;
        }
    }

    public static Firestore initFirestoreConnection() {
        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(getProjectId())
                .build();
        return firestoreOptions.getService();
    }

    public static String getAccessToken() {
        List<String> scopes = new ArrayList<>();
        scopes.add("https://www.googleapis.com/auth/cloud-platform");
        final AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
        return appIdentity.getAccessToken(scopes).getAccessToken();
    }

    public static HttpURLConnection sendRequest(String method, URL url, JSONObject data) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod(method);
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        conn.setRequestProperty("Content-Type", "application/json");

        if (data != null) {
            try (OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream())) {
                data.write(writer);
            }
        }
        return conn;
    }

    public static String getRequestOutput(HttpURLConnection conn) throws IOException {
        StringBuilder stringBuilder = new StringBuilder();
        if (conn.getResponseCode() == HTTP_OK) {
            String line;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
                while ((line = reader.readLine()) != null) {
                    stringBuilder.append(line);
                    stringBuilder.append("\n");
                }
            }
        } else {
            StringWriter w = new StringWriter();
            IOUtils.copy(conn.getErrorStream(), w, "UTF-8");
            stringBuilder.append(w.toString());
        }
        return stringBuilder.toString();
    }
}
