package com.google.allenday.nanostream.gcloud;

import com.google.allenday.nanostream.pubsub.GCSUtils;
import com.google.cloud.ReadChannel;
import com.google.cloud.storage.*;

import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

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

    public Blob getBlob(String bucketName, String blobName) throws StorageException {
        return storage.get(BlobId.of(bucketName, blobName));
    }


    public Blob saveToGcs(String bucketName, String blobName, byte[] content) {
        return storage.create(BlobInfo.newBuilder(bucketName, blobName).build(), content);
    }

    public ReadChannel getBlobReaderByGCloudNotificationData(String bucketName, String blobName) throws StorageException {
        return storage.reader(BlobId.of(bucketName, blobName));
    }

    public Blob composeBlobs(Iterable<BlobId> blobIds, BlobId headers, BlobId destBlob) throws StorageException {
        Storage.ComposeRequest composeRequest = Storage.ComposeRequest
                .newBuilder()
                .addSource(headers.getName())
                .addSource(StreamSupport.stream(blobIds.spliterator(), false)
                        .map(BlobId::getName).collect(Collectors.toList()))
                .setTarget(BlobInfo.newBuilder(destBlob).build())
                .build();
        return storage.compose(composeRequest);
    }


    public boolean isExists(BlobId blobId) {
        return Optional.ofNullable(storage.get(blobId)).map(Blob::exists).orElse(false);
    }
}
