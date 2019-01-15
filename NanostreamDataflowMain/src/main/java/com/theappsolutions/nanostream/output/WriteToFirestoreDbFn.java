package com.theappsolutions.nanostream.output;

import com.google.cloud.firestore.WriteResult;
import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.concurrent.Future;

/**
 * Writes sequence data to Firestore Database
 * See <a href="https://cloud.google.com/storage/docs/object-change-notification">Firestore Database</a>
 * documentation
 */
public class WriteToFirestoreDbFn extends DoFn<Iterable<KV<String, Sequence>>, String> {

    private Logger LOG = LoggerFactory.getLogger(WriteToFirestoreDbFn.class);

    private FirebaseDatastoreService firebaseDatastoreService;
    private String firestoreDatabaseUrl;
    private DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm'Z'");
    private String firestoreDestCollection;
    private String projectId;

    public WriteToFirestoreDbFn(String firestoreDatabaseUrl, String firestoreDestCollection, String projectId) {
        this.firestoreDatabaseUrl = firestoreDatabaseUrl;
        this.firestoreDestCollection = firestoreDestCollection;
        this.projectId = projectId;
    }

    @Setup
    public void setup() {
        try {
            firebaseDatastoreService = FirebaseDatastoreService.initialize(projectId, firestoreDatabaseUrl);
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        if (firebaseDatastoreService == null){
            return;
        }
        Iterable<KV<String, Sequence>> sequenceIterableKV = c.element();
        SequenceInfoGenerator sequenceInfoGenerator = new SequenceInfoGenerator();
        /*SequenceInfoResult sequenceInfoResult = new SequenceInfoResult(new Date(), sequenceKV.getKey(),
                sequenceKV.getValue().toString(), new Random().nextFloat(), new Random().nextFloat());*/
        SequenceInfoResult sequenceInfoResult = sequenceInfoGenerator.genereteSequnceInfo(sequenceIterableKV);
        Future<WriteResult> result = firebaseDatastoreService.writeObjectToFirestoreCollection(firestoreDestCollection, "resultDocument", sequenceInfoResult);
        c.output(result.toString());
    }
}
