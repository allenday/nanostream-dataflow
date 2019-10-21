package com.google.allenday.nanostream.cannabis;

import com.google.allenday.genomics.core.pipeline.AlignerPipelineOptions;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

import java.util.List;

public interface NanostreamCannabisPipelineOptions extends AlignerPipelineOptions {

    @Description("GCS uri of CSV file with input data")
    @Validation.Required
    String getInputCsvUri();

    void setInputCsvUri(String value);

    @Description("SRA samples to filter")
    List<String> getSraSamplesToFilter();

    void setSraSamplesToFilter(List<String> value);

    @Description("GCS path for logging anomaly examples")
    String getAnomalyOutputPath();

    void setAnomalyOutputPath(String value);

}