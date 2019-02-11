package com.theappsolutions.nanostream.injection;

import com.google.inject.AbstractModule;
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
    protected String outputFirestoreDbUrl;
    protected String outputFirestoreSequencesStatisticCollection;
    protected String outputFirestoreSequencesBodiesCollection;
    protected String outputFirestoreGeneCacheCollection;
    protected String resistanceGenesFastaDB;
    protected String resistanceGenesList;

    public NanostreamModule(Builder builder) {
        this.projectId = builder.projectId;
        this.servicesUrl = builder.servicesUrl;
        this.bwaEndpoint = builder.bwaEndpoint;
        this.bwaDB = builder.bwaDB;
        this.kAlignEndpoint = builder.kAlignEndpoint;
        this.outputFirestoreDbUrl = builder.outputFirestoreDbUrl;
        this.outputFirestoreSequencesStatisticCollection = builder.outputFirestoreSequencesStatisticCollection;
        this.outputFirestoreSequencesBodiesCollection = builder.outputFirestoreSequencesBodiesCollection;
        this.outputFirestoreGeneCacheCollection = builder.outputFirestoreGeneCacheCollection;
        this.resistanceGenesFastaDB = builder.resistanceGenesFastaDB;
        this.resistanceGenesList = builder.resistanceGenesList;
    }

    public static class Builder {

        protected String projectId;
        protected String servicesUrl;
        protected String bwaEndpoint;
        protected String bwaDB;
        protected String kAlignEndpoint;
        protected String outputFirestoreDbUrl;
        protected String outputFirestoreSequencesStatisticCollection;
        protected String outputFirestoreSequencesBodiesCollection;
        protected String outputFirestoreGeneCacheCollection;
        protected String resistanceGenesFastaDB;
        protected String resistanceGenesList;

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

        public Builder setOutputFirestoreDbUrl(String outputFirestoreDbUrl) {
            this.outputFirestoreDbUrl = outputFirestoreDbUrl;
            return this;
        }

        public Builder setOutputFirestoreSequencesStatisticCollection(String outputFirestoreSequencesStatisticCollection) {
            this.outputFirestoreSequencesStatisticCollection = outputFirestoreSequencesStatisticCollection;
            return this;
        }

        public Builder setOutputFirestoreSequencesBodiesCollection(String outputFirestoreSequencesBodiesCollection) {
            this.outputFirestoreSequencesBodiesCollection = outputFirestoreSequencesBodiesCollection;
            return this;
        }

        public Builder setOutputFirestoreGeneCacheCollection(String outputFirestoreGeneCacheCollection) {
            this.outputFirestoreGeneCacheCollection = outputFirestoreGeneCacheCollection;
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

        public String getOutputFirestoreDbUrl() {
            return outputFirestoreDbUrl;
        }

        public String getOutputFirestoreSequencesStatisticCollection() {
            return outputFirestoreSequencesStatisticCollection;
        }

        public String getOutputFirestoreSequencesBodiesCollection() {
            return outputFirestoreSequencesBodiesCollection;
        }

        public String getOutputFirestoreGeneCacheCollection() {
            return outputFirestoreGeneCacheCollection;
        }

        public NanostreamModule buildFromOptions(NanostreamPipelineOptions nanostreamPipelineOptions) {
            setProjectId(nanostreamPipelineOptions.getProject());
            setServicesUrl(nanostreamPipelineOptions.getServicesUrl());
            setBwaEndpoint(nanostreamPipelineOptions.getBwaEndpoint());
            setBwaDB(nanostreamPipelineOptions.getBwaDatabase());
            setkAlignEndpoint(nanostreamPipelineOptions.getkAlignEndpoint());
            setOutputFirestoreDbUrl(nanostreamPipelineOptions.getOutputFirestoreDbUrl().get());
            setOutputFirestoreSequencesStatisticCollection(nanostreamPipelineOptions.getOutputFirestoreSequencesStatisticCollection().get());
            setOutputFirestoreSequencesBodiesCollection(nanostreamPipelineOptions.getOutputFirestoreSequencesBodiesCollection().get());
            setOutputFirestoreGeneCacheCollection(nanostreamPipelineOptions.getOutputFirestoreGeneCacheCollection().get());
            setResistanceGenesFastaDB(nanostreamPipelineOptions.getResistanceGenesFastaDB());
            setResistanceGenesList(nanostreamPipelineOptions.getResistanceGenesList());
            return build();
        }

        public NanostreamModule build() {
            return new NanostreamModule(this);
        }

    }
}
