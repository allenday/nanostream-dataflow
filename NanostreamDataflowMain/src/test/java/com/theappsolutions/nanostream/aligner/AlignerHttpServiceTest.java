package com.theappsolutions.nanostream.aligner;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.injection.TestModule;
import com.theappsolutions.nanostream.util.HttpHelper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.junit.Test;

import java.net.URI;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link AlignerHttpService}
 */
public class AlignerHttpServiceTest {

    @Test
    public void testAlignRequestExecution() throws Exception {
        String testDatabase = "test.db";
        String testServer = "http://test.com";

        Injector injector = Guice.createInjector(new TestModule(testDatabase, testServer));
        HttpHelper mockHttpHelper = injector.getInstance(HttpHelper.class);

        String testFastQData = "test_fastq_data";

        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        HttpEntity mockHttpEntity = mock(HttpEntity.class);
        HttpUriRequest mockHttpUriRequest = mock(HttpUriRequest.class);

        when(mockHttpHelper.createHttpClient())
                .thenReturn(mockClient);
        when(mockHttpHelper.buildStringContentBody(anyString()))
                .thenReturn(mock(ContentBody.class));
        when(mockHttpHelper.createMultipartHttpEntity(any(), any())).thenReturn(mockHttpEntity);
        when(mockHttpHelper.buildRequest(any(), any())).thenReturn(mockHttpUriRequest);
        when(mockHttpHelper.executeRequest(any(), any(), any())).thenReturn("");

        AlignerHttpService alignerHttpService = injector.getInstance(AlignerHttpService.class);
        alignerHttpService.generateAlignData(testFastQData);

        verify(mockHttpHelper, times(1)).createHttpClient();
        verify(mockHttpHelper, times(2)).buildStringContentBody(anyString());
        verify(mockHttpHelper, times(1)).createMultipartHttpEntity(any(), any());
        verify(mockHttpHelper, times(1)).buildRequest(eq(new URI(testServer)), eq(mockHttpEntity));
        verify(mockHttpHelper, times(1)).executeRequest(eq(mockClient), eq(mockHttpUriRequest), any());
    }
}