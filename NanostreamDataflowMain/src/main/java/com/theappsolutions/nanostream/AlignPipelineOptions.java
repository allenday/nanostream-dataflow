package com.theappsolutions.nanostream;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Default;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

public interface AlignPipelineOptions extends DataflowPipelineOptions {

    @Description("The window duration in which FastQ records will be collected")
    @Default.Integer(60)
    Integer getWindowTime();

    void setWindowTime(Integer value);


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
    String getKAlignEndpoint();

    void setKAlignEndpoint(String value);


    @Description("Base URL")
    @Validation.Required
    String getBaseUrl();

    void setBaseUrl(String value);
}