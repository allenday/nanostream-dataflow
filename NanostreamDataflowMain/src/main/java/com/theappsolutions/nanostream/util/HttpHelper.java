package com.theappsolutions.nanostream.util;

import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Map;

/**
 * Provides set of methods for working with Apache HTTP client
 */
public class HttpHelper implements Serializable {

    public CloseableHttpClient createHttpClient() {
        return HttpClients.createDefault();
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
