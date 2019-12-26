package com.google.allenday.nanostream.launcher.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;

import org.json.JSONObject;

import static java.lang.String.format;

@Service
public class Initializer {

    private static final Logger logger = LoggerFactory.getLogger(Initializer.class);

    private final String project;


    public Initializer() {
        this.project = getProjectId();
    }

    @PostConstruct
    public void init() throws IOException {
        logger.info("PostConstruct called");
//        addProjectToFirebase();
//        getFirebaseProjects();
        getFirebaseProjectInfo();
    }

    private void addProjectToFirebase() throws IOException {
        HttpURLConnection connection = sendAddProjectToFirebaseRequest();
        String output = getRequestOutput(connection);
        logger.info(output);
    }

    private void getFirebaseProjects() throws IOException {
        HttpURLConnection connection = sendGetFirebaseProjectsRequest();
        String output = getRequestOutput(connection);
        logger.info(output);
    }

    private void getFirebaseProjectInfo() throws IOException {
        HttpURLConnection connection = sendGetFirebaseProjectInfoRequest();
        String output = getRequestOutput(connection);
        logger.info(output);
    }

    private HttpURLConnection sendAddProjectToFirebaseRequest() throws IOException {
        String url = format("https://firebase.googleapis.com/v1beta1/projects/%s:addFirebase?alt=json", project);
        logger.info(format("Send request add project to firebase: %s", url));
        return sendRequest("POST", new URL(url), new JSONObject());
    }

    private HttpURLConnection sendGetFirebaseProjectsRequest() throws IOException {
        String url = "https://firebase.googleapis.com/v1beta1/projects";
        logger.info(format("Send request get firebase projects: %s", url));
        return sendRequest("GET", new URL(url), null);
    }

    private HttpURLConnection sendGetFirebaseProjectInfoRequest() throws IOException {
        String url = format("https://firebase.googleapis.com/v1beta1/projects/%s", project);
        logger.info(format("Send request get firebase project info: %s", url));
        return sendRequest("GET", new URL(url), null);
    }
}
