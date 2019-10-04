package com.google.allenday.nanostream.gcs;

import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.util.FastQUtils;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Gets fastq filename from GCloudNotification and extracts data from this file
 */
public class GetDataFromFastQFile extends DoFn<KV<GCSSourceData, String>, KV<GCSSourceData, String>> {


    private Logger LOG = LoggerFactory.getLogger(GetDataFromFastQFile.class);

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
            String blobName = data.getValue();
            if (gcsSourceData != null) {
                FastQUtils.readFastqBlob(gcsService.getBlobReaderByGCloudNotificationData(gcsSourceData.getBucket(), blobName),
                        fastq -> c.output(KV.of(gcsSourceData, fastq)));
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }
}
