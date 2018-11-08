package com.theappsolutions.nanostream.http;


import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.util.EntityUtils;

import java.io.IOException;

/**
 * {@link ResponseHandler} with http exceptions handling and response entity proceeding
 */
public class NanostreamResponseHandler implements ResponseHandler<String> {

    @Override
    public String handleResponse(HttpResponse response) throws IOException {
        int status = response.getStatusLine().getStatusCode();
        if (status >= 200 && status < 300) {
            HttpEntity responseEntity = response.getEntity();
            if (responseEntity != null){
                return EntityUtils.toString(responseEntity);
            } else {
                throw new IOException("Response entity is empty");
            }
        } else {
            throw new ClientProtocolException("Unexpected response status: " + status);
        }
    }
}
