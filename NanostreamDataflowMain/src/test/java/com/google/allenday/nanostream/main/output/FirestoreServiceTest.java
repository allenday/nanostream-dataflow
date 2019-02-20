package com.google.allenday.nanostream.main.output;

import com.google.allenday.nanostream.output.FirestoreService;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link FirestoreService} data writing
 */
public class FirestoreServiceTest {

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testDataWritingToFirestoreDB() {
        String testCollection= "testCollection";
        String testObject = "testObject";

        Firestore mockFirestore = mock(Firestore.class);
        CollectionReference mockCollectionReference = mock(CollectionReference.class);
        DocumentReference mockDocumentReference = mock(DocumentReference.class);

        when(mockFirestore.collection(testCollection)).thenReturn(mockCollectionReference);
        when(mockCollectionReference.document(anyString())).thenReturn(mockDocumentReference);

        FirestoreService gcsService = new FirestoreService(mockFirestore);
        gcsService.writeObjectToFirestoreCollection(testCollection, testObject);

        verify(mockFirestore, Mockito.times(1)).collection(testCollection);
        verify(mockCollectionReference, Mockito.times(1)).document(anyString());
        verify(mockDocumentReference, Mockito.times(1)).set(testObject);
    }
}
