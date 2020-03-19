package com.google.allenday.nanostream.launcher.data;

import java.util.List;

public class PipelineRequestParams {

    private String id;
    private String outputCollectionNamePrefix;
    private String outputDocumentNamePrefix;
    private String pipelineName;
    private String processingMode;
    private String inputDataSubscription;
    private String inputFolder;
    private Integer autoStopDelaySeconds;
    private Boolean pipelineAutoStart;
    private Boolean pipelineStartImmediately;
    private String uploadBucketName;
    private List<ReferenceDb> referenceDbs;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOutputCollectionNamePrefix() {
        return outputCollectionNamePrefix;
    }

    public void setOutputCollectionNamePrefix(String outputCollectionNamePrefix) {
        this.outputCollectionNamePrefix = outputCollectionNamePrefix;
    }

    public String getOutputDocumentNamePrefix() {
        return outputDocumentNamePrefix;
    }

    public void setOutputDocumentNamePrefix(String outputDocumentNamePrefix) {
        this.outputDocumentNamePrefix = outputDocumentNamePrefix;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public void setPipelineName(String pipelineName) {
        this.pipelineName = pipelineName;
    }

    public String getProcessingMode() {
        return processingMode;
    }

    public void setProcessingMode(String processingMode) {
        this.processingMode = processingMode;
    }

    public String getInputDataSubscription() {
        return inputDataSubscription;
    }

    public void setInputDataSubscription(String inputDataSubscription) {
        this.inputDataSubscription = inputDataSubscription;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public Integer getAutoStopDelaySeconds() {
        return autoStopDelaySeconds;
    }

    public void setAutoStopDelaySeconds(Integer autoStopDelaySeconds) {
        this.autoStopDelaySeconds = autoStopDelaySeconds;
    }

    public Boolean getPipelineAutoStart() {
        return pipelineAutoStart;
    }

    public void setPipelineAutoStart(Boolean pipelineAutoStart) {
        this.pipelineAutoStart = pipelineAutoStart;
    }

    public Boolean getPipelineStartImmediately() {
        return pipelineStartImmediately;
    }

    public void setPipelineStartImmediately(Boolean pipelineStartImmediately) {
        this.pipelineStartImmediately = pipelineStartImmediately;
    }

    public String getUploadBucketName() {
        return uploadBucketName;
    }

    public void setUploadBucketName(String uploadBucketName) {
        this.uploadBucketName = uploadBucketName;
    }

    public List<ReferenceDb> getReferenceDbs() {
        return referenceDbs;
    }

    public void setReferenceDbs(List<ReferenceDb> referenceDbs) {
        this.referenceDbs = referenceDbs;
    }
}
