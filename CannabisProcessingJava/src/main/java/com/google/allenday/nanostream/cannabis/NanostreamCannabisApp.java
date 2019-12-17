package com.google.allenday.nanostream.cannabis;

import com.google.allenday.genomics.core.csv.ParseSourceCsvTransform;
import com.google.allenday.genomics.core.pipeline.PipelineSetupUtils;
import com.google.allenday.genomics.core.processing.AlignAndPostProcessTransform;
import com.google.allenday.genomics.core.utils.NameProvider;
import com.google.allenday.nanostream.cannabis.di.NanostreamCannabisModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;

import java.util.List;

public class NanostreamCannabisApp {

    private final static String JOB_NAME_PREFIX = "nanostream-cannabis--";

    public static boolean hasFilter(List<String> sraSamplesToFilter) {
        return sraSamplesToFilter != null && sraSamplesToFilter.size() > 0;
    }

    public static void main(String[] args) {

        NanostreamCannabisPipelineOptions pipelineOptions = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamCannabisPipelineOptions.class);
        PipelineSetupUtils.prepareForInlineAlignment(pipelineOptions);

        Injector injector = Guice.createInjector(new NanostreamCannabisModule.Builder()
                .setFromOptions(pipelineOptions)
                .build());

        NameProvider nameProvider = injector.getInstance(NameProvider.class);
        pipelineOptions.setJobName(nameProvider.buildJobName(JOB_NAME_PREFIX, pipelineOptions.getSraSamplesToFilter()));

        Pipeline pipeline = Pipeline.create(pipelineOptions);

        pipeline
                .apply("Parse data", injector.getInstance(ParseSourceCsvTransform.class))
                .apply("Align reads and prepare for DV", injector.getInstance(AlignAndPostProcessTransform.class))
        ;

        pipeline.run();
    }
}
