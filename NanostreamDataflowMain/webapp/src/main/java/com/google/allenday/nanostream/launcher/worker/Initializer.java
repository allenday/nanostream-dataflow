package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.config.GcpProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.getRequestOutput;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.sendRequest;
import static java.lang.String.format;

@Service
public class Initializer {

    private static final Logger logger = LoggerFactory.getLogger(Initializer.class);

    private final String project;

    @Autowired
    public Initializer(GcpProject gcpProject) {
        this.project = gcpProject.getId();
    }

    @PostConstruct
    public void init() throws IOException {
        logger.info("PostConstruct called");
        getFirebaseProjectInfo();
    }

    private void getFirebaseProjectInfo() throws IOException {
        HttpURLConnection connection = sendGetFirebaseProjectInfoRequest();
        String output = getRequestOutput(connection);
        logger.info(output);
    }

    private HttpURLConnection sendGetFirebaseProjectInfoRequest() throws IOException {
        String url = format("https://firebase.googleapis.com/v1beta1/projects/%s", project);
        logger.info(format("Send request get firebase project info: %s", url));
        return sendRequest("GET", new URL(url), null);
    }
}
