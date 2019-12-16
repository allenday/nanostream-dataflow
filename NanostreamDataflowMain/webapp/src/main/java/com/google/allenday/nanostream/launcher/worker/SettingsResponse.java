package com.google.allenday.nanostream.launcher.worker;

public class SettingsResponse {

    private final String projectId;
    private String apiKey = "AIzaSyA5_c4nxV9sEew5Uvxc-zvoZi2ofg9sXfk";
    private String messagingSenderId = "500629989505-zvoZi2ofg9sXfk";
    private String authDomain;
    private String databaseURL;
    private String storageBucket;


    public SettingsResponse(String project) {
        projectId = project;
        authDomain = String.format("%s.firebaseapp.com", projectId) ;
        databaseURL = String.format("https://%s.firebaseio.com", projectId);
        storageBucket = String.format("%s.appspot.com", projectId);
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
