package com.google.allenday.nanostream.cannabis.di;

import com.google.allenday.genomics.core.cmd.CmdExecutor;
import com.google.allenday.genomics.core.cmd.WorkerSetupService;
import com.google.allenday.genomics.core.csv.ParseSourceCsvTransform;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.TransformIoHandler;
import com.google.allenday.genomics.core.model.GeneExampleMetaData;
import com.google.allenday.genomics.core.processing.AlignAndPostProcessTransform;
import com.google.allenday.genomics.core.processing.SamBamManipulationService;
import com.google.allenday.genomics.core.processing.align.AlignFn;
import com.google.allenday.genomics.core.processing.align.AlignService;
import com.google.allenday.genomics.core.processing.align.GenomicsOptions;
import com.google.allenday.genomics.core.processing.other.CreateBamIndexFn;
import com.google.allenday.genomics.core.processing.other.MergeFn;
import com.google.allenday.genomics.core.processing.other.SortFn;
import com.google.allenday.genomics.core.reference.ReferencesProvider;
import com.google.allenday.genomics.core.utils.NameProvider;
import com.google.allenday.nanostream.cannabis.NanostreamCannabisPipelineOptions;
import com.google.allenday.nanostream.cannabis.anomaly.DetectAnomalyTransform;
import com.google.allenday.nanostream.cannabis.anomaly.RecognizePairedReadsWithAnomalyFn;
import com.google.allenday.nanostream.cannabis.io.CannabisCsvParser;
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
    private String anomalyOutputPath;
    private List<String> sraSamplesToFilter;

    private GenomicsOptions genomicsOptions;

    public NanostreamCannabisModule(Builder builder) {
        this.inputCsvUri = builder.inputCsvUri;
        this.anomalyOutputPath = builder.anomalyOutputPath;
        this.sraSamplesToFilter = builder.sraSamplesToFilter;
        this.genomicsOptions = builder.genomicsOptions;
        this.srcBucket = builder.srcBucket;
    }

    public static class Builder {
        private String srcBucket;
        private String inputCsvUri;
        private String anomalyOutputPath;
        private GenomicsOptions genomicsOptions;

        private List<String> sraSamplesToFilter;

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

        public void setGenomicsOptions(GenomicsOptions genomicsOptions) {
            this.genomicsOptions = genomicsOptions;
        }

        public Builder setSrcBucket(String srcBucket) {
            this.srcBucket = srcBucket;
            return this;
        }

        public Builder setFromOptions(NanostreamCannabisPipelineOptions nanostreamPipelineOptions) {
            setInputCsvUri(nanostreamPipelineOptions.getInputCsvUri());
            setAnomalyOutputPath(nanostreamPipelineOptions.getAnomalyOutputPath());
            setSraSamplesToFilter(nanostreamPipelineOptions.getSraSamplesToFilter());
            setGenomicsOptions(GenomicsOptions.fromAlignerPipelineOptions(nanostreamPipelineOptions));
            setSrcBucket(nanostreamPipelineOptions.getSrcBucket());
            return this;
        }

        public NanostreamCannabisModule build() {
            return new NanostreamCannabisModule(this);
        }

    }

    @Provides
    @Singleton
    public NameProvider provideNameProvider() {
        return NameProvider.initialize();
    }

    @Provides
    @Singleton
    public RecognizePairedReadsWithAnomalyFn provideParseCannabisDataFn(FileUtils fileUtils) {
        return new RecognizePairedReadsWithAnomalyFn(srcBucket, fileUtils);
    }

    @Provides
    @Singleton
    public DetectAnomalyTransform provideGroupByPairedReadsAndFilter(RecognizePairedReadsWithAnomalyFn recognizePairedReadsWithAnomalyFn,
                                                                     NameProvider nameProvider) {
        return new DetectAnomalyTransform("Filter anomaly and prepare for processing", genomicsOptions.getResultBucket(),
                String.format(genomicsOptions.getAnomalyOutputDirPattern(), nameProvider.getCurrentTimeInDefaultFormat()), recognizePairedReadsWithAnomalyFn);
    }

    @Provides
    @Singleton
    public ReferencesProvider provideReferencesProvider(FileUtils fileUtils) {
        return new ReferencesProvider(fileUtils, genomicsOptions.getAllReferencesDirGcsUri());
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
    public MergeFn provideMergeFn(SamBamManipulationService samBamManipulationService, FileUtils fileUtils, NameProvider nameProvider) {
        TransformIoHandler mergeIoHandler = new TransformIoHandler(genomicsOptions.getResultBucket(),
                String.format(genomicsOptions.getMergedOutputDirPattern(), nameProvider.getCurrentTimeInDefaultFormat()),
                genomicsOptions.getMemoryOutputLimit(), fileUtils);

        return new MergeFn(mergeIoHandler, samBamManipulationService, fileUtils);
    }

    @Provides
    @Singleton
    public SortFn provideSortFn(SamBamManipulationService samBamManipulationService, FileUtils fileUtils, NameProvider nameProvider) {
        TransformIoHandler sortIoHandler = new TransformIoHandler(genomicsOptions.getResultBucket(),
                String.format(genomicsOptions.getSortedOutputDirPattern(), nameProvider.getCurrentTimeInDefaultFormat()),
                genomicsOptions.getMemoryOutputLimit(), fileUtils);

        return new SortFn(sortIoHandler, samBamManipulationService, fileUtils);
    }

    @Provides
    @Singleton
    public AlignFn provideAlignFn(AlignService alignService, ReferencesProvider referencesProvider, FileUtils fileUtils, NameProvider nameProvider) {
        TransformIoHandler alignIoHandler = new TransformIoHandler(genomicsOptions.getResultBucket(),
                String.format(genomicsOptions.getAlignedOutputDirPattern(), nameProvider.getCurrentTimeInDefaultFormat()),
                genomicsOptions.getMemoryOutputLimit(), fileUtils);

        return new AlignFn(alignService, referencesProvider, genomicsOptions.getGeneReferences(), alignIoHandler, fileUtils);
    }

    @Provides
    @Singleton
    public CreateBamIndexFn provideCreateBamIndexFn(SamBamManipulationService samBamManipulationService, FileUtils fileUtils, NameProvider nameProvider) {
        TransformIoHandler indexIoHandler = new TransformIoHandler(genomicsOptions.getResultBucket(),
                String.format(genomicsOptions.getBamIndexOutputDirPattern(), nameProvider.getCurrentTimeInDefaultFormat()),
                genomicsOptions.getMemoryOutputLimit(), fileUtils);

        return new CreateBamIndexFn(indexIoHandler, samBamManipulationService, fileUtils);
    }

    @Provides
    @Singleton
    public AlignAndPostProcessTransform provideAlignAndPostProcessTransform(AlignFn alignFn, SortFn sortFn,
                                                                            MergeFn mergeFn, CreateBamIndexFn createBamIndexFn) {
        return new AlignAndPostProcessTransform("Align -> Sort -> Merge transform -> Create index",
                alignFn,
                sortFn,
                mergeFn,
                createBamIndexFn);
    }

    @Provides
    @Singleton
    public GeneExampleMetaData.Parser provideGeneExampleMetaDataParser() {
        return new CannabisCsvParser();
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
