package com.google.allenday.nanostream.main.injection;

import com.google.allenday.genomics.core.align.KAlignService;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.nanostream.injection.NanostreamModule;
import com.google.allenday.nanostream.kalign.ProceedKAlignmentFn;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.util.HttpHelper;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import org.mockito.Mockito;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.withSettings;

public class TestModule extends NanostreamModule {

    public final static String TEST_BUCKET = "test_bucket";
    public final static String TEST_FOLDER = "test/folder";

    public TestModule(TestModule.Builder builder) {
        super(builder);
    }

    public static class Builder extends NanostreamModule.Builder {

        @Override
        public TestModule build() {
            return new TestModule(this);
        }
    }

    @Provides
    @Singleton
    public HttpHelper provideHttpHelper() {
        return Mockito.mock(HttpHelper.class);
    }


    @Provides
    public ProceedKAlignmentFn provideProceedKAlignmentFn() {
        return new ProceedKAlignmentFn(Mockito.mock(FileUtils.class), Mockito.mock(KAlignService.class));
    }

    @Provides
    public GCSSourceData provideGCSSourceData() {
        return new GCSSourceData(TEST_BUCKET, TEST_FOLDER);
    }
}