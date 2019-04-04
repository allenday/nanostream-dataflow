package com.google.allenday.nanostream.debugging;

import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.util.CoderUtils;
import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NanostreamDirectDebug {

    private static Logger LOG = LoggerFactory.getLogger(NanostreamDirectDebug.class);


    public static void main(String[] args) {
        PipelineOptions pipelineOptions = PipelineOptionsFactory.create().as(PipelineOptions.class);
        pipelineOptions.setRunner(DirectRunner.class);
        Pipeline testPipeline = Pipeline.create(pipelineOptions);
        CoderUtils.setupCoders(testPipeline, new SequenceOnlyDNACoder()/*, new GCSSourceDataCoder()*/);


        testPipeline.apply(Create.of("1","2","1"))
                .apply(ParDo.of(new DoFn<String, KV<KV<GCSSourceData, String>, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String element = c.element();
                        c.output(KV.of(KV.of(new GCSSourceData("bucket", "folder"), element), element));
                    }
                }))
                .apply(GroupByKey.create())
                .apply("toString()", ToString.elements())
                .apply("Write to file", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));


       /* testPipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription("projects/nano-stream-qa/subscriptions/nanostream_for_testing-events-subscription"))
                .apply(new LoggerTransform<>())
                .apply(Window.into(FixedWindows.of(Duration.standardSeconds(20))))
                .apply(new LoggerTransform<>())
                .apply(ParDo.of(new DoFn<PubsubMessage, KV<KV<String, String>, String>>() {

                    private Gson gson;

                    @Setup
                    public void setup() {
                        gson = new Gson();
                    }

                    @ProcessElement
                    public void processElement(ProcessContext c) {

                        PubsubMessage pubsubMessage = c.element();
                        String data = new String(pubsubMessage.getPayload());

                        try {
                            GCloudNotification gcloudNotification = gson.fromJson(data, GCloudNotification.class);
                            c.output(KV.of(KV.of("keyy", "valuee"), new String(c.element().getPayload())));
                        } catch (JsonSyntaxException e) {
                            LOG.error(e.getMessage());
                        }
                    }
                }))
                .apply(GroupByKey.create())
                .apply(new LoggerTransform<>())
                *//* .apply(Window.into(FixedWindows.of(Duration.standardSeconds(20))))
                 .apply(Combine.globally(new CombineIterableAccumulatorFn<String>()).withoutDefaults())
                 .apply(
                         Window.<Iterable<String>>into(
                                 new GlobalWindows())
                                 .triggering(Repeatedly.forever(AfterProcessingTime
                                         .pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(20))))
                                 .withAllowedLateness(Duration.ZERO)
                                 .accumulatingFiredPanes())
                 .apply(Combine.globally(new CombineIterableAccumulatorFn<Iterable<String>>()))*//*
                .apply("toString()", ToString.elements())
                .apply("Write to file", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));*/
        PipelineResult testResult = testPipeline.run();
        testResult.waitUntilFinish();
    }
}
