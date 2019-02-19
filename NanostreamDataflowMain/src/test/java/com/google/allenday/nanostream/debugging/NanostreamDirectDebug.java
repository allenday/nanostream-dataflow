package com.google.allenday.nanostream.debugging;

import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.util.trasform.CombineIterableAccumulatorFn;
import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.ToString;
import org.apache.beam.sdk.transforms.windowing.*;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NanostreamDirectDebug {

    private static Logger LOG = LoggerFactory.getLogger(NanostreamDirectDebug.class);


    public static void main(String[] args) {
        PipelineOptions pipelineOptions = PipelineOptionsFactory.create().as(PipelineOptions.class);
        pipelineOptions.setRunner(DirectRunner.class);
        Pipeline testPipeline = Pipeline.create(pipelineOptions);
        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        testPipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);
        /*PCollectionView<Map<String, GeneInfo>> geneInfoMap = testPipeline.apply(new LoadGeneInfoTransform(
                "gs://nano-stream-test/ResistanceGenes/resFinder/DB.fasta",
                "gs://nano-stream-test/ResistanceGenes/resFinder/geneList"))
                .apply(
                        View.asMap()
                );*/
        /*testPipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription("projects/upwork-nano-stream/subscriptions/manual_test_topic_subscription"))
                .apply(ParDo.of(new DoFn<PubsubMessage, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        Map<String, GeneInfo> stringGeneInfoMap = c.sideInput(geneInfoMap);
                        c.output(new String(c.element().getPayload()));
                    }
                }).withSideInputs(geneInfoMap))
                .apply(ParDo.of(new AddValueDoFn<>()))
                .apply(
                        Window.<KV<String, List<String>>>into(
                                new GlobalWindows())
                                .triggering(Repeatedly.forever(AfterProcessingTime
                                        .pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(20))))
                                .withAllowedLateness(Duration.ZERO)
                                .accumulatingFiredPanes())
                .apply(Combine.globally(new KVCalculationAccumulatorFn()))
                .apply("Take only last pane", Window.<Map<String, SequenceCountAndTaxonomyData>>into(new GlobalWindows())
                        .triggering(Repeatedly.forever(AfterProcessingTime
                                .pastFirstElementInPane().plusDelayOf(Duration.ZERO)))
                        .withAllowedLateness(Duration.ZERO)
                        .discardingFiredPanes())
                .apply("toString()", ToString.elements())
                .apply("Write to file", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));*/
        testPipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription("projects/upwork-nano-stream/subscriptions/test_1927_subscription"))
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
                .apply("Write to file", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));
        PipelineResult testResult = testPipeline.run();
        testResult.waitUntilFinish();
    }
}
