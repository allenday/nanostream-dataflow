package com.google.allenday.nanostream.http;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * {@link ResponseHandler} with http exceptions handling and response entity proceeding
 */
public class NanostreamResponseHandler implements ResponseHandler<String> {

    private Logger LOG = LoggerFactory.getLogger(NanostreamResponseHandler.class);

    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        StringBuilder logMsg = new StringBuilder();
        logMsg.append(String.format("Status: %d", status));
        if (status >= 200 && status < 300) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null){
                String respEntityString = EntityUtils.toString(responseEntity);
                logMsg.append(String.format(", response length: %d", respEntityString.length()));
                log(logMsg.toString());
                return respEntityString;
            } else {
                log(logMsg.toString());
                throw new IOException("Response entity is empty");
            }
        } else {
            log(logMsg.toString());
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    }

    private void log(String logMsg){
        LOG.info(logMsg);
    }
}
