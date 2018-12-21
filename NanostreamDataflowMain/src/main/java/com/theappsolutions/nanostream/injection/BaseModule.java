package com.theappsolutions.nanostream.injection;

import com.google.inject.AbstractModule;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class BaseModule extends AbstractModule {

    protected String baseUrl;
    protected String bwaDb;
    protected String bwaEndpoint;
    protected String kalignEndpoint;
    protected String firestoreDatabaseUrl;
    protected String firestoreDestCollection;
    protected String projectId;

    public BaseModule(String baseUrl, String bwaDb, String bwaEndpoint, String kalignEndpoint,
                      String firestoreDatabaseUrl, String firestoreDestCollection, String projectId) {
        this.baseUrl = baseUrl;
        this.bwaDb = bwaDb;
        this.bwaEndpoint = bwaEndpoint;
        this.kalignEndpoint = kalignEndpoint;
        this.firestoreDatabaseUrl = firestoreDatabaseUrl;
        this.firestoreDestCollection = firestoreDestCollection;
        this.projectId = projectId;
    }

    public static class Builder{

        String baseUrl;
        String bwaDb;
        String bwaEndpoint;
        String kalignEndpoint;
        String firestoreDatabaseUrl;
        String firestoreDestCollection;
        String projectId;

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setBwaDb(String bwaDb) {
            this.bwaDb = bwaDb;
            return this;
        }

        public Builder setBwaEndpoint(String bwaEndpoint) {
            this.bwaEndpoint = bwaEndpoint;
            return this;
        }

        public Builder setKalignEndpoint(String kalignEndpoint) {
            this.kalignEndpoint = kalignEndpoint;
            return this;
        }

        public Builder setFirestoreDatabaseUrl(String firestoreDatabaseUrl) {
            this.firestoreDatabaseUrl = firestoreDatabaseUrl;
            return this;
        }

        public Builder setFirestoreDestCollection(String firestoreDestCollection) {
            this.firestoreDestCollection = firestoreDestCollection;
            return this;
        }

        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public BaseModule build() {
            return new BaseModule(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint,
                    firestoreDatabaseUrl, firestoreDestCollection, projectId);
        }
    }
}