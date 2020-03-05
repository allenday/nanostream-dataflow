package com.google.allenday.nanostream.fastq;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.allenday.nanostream.gcs.GCSSourceData;
import com.google.allenday.nanostream.util.FastQUtils;
import com.google.cloud.storage.BlobId;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Gets fastq filename from GCSNotification and extracts data from this file
 */
public class GetDataFromFastQFileFn extends DoFn<KV<GCSSourceData, FileWrapper>, KV<GCSSourceData, String>> {

    private Logger LOG = LoggerFactory.getLogger(GetDataFromFastQFileFn.class);

    private GCSService gcsService;
    private FileUtils fileUtils;


    public GetDataFromFastQFileFn(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    @Setup
    public void setup() {
        gcsService = GCSService.initialize(fileUtils);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<GCSSourceData, FileWrapper> data = c.element();

        LOG.info(data.toString());
        try {
            GCSSourceData gcsSourceData = data.getKey();
            FileWrapper fileWrapper = data.getValue();
            BlobId blobId = gcsService.getBlobIdFromUri(fileWrapper.getBlobUri());
            if (gcsSourceData != null) {
                FastQUtils.readFastqBlob(gcsService.getBlobReaderByGCloudNotificationData(blobId.getBucket(), blobId.getName()),
                        fastq -> c.output(KV.of(gcsSourceData, fastq)));
            }
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }
}
