package com.google.allenday.nanostream.launcher.worker;

import java.util.ArrayList;
import java.util.List;

public class PipelineEntity {
    private String id;
    private String pipelineName;
    private String inputFolder;
    private String outputCollectionNamePrefix;
    private String processingMode;
    private String inputDataSubscription;
    private String referenceNameList;
    private Integer autoStopDelaySeconds;
    private Boolean pipelineAutoStart;
    private String uploadBucketName;
    private String createdAt;
    private String updatedAt;
    private List<String> jobIds = new ArrayList<>();
    private String lockStatus = "UNLOCKED";  // Possible values: LOCKED, UNLOCKED
    private String version = "1";  // use a version or github commit to identify pipeline version


    public PipelineEntity() {
        // Default constructor required for deserialize
    }

    public PipelineEntity(PipelineRequestParams pipelineRequestParams) {
        this.pipelineName = pipelineRequestParams.getPipelineName();
        this.inputFolder = pipelineRequestParams.getInputFolder();
        this.processingMode = pipelineRequestParams.getProcessingMode();
        this.inputDataSubscription = pipelineRequestParams.getInputDataSubscription();
        this.uploadBucketName = pipelineRequestParams.getUploadBucketName();
        this.referenceNameList = pipelineRequestParams.getReferenceNameList();
        this.autoStopDelaySeconds = pipelineRequestParams.getAutoStopDelaySeconds();
        this.pipelineAutoStart = pipelineRequestParams.getPipelineAutoStart();
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getPipelineName() {
        return pipelineName;
    }

    public String getInputFolder() {
        return inputFolder;
    }

    public void setInputFolder(String inputFolder) {
        this.inputFolder = inputFolder;
    }

    public String getOutputCollectionNamePrefix() {
        return outputCollectionNamePrefix;
    }

    public void setOutputCollectionNamePrefix(String outputCollectionNamePrefix) {
        this.outputCollectionNamePrefix = outputCollectionNamePrefix;
    }

    public String getProcessingMode() {
        return processingMode;
    }

    public String getInputDataSubscription() {
        return inputDataSubscription;
    }

    public String getUploadBucketName() {
        return uploadBucketName;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }

    public List<String> getJobIds() {
        return jobIds;
    }

    public String getReferenceNameList() {
        return referenceNameList;
    }

    public Integer getAutoStopDelaySeconds() {
        return autoStopDelaySeconds;
    }

    public Boolean getPipelineAutoStart() {
        return pipelineAutoStart;
    }

    public String getLockStatus() {
        return lockStatus;
    }

    public void setLockStatus(String lockStatus) {
        this.lockStatus = lockStatus;
    }

    public String getVersion() {
        return version;
    }
}
