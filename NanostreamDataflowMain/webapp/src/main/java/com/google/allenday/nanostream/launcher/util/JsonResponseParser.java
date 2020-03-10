package com.google.allenday.nanostream.launcher.util;

import com.google.allenday.nanostream.launcher.exception.BadRequestException;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static com.jayway.jsonpath.Criteria.where;
import static com.jayway.jsonpath.Filter.filter;
import static com.jayway.jsonpath.JsonPath.parse;
import static java.util.Collections.emptyList;

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


    public static List<Map<String, Object>> parseRunningJobs(String json) {
        // Json sample:
//    {
//      "jobs": [
//        {
//          "id": "2019-12-12_05_14_07-11307687861672664813",
//          "projectId": "nanostream-test1",
//          "name": "template-32a4ca21-24d5-41fe-b531-f551f5179cdf",
//          "type": "JOB_TYPE_STREAMING",
//          "currentState": "JOB_STATE_CANCELLING",
//          "currentStateTime": "2019-12-12T13:19:24.867468Z",
//          "requestedState": "JOB_STATE_CANCELLED",
//          "createTime": "2019-12-12T13:14:08.566549Z",
//          "location": "us-central1",
//          "jobMetadata": {
//            "sdkVersion": {
//              "version": "2.16.0",
//              "versionDisplayName": "Apache Beam SDK for Java",
//              "sdkSupportStatus": "SUPPORTED"
//            }
//          },
//          "startTime": "2019-12-12T13:14:08.566549Z"
//        }
//      ]
//    }

        // JsonPath docs: https://github.com/json-path/JsonPath
        try {
            return parse(json).read("$.jobs[?]", filter(
                    where("currentState").in("JOB_STATE_RUNNING", "JOB_STATE_PENDING", "JOB_STATE_QUEUED")
            ));
        } catch (PathNotFoundException e) {
            logger.warn("No jobs found: " + json);
            return emptyList();
        } catch (Exception e) {
            String message = "Unexpected json response: " + json;
            logger.error(message, e);
            throw new BadRequestException("UNKNOWN_ERROR", message);
        }
    }

}
