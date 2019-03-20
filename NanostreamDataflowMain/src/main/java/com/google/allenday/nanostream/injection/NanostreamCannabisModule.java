package com.google.allenday.nanostream.injection;

import com.google.allenday.nanostream.NanostreamCannabisPipelineOptions;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

//TODO

/**
 *
 */
public class NanostreamCannabisModule extends AbstractModule {

    protected String projectId;
    protected String servicesUrl;
    protected String bwaEndpoint;
    protected String bwaDB;
    protected String bwaArguments;

    public NanostreamCannabisModule(Builder builder) {
        this.projectId = builder.projectId;
        this.servicesUrl = builder.servicesUrl;
        this.bwaEndpoint = builder.bwaEndpoint;
        this.bwaDB = builder.bwaDB;
        this.bwaArguments = builder.bwaArguments;
    }

    public static class Builder {

        protected String projectId;
        protected String servicesUrl;
        protected String bwaEndpoint;
        protected String bwaDB;
        protected String bwaArguments;


        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder setServicesUrl(String servicesUrl) {
            this.servicesUrl = servicesUrl;
            return this;
        }

        public Builder setBwaEndpoint(String bwaEndpoint) {
            this.bwaEndpoint = bwaEndpoint;
            return this;
        }

        public Builder setBwaDB(String bwaDB) {
            this.bwaDB = bwaDB;
            return this;
        }

        public Builder setBwaArguments(String bwaArguments) {
            this.bwaArguments = bwaArguments;
            return this;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getServicesUrl() {
            return servicesUrl;
        }

        public String getBwaEndpoint() {
            return bwaEndpoint;
        }

        public String getBwaDB() {
            return bwaDB;
        }



        public NanostreamCannabisModule buildFromOptions(NanostreamCannabisPipelineOptions nanostreamPipelineOptions) {
            setProjectId(nanostreamPipelineOptions.getProject());
            setServicesUrl(nanostreamPipelineOptions.getServicesUrl());
            setBwaEndpoint(nanostreamPipelineOptions.getBwaEndpoint());
            setBwaDB(nanostreamPipelineOptions.getBwaDatabase());
            setBwaArguments(nanostreamPipelineOptions.getBwaArguments());
            return build();
        }

        public NanostreamCannabisModule build() {
            return new NanostreamCannabisModule(this);
        }

    }

    @Provides
    @Singleton
    public EntityNamer provideEntityNamer() {
        return EntityNamer.initialize();
    }
}
