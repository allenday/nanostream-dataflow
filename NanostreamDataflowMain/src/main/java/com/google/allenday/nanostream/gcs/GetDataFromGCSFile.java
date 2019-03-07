package com.google.allenday.nanostream.gcs;

import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.StorageException;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets GCS filename from GCloudNotification and extracts data from this file
 */
public class GetDataFromGCSFile extends DoFn<KV<GCSSourceData, String>, KV<GCSSourceData, byte[]>> {

    private Logger LOG = LoggerFactory.getLogger(GetDataFromGCSFile.class);

    private GCSService gcsService;

    @Setup
    public void setup() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<GCSSourceData, String> data = c.element();

        LOG.info(data.toString());
        try {
            GCSSourceData gcsSourceData = data.getKey();
            if (gcsSourceData != null) {
                Blob blob = gcsService.getBlobByGCloudNotificationData(
                        gcsSourceData.getBucket(), data.getValue()
                );
                if (blob != null && blob.exists()) {
                    c.output(KV.of(gcsSourceData, blob.getContent()));
                }
            }
        } catch (StorageException e) {
            LOG.error(e.getMessage());
        }
    }
}
