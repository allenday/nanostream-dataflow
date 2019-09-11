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
    protected String alignerTopicId;
    protected String resultBucket;
    protected String srсBucket;
    protected String samHeadersPath;

    public NanostreamCannabisModule(Builder builder) {
        this.projectId = builder.projectId;
        this.alignerTopicId = builder.alignerTopicId;
        this.resultBucket = builder.resultBucket;
        this.srсBucket = builder.srsBucket;
        this.samHeadersPath = builder.samHeadersPath;
    }

    public static class Builder {

        protected String projectId;
        protected String alignerTopicId;
        protected String resultBucket;
        protected String srsBucket;
        protected String samHeadersPath;


        public Builder setProjectId(String projectId) {
            this.projectId = projectId;
            return this;
        }

        public Builder setAlignerTopicId(String alignerTopicId) {
            this.alignerTopicId = alignerTopicId;
            return this;
        }

        public Builder setResultBucket(String resultBucket) {
            this.resultBucket = resultBucket;
            return this;
        }

        public Builder setSrsBucket(String srsBucket) {
            this.srsBucket = srsBucket;
            return this;
        }

        public Builder setSamHeadersPath(String samHeadersPath) {
            this.samHeadersPath = samHeadersPath;
            return this;
        }

        public String getProjectId() {
            return projectId;
        }

        public String getAlignerTopicId() {
            return alignerTopicId;
        }

        public NanostreamCannabisModule buildFromOptions(NanostreamCannabisPipelineOptions nanostreamPipelineOptions) {
            setProjectId(nanostreamPipelineOptions.getProject());
            /*setAlignerTopicId(nanostreamPipelineOptions.getAlignerTopicId());
            setResultBucket(nanostreamPipelineOptions.getResultBucket());
            setSrsBucket(nanostreamPipelineOptions.getSrсBucket());
            setSamHeadersPath(nanostreamPipelineOptions.getSamHeadersPath());*/
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
