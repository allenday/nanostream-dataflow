package com.google.allenday.nanostream.util;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.ServiceUnavailableRetryStrategy;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.protocol.HttpContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

/**
 * Provides set of methods for working with Apache HTTP client
 */
public class HttpHelper implements Serializable {

    private static int TOO_MANY_REQUESTS_RESPONSE_CODE = 429;
    private static int RETRY_MAX_INTERVAL = 30000;

    private final static long DEFAULT_REQUEST_TIMEOUT = TimeUnit.SECONDS.toMillis(10);
    private final static long DEFAULT_REQUEST_RETRY_COUNT = 5;

    private Logger LOG = LoggerFactory.getLogger(HttpHelper.class);

    public CloseableHttpClient createHttpClient() {
        RequestConfig requestConfig = RequestConfig.custom()
                .setCookieSpec(CookieSpecs.STANDARD)
                .build();

        return HttpClients.custom().setDefaultRequestConfig(requestConfig)
                .setServiceUnavailableRetryStrategy(new ServiceUnavailableRetryStrategy() {

                    @Override
                    public boolean retryRequest(
                            final HttpResponse response, final int executionCount, final HttpContext context) {
                        int statusCode = response.getStatusLine().getStatusCode();
                        if (statusCode == TOO_MANY_REQUESTS_RESPONSE_CODE) {

                            LOG.info("Status: " + statusCode + ".Need to Retry. Execution: " + executionCount);
                        }
                        return statusCode == TOO_MANY_REQUESTS_RESPONSE_CODE && executionCount < DEFAULT_REQUEST_RETRY_COUNT;
                    }

                    @Override
                    public long getRetryInterval() {
                        long interval = ThreadLocalRandom.current().nextLong(RETRY_MAX_INTERVAL);
                        LOG.info("Retry interval:  " + interval);
                        return interval;
                    }
                })
                .build();

    }

    public ContentBody buildStringContentBody(String data) {
        return new StringBody(data, ContentType.DEFAULT_TEXT);
    }

    public HttpEntity createMultipartHttpEntity(Map<String, ContentBody> preparedContent) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        preparedContent.forEach(builder::addPart);
        return builder.build();
    }

    public URI buildURI(String baseUrl, Map<String, String> params) throws URISyntaxException {
        URIBuilder uriBuilder = new URIBuilder(baseUrl);
        params.forEach(uriBuilder::addParameter);
        return uriBuilder.build();
    }

    public HttpUriRequest buildRequest(URI uri) {
        return buildRequest(uri, null);
    }

    public HttpUriRequest buildRequest(URI uri, HttpEntity httpEntity) {
        RequestBuilder requestBuilder =  RequestBuilder
                .post(uri);
        if (httpEntity != null) {
            requestBuilder = requestBuilder.setEntity(httpEntity);
        }
        return requestBuilder.build();
    }

    public <T> T executeRequest(HttpClient client, HttpUriRequest request, ResponseHandler<T> httpEntity)
            throws IOException {
        return client.execute(request, httpEntity);
    }
}
