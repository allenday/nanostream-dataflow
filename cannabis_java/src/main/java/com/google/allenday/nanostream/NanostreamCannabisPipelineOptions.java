package com.google.allenday.nanostream;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.Description;
import org.apache.beam.sdk.options.Validation;

import java.util.List;

public interface NanostreamCannabisPipelineOptions extends DataflowPipelineOptions {

    @Description("GCS uri of CSV file with input data")
    @Validation.Required
    String getInputCsvUri();

    void setInputCsvUri(String value);


    @Description("Name of GCS bucket with all source data")
    @Validation.Required
    String getSrcBucket();

    void setSrcBucket(String value);

    @Description("Name of GCS bucket with all resuts data")
    @Validation.Required
    String getResultBucket();

    void setResultBucket(String value);

    @Description("Fasta gene reference names list")
    @Validation.Required
    List<String> getReferenceNamesList();

    void setReferenceNamesList(List<String> value);


    @Description("SRA samples to filter")
    List<String> getSraSamplesToFilter();

    void setSraSamplesToFilter(List<String> value);

    @Description("GCS dir path with references")
    String getAllReferencesDirGcsUri();

    void setAllReferencesDirGcsUri(String value);

    @Description("GCS dir path of previous run align output")
    String getPreviousAlignedOutputDir();

    void setPreviousAlignedOutputDir(String value);

    @Description("GCS dir path for align output")
    String getAlignedOutputDir();

    void setAlignedOutputDir(String value);

    @Description("GCS dir path for sor output")
    String getSortedOutputDir();

    void setSortedOutputDir(String value);

    @Description("GCS dir path for merge output")
    String getMergedOutputDir();

    void setMergedOutputDir(String value);

    @Description("GCS path for logging anomaly examples")
    String getAnomalyOutputPath();

    void setAnomalyOutputPath(String value);

    @Description("Threshold to decide how to pass data between transforms")
    long getMemoryOutputLimit();

    void setMemoryOutputLimit(long value);
}
