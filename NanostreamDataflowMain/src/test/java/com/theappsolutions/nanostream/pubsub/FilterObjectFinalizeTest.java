package com.theappsolutions.nanostream.pubsub;

import com.theappsolutions.nanostream.pubsub.FilterObjectFinalizeMessage;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Random;
import java.util.stream.StreamSupport;

/**
 * Tests filtering of PubSub messages
 */
public class FilterObjectFinalizeTest {

    private final static String EVENT_TYPE_KEY = "eventType";
    private final static String EVENT_TYPE_OBJECT_FINALIZE = "OBJECT_FINALIZE";
    private final static String EVENT_TYPE_OBJECT_DELETE = "OBJECT_DELETE";

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testDuplicatedMessageIsEliminated() {
        PubsubMessage msgFinalize1 = new PubsubMessage(generateRandomByteArray(), new HashMap<String, String>() {{
            put(EVENT_TYPE_KEY, EVENT_TYPE_OBJECT_FINALIZE);
        }});
        PubsubMessage msgDelete1 = new PubsubMessage(generateRandomByteArray(), new HashMap<String, String>() {{
            put(EVENT_TYPE_KEY, EVENT_TYPE_OBJECT_DELETE);
        }});
        PubsubMessage msgFinalize2 = new PubsubMessage(generateRandomByteArray(), new HashMap<String, String>() {{
            put(EVENT_TYPE_KEY, EVENT_TYPE_OBJECT_FINALIZE);
        }});

        PCollection<PubsubMessage> filteredMessages = testPipeline
                .apply(Create.of(msgFinalize1, msgDelete1, msgFinalize2))
                .apply(ParDo.of(new FilterObjectFinalizeMessage()));

        PAssert
                .that(filteredMessages)
                .satisfies((SerializableFunction<Iterable<PubsubMessage>, Void>) input -> {
                    Assert.assertTrue(
                            StreamSupport
                                    .stream(input.spliterator(), false)
                                    .map(PubsubMessage::getAttributeMap)
                                    .filter(map -> !map.containsKey(EVENT_TYPE_KEY) ||
                                            !map.get(EVENT_TYPE_KEY).equals(EVENT_TYPE_OBJECT_FINALIZE))
                                    .count() == 0
                    );
                    return null;
                });

        testPipeline.run();
    }

    private byte[] generateRandomByteArray() {
        Random rand = new Random();

        byte[] bytes = new byte[10];
        rand.nextBytes(bytes);
        return bytes;
    }
}
