package com.google.allenday.nanostream.gcs;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.cloud.storage.BlobId;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

/**
 *
 */
public class ParseGCloudNotification extends DoFn<GCSNotification, KV<GCSSourceData, FileWrapper>> {

    private FileUtils fileUtils;
    private GCSService gcsService;

    public ParseGCloudNotification(FileUtils fileUtils) {
        this.fileUtils = fileUtils;
    }

    @Setup
    public void setUp() {
        gcsService = GCSService.initialize(fileUtils);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        GCSNotification GCSNotification = c.element();

        GCSSourceData gcsSourceData = GCSSourceData.fromGCloudNotification(GCSNotification);
        BlobId blobId = BlobId.of(GCSNotification.getBucket(), GCSNotification.getName());
        c.output(KV.of(gcsSourceData,
                FileWrapper.fromBlobUri(gcsService.getUriFromBlob(blobId), fileUtils.getFilenameFromPath(blobId.getName()))));
    }
}
