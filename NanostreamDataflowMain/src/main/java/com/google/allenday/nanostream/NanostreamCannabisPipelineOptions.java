package com.google.allenday.nanostream;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

import static com.google.allenday.nanostream.other.Configuration.DEFAULT_ALIGNMENT_BATCH_SIZE;

/**
 * Provides list of {@link org.apache.beam.sdk.Pipeline} options
 * for implementation {@link NanostreamApp} Dataflow transformation
 */
public interface NanostreamCannabisPipelineOptions extends DataflowPipelineOptions {

    @Description("Aligner Pub Sub Topic id")
    @Validation.Required
    String getTopicId();

    void setTopicId(String value);


    @Description("Max size of batch that will be generated before alignment")
    @Default.Integer(DEFAULT_ALIGNMENT_BATCH_SIZE)
    int getAlignmentBatchSize();

    void setAlignmentBatchSize(int value);


    String getResultBucket();

    void setResultBucket(String value);


    String getSrсBucket();

    void setSrсBucket(String value);


    String getSamHeadersPath();

    void setSamHeadersPath(String value);
}
