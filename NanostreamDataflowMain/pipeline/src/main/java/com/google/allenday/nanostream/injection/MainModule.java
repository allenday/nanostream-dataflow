package com.google.allenday.nanostream.injection;

import com.google.allenday.genomics.core.cmd.CmdExecutor;
import com.google.allenday.genomics.core.cmd.WorkerSetupService;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.TransformIoHandler;
import com.google.allenday.genomics.core.processing.align.AlignFn;
import com.google.allenday.genomics.core.processing.align.AlignService;
import com.google.allenday.genomics.core.processing.align.AlignTransform;
import com.google.allenday.genomics.core.processing.align.KAlignService;
import com.google.allenday.genomics.core.processing.sam.SamBamManipulationService;
import com.google.allenday.genomics.core.reference.ReferencesProvider;
import com.google.allenday.genomics.core.utils.NameProvider;
import com.google.allenday.nanostream.aligner.GetSequencesFromSamDataFn;
import com.google.allenday.nanostream.batch.CreateBatchesTransform;
import com.google.allenday.nanostream.fastq.ParseFastQFn;
import com.google.allenday.nanostream.gcs.GetDataFromFastQFileFn;
import com.google.allenday.nanostream.gcs.ParseGCloudNotification;
import com.google.allenday.nanostream.geneinfo.LoadGeneInfoTransform;
import com.google.allenday.nanostream.other.Configuration;
import com.google.allenday.nanostream.output.PrepareSequencesStatisticToOutputDbFn;
import com.google.allenday.nanostream.output.WriteDataToFirestoreDbFn;
import com.google.allenday.nanostream.pipeline.LoopingTimerTransform;
import com.google.allenday.nanostream.pipeline.PipelineManagerService;
import com.google.allenday.nanostream.taxonomy.GetResistanceGenesTaxonomyDataFn;
import com.google.allenday.nanostream.taxonomy.GetSpeciesTaxonomyDataFn;
import com.google.allenday.nanostream.taxonomy.GetTaxonomyFromTree;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.allenday.nanostream.util.ResourcesHelper;
import com.google.inject.Provides;
import com.google.inject.Singleton;

import static com.google.allenday.nanostream.other.Configuration.RESISTANCE_GENES_GENE_DATA_FILE_NAME;
import static com.google.allenday.nanostream.other.Configuration.SPECIES_GENE_DATA_FILE_NAME;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class MainModule extends NanostreamModule {

    public MainModule(Builder builder) {
        super(builder);
    }


    public static class Builder extends NanostreamModule.Builder {

        @Override
        public MainModule build() {
            return new MainModule(this);
        }
    }

    @Provides
    @Singleton
    public NameProvider provideNameProvider() {
        return NameProvider.initialize();
    }

    @Provides
    public KAlignService provideKAlignService(WorkerSetupService workerSetupService, CmdExecutor cmdExecutor) {
        return new KAlignService(workerSetupService, cmdExecutor);
    }

    @Provides
    public WriteDataToFirestoreDbFn provideWriteDataToFirestoreDbFn() {
        return new WriteDataToFirestoreDbFn(projectId);
    }

    @Provides
    public GetSpeciesTaxonomyDataFn provideGetTaxonomyDataFn() {
        return new GetSpeciesTaxonomyDataFn(EntityNamer.addPrefix(Configuration.GENE_CACHE_COLLECTION_NAME_BASE,
                processingMode.label), projectId);
    }

    @Provides
    public LoadGeneInfoTransform provideLoadGeneInfoTransform() {
        return new LoadGeneInfoTransform(resistanceGenesList);
    }

    @Provides
    public PrepareSequencesStatisticToOutputDbFn providePrepareSequencesStatisticToOutputDbFn(EntityNamer entityNamer) {
        return new PrepareSequencesStatisticToOutputDbFn(outputCollectionNamePrefix, outputDocumentNamePrefix,
                entityNamer.getInitialTimestamp());
    }

    @Provides
    public GetTaxonomyFromTree provideGetTaxonomyFromTree() {
        return new GetTaxonomyFromTree(
                new ResourcesHelper().getFileContent(SPECIES_GENE_DATA_FILE_NAME));
    }

    @Provides
    public GetResistanceGenesTaxonomyDataFn provideGetResistanceGenesTaxonomyDataFn() {
        return new GetResistanceGenesTaxonomyDataFn(new ResourcesHelper().getFileContent(RESISTANCE_GENES_GENE_DATA_FILE_NAME));
    }

    @Provides
    public ParseGCloudNotification provideParseGCloudNotification(FileUtils fileUtils) {
        return new ParseGCloudNotification(fileUtils);
    }

    @Provides
    @Singleton
    public ReferencesProvider provideReferencesProvider(FileUtils fileUtils) {
        return new ReferencesProvider(fileUtils, genomicsOptions.getAllReferencesDirGcsUri(), ".fasta");
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
    public AlignService.Instrument provideInstrument() {
        return AlignService.Instrument.OXFORD_NANOPORE;
    }

    @Provides
    @Singleton
    public AlignService provideAlignService(WorkerSetupService workerSetupService, CmdExecutor cmdExecutor, FileUtils fileUtils) {
        return new AlignService(workerSetupService, cmdExecutor, fileUtils);
    }

    @Provides
    @Singleton
    public AlignFn provideAlignFn(AlignService alignService, ReferencesProvider referencesProvider,
                                  FileUtils fileUtils, NameProvider nameProvider) {
        TransformIoHandler alignIoHandler = new TransformIoHandler(genomicsOptions.getResultBucket(),
                String.format(genomicsOptions.getAlignedOutputDirPattern(), nameProvider.getCurrentTimeInDefaultFormat()),
                genomicsOptions.getMemoryOutputLimit(), fileUtils);

        return new AlignFn(alignService, referencesProvider, alignIoHandler, fileUtils);
    }

    @Provides
    @Singleton
    public SamBamManipulationService provideSamBamManipulationService(FileUtils fileUtils) {
        return new SamBamManipulationService(fileUtils);
    }

    @Provides
    @Singleton
    public GetSequencesFromSamDataFn provideGetSequencesFromSamDataFn(FileUtils fileUtils,
                                                                      SamBamManipulationService samBamManipulationService,
                                                                      NameProvider nameProvider) {
        TransformIoHandler ioHandler = new TransformIoHandler(genomicsOptions.getResultBucket(),
                String.format(genomicsOptions.getAlignedOutputDirPattern(), nameProvider.getCurrentTimeInDefaultFormat()),
                genomicsOptions.getMemoryOutputLimit(), fileUtils);
        return new GetSequencesFromSamDataFn(fileUtils, ioHandler, samBamManipulationService);
    }


    @Provides
    @Singleton
    public AlignTransform provideAlignTransform(AlignFn alignFn) {
        return new AlignTransform("Align reads transform", alignFn, genomicsOptions.getGeneReferences());
    }


    @Provides
    @Singleton
    public CreateBatchesTransform provideCreateBatchesTransform(GetDataFromFastQFileFn getDataFromFastQFileFn,
                                                                ParseFastQFn parseFastQFn,
                                                                CreateBatchesTransform.SequenceBatchesToFastqFiles sequenceBatchesToFastqFiles) {
        return new CreateBatchesTransform(getDataFromFastQFileFn, parseFastQFn, sequenceBatchesToFastqFiles,
                batchSize, alignmentWindow);
    }

    @Provides
    @Singleton
    public GetDataFromFastQFileFn provideGetDataFromFastQFileFn(FileUtils fileUtils) {
        return new GetDataFromFastQFileFn(fileUtils);
    }

    @Provides
    @Singleton
    public CreateBatchesTransform.SequenceBatchesToFastqFiles provideCreateBatchesTransformSequenceBatchesToFastqFiles(AlignService.Instrument instrument) {
        return new CreateBatchesTransform.SequenceBatchesToFastqFiles(batchSize, instrument);
    }

    @Provides
    @Singleton
    public PipelineManagerService providePipelineManagerService() {
        return new PipelineManagerService(autoStopTopic);
    }
}
