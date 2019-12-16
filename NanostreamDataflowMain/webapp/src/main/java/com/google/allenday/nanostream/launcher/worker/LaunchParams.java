package com.google.allenday.nanostream.launcher.worker;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.http.HttpServletRequest;
import java.util.UUID;

public class LaunchParams {

    private static final Logger logger = LoggerFactory.getLogger(LaunchParams.class);

    String outputCollectionNamePrefix;
    String outputDocumentNamePrefix;
    String pipelineName;
    String processingMode;

    public LaunchParams(HttpServletRequest request) {
        outputCollectionNamePrefix = getParameter(request, "collection_name_prefix", "");
        outputDocumentNamePrefix = getParameter(request, "document_name_prefix", "");
        pipelineName  = getParameter(request, "pipeline_name", "template-" + UUID.randomUUID().toString());
        processingMode = getParameter(request, "processing_mode", "species");
    }

    private String getParameter(HttpServletRequest request, String paramName, String defaultValue) {
        String parameter = request.getParameter(paramName);

        logger.info(paramName);
        logger.info(parameter);
        logger.info(defaultValue);

        if (parameter == null || parameter.isEmpty()) {
            parameter = defaultValue;
        }
        return parameter;
    }

}
