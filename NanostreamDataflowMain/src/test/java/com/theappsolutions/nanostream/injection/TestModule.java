package com.theappsolutions.nanostream.injection;

import com.google.inject.Provides;
import com.google.inject.Singleton;
import com.theappsolutions.nanostream.AlignPipelineOptions;
import com.theappsolutions.nanostream.util.HttpHelper;
import org.mockito.Mockito;

public class TestModule extends MainModule {

    private TestModule(String baseUrl, String bwaDb, String bwaEndpoint, String kalignEndpoint) {
        super(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint);
    }
    public static class Builder extends MainModule.Builder{

        public TestModule build() {
            return new TestModule(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint);
        }
    }

    @Provides
    @Singleton
    public HttpHelper provideHttpHelper() {
        return Mockito.mock(HttpHelper.class);
    }
}