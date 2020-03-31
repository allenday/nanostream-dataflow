package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.config.GcpProject;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.TimeUnit;

import static com.google.allenday.nanostream.launcher.util.DateTimeUtil.makeTimestamp;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;
import static com.google.common.util.concurrent.Uninterruptibles.sleepUninterruptibly;
import static java.lang.String.format;

@Service
public class SubscriptionCreator {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionCreator.class);

    private String project;

    @Autowired
    public SubscriptionCreator(GcpProject gcpProject) {
        project = gcpProject.getId();
    }

    public String invoke(String topic) throws IOException {
        String subscriptionName = makeRandomSubscriptionName();
        JSONObject jsonObj = makeParams(topic);

        HttpURLConnection connection = sendCreatePubSubSubscriptionRequest(jsonObj, subscriptionName);

        return getRequestOutput(connection);
    }

    private String makeRandomSubscriptionName() {
        String currentDate = makeTimestamp();
        sleepUninterruptibly(10, TimeUnit.MILLISECONDS); // make a pause between consequent requests to avoid producing the same timestamp
        return format("nanostream-%s", currentDate).toLowerCase();
    }

    private JSONObject makeParams(String topic) {
        JSONObject jsonObj = new JSONObject();
        try {
            String fullTopicName = format("projects/%s/topics/%s", project, topic);
            jsonObj.put("topic", fullTopicName);
        } catch (JSONException e) {
            logger.error(e.getMessage(), e);
        }
        return jsonObj;
    }

    private HttpURLConnection sendCreatePubSubSubscriptionRequest(JSONObject jsonObj, String subscriptionName) throws IOException {
        URL url = getUrl(subscriptionName);
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

    private URL getUrl(String subscriptionName) throws MalformedURLException {
        // See: https://cloud.google.com/pubsub/docs/reference/rest/v1/projects.subscriptions/create
        return new URL(format(PUBSUB_API_BASE_URI + "projects/%s/subscriptions/%s", project, subscriptionName));
    }

}
