package com.google.allenday.nanostream;

import com.google.allenday.genomics.core.pipeline.GenomicsOptions;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.apache.beam.sdk.options.ValueProvider;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class NanostreamModule extends AbstractModule {

    protected String projectId;
    protected String resistanceGenesList;
    protected ValueProvider<String> outputCollectionNamePrefix;
    protected ValueProvider<String> outputDocumentNamePrefix;
    protected ProcessingMode processingMode;
    protected GenomicsOptions genomicsOptions;
    protected int batchSize;
    protected int alignmentWindow;
    protected ValueProvider<Integer> autoStopDelay;
    protected String autoStopTopic;
    protected ValueProvider<String> inputDir;

    public NanostreamModule(Builder builder) {
        this.projectId = builder.projectId;
        this.resistanceGenesList = builder.resistanceGenesList;
        this.outputCollectionNamePrefix = builder.outputCollectionNamePrefix;
        this.processingMode = builder.processingMode;
        this.outputDocumentNamePrefix = builder.outputDocumentNamePrefix;
        this.genomicsOptions = builder.alignerOptions;
        this.batchSize = builder.batchSize;
        this.alignmentWindow = builder.alignmentWindow;
        this.autoStopTopic = builder.autoStopTopic;
        this.autoStopDelay = builder.autoStopDelay;
        this.inputDir = builder.inputDir;
    }

    @Provides
    @Singleton
    public EntityNamer provideEntityNamer() {
        return EntityNamer.initialize();
    }

    public static class Builder {

        protected String projectId;
        protected String resistanceGenesList;
        protected ValueProvider<String> outputCollectionNamePrefix;
        protected ValueProvider<String> outputDocumentNamePrefix;
        protected ProcessingMode processingMode;
        protected GenomicsOptions alignerOptions;
        protected int batchSize;
        protected int alignmentWindow;
        protected String autoStopTopic;
        protected ValueProvider<Integer> autoStopDelay;
        protected ValueProvider<String> inputDir;

        public Builder setResistanceGenesList(String resistanceGenesList) {
            this.resistanceGenesList = resistanceGenesList;
            return this;
        }

        public Builder setOutputCollectionNamePrefix(ValueProvider<String> outputCollectionNamePrefix) {
            this.outputCollectionNamePrefix = outputCollectionNamePrefix;
            return this;
        }

        public Builder setProcessingMode(ProcessingMode processingMode) {
            this.processingMode = processingMode;
            return this;
        }

        public Builder setOutputDocumentNamePrefix(ValueProvider<String> outputDocumentNamePrefix) {
            this.outputDocumentNamePrefix = outputDocumentNamePrefix;
            return this;
        }

        public Builder setAlignerOptions(GenomicsOptions alignerOptions) {
            this.alignerOptions = alignerOptions;
            return this;
        }

        public Builder setBatchSize(int batchSize) {
            this.batchSize = batchSize;
            return this;
        }

        private Builder setAlignmentWindow(Integer alignmentWindow) {
            this.alignmentWindow = alignmentWindow;
            return this;
        }

        private Builder setAutoStopTopic(String autoStopTopic) {
            this.autoStopTopic = autoStopTopic;
            return this;
        }

        private Builder setAutoStopDelay(ValueProvider<Integer> autoStopDelay) {
            this.autoStopDelay = autoStopDelay;
            return this;
        }

        public Builder setInputDir(ValueProvider<String> inputDir) {
            this.inputDir = inputDir;
            return this;
        }

        public String getProjectId() {
            return projectId;
        }

        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public NanostreamModule.Builder fromOptions(NanostreamPipelineOptions nanostreamPipelineOptions) {
            setProjectId(nanostreamPipelineOptions.getProject());
            setResistanceGenesList(nanostreamPipelineOptions.getResistanceGenesList());
            setOutputCollectionNamePrefix(nanostreamPipelineOptions.getOutputCollectionNamePrefix());
            setProcessingMode(ProcessingMode.findByLabel(nanostreamPipelineOptions.getProcessingMode()));
            setOutputDocumentNamePrefix(nanostreamPipelineOptions.getOutputDocumentNamePrefix());
            setAlignerOptions(GenomicsOptions.fromAlignerPipelineOptions(nanostreamPipelineOptions));
            setBatchSize(nanostreamPipelineOptions.getAlignmentBatchSize());
            setAlignmentWindow(nanostreamPipelineOptions.getAlignmentWindow());
            setAutoStopTopic(nanostreamPipelineOptions.getAutoStopTopic());
            setAutoStopDelay(nanostreamPipelineOptions.getAutoStopDelay());
            setInputDir(nanostreamPipelineOptions.getInputDir());
            return this;
        }

        public NanostreamModule build() {
            return new NanostreamModule(this);
        }

    }
}
