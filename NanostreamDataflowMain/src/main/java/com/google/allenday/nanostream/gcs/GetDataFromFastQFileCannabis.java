package com.google.allenday.nanostream.gcs;

import com.google.allenday.nanostream.cannabis_source.CannabisSourceFileMetaData;
import com.google.allenday.nanostream.util.FastQUtils;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 *
 */
public class GetDataFromFastQFileCannabis extends DoFn<CannabisSourceFileMetaData, KV<CannabisSourceFileMetaData, String>> {


    private Logger LOG = LoggerFactory.getLogger(GetDataFromFastQFileCannabis.class);

    private GCSService gcsService;

    @Setup
    public void setup() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        CannabisSourceFileMetaData data = c.element();
        LOG.info(data.toString());
        try {
            FastQUtils.readFastqBlob(gcsService.getBlobReaderByGCloudNotificationData("cannabis-3k-test", data.generateGCSBlobName()),
                    fastq -> c.output(KV.of(data, fastq)));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }
}
