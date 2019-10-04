package com.google.allenday.nanostream.injection;

import com.google.allenday.nanostream.NanostreamPipelineOptions;
import com.google.allenday.nanostream.ProcessingMode;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class NanostreamModule extends AbstractModule {

    protected String projectId;
    protected String servicesUrl;
    protected String bwaEndpoint;
    protected String bwaDB;
    protected String kAlignEndpoint;
    protected String resistanceGenesList;
    protected String outputCollectionNamePrefix;
    protected String outputDocumentNamePrefix;
    protected ProcessingMode processingMode;
    protected String bwaArguments;

    public NanostreamModule(Builder builder) {
        this.projectId = builder.projectId;
        this.servicesUrl = builder.servicesUrl;
        this.bwaEndpoint = builder.bwaEndpoint;
        this.bwaDB = builder.bwaDB;
        this.kAlignEndpoint = builder.kAlignEndpoint;
        this.resistanceGenesList = builder.resistanceGenesList;
        this.outputCollectionNamePrefix = builder.outputCollectionNamePrefix;
        this.processingMode = builder.processingMode;
        this.outputDocumentNamePrefix = builder.outputDocumentNamePrefix;
        this.bwaArguments = builder.bwaArguments;
    }

    public static class Builder {

        protected String projectId;
        protected String servicesUrl;
        protected String bwaEndpoint;
        protected String bwaDB;
        protected String kAlignEndpoint;
        protected String resistanceGenesList;
        protected String outputCollectionNamePrefix;
        protected ProcessingMode processingMode;
        protected String outputDocumentNamePrefix;
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

        public Builder setkAlignEndpoint(String kAlignEndpoint) {
            this.kAlignEndpoint = kAlignEndpoint;
            return this;
        }

        public Builder setResistanceGenesList(String resistanceGenesList) {
            this.resistanceGenesList = resistanceGenesList;
            return this;
        }

        public Builder setOutputCollectionNamePrefix(String outputCollectionNamePrefix) {
            this.outputCollectionNamePrefix = outputCollectionNamePrefix;
            return this;
        }

        public Builder setProcessingMode(ProcessingMode processingMode) {
            this.processingMode = processingMode;
            return this;
        }

        public Builder setOutputDocumentNamePrefix(String outputDocumentNamePrefix) {
            this.outputDocumentNamePrefix = outputDocumentNamePrefix;
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

        public String getkAlignEndpoint() {
            return kAlignEndpoint;
        }


        public NanostreamModule buildFromOptions(NanostreamPipelineOptions nanostreamPipelineOptions) {
            setProjectId(nanostreamPipelineOptions.getProject());
            setServicesUrl(nanostreamPipelineOptions.getServicesUrl());
            setBwaEndpoint(nanostreamPipelineOptions.getBwaEndpoint());
            setBwaDB(nanostreamPipelineOptions.getBwaDatabase());
            setkAlignEndpoint(nanostreamPipelineOptions.getkAlignEndpoint());
            setResistanceGenesList(nanostreamPipelineOptions.getResistanceGenesList());
            setOutputCollectionNamePrefix(nanostreamPipelineOptions.getOutputCollectionNamePrefix());
            setProcessingMode(ProcessingMode.findByLabel(nanostreamPipelineOptions.getProcessingMode()));
            setOutputDocumentNamePrefix(nanostreamPipelineOptions.getOutputDocumentNamePrefix());
            setBwaArguments(nanostreamPipelineOptions.getBwaArguments());
            return build();
        }

        public NanostreamModule build() {
            return new NanostreamModule(this);
        }

    }

    @Provides
    @Singleton
    public EntityNamer provideEntityNamer() {
        return EntityNamer.initialize();
    }
}
