package com.google.allenday.nanostream.launcher.worker;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;
import static java.lang.String.format;

public class JobInfoFetcher {

    private final String project;
    private final String location;
    private final String jobId;

    public JobInfoFetcher(HttpServletRequest request) {
        project = getProjectId();
        jobId = request.getParameter("jobId");
        location = request.getParameter("location");
    }

    public String invoke() throws IOException {
        HttpURLConnection connection = sendGetJobsInfoRequest();

        return getRequestOutput(connection);
    }

    private HttpURLConnection sendGetJobsInfoRequest() throws IOException {
        return sendRequest("GET", getUrl(), null);
    }

    private URL getUrl() throws MalformedURLException {
        // see https://cloud.google.com/dataflow/docs/reference/rest/v1b3/projects.jobs/get
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/jobs/%s?view=%s&location=%s", project, jobId, "JOB_VIEW_ALL", location));
    }

}
