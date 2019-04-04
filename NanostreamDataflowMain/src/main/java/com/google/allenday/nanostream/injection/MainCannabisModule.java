package com.google.allenday.nanostream.injection;

import com.google.allenday.nanostream.aligner.ComposeAlignedDataDoFn;
import com.google.allenday.nanostream.aligner.MakeAlignmentViaPubSubDoFn;
import com.google.allenday.nanostream.aligner.SaveInterleavedFastQDataToGCSDoFn;
import com.google.inject.Provides;

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
    public MakeAlignmentViaPubSubDoFn provideMakeAlignmentViaHttpFn() {
        return new MakeAlignmentViaPubSubDoFn(projectId, topicId);
    }


    @Provides
    public SaveInterleavedFastQDataToGCSDoFn provideSaveInterleavedFastQDataToGCSDoFn() {
        return new SaveInterleavedFastQDataToGCSDoFn(resultBucket);
    }


    @Provides
    public ComposeAlignedDataDoFn provideComposeAlignedDataDoFn() {
        return new ComposeAlignedDataDoFn(resultBucket, srсBucket, samHeadersPath);
    }
}
