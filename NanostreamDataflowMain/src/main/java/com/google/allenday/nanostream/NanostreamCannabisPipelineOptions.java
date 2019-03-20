package com.google.allenday.nanostream;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

import static com.google.allenday.nanostream.other.Configuration.*;

/**
 * Provides list of {@link org.apache.beam.sdk.Pipeline} options
 * for implementation {@link NanostreamApp} Dataflow transformation
 */
public interface NanostreamCannabisPipelineOptions extends DataflowPipelineOptions {

    @Description("Base URL")
    @Validation.Required
    String getServicesUrl();

    void setServicesUrl(String value);

    @Description("BWA Alignment server endpoint to use")
    @Validation.Required
    String getBwaEndpoint();

    void setBwaEndpoint(String value);


    @Description("BWA Alignment database")
    @Validation.Required
    String getBwaDatabase();

    void setBwaDatabase(String value);


    @Description("Max size of batch that will be generated before alignment")
    @Default.Integer(DEFAULT_ALIGNMENT_BATCH_SIZE)
    int getAlignmentBatchSize();

    void setAlignmentBatchSize(int value);

    @Description("Arguments that will be passed to BWA aligner")
    @Default.String(DEFAULT_BWA_ARGUMENTS)
    String getBwaArguments();

    void setBwaArguments(String value);
}
