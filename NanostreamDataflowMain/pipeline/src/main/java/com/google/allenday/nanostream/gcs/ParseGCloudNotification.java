package com.google.allenday.nanostream.gcs;

import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.pubsub.GCloudNotification;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

/**
 *
 */
public class ParseGCloudNotification extends DoFn<GCloudNotification, KV<GCSSourceData, String>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        GCloudNotification gCloudNotification = c.element();
        c.output(KV.of(GCSSourceData.fromGCloudNotification(gCloudNotification), gCloudNotification.getName()));
    }
}
