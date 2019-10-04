package com.google.allenday.nanostream.gcs;

import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;

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

    public Blob getBlobByGCloudNotificationData(String bucketName, String blobName) throws StorageException {
        return storage.get(BlobId.of(bucketName, blobName));
    }

    public ReadChannel getBlobReaderByGCloudNotificationData(String bucketName, String blobName) throws StorageException {
        return storage.reader(BlobId.of(bucketName, blobName));
    }
}
