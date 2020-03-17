package com.google.allenday.nanostream;

import com.google.allenday.genomics.core.cmd.CmdExecutor;
import com.google.allenday.genomics.core.cmd.WorkerSetupService;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.TransformIoHandler;
import com.google.allenday.genomics.core.processing.align.*;
import com.google.allenday.genomics.core.processing.sam.SamBamManipulationService;
import com.google.allenday.genomics.core.reference.ReferencesProvider;
import com.google.allenday.genomics.core.utils.NameProvider;
import com.google.allenday.nanostream.batch.CreateBatchesTransform;
import com.google.allenday.nanostream.fastq.GetDataFromFastQFileFn;
import com.google.allenday.nanostream.fastq.ParseFastQFn;
import com.google.allenday.nanostream.gcs.FilterGcsDirectoryTransform;
import com.google.allenday.nanostream.gcs.ParseGCloudNotification;
import com.google.allenday.nanostream.output.PrepareSequencesStatisticToOutputDbFn;
import com.google.allenday.nanostream.output.WriteDataToFirestoreDbFn;
import com.google.allenday.nanostream.pipeline.PipelineManagerService;
import com.google.allenday.nanostream.sam.GetReferencesFromSamDataFn;
import com.google.allenday.nanostream.taxonomy.GetSpeciesTaxonomyDataFromGeneBankFn;
import com.google.allenday.nanostream.taxonomy.GetTaxonomyFromTree;
import com.google.allenday.nanostream.taxonomy.TaxonomyProvider;
import com.google.allenday.nanostream.taxonomy.resistant_genes.GetResistanceGenesTaxonomyDataFn;
import com.google.allenday.nanostream.taxonomy.resistant_genes.LoadResistantGeneInfoTransform;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.inject.Provides;
import com.google.inject.Singleton;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class MainModule extends NanostreamModule {


    public MainModule(Builder builder) {
        super(builder);
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
    public GetSpeciesTaxonomyDataFromGeneBankFn provideGetTaxonomyDataFn() {
        return new GetSpeciesTaxonomyDataFromGeneBankFn(EntityNamer.addPrefix(Configuration.GENE_CACHE_COLLECTION_NAME_BASE,
                processingMode.label), projectId);
    }

    @Provides
    public LoadResistantGeneInfoTransform provideLoadGeneInfoTransform() {
        return new LoadResistantGeneInfoTransform(resistanceGenesList);
    }

    @Provides
    public PrepareSequencesStatisticToOutputDbFn providePrepareSequencesStatisticToOutputDbFn(EntityNamer entityNamer) {
        return new PrepareSequencesStatisticToOutputDbFn(outputCollectionNamePrefix, outputDocumentNamePrefix,
                entityNamer.getInitialTimestamp());
    }

    @Provides
    @Singleton
    public TaxonomyProvider provideTaxonomyProvider() {
        return new TaxonomyProvider();
    }

    @Provides
    public GetTaxonomyFromTree provideGetTaxonomyFromTree(TaxonomyProvider taxonomyProvider, FileUtils fileUtils) {
        return new GetTaxonomyFromTree(taxonomyProvider, fileUtils);
    }

    @Provides
    public GetResistanceGenesTaxonomyDataFn provideGetResistanceGenesTaxonomyDataFn(TaxonomyProvider taxonomyProvider, FileUtils fileUtils) {
        return new GetResistanceGenesTaxonomyDataFn(taxonomyProvider, fileUtils);
    }

    @Provides
    public ParseGCloudNotification provideParseGCloudNotification(FileUtils fileUtils) {
        return new ParseGCloudNotification(fileUtils);
    }

    @Provides
    @Singleton
    public ReferencesProvider provideReferencesProvider(FileUtils fileUtils) {
        return new ReferencesProvider(fileUtils);
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
    public GetReferencesFromSamDataFn provideGetSequencesFromSamDataFn(FileUtils fileUtils,
                                                                       SamBamManipulationService samBamManipulationService,
                                                                       NameProvider nameProvider) {
        TransformIoHandler ioHandler = new TransformIoHandler(genomicsOptions.getResultBucket(),
                String.format(genomicsOptions.getAlignedOutputDirPattern(), nameProvider.getCurrentTimeInDefaultFormat()),
                genomicsOptions.getMemoryOutputLimit(), fileUtils);
        return new GetReferencesFromSamDataFn(fileUtils, ioHandler, samBamManipulationService);
    }

    @Provides
    @Singleton
    public AddReferenceDataSourceFn provideAddReferenceDataSourceFn() {
        if (genomicsOptions.getRefDataJsonString() != null) {
            return new AddReferenceDataSourceFn.Explicitly(genomicsOptions.getRefDataJsonString());
        } else if (genomicsOptions.getGeneReferences() != null && genomicsOptions.getAllReferencesDirGcsUri() != null) {
            return new AddReferenceDataSourceFn.FromNameAndDirPath(genomicsOptions.getAllReferencesDirGcsUri(),
                    genomicsOptions.getGeneReferences());
        } else {
            throw new RuntimeException("You must provide refDataJsonString or allReferencesDirGcsUri+gneReferences");
        }
    }

    @Provides
    @Singleton
    public AlignTransform provideAlignTransform(AlignFn alignFn, AddReferenceDataSourceFn addReferenceDataSourceFn) {
        return new AlignTransform("Align reads transform", alignFn, addReferenceDataSourceFn);
    }

    @Provides
    @Singleton
    public CreateBatchesTransform provideCreateBatchesTransform(GetDataFromFastQFileFn getDataFromFastQFileFn,
                                                                ParseFastQFn parseFastQFn,
                                                                CreateBatchesTransform.SequenceBatchesToFastqFiles sequenceBatchesToFastqFiles) {
        return new CreateBatchesTransform(/*getDataFromFastQFileFn, parseFastQFn,*/ sequenceBatchesToFastqFiles,
                batchSize, alignmentWindow);
    }

    @Provides
    @Singleton
    public GetDataFromFastQFileFn provideGetDataFromFastQFileFn(FileUtils fileUtils) {
        return new GetDataFromFastQFileFn(fileUtils);
    }

    @Provides
    @Singleton
    public CreateBatchesTransform.SequenceBatchesToFastqFiles provideCreateBatchesTransformSequenceBatchesToFastqFiles(
            FileUtils fileUtils,
            AlignService.Instrument instrument) {
        return new CreateBatchesTransform.SequenceBatchesToFastqFiles(fileUtils, batchSize, instrument);
    }

    @Provides
    @Singleton
    public PipelineManagerService providePipelineManagerService() {
        return new PipelineManagerService(autoStopTopic);
    }


    @Provides
    @Singleton
    public FilterGcsDirectoryTransform provideFilterGcsDirectoryTransform() {
        return new FilterGcsDirectoryTransform(inputDir);
    }

    public static class Builder extends NanostreamModule.Builder {

        @Override
        public MainModule build() {
            return new MainModule(this);
        }
    }
}
