package com.google.allenday.nanostream.di;

import com.google.allenday.genomics.core.transform.AlignSortMergeTransform;
import com.google.allenday.nanostream.NanostreamCannabisPipelineOptions;
import com.google.allenday.nanostream.cannabis_parsing.ParseCannabisDataFn;
import com.google.allenday.nanostream.transforms.GroupByPairedReadsAndFilter;
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

    private String referenceDir;
    private String previousAlignedOutputDir;
    private String alignedOutputDir;
    private String sortedOutputDir;
    private String mergedOutputDir;
    private String anomalyOutputPath;
    private long memoryOutputLimit;

    public NanostreamCannabisModule(Builder builder) {
        this.srcBucket = builder.srcBucket;
        this.geneReferences = builder.geneReferences;
        this.resultBucket = builder.resultBucket;
        this.jobTime = builder.jobTime;
        this.referenceDir = builder.referenceDir;
        this.previousAlignedOutputDir = builder.previousAlignedOutputDir;
        this.alignedOutputDir = builder.alignedOutputDir;
        this.sortedOutputDir = builder.sortedOutputDir;
        this.mergedOutputDir = builder.mergedOutputDir;
        this.anomalyOutputPath = builder.anomalyOutputPath;
        this.memoryOutputLimit = builder.memoryOutputLimit;
    }

    public static class Builder {

        private String srcBucket;
        private List<String> geneReferences;
        private String resultBucket;
        private String jobTime;

        private String referenceDir;
        private String previousAlignedOutputDir;
        private String alignedOutputDir;
        private String sortedOutputDir;
        private String mergedOutputDir;
        private String anomalyOutputPath;

        private long memoryOutputLimit;

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

        public Builder setReferenceDir(String referenceDir) {
            this.referenceDir = referenceDir;
            return this;
        }

        public Builder setPreviousAlignedOutputDir(String previousAlignedOutputDir) {
            this.previousAlignedOutputDir = previousAlignedOutputDir;
            return this;
        }

        public Builder setAlignedOutputDir(String alignedOutputDir) {
            this.alignedOutputDir = alignedOutputDir;
            return this;
        }

        public Builder setSortedOutputDir(String sortedOutputDir) {
            this.sortedOutputDir = sortedOutputDir;
            return this;
        }

        public Builder setMergedOutputDir(String mergedOutputDir) {
            this.mergedOutputDir = mergedOutputDir;
            return this;
        }

        public Builder setMemoryOutputLimit(long memoryOutputLimit) {
            this.memoryOutputLimit = memoryOutputLimit;
            return this;
        }

        public Builder setAnomalyOutputPath(String anomalyOutputPath) {
            this.anomalyOutputPath = anomalyOutputPath;
            return this;
        }

        public Builder setFromOptions(NanostreamCannabisPipelineOptions nanostreamPipelineOptions) {
            setSrcBucket(nanostreamPipelineOptions.getSrcBucket());
            setGeneReferences(nanostreamPipelineOptions.getReferenceNamesList());
            setResultBucket(nanostreamPipelineOptions.getResultBucket());
            setReferenceDir(nanostreamPipelineOptions.getReferenceDir());
            setPreviousAlignedOutputDir(nanostreamPipelineOptions.getPreviousAlignedOutputDir());
            setAlignedOutputDir(nanostreamPipelineOptions.getAlignedOutputDir());
            setSortedOutputDir(nanostreamPipelineOptions.getSortedOutputDir());
            setMergedOutputDir(nanostreamPipelineOptions.getMergedOutputDir());
            setMemoryOutputLimit(nanostreamPipelineOptions.getMemoryOutputLimit());
            setAnomalyOutputPath(nanostreamPipelineOptions.getAnomalyOutputPath());
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
    public GroupByPairedReadsAndFilter provideGroupByPairedReadsAndFilter() {
        return new GroupByPairedReadsAndFilter("Filter anomaly and prepare for processing", resultBucket,
                String.format(anomalyOutputPath, jobTime));
    }

    @Provides
    @Singleton
    public AlignSortMergeTransform provideAlignSortMergeTransform() {
        AlignSortMergeTransform alignSortMergeTransform = new AlignSortMergeTransform("Align -> Sort -> Merge transform",
                srcBucket,
                resultBucket,
                referenceDir,
                geneReferences,
                String.format(alignedOutputDir, jobTime),
                String.format(sortedOutputDir, jobTime),
                String.format(mergedOutputDir, jobTime),
                memoryOutputLimit);
        if (previousAlignedOutputDir != null){
            alignSortMergeTransform = alignSortMergeTransform.withPreviousAlignDestGcsPrefix(previousAlignedOutputDir);
        }
        return alignSortMergeTransform;
    }


}
