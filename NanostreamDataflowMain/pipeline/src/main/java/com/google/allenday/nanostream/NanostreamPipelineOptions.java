package com.google.allenday.nanostream;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

import static com.google.allenday.nanostream.other.Configuration.*;

/**
 * Provides list of {@link org.apache.beam.sdk.Pipeline} options
 * for implementation {@link NanostreamPipeline} Dataflow transformation
 */
public interface NanostreamPipelineOptions extends DataflowPipelineOptions {

    @Description("GCP PubSub subscription name to read messages from")
    @Validation.Required
    String getInputDataSubscription();

    void setInputDataSubscription(String value);


    @Description("Size of the Window in which FastQ records will be collected for Alignment")
    @Default.Integer(DEFAULT_ALIGNMENT_WINDOW)
    Integer getAlignmentWindow();

    void setAlignmentWindow(Integer value);

    @Description("Delay between updating output statistic data")
    @Default.Integer(DEFAULT_STATIC_UPDATING_DELAY)
    Integer getStatisticUpdatingDelay();

    void setStatisticUpdatingDelay(Integer value);


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


    @Description("K-Align server endpoint to use")
    @Validation.Required
    String getkAlignEndpoint();

    void setkAlignEndpoint(String value);


    @Description("Variable that specifies \"species\" or \"resistance_genes\" mode of data processing")
    @Validation.Required
    @Default.String("species")
    String getProcessingMode();

    void setProcessingMode(String value);


    @Description("Path to fasta file with resistance genes list")
    String getResistanceGenesList();

    void setResistanceGenesList(String value);

    @Description("Prefix for Firestore collections names that used for output")
    String getOutputCollectionNamePrefix();

    void setOutputCollectionNamePrefix(String value);


    @Description("Prefix for Firestore statistic result document")
    String getOutputDocumentNamePrefix();

    void setOutputDocumentNamePrefix(String value);


    @Description("Max size of batch that will be generated before alignment")
    @Default.Integer(DEFAULT_ALIGNMENT_BATCH_SIZE)
    int getAlignmentBatchSize();

    void setAlignmentBatchSize(int value);

    @Description("Arguments that will be passed to BWA aligner")
    @Default.String(DEFAULT_BWA_ARGUMENTS)
    String getBwaArguments();

    void setBwaArguments(String value);
}
