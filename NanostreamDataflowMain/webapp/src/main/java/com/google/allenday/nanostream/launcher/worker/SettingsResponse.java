package com.google.allenday.nanostream.launcher.worker;

public class SettingsResponse {

    private final String projectId;
    private String apiKey;
    private String messagingSenderId;
    private String authDomain;
    private String databaseURL;
    private String storageBucket;


    public SettingsResponse(String project, String fireBaseApiKey, String fireBaseMessagingSenderId) {
        this.projectId = project;
        this.apiKey = fireBaseApiKey;
        this.messagingSenderId = fireBaseMessagingSenderId;
        this.authDomain = String.format("%s.firebaseapp.com", projectId) ;
        this.databaseURL = String.format("https://%s.firebaseio.com", projectId);
        this.storageBucket = String.format("%s.appspot.com", projectId);
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getMessagingSenderId() {
        return messagingSenderId;
    }

    public void setMessagingSenderId(String messagingSenderId) {
        this.messagingSenderId = messagingSenderId;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getAuthDomain() {
        return authDomain;
    }

    public void setAuthDomain(String authDomain) {
        this.authDomain = authDomain;
    }

    public String getDatabaseURL() {
        return databaseURL;
    }

    public void setDatabaseURL(String databaseURL) {
        this.databaseURL = databaseURL;
    }

    public String getStorageBucket() {
        return storageBucket;
    }

    public void setStorageBucket(String storageBucket) {
        this.storageBucket = storageBucket;
    }
}
