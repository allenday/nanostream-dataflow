package com.google.allenday.nanostream.launcher.util;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
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

import static java.net.HttpURLConnection.HTTP_OK;

public final class PipelineUtil {
    public static final String DATAFLOW_API_BASE_URI = "https://dataflow.googleapis.com/v1b3/";
    public static final String PUBSUB_API_BASE_URI = "https://pubsub.googleapis.com/v1/";
    public static final String DATAFLOW_JOB_STATE_CANCELLED = "JOB_STATE_CANCELLED";
    public static final String FIRESTORE_PIPELINES_COLLECTION = "_pipelines";


    private static final Logger logger = LoggerFactory.getLogger(PipelineUtil.class);

    public static Firestore initFirestoreConnection(String projectId) {
        FirestoreOptions firestoreOptions = FirestoreOptions.getDefaultInstance().toBuilder()
                .setProjectId(projectId)
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
