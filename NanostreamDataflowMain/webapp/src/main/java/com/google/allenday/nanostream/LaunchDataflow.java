package com.google.allenday.nanostream;

import javax.servlet.annotation.WebServlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

import com.google.appengine.api.appidentity.AppIdentityService;
import com.google.appengine.api.appidentity.AppIdentityServiceFactory;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.UUID;

import com.google.apphosting.api.ApiProxy;
import org.apache.commons.io.IOUtils;
import org.json.JSONException;
import org.json.JSONObject;

@WebServlet(name = "LaunchDataflow", value = "/launch")
public class LaunchDataflow extends HttpServlet {
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse response) throws ServletException, IOException {
        ApiProxy.Environment env = ApiProxy.getCurrentEnvironment();
        String project = env.getAppId();
        String bucket = "gs://" + project + "-dataflow";


        ArrayList<String> scopes = new ArrayList<String>();
        scopes.add("https://www.googleapis.com/auth/cloud-platform");
        final AppIdentityService appIdentity = AppIdentityServiceFactory.getAppIdentityService();
        final AppIdentityService.GetAccessTokenResult accessToken = appIdentity.getAccessToken(scopes);

        JSONObject jsonObj = null;
        try {
            JSONObject parameters = new JSONObject();
            JSONObject environment = new JSONObject()
                    .put("tempLocation", bucket + "/tmp/")
                    .put("bypassTempDirValidation", false);
            jsonObj = new JSONObject()
                    .put("jobName", "template-" + UUID.randomUUID().toString())
                    .put("parameters", parameters)
                    .put("environment", environment);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        URL url = new URL(String.format("https://dataflow.googleapis.com/v1b3/projects/%s/templates"
                + ":launch?gcs_path=" + bucket + "/templates/nanostream-species", project));
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Authorization", "Bearer " + accessToken.getAccessToken());
        conn.setRequestProperty("Content-Type", "application/json");

        OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
        jsonObj.write(writer);
        writer.close();

        int respCode = conn.getResponseCode();
        if (respCode == HttpURLConnection.HTTP_OK) {
            response.setContentType("application/json");
            String line;
            BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            while ((line = reader.readLine()) != null) {
                response.getWriter().println(line);
            }
            reader.close();

        } else {
            StringWriter w = new StringWriter();
            IOUtils.copy(conn.getErrorStream(), w, "UTF-8");
            response.getWriter().println(w.toString());
        }
    }
}
