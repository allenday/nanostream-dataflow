package com.google.allenday.nanostream.output;

import com.google.cloud.firestore.WriteResult;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.Future;

/**
 * Writes data to Firestore Database
 */
public class WriteDataToFirestoreDbFn<T> extends DoFn<KV<String, T>, String> {


    private Logger LOG = LoggerFactory.getLogger(WriteDataToFirestoreDbFn.class);

    private FirestoreService firebaseDatastoreService;
    private String firestoreDestCollection;
    private String projectId;

    public WriteDataToFirestoreDbFn(String firestoreDestCollection, String projectId) {
        this.firestoreDestCollection = firestoreDestCollection;
        this.projectId = projectId;
    }

    @Setup
    public void setup() {
        try {
            firebaseDatastoreService = FirestoreService.initialize(projectId);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        if (firebaseDatastoreService == null) {
            return;
        }
        Future<WriteResult> result = firebaseDatastoreService.writeObjectToFirestoreCollection(firestoreDestCollection,
                c.element().getKey(), c.element().getValue());
        c.output(result.toString());
    }
}
