package com.theappsolutions.nanostream.output;

import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.theappsolutions.nanostream.gcs.GCSService;
import com.theappsolutions.nanostream.models.GCloudNotification;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static org.mockito.Mockito.*;
import static org.mockito.Mockito.verify;

/**
 * Tests {@link FirebaseDatastoreService} data writing
 */
public class FirebaseDatastoreServiceTest {

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

        FirebaseDatastoreService gcsService = new FirebaseDatastoreService(mockFirestore);
        gcsService.writeObjectToFirestoreCollection(testCollection, testObject);

        verify(mockFirestore, Mockito.times(1)).collection(testCollection);
        verify(mockCollectionReference, Mockito.times(1)).document(anyString());
        verify(mockDocumentReference, Mockito.times(1)).set(testObject);
    }
}
