package com.google.allenday.nanostream.pubsub;

import com.google.allenday.nanostream.gcs.GCSNotification;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets payload data from PubsubMessage and deserialize it to GCSNotification
 */
public class DecodeNotificationJsonMessage extends DoFn<PubsubMessage, GCSNotification> {
    private Gson gson;

    private Logger LOG = LoggerFactory.getLogger(DecodeNotificationJsonMessage.class);

    @Setup
    public void setup() {
        gson = new Gson();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        PubsubMessage pubsubMessage = c.element();
        String data = new String(pubsubMessage.getPayload());

        try {
            GCSNotification gcloudNotification = gson.fromJson(data, GCSNotification.class);
            c.output(gcloudNotification);
        } catch (JsonSyntaxException e) {
            LOG.error(e.getMessage());
        }
    }
}
