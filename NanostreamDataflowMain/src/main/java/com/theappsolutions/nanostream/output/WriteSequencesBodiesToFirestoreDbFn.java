package com.theappsolutions.nanostream.output;

public class WriteSequencesBodiesToFirestoreDbFn extends WriteDataToFirestoreDbFn<SequenceBodyResult> {

    public WriteSequencesBodiesToFirestoreDbFn(String firestoreDestCollection, String projectId) {
        super(firestoreDestCollection, projectId);
    }
}
