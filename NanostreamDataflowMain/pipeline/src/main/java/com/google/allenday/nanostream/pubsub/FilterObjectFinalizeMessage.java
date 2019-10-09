package com.google.allenday.nanostream.pubsub;

import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Filters data from PubSub only for OBJECT_FINALIZE events
 */
public class FilterObjectFinalizeMessage extends DoFn<PubsubMessage, PubsubMessage> {

    private final static String EVENT_TYPE_KEY = "eventType";
    private final static String EVENT_TYPE_OBJECT_FINALIZE = "OBJECT_FINALIZE";

    private Logger LOG = LoggerFactory.getLogger(FilterObjectFinalizeMessage.class);

    @ProcessElement
    public void processElement(ProcessContext c) {
        PubsubMessage pubsubMessage = c.element();
        LOG.info(pubsubMessage.getAttributeMap().toString());
        if (EVENT_TYPE_OBJECT_FINALIZE.equals(pubsubMessage.getAttribute(EVENT_TYPE_KEY))) {
            c.output(pubsubMessage);
        }
    }
}