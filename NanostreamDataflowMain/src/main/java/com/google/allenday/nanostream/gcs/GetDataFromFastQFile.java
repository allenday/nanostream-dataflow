package com.google.allenday.nanostream.gcs;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets fastq filename from GCloudNotification and extracts data from this file
 */
public class GetDataFromFastQFile extends DoFn<KV<String, String>, String> {

    private Logger LOG = LoggerFactory.getLogger(GetDataFromFastQFile.class);

    private GCSService gcsService;

    @Setup
    public void setup() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<String, String> data = c.element();

        LOG.info(data.toString());
        try {
            Blob blob = gcsService.getBlobByGCloudNotificationData(
                    data.getKey(), data.getValue()
            );
            if (blob != null && blob.exists()) {
                c.output(new String(blob.getContent()));
            }
        } catch (StorageException e) {
            LOG.error(e.getMessage());
        }
    }
}
