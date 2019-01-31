package com.theappsolutions.nanostream.output;

public class WriteSequencesBodiesToFirestoreDbFn extends WriteDataToFirestoreDbFn<SequenceBodyResult> {

    public WriteSequencesBodiesToFirestoreDbFn(String firestoreDatabaseUrl, String firestoreDestCollection, String projectId) {
        super(firestoreDatabaseUrl, firestoreDestCollection, projectId);
    }
}
