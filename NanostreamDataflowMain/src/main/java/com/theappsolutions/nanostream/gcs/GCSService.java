package com.theappsolutions.nanostream.gcs;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;
import com.theappsolutions.nanostream.models.GCloudNotification;

/**
 * Provides access to {@link Storage} instance with convenient interface
 */
public class GCSService {

    private Storage storage;

    public GCSService(Storage storage) {
        this.storage = storage;
    }

    public static GCSService initialize(){
        return new GCSService(StorageOptions.getDefaultInstance().getService());
    }

    public Blob getBlobByGCloudNotificationData(GCloudNotification gCloudNotification){
        return storage.get(BlobId.of(gCloudNotification.getBucket(), gCloudNotification.getName()));
    }
}
