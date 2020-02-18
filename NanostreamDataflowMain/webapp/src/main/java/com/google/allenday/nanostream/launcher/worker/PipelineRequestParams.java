package com.google.allenday.nanostream.launcher.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

import static com.google.common.base.Preconditions.checkNotNull;
import static java.lang.String.format;
import static org.hibernate.validator.internal.util.Contracts.assertNotEmpty;

public class PipelineRequestParams {

    private static final Logger logger = LoggerFactory.getLogger(PipelineRequestParams.class);

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


////    public PipelineRequestParams(HttpServletRequest request) {
////        outputCollectionNamePrefix = getParameter(request, "collectionNamePrefix", "");
////        outputDocumentNamePrefix = getParameter(request, "documentNamePrefix", "");
////        pipelineName  = getParameter(request, "pipeline_name", "template-" + UUID.randomUUID().toString());
////        processingMode = getParameter(request, "processing_mode", "species");
////        inputDataSubscription = getParameter(request, "inputDataSubscription", "");
////        inputFolder = getParameter(request, "input_folder", "");
////        referenceNameList = getParameter(request, "reference_name_list", "");
////        pipelineAutoStart = getParameter(request, "pipeline_autostart", "true");
////        pipelineStartImmediately = getParameter(request, "pipeline_start_immediately", "false");
////        uploadBucketName = getParameter(request, "upload_bucket_name", "");
////    }
//
//    private String getParameter(HttpServletRequest request, String paramName, String defaultValue) {
//        String parameter = request.getParameter(paramName);
//
//        logger.info(format("parameter %s, value: %s, default value: %s", paramName, parameter, defaultValue));
//
//        if (parameter == null || parameter.isEmpty()) {
//            parameter = defaultValue;
//        }
//        return parameter;
//    }

//    public String getOutputCollectionNamePrefix() {
//        return outputCollectionNamePrefix;
//    }
//
//    public String getOutputDocumentNamePrefix() {
//        return outputDocumentNamePrefix;
//    }
//
//    public String getPipelineName() {
//        return pipelineName;
//    }
//
//    public String getProcessingMode() {
//        return processingMode;
//    }
//
//    public String getInputDataSubscription() {
//        return inputDataSubscription;
//    }
//
//    public String getInputFolder() {
//        return inputFolder;
//    }
//
//    public String getUploadBucketName() {
//        return uploadBucketName;
//    }
//
//    public String getReferenceNameList() {
//        return referenceNameList;
//    }
//
//    public String getPipelineAutoStart() {
//        return pipelineAutoStart;
//    }
//
//    public String getPipelineStartImmediately() {
//        return pipelineStartImmediately;
//    }


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
