package com.google.allenday.nanostream.injection;

import com.google.allenday.genomics.core.processing.align.AlignerOptions;
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
    protected String resistanceGenesList;
    protected String outputCollectionNamePrefix;
    protected String outputDocumentNamePrefix;
    protected ProcessingMode processingMode;
    protected AlignerOptions alignerOptions;

    public NanostreamModule(Builder builder) {
        this.projectId = builder.projectId;
        this.resistanceGenesList = builder.resistanceGenesList;
        this.outputCollectionNamePrefix = builder.outputCollectionNamePrefix;
        this.processingMode = builder.processingMode;
        this.outputDocumentNamePrefix = builder.outputDocumentNamePrefix;
        this.alignerOptions = builder.alignerOptions;
    }

    public static class Builder {

        protected String projectId;
        protected String resistanceGenesList;
        protected String outputCollectionNamePrefix;
        protected ProcessingMode processingMode;
        protected String outputDocumentNamePrefix;
        protected AlignerOptions alignerOptions;


        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
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

        public Builder setAlignerOptions(AlignerOptions alignerOptions) {
            this.alignerOptions = alignerOptions;
            return this;
        }

        public String getProjectId() {
            return projectId;
        }


        public NanostreamModule.Builder fromOptions(NanostreamPipelineOptions nanostreamPipelineOptions) {
            setProjectId(nanostreamPipelineOptions.getProject());
            setResistanceGenesList(nanostreamPipelineOptions.getResistanceGenesList());
            setOutputCollectionNamePrefix(nanostreamPipelineOptions.getOutputCollectionNamePrefix());
            setProcessingMode(ProcessingMode.findByLabel(nanostreamPipelineOptions.getProcessingMode()));
            setOutputDocumentNamePrefix(nanostreamPipelineOptions.getOutputDocumentNamePrefix());
            setAlignerOptions(AlignerOptions.fromAlignerPipelineOptions(nanostreamPipelineOptions));
            return this;
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
