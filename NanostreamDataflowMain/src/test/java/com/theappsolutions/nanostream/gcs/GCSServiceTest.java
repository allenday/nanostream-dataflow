package com.theappsolutions.nanostream.gcs;

import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.Storage;
import com.google.gson.Gson;
import com.theappsolutions.nanostream.models.GCloudNotification;
import com.theappsolutions.nanostream.gcs.GCSService;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.IOException;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Tests {@link GCSService} blob searching method
 */
public class GCSServiceTest {

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);
    private Gson gson;


    @Before
    public void setup() {
        gson = new Gson();
    }

    @Test
    public void testBlobSearchingByBlobId() {
        Storage mockStorage = Mockito.mock(Storage.class);

        GCSService gcsService = new GCSService(mockStorage);
        String data = null;
        try {
            // TODO: looks like an overkill. You just need GCloudNotification object with 2 fields - bucket and name
            data = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("pubSubTestMessage.txt"), UTF_8.name());
        } catch (IOException e) {
            e.printStackTrace();
        }
        GCloudNotification testGCloudNotification = gson.fromJson(data, GCloudNotification.class);
        gcsService.getBlobByGCloudNotificationData(testGCloudNotification);

        Mockito.verify(mockStorage).get(BlobId.of(testGCloudNotification.getBucket(), testGCloudNotification.getName()));
    }
}
