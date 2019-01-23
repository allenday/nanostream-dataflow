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

    @Description("The window duration in which FastQ records will be collected")
    @Default.Integer(60)
    Integer getDataCollectionWindow();

    void setDataCollectionWindow(Integer value);

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


    @Description("Collection name of the Firebase Datastore database that will be used for writing output data")
    @Validation.Required
    ValueProvider<String> getOutputFirestoreMainCollection();

    void setOutputFirestoreMainCollection(ValueProvider<String> value);

    //TODO
    @Description("")
    @Validation.Required
    ValueProvider<String> getOutputFirestoreSequencesStatisticCollection();

    void setOutputFirestoreSequencesStatisticCollection(ValueProvider<String> value);

    //TODO
    @Description("")
    @Validation.Required
    ValueProvider<String> getOutputFirestoreSequencesBodyCollection();

    void setOutputFirestoreSequencesBodyCollection(ValueProvider<String> value);

    //TODO
    @Description("")
    @Validation.Required
    ValueProvider<String> getOutputFirestoreGeneCacheCollection();

    void setOutputFirestoreGeneCacheCollection(ValueProvider<String> value);
}