package com.theappsolutions.nanostream.util;

import javafx.util.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.util.Arrays;

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

    public  HttpEntity createMultipartHttpEntity(Pair<String, ContentBody>... parts) {
        MultipartEntityBuilder builder = MultipartEntityBuilder.create();
        Arrays.stream(parts).forEach(part -> builder.addPart(part.getKey(), part.getValue()));
        return builder.build();
    }

    public HttpUriRequest buildRequest(URI uri, HttpEntity httpEntity) {
        return RequestBuilder
                .post(uri)
                .setEntity(httpEntity)
                .build();
    }

    public <T> T executeRequest(HttpClient client, HttpUriRequest request, ResponseHandler<T> httpEntity)
            throws IOException {
        return client.execute(request, httpEntity);
    }
}
