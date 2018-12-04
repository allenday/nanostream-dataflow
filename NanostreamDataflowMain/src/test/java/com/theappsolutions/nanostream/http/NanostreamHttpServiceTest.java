package com.theappsolutions.nanostream.http;

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
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

/**
 * Tests for {@link com.theappsolutions.nanostream.http.NanostreamHttpService}
 */
public class NanostreamHttpServiceTest {

    @Test
    public void testAlignRequestExecution() throws Exception {
        String testDatabase = "test.db";
        String testBaseUrl = "http://test.com";
        String testEndpoint = "api/endpoint";

        String testKey = "key";
        String testData = "test_fastq_data";
        Map<String, String> testContent = new HashMap<String, String>(){{
            put(testKey, testData);
        }};

        Injector injector = Guice.createInjector(
                new TestModule.Builder()
                        .setBaseUrl(testBaseUrl)
                        .setBwaDb(testDatabase)
                        .setBwaEndpoint(testEndpoint)
                        .build()
        );

        HttpHelper mockHttpHelper = injector.getInstance(HttpHelper.class);

        CloseableHttpClient mockClient = mock(CloseableHttpClient.class);
        HttpEntity mockHttpEntity = mock(HttpEntity.class);
        HttpUriRequest mockHttpUriRequest = mock(HttpUriRequest.class);

        when(mockHttpHelper.createHttpClient())
                .thenReturn(mockClient);
        when(mockHttpHelper.buildStringContentBody(anyString()))
                .thenReturn(mock(ContentBody.class));
        when(mockHttpHelper.createMultipartHttpEntity(any()))
                .thenReturn(mockHttpEntity);
        when(mockHttpHelper.buildRequest(any(), any()))
                .thenReturn(mockHttpUriRequest);
        when(mockHttpHelper.executeRequest(any(), any(), any()))
                .thenReturn("");

        NanostreamHttpService alignerHttpService = new NanostreamHttpService(mockHttpHelper, testBaseUrl);
        alignerHttpService.generateAlignData(testEndpoint, testContent);

        verify(mockHttpHelper, times(1)).createHttpClient();
        verify(mockHttpHelper, times(testContent.size())).buildStringContentBody(anyString());
        verify(mockHttpHelper, times(1)).buildStringContentBody(testData);
        verify(mockHttpHelper, times(1)).createMultipartHttpEntity(any());
        verify(mockHttpHelper, times(1)).buildRequest(eq(new URI(testBaseUrl+testEndpoint)), eq(mockHttpEntity));
        verify(mockHttpHelper, times(1)).executeRequest(eq(mockClient), eq(mockHttpUriRequest), any());
    }
}