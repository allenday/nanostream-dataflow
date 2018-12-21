package com.theappsolutions.nanostream.output;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.cloud.storage.*;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;

import java.io.IOException;
import java.util.UUID;

/**
 * Provides access to {@link Firestore} instance with convenient interface
 */
public class FirebaseDatastoreService {

    private final static String FIREBASE_APP_NAME = "NanostreamFirebaseApp";
    private final Firestore firestore;

    public FirebaseDatastoreService(Firestore firestore) {
        this.firestore = firestore;
    }

    public static FirebaseDatastoreService initialize(String projectId, String databaseUrl) throws IOException {

        FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                .setCredentials(GoogleCredentials.getApplicationDefault())
                .setDatabaseUrl(databaseUrl)
                .setProjectId(projectId)
                .build();

        if (FirebaseApp.getApps().stream().noneMatch(firebaseApp -> firebaseApp.getName().equals(FIREBASE_APP_NAME))){
            try {
                FirebaseApp.initializeApp(firebaseOptions, FIREBASE_APP_NAME);
            } catch (RuntimeException ignored){
            }
        }
        return new FirebaseDatastoreService(FirestoreClient.getFirestore(FirebaseApp.getInstance(FIREBASE_APP_NAME)));
    }

    public ApiFuture<WriteResult> writeObjectToFirestoreCollection(String firestoreCollection, Object objectToWrite) throws StorageException {
        return firestore.collection(firestoreCollection).document(UUID.randomUUID().toString())
                .set(objectToWrite);
    }
}
