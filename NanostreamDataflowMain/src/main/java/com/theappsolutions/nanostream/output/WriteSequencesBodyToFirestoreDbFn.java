package com.theappsolutions.nanostream.output;

import com.google.cloud.firestore.WriteResult;
import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.Future;

/**
 *
 */
public class WriteSequencesBodyToFirestoreDbFn extends DoFn<KV<String, Sequence>, String> {

    private Logger LOG = LoggerFactory.getLogger(WriteSequencesBodyToFirestoreDbFn.class);

    private final static String SEQUENCE_BODY_KEY = "sequence_body";

    private FirestoreService firebaseDatastoreService;
    private String firestoreDatabaseUrl;
    private String firestoreDestCollection;
    private String projectId;

    public WriteSequencesBodyToFirestoreDbFn(String firestoreDatabaseUrl, String firestoreDestCollection, String projectId) {
        this.firestoreDatabaseUrl = firestoreDatabaseUrl;
        this.firestoreDestCollection = firestoreDestCollection;
        this.projectId = projectId;
    }

    @Setup
    public void setup() {
        try {
            firebaseDatastoreService = FirestoreService.initialize(projectId, firestoreDatabaseUrl);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        if (firebaseDatastoreService == null) {
            return;
        }
        KV<String, Sequence> sequenceKV = c.element();
        Future<WriteResult> result = firebaseDatastoreService.writeObjectToFirestoreCollection(firestoreDestCollection, sequenceKV.getKey(), new HashMap<String, String>() {{
            put(SEQUENCE_BODY_KEY, sequenceKV.getValue().toString());
        }});
        c.output(result.toString());
    }
}
