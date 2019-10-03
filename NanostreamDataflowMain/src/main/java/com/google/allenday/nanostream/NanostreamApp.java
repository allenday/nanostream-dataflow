package com.google.allenday.nanostream;

import org.apache.beam.sdk.options.PipelineOptionsFactory;

/**
 * Main class of the Nanostream Dataflow App that provides dataflow pipeline
 * with transformation from PubsubMessage to Sequences Statistic and Sequences Bodies
 */
public class NanostreamApp {

    public static void main(String[] args) {
        NanostreamPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamPipelineOptions.class);

        new NanostreamPipeline(options).run();
    }

}
