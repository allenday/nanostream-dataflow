package com.theappsolutions.nanostream.trasnform;

import com.google.gson.Gson;
import com.theappsolutions.nanostream.models.GCloudNotification;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets payload data from PubsubMessage and deserialize it to GCloudNotification
 */
public class DecodeNotificationJsonMessage extends DoFn<PubsubMessage, GCloudNotification> {
    private Gson gson;

    private Logger LOG = LoggerFactory.getLogger(DecodeNotificationJsonMessage.class);

    @DoFn.Setup
    public void setup() {
        gson = new Gson();
    }

    @DoFn.ProcessElement
    public void processElement(ProcessContext c) {
        PubsubMessage pubsubMessage = c.element();
        String data = new String(pubsubMessage.getPayload());
        LOG.info(data);
        GCloudNotification gcloudNotification = gson.fromJson(data, GCloudNotification.class);
        c.output(gcloudNotification);
    }
}
