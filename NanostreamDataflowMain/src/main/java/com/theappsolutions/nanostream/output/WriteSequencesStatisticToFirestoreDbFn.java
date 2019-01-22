package com.theappsolutions.nanostream.output;

import com.google.cloud.firestore.WriteResult;
import com.theappsolutions.nanostream.probecalculation.SequenceCountAndTaxonomyData;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.Future;

/**
 * Writes {@link SequenceStatisticResult} data to Firestore Database
 */
public class WriteSequencesStatisticToFirestoreDbFn extends DoFn<Map<String, SequenceCountAndTaxonomyData>, String> {


    private Logger LOG = LoggerFactory.getLogger(WriteSequencesStatisticToFirestoreDbFn.class);

    private FirestoreService firebaseDatastoreService;
    private String firestoreDatabaseUrl;
    private String firestoreDestCollection;
    private String projectId;

    public WriteSequencesStatisticToFirestoreDbFn(String firestoreDatabaseUrl, String firestoreDestCollection, String projectId) {
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
        if (firebaseDatastoreService == null){
            return;
        }
        Map<String, SequenceCountAndTaxonomyData> sequenceIterableKV = c.element();
        SequenceInfoGenerator sequenceInfoGenerator = new SequenceInfoGenerator();

        SequenceStatisticResult sequenceStatisticResult = sequenceInfoGenerator.genereteSequnceInfo(sequenceIterableKV);
        Future<WriteResult> result = firebaseDatastoreService.writeObjectToFirestoreCollection(firestoreDestCollection,
                SequenceStatisticResult.SEQUENCE_STATISTIC_DOCUMENT_NAME, sequenceStatisticResult);
        c.output(result.toString());
    }
}
