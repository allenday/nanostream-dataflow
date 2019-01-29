package com.theappsolutions.nanostream.output;

public class WriteSequencesStatisticToFirestoreDbFn extends WriteDataToFirestoreDbFn<SequenceStatisticResult> {

    public WriteSequencesStatisticToFirestoreDbFn(String firestoreDatabaseUrl, String firestoreDestCollection, String projectId) {
        super(firestoreDatabaseUrl, firestoreDestCollection, projectId);
    }
}
