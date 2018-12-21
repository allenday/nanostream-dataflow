package com.theappsolutions.nanostream.injection;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.theappsolutions.nanostream.AlignPipelineOptions;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.http.NanostreamHttpService;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.output.WriteToFirestoreDbFn;
import com.theappsolutions.nanostream.util.HttpHelper;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class MainModule extends BaseModule {

    private MainModule(String baseUrl, String bwaDb, String bwaEndpoint, String kalignEndpoint,
                       String firestoreDatabaseUrl, String firestoreDestCollection, String projectId) {
        super(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint, firestoreDatabaseUrl, firestoreDestCollection, projectId);
    }

    public static class Builder<T extends BaseModule> extends BaseModule.Builder {

        public MainModule build() {
            return new MainModule(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint,
                    firestoreDatabaseUrl, firestoreDestCollection, projectId);
        }


        public MainModule buildWithPipelineOptions(AlignPipelineOptions alignPipelineOptions) {
            return new MainModule(alignPipelineOptions.getBaseUrl(),
                    alignPipelineOptions.getBwaDatabase(),
                    alignPipelineOptions.getBwaEndpoint(),
                    alignPipelineOptions.getkAlignEndpoint(),
                    alignPipelineOptions.getOutputDatastoreDbUrl().get(),
                    alignPipelineOptions.getOutputDatastoreDbCollection().get(),
                    alignPipelineOptions.getProject());
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
}