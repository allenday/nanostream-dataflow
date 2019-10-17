package com.google.allenday.nanostream.cannabis.di;

import com.google.allenday.genomics.core.align.AlignService;
import com.google.allenday.genomics.core.align.SamBamManipulationService;
import com.google.allenday.genomics.core.align.transform.AlignFn;
import com.google.allenday.genomics.core.align.transform.AlignSortMergeTransform;
import com.google.allenday.genomics.core.align.transform.MergeFn;
import com.google.allenday.genomics.core.align.transform.SortFn;
import com.google.allenday.genomics.core.cmd.CmdExecutor;
import com.google.allenday.genomics.core.cmd.WorkerSetupService;
import com.google.allenday.genomics.core.csv.ParseSourceCsvTransform;
import com.google.allenday.genomics.core.gene.GeneExampleMetaData;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.TransformIoHandler;
import com.google.allenday.genomics.core.reference.ReferencesProvider;
import com.google.allenday.nanostream.cannabis.NanostreamCannabisPipelineOptions;
import com.google.allenday.nanostream.cannabis.io.CannabisUriProvider;
import com.google.allenday.nanostream.cannabis.anomaly.RecognizePairedReadsWithAnomalyFn;
import com.google.allenday.nanostream.cannabis.anomaly.DetectAnomalyTransform;
import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import java.util.List;

//TODO

/**
 *
 */
public class NanostreamCannabisModule extends AbstractModule {

    private String inputCsvUri;

    private String srcBucket;
    private String resultBucket;
    private List<String> geneReferences;
    private String jobTime;

    private String allReferencesDirGcsUri;
    private String alignedOutputDir;
    private String sortedOutputDir;
    private String mergedOutputDir;
    private String anomalyOutputPath;
    private long memoryOutputLimit;
    private List<String> sraSamplesToFilter;

    public NanostreamCannabisModule(Builder builder) {
        this.inputCsvUri = builder.inputCsvUri;
        this.srcBucket = builder.srcBucket;
        this.geneReferences = builder.geneReferences;
        this.resultBucket = builder.resultBucket;
        this.jobTime = builder.jobTime;
        this.allReferencesDirGcsUri = builder.allReferencesDirGcsUri;
        this.alignedOutputDir = builder.alignedOutputDir;
        this.sortedOutputDir = builder.sortedOutputDir;
        this.mergedOutputDir = builder.mergedOutputDir;
        this.anomalyOutputPath = builder.anomalyOutputPath;
        this.memoryOutputLimit = builder.memoryOutputLimit;
        this.sraSamplesToFilter = builder.sraSamplesToFilter;
    }

    public static class Builder {
        private String inputCsvUri;

        private String srcBucket;
        private List<String> geneReferences;
        private String resultBucket;
        private String jobTime;

        private String allReferencesDirGcsUri;
        private String alignedOutputDir;
        private String sortedOutputDir;
        private String mergedOutputDir;
        private String anomalyOutputPath;

        private long memoryOutputLimit;
        private List<String> sraSamplesToFilter;

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

        public Builder setAllReferencesDirGcsUri(String allReferencesDirGcsUri) {
            this.allReferencesDirGcsUri = allReferencesDirGcsUri;
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

        public Builder setInputCsvUri(String inputCsvUri) {
            this.inputCsvUri = inputCsvUri;
            return this;
        }

        public Builder setSraSamplesToFilter(List<String> sraSamplesToFilter) {
            this.sraSamplesToFilter = sraSamplesToFilter;
            return this;
        }

        public Builder setFromOptions(NanostreamCannabisPipelineOptions nanostreamPipelineOptions) {
            setInputCsvUri(nanostreamPipelineOptions.getInputCsvUri());
            setSrcBucket(nanostreamPipelineOptions.getSrcBucket());
            setGeneReferences(nanostreamPipelineOptions.getReferenceNamesList());
            setResultBucket(nanostreamPipelineOptions.getResultBucket());
            setAllReferencesDirGcsUri(nanostreamPipelineOptions.getAllReferencesDirGcsUri());
            setAlignedOutputDir(nanostreamPipelineOptions.getAlignedOutputDir());
            setSortedOutputDir(nanostreamPipelineOptions.getSortedOutputDir());
            setMergedOutputDir(nanostreamPipelineOptions.getMergedOutputDir());
            setMemoryOutputLimit(nanostreamPipelineOptions.getMemoryOutputLimit());
            setAnomalyOutputPath(nanostreamPipelineOptions.getAnomalyOutputPath());
            setSraSamplesToFilter(nanostreamPipelineOptions.getSraSamplesToFilter());
            return this;
        }

        public NanostreamCannabisModule build() {
            return new NanostreamCannabisModule(this);
        }

    }

    @Provides
    @Singleton
    public RecognizePairedReadsWithAnomalyFn provideParseCannabisDataFn(FileUtils fileUtils) {
        return new RecognizePairedReadsWithAnomalyFn(srcBucket, fileUtils);
    }

    @Provides
    @Singleton
    public DetectAnomalyTransform provideGroupByPairedReadsAndFilter(RecognizePairedReadsWithAnomalyFn recognizePairedReadsWithAnomalyFn) {
        return new DetectAnomalyTransform("Filter anomaly and prepare for processing", resultBucket,
                String.format(anomalyOutputPath, jobTime), recognizePairedReadsWithAnomalyFn);
    }

    @Provides
    @Singleton
    public ReferencesProvider provideReferencesProvider(FileUtils fileUtils) {
        return new ReferencesProvider(fileUtils, allReferencesDirGcsUri);
    }

    @Provides
    @Singleton
    public FileUtils provideFileUtils() {
        return new FileUtils();
    }

    @Provides
    @Singleton
    public CmdExecutor provideCmdExecutor() {
        return new CmdExecutor();
    }

    @Provides
    @Singleton
    public WorkerSetupService provideWorkerSetupService(CmdExecutor cmdExecutor) {
        return new WorkerSetupService(cmdExecutor);
    }

    @Provides
    @Singleton
    public AlignService provideAlignService(WorkerSetupService workerSetupService, CmdExecutor cmdExecutor, FileUtils fileUtils) {
        return new AlignService(workerSetupService, cmdExecutor, fileUtils);
    }

    @Provides
    @Singleton
    public SamBamManipulationService provideSamBamManipulationService(FileUtils fileUtils) {
        return new SamBamManipulationService(fileUtils);
    }

    @Provides
    @Singleton
    public MergeFn provideMergeFn(SamBamManipulationService samBamManipulationService, FileUtils fileUtils) {
        TransformIoHandler mergeIoHandler = new TransformIoHandler(resultBucket, String.format(mergedOutputDir, jobTime),
                memoryOutputLimit, fileUtils);

        return new MergeFn(mergeIoHandler, samBamManipulationService, fileUtils);
    }

    @Provides
    @Singleton
    public SortFn provideSortFn(SamBamManipulationService samBamManipulationService, FileUtils fileUtils) {
        TransformIoHandler sortIoHandler = new TransformIoHandler(resultBucket, String.format(sortedOutputDir, jobTime),
                memoryOutputLimit, fileUtils);

        return new SortFn(sortIoHandler, fileUtils, samBamManipulationService);
    }

    @Provides
    @Singleton
    public AlignFn provideAlignFn(AlignService alignService, ReferencesProvider referencesProvider, FileUtils fileUtils) {
        TransformIoHandler alignIoHandler = new TransformIoHandler(resultBucket, String.format(alignedOutputDir, jobTime),
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

    @Provides
    @Singleton
    public GeneExampleMetaData.Parser provideGeneExampleMetaDataParser() {
        return GeneExampleMetaData.Parser.withDefaultSchema();
    }

    @Provides
    @Singleton
    public CannabisUriProvider provideCannabisUriProvider() {
        return CannabisUriProvider.withDefaultProviderRule(srcBucket);
    }

    @Provides
    @Singleton
    public ParseSourceCsvTransform provideParseSourceCsvTransform(FileUtils fileUtils,
                                                                  GeneExampleMetaData.Parser geneExampleMetaDataParser,
                                                                  CannabisUriProvider cannabisUriProvider,
                                                                  DetectAnomalyTransform detectAnomalyTransform) {

        ParseSourceCsvTransform parseSourceCsvTransform = new ParseSourceCsvTransform("Parse CSV", inputCsvUri,
                geneExampleMetaDataParser, cannabisUriProvider, fileUtils);
        parseSourceCsvTransform.setSraSamplesToFilter(sraSamplesToFilter);
        parseSourceCsvTransform.setPreparingTransforms(detectAnomalyTransform);
        return parseSourceCsvTransform;
    }


}
