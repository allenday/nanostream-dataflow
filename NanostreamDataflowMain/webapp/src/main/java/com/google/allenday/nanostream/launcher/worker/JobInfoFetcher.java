package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.config.GcpProject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;
import static java.lang.String.format;

@Service
public class JobInfoFetcher {

    private final String project;

    @Autowired
    public JobInfoFetcher(GcpProject gcpProject) {
        project = gcpProject.getId();
    }

    public String invoke(String location, String jobId) throws IOException {
        HttpURLConnection connection = sendGetJobsInfoRequest(location, jobId);

        return getRequestOutput(connection);
    }

    private HttpURLConnection sendGetJobsInfoRequest(String location, String jobId) throws IOException {
        return sendRequest("GET", getUrl(location, jobId), null);
    }

    private URL getUrl(String location, String jobId) throws MalformedURLException {
        // see https://cloud.google.com/dataflow/docs/reference/rest/v1b3/projects.jobs/get
        return new URL(format(DATAFLOW_API_BASE_URI + "projects/%s/jobs/%s?view=%s&location=%s", project, jobId, "JOB_VIEW_ALL", location));
    }

}
