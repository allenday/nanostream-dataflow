package com.google.allenday.nanostream.launcher.worker;

import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;
import static java.lang.String.format;

@Service
public class JobListFetcher {
    private final String project;

    public JobListFetcher() {
        project = getProjectId();
    }

    public String invoke() throws IOException {
        HttpURLConnection connection = sendListDataflowJobsRequest();

        return getRequestOutput(connection);
    }

    private HttpURLConnection sendListDataflowJobsRequest() throws IOException {
        return sendRequest("GET", getUrl(), null);
    }

    private URL getUrl() throws MalformedURLException {
        // see https://cloud.google.com/dataflow/docs/reference/rest/v1b3/projects.jobs/list
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/jobs", project));
    }
}
