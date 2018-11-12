package com.theappsolutions.nanostream.gcs;

import com.google.cloud.storage.*;
import com.theappsolutions.nanostream.models.GCloudNotification;
import org.apache.beam.sdk.transforms.DoFn;

/**
 * Gets fastq filename from GCloudNotification and extracts data from this file
 */
public class GetDataFromFastQFile extends DoFn<GCloudNotification, String> {

    private GCSService gcsService;

    @Setup
    public void setup() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        GCloudNotification gCloudNotification = c.element();

        // TODO: I believe it's better to handle possible exception here
        Blob blob = gcsService.getBlobByGCloudNotificationData(gCloudNotification);

        if (blob != null && blob.exists()) {
            c.output(new String(blob.getContent()));
        }
    }
}
