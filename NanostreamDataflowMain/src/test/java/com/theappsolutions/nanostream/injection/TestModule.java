package com.theappsolutions.nanostream.injection;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.http.NanostreamHttpService;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.util.HttpHelper;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class TestModule extends BaseModule {

    public TestModule(String baseUrl, String bwaDb, String bwaEndpoint, String kalignEndpoint,
                      String firestoreDatabaseUrl, String firestoreDestCollection, String resistantGenesFastaFile,
                      String resistantGenesListFile, String projectId) {
        super(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint, firestoreDatabaseUrl, firestoreDestCollection,
                resistantGenesFastaFile, resistantGenesListFile, projectId);
    }

    public static class Builder extends BaseModule.Builder {

    }

    @Provides
    @Singleton
    public HttpHelper provideHttpHelper() {
        return Mockito.mock(HttpHelper.class);
    }

    @Provides
    @Singleton
    public NanostreamHttpService provideNanostreamHttpService(HttpHelper httpHelper) {
        return mock(NanostreamHttpService.class,
                withSettings().serializable());
    }

    @Provides
    public MakeAlignmentViaHttpFn provideMakeAlignmentViaHttpFn(NanostreamHttpService service) {
        return new MakeAlignmentViaHttpFn(service, bwaDb, bwaEndpoint);
    }

    @Provides
    public ProceedKAlignmentFn provideProceedKAlignmentFn(NanostreamHttpService service) {
        return new ProceedKAlignmentFn(service, kalignEndpoint);
    }

}