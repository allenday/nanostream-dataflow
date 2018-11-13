package com.theappsolutions.nanostream.injection;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.theappsolutions.nanostream.AlignPipelineOptions;
import com.theappsolutions.nanostream.aligner.AlignerHttpService;
import com.theappsolutions.nanostream.util.HttpHelper;

public class MainModule extends AbstractModule {

    private AlignPipelineOptions pipelineOptions;

    public MainModule(AlignPipelineOptions pipelineOptions) {
        this.pipelineOptions = pipelineOptions;
    }

    @Provides
    public AlignerHttpService provideAlignerHttpService(HttpHelper httpHelper) {
        return new AlignerHttpService(httpHelper, pipelineOptions.getAlignmentDatabase(),
                pipelineOptions.getAlignmentServer());
    }
}