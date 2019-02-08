package com.theappsolutions.nanostream;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;
import org.apache.beam.sdk.options.ValueProvider;

/**
 * Provides list of {@link org.apache.beam.sdk.Pipeline} options
 * for implementation {@link NanostreamApp} Dataflow transformation
 */
public interface NanostreamPipelineOptions extends DataflowPipelineOptions {

    @Description("GCP PubSub subscription name to read messages from")
    @Validation.Required
    String getInputDataSubscription();

    void setInputDataSubscription(String value);


    @Description("Size of the Window in which FastQ records will be collected for Alignment")
    @Default.Integer(60)
    Integer getAlignmentWindow();

    void setAlignmentWindow(Integer value);

    @Description("Delay between updating output statistic data")
    @Default.Integer(60)
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


    @Description("Url of the Firebase Datastore database that will be used for writing output data")
    @Validation.Required
    ValueProvider<String> getOutputFirestoreDbUrl();

    void setOutputFirestoreDbUrl(ValueProvider<String> value);


    @Description("Collection name of the Firestore database that will be used for writing output statistic data")
    @Validation.Required
    ValueProvider<String> getOutputFirestoreSequencesStatisticCollection();

    void setOutputFirestoreSequencesStatisticCollection(ValueProvider<String> value);


    @Description("Collection name of the Firestore database that will be used for writing output Sequences Body data")
    @Validation.Required
    ValueProvider<String> getOutputFirestoreSequencesBodiesCollection();

    void setOutputFirestoreSequencesBodiesCollection(ValueProvider<String> value);


    @Description("Collection name of the Firestore database that will be used for saving NCBI genome data cache")
    @Validation.Required
    ValueProvider<String> getOutputFirestoreGeneCacheCollection();

    void setOutputFirestoreGeneCacheCollection(ValueProvider<String> value);


    @Description("Variable that specifies \"species\" or \"resistant_genes\" mode of data processing")
    @Validation.Required
    @Default.String("species")
    String getProcessingMode();

    void setProcessingMode(String value);


    @Description("Path to fasta file with resistant genes database")
    String getResistantGenesFastDB();

    void setResistantGenesFastDB(String value);


    @Description("Path to fasta file with resistant genes list")
    String getResistantGenesList();

    void setResistantGenesList(String value);

    @Description("Prefix for Firestore collections names that used for output")
    String getOutputFirestoreCollectionNamePrefix();

    void setOutputFirestoreCollectionNamePrefix(String value);
}