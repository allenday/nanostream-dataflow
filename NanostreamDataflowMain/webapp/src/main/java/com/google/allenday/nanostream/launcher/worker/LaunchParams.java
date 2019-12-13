package com.google.allenday.nanostream.launcher.worker;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

class LaunchParams {

    String outputCollectionNamePrefix;
    String outputDocumentNamePrefix;
    String pipelineName;
    String processingMode;

    LaunchParams(HttpServletRequest request) {
        outputCollectionNamePrefix = getParameter(request, "collection_name_prefix", "");
        outputDocumentNamePrefix = getParameter(request, "document_name_prefix", "");
        pipelineName  = getParameter(request, "pipeline_name", "template-" + UUID.randomUUID().toString());
        processingMode = getParameter(request, "processing_mode", "species");
    }

    private String getParameter(HttpServletRequest request, String paramName, String defaultValue) {
        String parameter = request.getParameter(paramName);
        if (parameter == null || parameter.isEmpty()) {
            parameter = defaultValue;
        }
        return parameter;
    }

}
