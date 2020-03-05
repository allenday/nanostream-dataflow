package com.google.allenday.nanostream.pipeline;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.pubsub.v1.PubsubMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class PipelineManagerService implements Serializable {
    private static final String PROJECT_ID_KEY = "project_id";
    private static final String JOB_NAME_KEY = "job_name";
    private Logger LOG = LoggerFactory.getLogger(PipelineManagerService.class);
    private String topicName;

    public PipelineManagerService(String topicName) {
        this.topicName = topicName;
    }

    public void sendStopPipelineCommand(String project, String jobName) throws IOException {
        Publisher publisher = Publisher.newBuilder(topicName).build();

        Map<String, String> attr = new HashMap<String, String>();
        attr.put(PROJECT_ID_KEY, project);
        attr.put(JOB_NAME_KEY, jobName);

        PubsubMessage msg = PubsubMessage.newBuilder().putAllAttributes(attr).build();

        ApiFuture<String> resultFuture = publisher.publish(msg);

        ApiFutures.addCallback(resultFuture, new ApiFutureCallback<String>() {
            public void onSuccess(String messageId) {
                LOG.info("published with message id: " + messageId);
            }

            public void onFailure(Throwable t) {
                LOG.info("failed to publish: " + t);
            }
        }, MoreExecutors.directExecutor());
    }
}
