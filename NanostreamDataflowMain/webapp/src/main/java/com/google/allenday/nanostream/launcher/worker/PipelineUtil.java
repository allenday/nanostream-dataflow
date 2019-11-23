package com.google.allenday.nanostream.launcher.worker;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static java.net.HttpURLConnection.HTTP_OK;

class PipelineUtil {
    public static final String DATAFLOW_API_BASE_URI = "https://dataflow.googleapis.com/v1b3/";
    public static final String DATAFLOW_JOB_STATE_CANCELLED = "JOB_STATE_CANCELLED";

    public static String getProjectId() {
//        ApiProxy.Environment env = getCurrentEnvironment();
//        return env.getAppId();
        return System.getenv("GOOGLE_CLOUD_PROJECT");
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

    public static String printOutput(HttpURLConnection conn) throws IOException {
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
