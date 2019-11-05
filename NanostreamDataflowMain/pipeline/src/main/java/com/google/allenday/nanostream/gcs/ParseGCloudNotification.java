package com.google.allenday.nanostream.gcs;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.allenday.genomics.core.model.GeneExampleMetaData;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.pubsub.GCloudNotification;
import com.google.cloud.storage.BlobId;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.Collections;
import java.util.List;

/**
 *
 */
public class ParseGCloudNotification extends DoFn<GCloudNotification, KV<GeneExampleMetaData, List<FileWrapper>>> {

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
        GCloudNotification gCloudNotification = c.element();

        GCSSourceData gcsSourceData = GCSSourceData.fromGCloudNotification(gCloudNotification);
        BlobId blobId = BlobId.of(gCloudNotification.getBucket(), gCloudNotification.getName());
        c.output(KV.of(GeneExampleMetaData.createSingleEndUnique(gcsSourceData.toJsonString()),
                Collections.singletonList(FileWrapper.fromBlobUri(gcsService.getUriFromBlob(blobId), fileUtils.getFilenameFromPath(blobId.getName())))));
    }
}
