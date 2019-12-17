package com.google.allenday.nanostream.launcher.worker;

import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;
import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;


@Service
public class SettingsFetcher {
    private static final Logger logger = LoggerFactory.getLogger(SettingsFetcher.class);

    private final String project;
    private final Environment env;

    @Autowired
    public SettingsFetcher(Environment env) {
        this.env = env;
        this.project = getProjectId();
    }

    public SettingsResponse invoke() throws IOException {
        String fireBaseApiKey = env.getProperty("firebase.api.key");
        String fireBaseMessagingSenderId= env.getProperty("firebase.api.messagingSenderId");

        return new SettingsResponse(project, fireBaseApiKey, fireBaseMessagingSenderId);
    }
}
