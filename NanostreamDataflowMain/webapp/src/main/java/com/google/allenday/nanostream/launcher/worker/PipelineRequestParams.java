package com.google.allenday.nanostream.launcher.worker;

public class PipelineRequestParams {

    private String id;
    private String outputCollectionNamePrefix;
    private String outputDocumentNamePrefix;
    private String pipelineName;
    private String processingMode;
    private String inputDataSubscription;
    private String inputFolder;
    private String referenceNameList;
    private Boolean pipelineAutoStart;
    private Boolean pipelineStartImmediately;
    private String uploadBucketName;

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

    public String getReferenceNameList() {
        return referenceNameList;
    }

    public void setReferenceNameList(String referenceNameList) {
        this.referenceNameList = referenceNameList;
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
}
