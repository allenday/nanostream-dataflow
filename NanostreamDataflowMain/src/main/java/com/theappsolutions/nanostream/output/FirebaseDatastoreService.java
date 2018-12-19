package com.theappsolutions.nanostream.output;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.storage.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;

import java.io.IOException;

/**
 * Provides access to {@link Storage} instance with convenient interface
 */
public class FirebaseDatastoreService {

    private Storage storage;

    private FirebaseDatastoreService() {
    }

    public static FirebaseDatastoreService initialize() throws IOException {

        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setDatabaseUrl("https://upwork-nano-stream.firebaseio.com")
                .build();
        FirebaseApp.initializeApp(firebaseOptions);

        return new FirebaseDatastoreService();
    }

    public Blob getBlobByGCloudNotificationData(String bucketName, String blobName) throws StorageException {
        return storage.get(BlobId.of(bucketName, blobName));
    }
}
