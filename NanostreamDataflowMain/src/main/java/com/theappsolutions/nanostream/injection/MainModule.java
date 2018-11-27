package com.theappsolutions.nanostream.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.theappsolutions.nanostream.AlignPipelineOptions;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.http.NanostreamHttpService;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.util.HttpHelper;

/**
 * App dependency injection module, that provide graph of main dependencies in app
 */
public class MainModule extends AbstractModule {

    private String baseUrl;
    private String bwaDb;
    private String bwaEndpoint;
    private String kalignEndpoint;

    MainModule(String baseUrl, String bwaDb, String bwaEndpoint, String kalignEndpoint) {
        this.baseUrl = baseUrl;
        this.bwaDb = bwaDb;
        this.bwaEndpoint = bwaEndpoint;
        this.kalignEndpoint = kalignEndpoint;
    }

    public static class Builder {

        String baseUrl;
        String bwaDb;
        String bwaEndpoint;
        String kalignEndpoint;

        public Builder setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder setBwaDb(String bwaDb) {
            this.bwaDb = bwaDb;
            return this;
        }

        public Builder setBwaEndpoint(String bwaEndpoint) {
            this.bwaEndpoint = bwaEndpoint;
            return this;
        }

        public Builder setKalignEndpoint(String kalignEndpoint) {
            this.kalignEndpoint = kalignEndpoint;
            return this;
        }

        public MainModule build() {
            return new MainModule(baseUrl, bwaDb, bwaEndpoint, kalignEndpoint);
        }

        public MainModule buildWithPipelineOptions(AlignPipelineOptions alignPipelineOptions) {
            return new MainModule(alignPipelineOptions.getBaseUrl(),
                    alignPipelineOptions.getBwaDatabase(),
                    alignPipelineOptions.getBwaEndpoint(),
                    alignPipelineOptions.getkAlignEndpoint());
        }
    }

    @Provides
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
}