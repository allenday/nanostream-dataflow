package com.theappsolutions.nanostream.aligner;

import com.theappsolutions.nanostream.http.NanostreamResponseHandler;
import com.theappsolutions.nanostream.util.HttpHelper;
import javafx.util.Pair;
import org.apache.http.HttpEntity;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.impl.client.CloseableHttpClient;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Service that provides access to remote server with alignment functionality
 */
public class AlignerHttpService implements Serializable {

    private final static String DATABASE_MULTIPART_KEY = "database";
    private final static String FASTQ_DATA_MULTIPART_KEY = "fastq";

    private HttpHelper httpHelper;
    private String alignmentDatabase;
    private String alignmentServer;

    /**
     * Creates instance of {@link AlignerHttpService}
     *
     * @param httpHelper        instance of {@link HttpHelper}
     * @param alignmentDatabase name of database with predefined result examples
     * @param alignmentServer   address of http alignment server
     */
    public AlignerHttpService(HttpHelper httpHelper, String alignmentDatabase, String alignmentServer) {
        this.httpHelper = httpHelper;
        this.alignmentDatabase = alignmentDatabase;
        this.alignmentServer = alignmentServer;
    }

    /**
     * Makes call to http alignment server
     *
     * @param fastQData prepared fast data
     * @return response body with aligned result
     */
    public String generateAlignData(String fastQData) throws URISyntaxException, IOException {
        CloseableHttpClient httpClient = httpHelper.createHttpClient();

        ContentBody fastqBody = httpHelper.buildStringContentBody(fastQData);
        ContentBody dbBody = httpHelper.buildStringContentBody(alignmentDatabase);

        HttpEntity entity = httpHelper.createMultipartHttpEntity(
                new Pair<>(DATABASE_MULTIPART_KEY, dbBody),
                new Pair<>(FASTQ_DATA_MULTIPART_KEY, fastqBody)
        );

        HttpUriRequest request = httpHelper.buildRequest(new URI(alignmentServer), entity);

        @Nonnull
        String responseBody = httpHelper.executeRequest(httpClient, request, new NanostreamResponseHandler());
        httpClient.close();

        return responseBody;
    }
}
