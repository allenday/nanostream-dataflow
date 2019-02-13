package com.theappsolutions.nanostream.injection;

import com.google.inject.AbstractModule;
import com.theappsolutions.nanostream.NanostreamApp;
import com.theappsolutions.nanostream.NanostreamPipelineOptions;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class NanostreamModule extends AbstractModule {

    protected String projectId;
    protected String servicesUrl;
    protected String bwaEndpoint;
    protected String bwaDB;
    protected String kAlignEndpoint;
    protected String resistanceGenesFastaDB;
    protected String resistanceGenesList;
    protected String outputFirestoreCollectionNamePrefix;
    protected NanostreamApp.ProcessingMode processingMode;

    public NanostreamModule(Builder builder) {
        this.projectId = builder.projectId;
        this.servicesUrl = builder.servicesUrl;
        this.bwaEndpoint = builder.bwaEndpoint;
        this.bwaDB = builder.bwaDB;
        this.kAlignEndpoint = builder.kAlignEndpoint;
        this.resistanceGenesFastaDB = builder.resistanceGenesFastaDB;
        this.resistanceGenesList = builder.resistanceGenesList;
        this.outputFirestoreCollectionNamePrefix = builder.outputFirestoreCollectionNamePrefix;
        this.processingMode = builder.processingMode;
    }

    public static class Builder {

        protected String projectId;
        protected String servicesUrl;
        protected String bwaEndpoint;
        protected String bwaDB;
        protected String kAlignEndpoint;
        protected String resistanceGenesFastaDB;
        protected String resistanceGenesList;
        protected String outputFirestoreCollectionNamePrefix;
        protected NanostreamApp.ProcessingMode processingMode;


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

        public Builder setResistanceGenesFastaDB(String resistanceGenesFastaDB) {
            this.resistanceGenesFastaDB = resistanceGenesFastaDB;
            return this;
        }

        public Builder setResistanceGenesList(String resistanceGenesList) {
            this.resistanceGenesList = resistanceGenesList;
            return this;
        }

        public Builder setOutputFirestoreCollectionNamePrefix(String outputFirestoreCollectionNamePrefix) {
            this.outputFirestoreCollectionNamePrefix = outputFirestoreCollectionNamePrefix;
            return this;
        }

        public Builder setProcessingMode(NanostreamApp.ProcessingMode processingMode) {
            this.processingMode = processingMode;
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
            setResistanceGenesFastaDB(nanostreamPipelineOptions.getResistanceGenesFastaDB());
            setResistanceGenesList(nanostreamPipelineOptions.getResistanceGenesList());
            setOutputFirestoreCollectionNamePrefix(nanostreamPipelineOptions.getOutputFirestoreCollectionNamePrefix());
            setProcessingMode(NanostreamApp.ProcessingMode.findByLabel(nanostreamPipelineOptions.getProcessingMode()));
            return build();
        }

        public NanostreamModule build() {
            return new NanostreamModule(this);
        }

    }
}
