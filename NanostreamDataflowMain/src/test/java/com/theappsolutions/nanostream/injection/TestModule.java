package com.theappsolutions.nanostream.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.theappsolutions.nanostream.aligner.AlignerHttpService;
import com.theappsolutions.nanostream.util.HttpHelper;
import org.mockito.Mockito;

public class TestModule extends AbstractModule {

    private String testDatabase;
    private String testServer;

    public TestModule(String testDatabase, String testServer) {
        this.testDatabase = testDatabase;
        this.testServer = testServer;
    }

    @Provides
    public AlignerHttpService provideAlignerHttpService(HttpHelper httpHelper) {
        return new AlignerHttpService(httpHelper, testDatabase, testServer);
    }

    @Provides
    @Singleton
    public HttpHelper provideHttpHelper() {
        return Mockito.mock(HttpHelper.class);
    }
}