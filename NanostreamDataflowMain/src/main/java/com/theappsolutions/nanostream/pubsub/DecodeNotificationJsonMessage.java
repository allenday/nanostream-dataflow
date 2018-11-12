package com.theappsolutions.nanostream.pubsub;

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

    @Setup
    public void setup() {
        gson = new Gson();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        PubsubMessage pubsubMessage = c.element();
        String data = new String(pubsubMessage.getPayload());
        LOG.info(data);
        // TODO: I believe it is a good idea to handle exception here
        GCloudNotification gcloudNotification = gson.fromJson(data, GCloudNotification.class);

        c.output(gcloudNotification);
    }
}
