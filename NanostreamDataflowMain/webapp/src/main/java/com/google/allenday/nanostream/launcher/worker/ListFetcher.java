package com.google.allenday.nanostream.launcher.worker;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;
import static java.lang.String.format;

public class ListFetcher {
    private final HttpServletResponse response;
    private final String project;

    public ListFetcher(HttpServletRequest request, HttpServletResponse response) {
        this.response = response;
        project = getProjectId();
    }

    public void invoke() throws IOException {
        HttpURLConnection connection = sendListDataflowJobsRequest();

        printOutput(connection, response);
    }

    private HttpURLConnection sendListDataflowJobsRequest() throws IOException {
        return sendRequest("GET", getUrl(), null);
    }

    private URL getUrl() throws MalformedURLException {
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/jobs", project));
    }
}
