package com.google.allenday.nanostream.di;

import com.google.allenday.nanostream.NanostreamCannabisPipelineOptions;
import com.google.allenday.nanostream.cannabis_parsing.ParseCannabisDataFn;
import com.google.allenday.nanostream.cmd.CmdExecutor;
import com.google.allenday.nanostream.cmd.WorkerSetupService;
import com.google.allenday.nanostream.transforms.AddReferenceFn;
import com.google.allenday.nanostream.transforms.AlignSortFn;
import com.google.allenday.nanostream.transforms.MergeBamQFn;
import com.google.allenday.nanostream.utils.BamFilesMerger;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.util.List;

//TODO

/**
 *
 */
public class NanostreamCannabisModule extends AbstractModule {

    private String srcBucket;
    private String resultBucket;
    private List<String> geneReferences;
    private String jobTime;

    public NanostreamCannabisModule(Builder builder) {
        this.srcBucket = builder.srcBucket;
        this.geneReferences = builder.geneReferences;
        this.resultBucket = builder.resultBucket;
        this.jobTime = builder.jobTime;
    }

    public static class Builder {

        private String srcBucket;
        private List<String> geneReferences;
        private String resultBucket;
        private String jobTime;

        public Builder setSrcBucket(String inputBucket) {
            this.srcBucket = inputBucket;
            return this;
        }

        public Builder setGeneReferences(List<String> geneReferences) {
            this.geneReferences = geneReferences;
            return this;
        }

        public Builder setResultBucket(String resultBucket) {
            this.resultBucket = resultBucket;
            return this;
        }

        public Builder setJobTime(String jobTime) {
            this.jobTime = jobTime;
            return this;
        }

        public Builder setFromOptions(NanostreamCannabisPipelineOptions nanostreamPipelineOptions) {
            setSrcBucket(nanostreamPipelineOptions.getSrcBucket());
            setGeneReferences(nanostreamPipelineOptions.getReferenceNamesList());
            setResultBucket(nanostreamPipelineOptions.getResultBucket());
            return this;
        }

        public NanostreamCannabisModule build() {
            return new NanostreamCannabisModule(this);
        }

    }

    @Provides
    @Singleton
    public ParseCannabisDataFn provideParseCannabisDataFn() {
        return new ParseCannabisDataFn(srcBucket);
    }

    @Provides
    @Singleton
    public AddReferenceFn provideAddReferenceFn() {
        return new AddReferenceFn(geneReferences);
    }

    @Provides
    @Singleton
    public WorkerSetupService providWorkerSetupService(CmdExecutor cmdExecutor) {
        return new WorkerSetupService(cmdExecutor);
    }

    @Provides
    @Singleton
    public AlignSortFn provideAlignSortFn(CmdExecutor cmdExecutor, WorkerSetupService workerSetupService) {
        return new AlignSortFn(cmdExecutor, workerSetupService, srcBucket, resultBucket, geneReferences, jobTime);
    }

    @Provides
    @Singleton
    public MergeBamQFn provideAlignSortFn(BamFilesMerger bamFilesMerger) {
        return new MergeBamQFn(resultBucket, bamFilesMerger, jobTime);
    }
}
