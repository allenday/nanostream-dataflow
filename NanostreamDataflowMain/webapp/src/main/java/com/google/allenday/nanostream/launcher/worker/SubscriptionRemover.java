package com.google.allenday.nanostream.launcher.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;
import static java.lang.String.format;

@Service
public class SubscriptionRemover {

    private static final Logger logger = LoggerFactory.getLogger(SubscriptionRemover.class);

    private String project;


    public SubscriptionRemover() {
        project = getProjectId();
    }

    public String invoke(String subscription) throws IOException {
        logger.info(format("Deleting subscription '%s'", subscription));
        HttpURLConnection connection = sendDeleteSubscriptionRequest(subscription);

        return getRequestOutput(connection);
    }


    private HttpURLConnection sendDeleteSubscriptionRequest(String subscriptionName) throws IOException {
        URL url = getUrl(subscriptionName);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setDoOutput(true);
        conn.setRequestMethod("DELETE");
        conn.setRequestProperty("Authorization", "Bearer " + getAccessToken());
        conn.setRequestProperty("Content-Type", "application/json");

        return conn;
    }

    private URL getUrl(String subscriptionName) throws MalformedURLException {
        // See: https://cloud.google.com/pubsub/docs/reference/rest/v1/projects.subscriptions/delete
        return new URL(PUBSUB_API_BASE_URI + subscriptionName);
    }

}
