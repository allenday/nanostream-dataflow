package com.google.allenday.nanostream.http;

import com.google.allenday.nanostream.util.HttpHelper;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.CloseableHttpClient;

import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.AbstractMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Service that provides access to remote Nanostream server APIs
 */
public class NanostreamHttpService implements Serializable {

    private HttpHelper httpHelper;
    private String url;

    /**
     * Creates instance of {@link NanostreamHttpService}
     *
     * @param httpHelper instance of {@link HttpHelper}
     * @param url        URL address of remote server
     */
    public NanostreamHttpService(HttpHelper httpHelper, String url) {
        this.httpHelper = httpHelper;
        this.url = url;
    }

    /**
     * Makes call to http server with provided content
     *
     * @param content HttpEntity content
     * @return response body with aligned result
     */
    public String generateAlignData(String endpoint, Map<String, String> content) throws URISyntaxException, IOException {
        CloseableHttpClient httpClient = httpHelper.createHttpClient();

        Map<String, ContentBody> preparedContent = content.entrySet().stream()
                .map((entry) -> new AbstractMap.SimpleEntry<>(entry.getKey(),
                        httpHelper.buildStringContentBody(entry.getValue())))
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));

        HttpEntity entity = httpHelper.createMultipartHttpEntity(preparedContent);

        HttpUriRequest request = httpHelper.buildRequest(new URI(url.concat(endpoint)), entity);

        String responseBody = httpHelper.executeRequest(httpClient, request, new NanostreamResponseHandler());
        httpClient.close();

        return responseBody;
    }
}
