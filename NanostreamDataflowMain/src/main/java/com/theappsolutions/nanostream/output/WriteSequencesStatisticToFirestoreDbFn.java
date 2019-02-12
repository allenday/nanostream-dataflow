package com.theappsolutions.nanostream.output;

public class WriteSequencesStatisticToFirestoreDbFn extends WriteDataToFirestoreDbFn<SequenceStatisticResult> {

    public WriteSequencesStatisticToFirestoreDbFn(String firestoreDestCollection, String projectId) {
        super(firestoreDestCollection, projectId);
    }
}
