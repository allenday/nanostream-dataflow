package com.google.allenday.nanostream.cannabis.di;

import com.google.allenday.genomics.core.align.AlignService;
import com.google.allenday.genomics.core.align.AlignerOptions;
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
import com.google.allenday.nanostream.cannabis.anomaly.DetectAnomalyTransform;
import com.google.allenday.nanostream.cannabis.anomaly.RecognizePairedReadsWithAnomalyFn;
import com.google.allenday.nanostream.cannabis.io.CannabisUriProvider;
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
    private String inputCsvUri;
    private String jobTime;
    private String anomalyOutputPath;
    private List<String> sraSamplesToFilter;

    private AlignerOptions alignerOptions;

    public NanostreamCannabisModule(Builder builder) {
        this.inputCsvUri = builder.inputCsvUri;
        this.jobTime = builder.jobTime;
        this.anomalyOutputPath = builder.anomalyOutputPath;
        this.sraSamplesToFilter = builder.sraSamplesToFilter;
        this.alignerOptions = builder.alignerOptions;
        this.srcBucket = builder.srcBucket;
    }

    public static class Builder {
        private String srcBucket;
        private String inputCsvUri;
        private String jobTime;
        private String anomalyOutputPath;
        private AlignerOptions alignerOptions;

        private List<String> sraSamplesToFilter;

        public Builder setJobTime(String jobTime) {
            this.jobTime = jobTime;
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

        public Builder setAlignerOptions(AlignerOptions alignerOptions) {
            this.alignerOptions = alignerOptions;
            return this;
        }

        public Builder setSrcBucket(String srcBucket) {
            this.srcBucket = srcBucket;
            return this;
        }

        public Builder setFromOptions(NanostreamCannabisPipelineOptions nanostreamPipelineOptions) {
            setInputCsvUri(nanostreamPipelineOptions.getInputCsvUri());
            setAnomalyOutputPath(nanostreamPipelineOptions.getAnomalyOutputPath());
            setSraSamplesToFilter(nanostreamPipelineOptions.getSraSamplesToFilter());
            setAlignerOptions(AlignerOptions.fromAlignerPipelineOptions(nanostreamPipelineOptions));
            setSrcBucket(nanostreamPipelineOptions.getSrcBucket());
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
        return new DetectAnomalyTransform("Filter anomaly and prepare for processing", alignerOptions.getResultBucket(),
                String.format(anomalyOutputPath, jobTime), recognizePairedReadsWithAnomalyFn);
    }

    @Provides
    @Singleton
    public ReferencesProvider provideReferencesProvider(FileUtils fileUtils) {
        return new ReferencesProvider(fileUtils, alignerOptions.getAllReferencesDirGcsUri());
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
        TransformIoHandler mergeIoHandler = new TransformIoHandler(alignerOptions.getResultBucket(), String.format(alignerOptions.getMergedOutputDir(), jobTime),
                alignerOptions.getMemoryOutputLimit(), fileUtils);

        return new MergeFn(mergeIoHandler, samBamManipulationService, fileUtils);
    }

    @Provides
    @Singleton
    public SortFn provideSortFn(SamBamManipulationService samBamManipulationService, FileUtils fileUtils) {
        TransformIoHandler sortIoHandler = new TransformIoHandler(alignerOptions.getResultBucket(), String.format(alignerOptions.getSortedOutputDir(), jobTime),
                alignerOptions.getMemoryOutputLimit(), fileUtils);

        return new SortFn(sortIoHandler, fileUtils, samBamManipulationService);
    }

    @Provides
    @Singleton
    public AlignFn provideAlignFn(AlignService alignService, ReferencesProvider referencesProvider, FileUtils fileUtils) {
        TransformIoHandler alignIoHandler = new TransformIoHandler(alignerOptions.getResultBucket(), String.format(alignerOptions.getAlignedOutputDir(), jobTime),
                alignerOptions.getMemoryOutputLimit(), fileUtils);

        return new AlignFn(alignService, referencesProvider, alignerOptions.getGeneReferences(), alignIoHandler, fileUtils);
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
