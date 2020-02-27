package com.google.allenday.nanostream.launcher.util;

import com.google.allenday.nanostream.launcher.exception.BadRequestException;
import com.jayway.jsonpath.DocumentContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static com.jayway.jsonpath.JsonPath.parse;

public final class JsonResponseParser {

    private static final Logger logger = LoggerFactory.getLogger(JsonResponseParser.class);

    public static String extractJobId(String json) {
        // Request output sample:
        //        {
        //          "job": {
        //            "id": "2020-02-11_03_16_40-8884773552833626077",
        //            "projectId": "nanostream-test1",
        //            "name": "id123467",
        //            "type": "JOB_TYPE_STREAMING",
        //            "currentStateTime": "1970-01-01T00:00:00Z",
        //            "createTime": "2020-02-11T11:16:41.405546Z",
        //            "location": "us-central1",
        //            "startTime": "2020-02-11T11:16:41.405546Z"
        //          }
        //        }

        String jobId;
        try {
            DocumentContext document = parse(json);
            jobId = document.read("$.job.id");
        } catch (Exception e) {
            String message = "Unexpected json response: " + json;
            logger.error(message, e);
            throw new BadRequestException("UNKNOWN_ERROR", message);
        }
        logger.info("new job id: " + jobId);
        return jobId;
    }

}
