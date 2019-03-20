package com.google.allenday.nanostream.injection;

import com.google.allenday.nanostream.aligner.MakeAlignmentViaHttpFnCannabis;
import com.google.allenday.nanostream.http.NanostreamHttpService;
import com.google.allenday.nanostream.util.HttpHelper;
import com.google.inject.Provides;
import com.google.inject.Singleton;

//TODO

/**
 *
 */
public class MainCannabisModule extends NanostreamCannabisModule {

    public MainCannabisModule(Builder builder) {
        super(builder);
    }


    public static class Builder extends NanostreamCannabisModule.Builder {

        @Override
        public MainCannabisModule build() {
            return new MainCannabisModule(this);
        }
    }

    @Provides
    @Singleton
    public NanostreamHttpService provideNanostreamHttpService(HttpHelper httpHelper) {
        return new NanostreamHttpService(httpHelper, servicesUrl);
    }

    @Provides
    public MakeAlignmentViaHttpFnCannabis provideMakeAlignmentViaHttpFn(NanostreamHttpService service) {
        return new MakeAlignmentViaHttpFnCannabis(service, bwaDB, bwaEndpoint, bwaArguments);
    }
}
