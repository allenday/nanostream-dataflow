package com.theappsolutions.nanostream.debugging;

import com.theappsolutions.nanostream.util.ObjectSizeFetcher;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.UUID;

public class NanostreamDataflowDebug {

    private static Logger LOG = LoggerFactory.getLogger(NanostreamDataflowDebug.class);


    public static void main(String[] args) {
        DataflowPipelineOptions pipelineOptions = PipelineOptionsFactory.create().as(DataflowPipelineOptions.class);
        pipelineOptions.setRunner(DataflowRunner.class);
        pipelineOptions.setProject("upwork-nano-stream");
        pipelineOptions.setJobName("manultest" + System.currentTimeMillis());

        Pipeline testPipeline = Pipeline.create(pipelineOptions);
        /*testPipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription("projects/upwork-nano-stream/subscriptions/manual_test_topic_subscription"))*/

        testPipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription("projects/upwork-nano-stream/subscriptions/manual_test_topic_subscription"))
                .apply(ParDo.of(new DoFn<PubsubMessage, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        StringBuilder b = new StringBuilder();
                        for (int i = 0; i < 1000000; i++) {
                            b.append(UUID.randomUUID().toString());
                        }
                        for (int i = 0; i < 100; i++) {
                            c.output(b.toString());
                        }
                    }
                }))
                .apply(ParDo.of(new DoFn<String, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        LOG.info("Output size: " + ObjectSizeFetcher.sizeOf(c.element()));
                    }
                }))
                /*.apply(ParDo.of(new AddValueDoFn<>()))
                .apply(
                        Window.<KV<String, List<String>>>into(
                                new GlobalWindows())
                                .triggering(Repeatedly.forever(AfterProcessingTime
                                        .pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(5))))
                                .withAllowedLateness(Duration.ZERO)
                                .accumulatingFiredPanes())
                .apply(Combine.globally(new KVCalculationAccumulatorFn()))
                .apply("Take only last pane", Window.<Map<String, SequenceCountAndTaxonomyData>>into(new GlobalWindows())
                        .triggering(Repeatedly.forever(AfterProcessingTime
                                .pastFirstElementInPane().plusDelayOf(Duration.ZERO)))
                        .withAllowedLateness(Duration.ZERO)
                        .discardingFiredPanes())
                .apply("toString()", ToString.elements())
                .apply("Write to GCS", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to(
                                new WindowedFilenamePolicy(
                                        "gs://nano-stream-test/manual_test",
                                        "output_",
                                        "W-P-SS-of-NN",
                                        ""))
                        .withTempDirectory(ValueProvider.NestedValueProvider.of(
                                ValueProvider.StaticValueProvider.of("gs://nano-stream-test/manual_test"),
                                (SerializableFunction<String, ResourceId>) FileBasedSink::convertToFileResourceIfPossible)))*/;
        PipelineResult testResult = testPipeline.run();
        testResult.waitUntilFinish();
    }
}
