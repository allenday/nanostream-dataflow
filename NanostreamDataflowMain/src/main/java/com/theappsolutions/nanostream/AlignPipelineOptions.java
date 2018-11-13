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

    @Description("Alignment server to use")
    @Validation.Required
    String getAlignmentServer();

    void setAlignmentServer(String value);

    @Description("Alignment database")
    @Validation.Required
    String getAlignmentDatabase();

    void setRAlignmentDatabase(String value);
}