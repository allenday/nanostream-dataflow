package com.google.allenday.nanostream.gcloud;

import com.google.api.core.ApiFuture;
import com.google.api.core.ApiFutureCallback;
import com.google.api.core.ApiFutures;
import com.google.cloud.pubsub.v1.Publisher;
import com.google.cloud.storage.Storage;
import com.google.common.util.concurrent.MoreExecutors;
import com.google.protobuf.ByteString;
import com.google.pubsub.v1.ProjectTopicName;
import com.google.pubsub.v1.PubsubMessage;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * Provides access to {@link Storage} instance with convenient interface
 */
public class PubSubService {


    public PubSubService() {
    }

    public static PubSubService initialize() {
        return new PubSubService();
    }

    public void publishMessage(String projectId, String topicId, Map<String, String> attributes, ApiFutureCallback<String> callback) throws Exception {
        publishMessage(projectId, topicId, null, attributes, callback);
    }

    public void publishMessage(String projectId, String topicId, String message, Map<String, String> attributes,
                               ApiFutureCallback<String> callback) throws Exception {
        ProjectTopicName topicName = ProjectTopicName.of(projectId, topicId);
        Publisher publisher = null;

        try {
            // Create a publisher instance with default settings bound to the topic
            publisher = Publisher.newBuilder(topicName).build();

            PubsubMessage.Builder builder = PubsubMessage.newBuilder()
                    .putAllAttributes(attributes);
            Optional.ofNullable(message).ifPresent(msg -> builder.setData(ByteString.copyFromUtf8(msg)));
            PubsubMessage pubsubMessage = builder.build();

            // Once published, returns a server-assigned message id (unique within the topic)
            ApiFuture<String> future = publisher.publish(pubsubMessage);

            // Add an asynchronous callback to handle success / failure
            ApiFutures.addCallback(future, callback, MoreExecutors.directExecutor());
        } finally {
            if (publisher != null) {
                // When finished with the publisher, shutdown to free up resources.
                publisher.shutdown();
                publisher.awaitTermination(1, TimeUnit.MINUTES);
            }
        }
    }
}
