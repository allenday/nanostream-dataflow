package com.google.allenday.nanostream.injection;

import com.google.allenday.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.google.allenday.nanostream.geneinfo.LoadGeneInfoTransform;
import com.google.allenday.nanostream.http.NanostreamHttpService;
import com.google.allenday.nanostream.kalign.ProceedKAlignmentFn;
import com.google.allenday.nanostream.other.Configuration;
import com.google.allenday.nanostream.output.PrepareSequencesStatisticToOutputDbFn;
import com.google.allenday.nanostream.output.WriteDataToFirestoreDbFn;
import com.google.allenday.nanostream.taxonomy.GetResistanceGenesTaxonomyDataFn;
import com.google.allenday.nanostream.taxonomy.GetSpeciesTaxonomyDataFn;
import com.google.allenday.nanostream.taxonomy.GetTaxonomyFromTree;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.allenday.nanostream.util.HttpHelper;
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
    public NanostreamHttpService provideNanostreamHttpService(HttpHelper httpHelper) {
        return new NanostreamHttpService(httpHelper, servicesUrl);
    }

    @Provides
    public MakeAlignmentViaHttpFn provideMakeAlignmentViaHttpFn(NanostreamHttpService service) {
        return new MakeAlignmentViaHttpFn(service, bwaDB, bwaEndpoint, bwaArguments);
    }

    @Provides
    public ProceedKAlignmentFn provideProceedKAlignmentFn(NanostreamHttpService service) {
        return new ProceedKAlignmentFn(service, kAlignEndpoint);
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
}
