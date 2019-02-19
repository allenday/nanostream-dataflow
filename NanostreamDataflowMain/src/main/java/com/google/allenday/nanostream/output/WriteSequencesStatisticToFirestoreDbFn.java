package com.google.allenday.nanostream.output;

public class WriteSequencesStatisticToFirestoreDbFn extends WriteDataToFirestoreDbFn<SequenceStatisticResult> {

    public WriteSequencesStatisticToFirestoreDbFn(String firestoreDestCollection, String projectId) {
        super(firestoreDestCollection, projectId);
    }
}
