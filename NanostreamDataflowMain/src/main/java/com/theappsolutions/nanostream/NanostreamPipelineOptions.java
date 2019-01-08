package com.theappsolutions.nanostream;

import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

/**
 * Provides list of {@link org.apache.beam.sdk.Pipeline} options
 * for implementation {@link NanostreamApp} Dataflow transformation
 */
public interface NanostreamPipelineOptions extends MainLogicPipelineOptions {

    @Description("GCP PubSub subscription name to read messages from")
    @Validation.Required
    String getSubscription();

    void setSubscription(String value);

    @Description("The maximum number of output shards produced when writing.")
    @Default.Integer(1)
    Integer getNumShards();

    void setNumShards(Integer value);

    @Description("The directory to output files to. Must end with a slash.")
    @Validation.Required
    ValueProvider<String> getOutputDirectory();

    void setOutputDirectory(ValueProvider<String> value);

    @Description("The filename prefix of the files to write to.")
    @Default.String("output_file_")
    @Validation.Required
    ValueProvider<String> getOutputFilenamePrefix();

    void setOutputFilenamePrefix(ValueProvider<String> value);

    @Description("The suffix of the files to write.")
    @Default.String("")
    ValueProvider<String> getOutputFilenameSuffix();

    void setOutputFilenameSuffix(ValueProvider<String> value);

    @Description("The shard template of the output file. Specified as repeating sequences "
            + "of the letters 'S' or 'N' (example: SSS-NNN). These are replaced with the "
            + "shard number, or number of shards respectively")
    @Default.String("W-P-SS-of-NN")
    ValueProvider<String> getOutputShardTemplate();

    void setOutputShardTemplate(ValueProvider<String> value);
}