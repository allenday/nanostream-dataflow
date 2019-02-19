package com.google.allenday.nanostream.debugging;

import com.google.allenday.nanostream.io.WindowedFilenamePolicy;
import com.google.allenday.nanostream.util.trasform.CombineIterableAccumulatorFn;
import org.apache.beam.runners.dataflow.DataflowRunner;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.FileBasedSink;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NanostreamDataflowDebug {

    private static Logger LOG = LoggerFactory.getLogger(NanostreamDataflowDebug.class);


    public static void main(String[] args) {
        DataflowPipelineOptions pipelineOptions = PipelineOptionsFactory.create().as(DataflowPipelineOptions.class);
        pipelineOptions.setRunner(DataflowRunner.class);
        pipelineOptions.setProject("upwork-nano-stream");
        pipelineOptions.setJobName("manultest" + System.currentTimeMillis());

        Pipeline testPipeline = Pipeline.create(pipelineOptions);

        testPipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription("projects/upwork-nano-stream/subscriptions/manual_test_topic_subscription"))
                .apply(ParDo.of(new DoFn<PubsubMessage, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        LOG.info(new String(c.element().getPayload()));
                        c.output(new String(c.element().getPayload()));
                    }
                }))
                .apply(Window.into(FixedWindows.of(Duration.standardSeconds(20))))
                .apply(Combine.globally(new CombineIterableAccumulatorFn<String>()).withoutDefaults())
                .apply(
                        Window.<Iterable<String>>into(
                                new GlobalWindows())
                                .triggering(Repeatedly.forever(AfterProcessingTime
                                        .pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(20))))
                                .withAllowedLateness(Duration.ZERO)
                                .accumulatingFiredPanes())
                .apply(Combine.globally(new CombineIterableAccumulatorFn<Iterable<String>>()))
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
                                (SerializableFunction<String, ResourceId>) FileBasedSink::convertToFileResourceIfPossible)));

        PipelineResult testResult = testPipeline.run();
        testResult.waitUntilFinish();
    }
}
