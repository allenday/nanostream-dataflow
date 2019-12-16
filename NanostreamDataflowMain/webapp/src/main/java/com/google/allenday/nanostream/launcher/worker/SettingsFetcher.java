package com.google.allenday.nanostream.launcher.worker;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;

import static java.lang.String.format;
import static com.google.allenday.nanostream.launcher.worker.PipelineUtil.*;


@Service
public class SettingsFetcher {
    private final String project;

    public SettingsFetcher() {
        project = getProjectId();
    }

    public SettingsResponse invoke() {
        // TODO: get real data
        return new SettingsResponse(project);
    }
}
