package com.google.allenday.nanostream.db;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.FirestoreOptions;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.storage.StorageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * Provides access to {@link Firestore} instance with convenient interface
 */
public class FirestoreService {
    private final static String FIREBASE_APP_NAME = "NanostreamFirebaseApp";
    private final Firestore firestore;
    private Logger LOG = LoggerFactory.getLogger(FirestoreService.class);

    public FirestoreService(Firestore firestore) {
        this.firestore = firestore;
    }

    public static FirestoreService initialize(String projectId) throws IOException {

        FirestoreOptions firestoreOptions =
                FirestoreOptions.getDefaultInstance().toBuilder()
                        .setCredentials(GoogleCredentials.getApplicationDefault())
                        .setProjectId(projectId)
                        .build();
        return new FirestoreService(firestoreOptions.getService());
    }

    public ApiFuture<WriteResult> writeObjectToFirestoreCollection(String firestoreCollection, Object objectToWrite) throws StorageException {
        return writeObjectToFirestoreCollection(firestoreCollection, UUID.randomUUID().toString(), objectToWrite);
    }

    public ApiFuture<WriteResult> writeObjectToFirestoreCollection(String firestoreCollection, String documentId, Object objectToWrite) throws StorageException {
        LOG.info(String.format("firestoreCollection %s, documentId %s", firestoreCollection, documentId));
        return firestore.collection(firestoreCollection).document(documentId)
                .set(objectToWrite);
    }

    public <T> T getObjectByDocumentId(String collectionName, String documentId, Class<T> objectClass)
            throws ExecutionException, InterruptedException {
        return firestore.collection(collectionName).document(documentId).get().get().toObject(objectClass);
    }
}
