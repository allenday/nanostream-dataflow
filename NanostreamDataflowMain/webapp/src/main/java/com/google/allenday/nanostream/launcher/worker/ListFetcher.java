package com.google.allenday.nanostream.launcher.worker;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;
import static java.lang.String.format;

public class ListFetcher {
    private final String project;

    public ListFetcher(HttpServletRequest request) {
        project = getProjectId();
    }

    public String invoke() throws IOException {
        HttpURLConnection connection = sendListDataflowJobsRequest();

        return printOutput(connection);
    }

    private HttpURLConnection sendListDataflowJobsRequest() throws IOException {
        return sendRequest("GET", getUrl(), null);
    }

    private URL getUrl() throws MalformedURLException {
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/jobs", project));
    }
}
