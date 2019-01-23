package com.theappsolutions.nanostream.gcs;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import com.theappsolutions.nanostream.pubsub.GCloudNotification;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets fastq filename from GCloudNotification and extracts data from this file
 */
public class GetDataFromFastQFile extends DoFn<GCloudNotification, String> {

    private Logger LOG = LoggerFactory.getLogger(GetDataFromFastQFile.class);

    private GCSService gcsService;

    @Setup
    public void setup() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        GCloudNotification gCloudNotification = c.element();

        try {
            Blob blob = gcsService.getBlobByGCloudNotificationData(
                    gCloudNotification.getBucket(), gCloudNotification.getName()
            );
            if (blob != null && blob.exists()) {
                c.output(new String(blob.getContent()));
            }
        } catch (StorageException e) {
            LOG.error(e.getMessage());
        }
    }
}
