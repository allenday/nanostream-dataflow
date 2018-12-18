package com.theappsolutions.nanostream.injection;

import com.google.inject.AbstractModule;
import com.theappsolutions.nanostream.AlignPipelineOptions;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class BaseModule extends AbstractModule {

    protected String baseUrl;
    protected String bwaDb;
    protected String bwaEndpoint;
    protected String kalignEndpoint;

    BaseModule(String baseUrl, String bwaDb, String bwaEndpoint, String kalignEndpoint) {
        this.baseUrl = baseUrl;
        this.bwaDb = bwaDb;
        this.bwaEndpoint = bwaEndpoint;
        this.kalignEndpoint = kalignEndpoint;
    }

    public static class Builder{

        String baseUrl;
        String bwaDb;
        String bwaEndpoint;
        String kalignEndpoint;

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

        public BaseModule build() {
            return new BaseModule(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint);
        }
    }
}