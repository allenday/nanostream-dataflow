package com.google.allenday.nanostream.main.gcs;

import com.google.allenday.nanostream.gcs.GCSService;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.allenday.nanostream.pubsub.GCloudNotification;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.mockito.Mockito.*;

/**
 * Tests {@link GCSService} blob searching method
 */
public class GCSServiceTest {

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testBlobSearchingByBlobId() throws IOException {
        String testBucketName = "bucket_name";
        String testBlobName = "blob_name";

        Storage mockStorage = mock(Storage.class);

        GCSService gcsService = new GCSService(mockStorage);

        GCloudNotification mockGCloudNotification = mock(GCloudNotification.class);
        when(mockGCloudNotification.getBucket()).thenReturn(testBucketName);
        when(mockGCloudNotification.getName()).thenReturn(testBlobName);

        gcsService.getBlobByGCloudNotificationData(mockGCloudNotification.getBucket(), mockGCloudNotification.getName());

        verify(mockStorage).get(BlobId.of(mockGCloudNotification.getBucket(), mockGCloudNotification.getName()));
    }
}
