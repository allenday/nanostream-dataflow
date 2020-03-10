package com.google.allenday.nanostream;

import com.google.allenday.genomics.core.pipeline.GenomicsPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

import static com.google.allenday.nanostream.Configuration.*;

/**
 * Provides list of {@link org.apache.beam.sdk.Pipeline} options
 * for implementation {@link NanostreamPipeline} Dataflow transformation
 */
public interface NanostreamPipelineOptions extends GenomicsPipelineOptions {

    @Description("GCP PubSub subscription name to read messages from")
    @Validation.Required
    ValueProvider<String> getInputDataSubscription();

    void setInputDataSubscription(ValueProvider<String> value);


    @Description("Size of the Window in which FastQ records will be collected for Alignment")
    @Default.Integer(DEFAULT_ALIGNMENT_WINDOW)
    Integer getAlignmentWindow();

    void setAlignmentWindow(Integer value);

    @Description("Delay between updating output statistic data")
    @Default.Integer(DEFAULT_STATIC_UPDATING_DELAY)
    Integer getStatisticUpdatingDelay();

    void setStatisticUpdatingDelay(Integer value);

    @Description("Variable that specifies \"species\" or \"resistance_genes\" mode of data processing")
    @Validation.Required
    @Default.String("species")
    String getProcessingMode();

    void setProcessingMode(String value);


    @Description("Path to fasta file with resistance genes list")
    String getResistanceGenesList();

    void setResistanceGenesList(String value);

    @Description("Prefix for Firestore collections names that used for output")
    ValueProvider<String> getOutputCollectionNamePrefix();

    void setOutputCollectionNamePrefix(ValueProvider<String> value);


    @Description("Prefix for Firestore statistic result document")
    ValueProvider<String> getOutputDocumentNamePrefix();

    void setOutputDocumentNamePrefix(ValueProvider<String> value);


    @Description("Max size of batch that will be generated before alignment")
    @Default.Integer(DEFAULT_ALIGNMENT_BATCH_SIZE)
    int getAlignmentBatchSize();

    void setAlignmentBatchSize(int value);

    @Description("Time period in seconds after which pipeline will be automatically stopped")
    @Default.Integer(Integer.MAX_VALUE)
    ValueProvider<Integer> getAutoStopDelay();

    void setAutoStopDelay(ValueProvider<Integer> value);

    @Description("PubSub topic for triggering autostop GCF")
    String getAutoStopTopic();

    void setAutoStopTopic(String value);

    @Description("Max size of batch that will be generated before writing to statistic results")
    @Default.Integer(DEFAULT_STATISTIC_OUTPUT_BATCH_SIZE)
    Integer getStatisticOutputCountTriggerSize();

    void setStatisticOutputCountTriggerSize(Integer value);


    @Description("JobName value provider to access from PTransforms")
    ValueProvider<String> getJobNameLabel();

    void setJobNameLabel(ValueProvider<String> value);


    @Description("Specifies if need to wait for data to init AutoStop timer")
    @Default.Boolean(false)
    Boolean getInitAutoStopOnlyIfDataPassed();

    void setInitAutoStopOnlyIfDataPassed(Boolean value);
}
