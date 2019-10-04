package com.google.allenday.nanostream.di;

import com.google.allenday.genomics.core.align.AlignService;
import com.google.allenday.genomics.core.align.SamBamManipulationService;
import com.google.allenday.genomics.core.cmd.CmdExecutor;
import com.google.allenday.genomics.core.cmd.WorkerSetupService;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.IoHandler;
import com.google.allenday.genomics.core.reference.ReferencesProvider;
import com.google.allenday.genomics.core.transform.AlignSortMergeTransform;
import com.google.allenday.genomics.core.transform.fn.AlignFn;
import com.google.allenday.genomics.core.transform.fn.MergeFn;
import com.google.allenday.genomics.core.transform.fn.SortFn;
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
    public ParseCannabisDataFn provideParseCannabisDataFn(FileUtils fileUtils) {
        return new ParseCannabisDataFn(srcBucket, fileUtils);
    }

    @Provides
    @Singleton
    public GroupByPairedReadsAndFilter provideGroupByPairedReadsAndFilter() {
        return new GroupByPairedReadsAndFilter("Filter anomaly and prepare for processing", resultBucket,
                String.format(anomalyOutputPath, jobTime));
    }

    @Provides
    @Singleton
    public ReferencesProvider provideReferencesProvider(FileUtils fileUtils){
        return new ReferencesProvider(fileUtils, referenceDir, referenceDir);
    }

    @Provides
    @Singleton
    public FileUtils provideFileUtils(){
        return new FileUtils();
    }

    @Provides
    @Singleton
    public CmdExecutor provideCmdExecutor(){
        return new CmdExecutor();
    }

    @Provides
    @Singleton
    public WorkerSetupService provideWorkerSetupService(CmdExecutor cmdExecutor){
        return new WorkerSetupService(cmdExecutor);
    }

    @Provides
    @Singleton
    public AlignService provideAlignService(WorkerSetupService workerSetupService, CmdExecutor cmdExecutor, FileUtils fileUtils){
        return new AlignService(workerSetupService, cmdExecutor, fileUtils);
    }

    @Provides
    @Singleton
    public SamBamManipulationService provideSamBamManipulationService(FileUtils fileUtils){
        return new SamBamManipulationService(fileUtils);
    }

    @Provides
    @Singleton
    public MergeFn provideMergeFn(SamBamManipulationService samBamManipulationService, FileUtils fileUtils){
        IoHandler mergeIoHandler = new IoHandler(resultBucket, String.format(mergedOutputDir, jobTime),
                memoryOutputLimit, fileUtils);

        return new MergeFn(mergeIoHandler, samBamManipulationService, fileUtils);
    }

    @Provides
    @Singleton
    public SortFn provideSortFn(SamBamManipulationService samBamManipulationService, FileUtils fileUtils){
        IoHandler sortIoHandler = new IoHandler(resultBucket, String.format(sortedOutputDir, jobTime),
                memoryOutputLimit, fileUtils);

        return new SortFn(sortIoHandler, fileUtils, samBamManipulationService);
    }

    @Provides
    @Singleton
    public AlignFn provideAlignFn(AlignService alignService, ReferencesProvider referencesProvider, FileUtils fileUtils){
        IoHandler alignIoHandler = new IoHandler(resultBucket, String.format(alignedOutputDir, jobTime),
                memoryOutputLimit, fileUtils);

        return new AlignFn(alignService, referencesProvider, geneReferences, alignIoHandler, fileUtils);
    }

    @Provides
    @Singleton
    public AlignSortMergeTransform provideAlignSortMergeTransform(AlignFn alignFn, SortFn sortFn, MergeFn mergeFn) {
        return new AlignSortMergeTransform("Align -> Sort -> Merge transform",
                alignFn,
                sortFn,
                mergeFn);
    }


}
