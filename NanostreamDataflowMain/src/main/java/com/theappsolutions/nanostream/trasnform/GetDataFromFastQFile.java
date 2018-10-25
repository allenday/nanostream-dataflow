package com.theappsolutions.nanostream.trasnform;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.theappsolutions.nanostream.models.GCloudNotification;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Gets fastq filename from GCloudNotification and extracts data from this file
 */
public class GetDataFromFastQFile extends DoFn<GCloudNotification, String> {

    private Storage storage;

    @DoFn.Setup
    public void setup() {
        storage = StorageOptions.getDefaultInstance().getService();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        GCloudNotification gCloudNotification = c.element();

        Blob blob = storage.get(BlobId.of(gCloudNotification.getBucket(), gCloudNotification.getName()));

        if (blob != null && blob.exists()) {
            c.output("\'" +gCloudNotification.getName()+"\' content -> " + new String(blob.getContent())+"\n");
        }
    }
}
