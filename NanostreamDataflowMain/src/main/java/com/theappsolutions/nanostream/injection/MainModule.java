package com.theappsolutions.nanostream.injection;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.theappsolutions.nanostream.MainLogicPipelineOptions;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.geneinfo.LoadGeneInfoTransform;
import com.theappsolutions.nanostream.http.NanostreamHttpService;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.output.WriteToFirestoreDbFn;
import com.theappsolutions.nanostream.util.HttpHelper;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class MainModule extends BaseModule {

    public MainModule(String baseUrl, String bwaDb, String bwaEndpoint, String kalignEndpoint,
                      String firestoreDatabaseUrl, String firestoreDestCollection, String resistantGenesFastaFile,
                      String resistantGenesListFile, String projectId) {
        super(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint, firestoreDatabaseUrl, firestoreDestCollection,
                resistantGenesFastaFile, resistantGenesListFile, projectId);
    }

    public static class Builder<T extends BaseModule> extends BaseModule.Builder {

        public MainModule build() {
            return new MainModule(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint,
                    firestoreDatabaseUrl, firestoreDestCollection, resistantGenesFastaFile, resistantGenesListFile,
                    projectId);
        }


        public MainModule buildWithPipelineOptions(MainLogicPipelineOptions mainLogicPipelineOptions) {
            return new MainModule(mainLogicPipelineOptions.getBaseUrl(),
                    mainLogicPipelineOptions.getBwaDatabase(),
                    mainLogicPipelineOptions.getBwaEndpoint(),
                    mainLogicPipelineOptions.getkAlignEndpoint(),
                    mainLogicPipelineOptions.getOutputDatastoreDbUrl().get(),
                    mainLogicPipelineOptions.getOutputDatastoreDbCollection().get(),
                    mainLogicPipelineOptions.getResistantGenesFastaFile().get(),
                    mainLogicPipelineOptions.getResistantGenesListFile().get(),
                    mainLogicPipelineOptions.getProject());
        }
    }


    @Provides
    @Singleton
    public NanostreamHttpService provideNanostreamHttpService(HttpHelper httpHelper) {
        return new NanostreamHttpService(httpHelper, baseUrl);
    }

    @Provides
    public MakeAlignmentViaHttpFn provideMakeAlignmentViaHttpFn(NanostreamHttpService service) {
        return new MakeAlignmentViaHttpFn(service, bwaDb, bwaEndpoint);
    }

    @Provides
    public ProceedKAlignmentFn provideProceedKAlignmentFn(NanostreamHttpService service) {
        return new ProceedKAlignmentFn(service, kalignEndpoint);
    }

    @Provides
    public WriteToFirestoreDbFn provideWriteToFirestoreDbFn() {
        return new WriteToFirestoreDbFn(firestoreDatabaseUrl, firestoreDestCollection, projectId);
    }

    @Provides
    public LoadGeneInfoTransform provideLoadGeneInfoTransform() {
        return new LoadGeneInfoTransform(resistantGenesFastaFile, resistantGenesListFile);
    }
}