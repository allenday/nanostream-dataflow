package com.google.allenday.nanostream.pubsub;

import com.google.cloud.storage.BlobId;

/**
 *
 */
public class GCSUtils {

    public static String getGCSPathFromBlobId(BlobId blobId) {
        return String.format("gs://%s/%s", blobId.getBucket(), blobId.getName());
    }
}
