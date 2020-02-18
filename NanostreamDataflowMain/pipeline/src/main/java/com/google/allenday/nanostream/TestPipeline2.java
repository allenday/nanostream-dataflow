package com.google.allenday.nanostream;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.BigEndianLongCoder;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.state.*;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Minutes;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.stream.StreamSupport;

public class TestPipeline2 implements Serializable {

    public static Logger LOG = LoggerFactory.getLogger(TestPipeline2.class);

    public static void main(String[] args) {


        // Create pipeline
        DataflowPipelineOptions options = PipelineOptionsFactory.fromArgs(args).withValidation()
                .as(DataflowPipelineOptions.class);
//        options.setRunner(DataflowRunner.class);
        options.setProject("upwork-nano-stream");
        options.setStreaming(true);
        options.setNumWorkers(5);
        options.setMaxNumWorkers(10);
        options.setNumberOfWorkerHarnessThreads(1);
        options.setJobName(String.format("statefull-test-%d", Instant.now().getMillis()));
        Pipeline p = Pipeline.create(options);

        Window<KV<String, String>> globalWindow = Window.<KV<String, String>>into(new GlobalWindows())
                .triggering(Repeatedly
                        .forever(AfterFirst.of(
                                AfterPane.elementCountAtLeast(10),
                                AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(30)))))
                .withAllowedLateness(Duration.ZERO)
                .discardingFiredPanes();

        Window<KV<String, String>> outputWindow = Window.<KV<String, String>>into(
                FixedWindows.<Integer>of(Duration.standardSeconds(30)))
                .triggering(AfterWatermark.pastEndOfWindow()
                        .withEarlyFirings(AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(40)))
                        .withLateFirings(AfterPane.elementCountAtLeast(1)))
                .withAllowedLateness(Duration.ZERO)
                .discardingFiredPanes();


        p.apply(PubsubIO.readMessages().fromSubscription("projects/upwork-nano-stream/subscriptions/test-subscription"))
                .apply(ParDo.of(new DoFn<PubsubMessage, KV<String, String>>() {

                    @ProcessElement
                    public void process(ProcessContext c, PipelineOptions pipelineOptions) throws InterruptedException {
                        String data = new String(c.element().getPayload());
                        LOG.info("INPUT: {}, {}", data, c.pane());
                        c.output(KV.of("key_0", data));
                    }
                }))
                .apply(globalWindow)
                .apply(GroupByKey.create())
                .apply(ParDo.of(new DoFn<KV<String, Iterable<String>>, KV<String, Iterable<String>>>() {

                    @ProcessElement
                    public void process(ProcessContext c) throws InterruptedException {

                        LOG.info("Count {}", StreamSupport.stream(c.element().getValue().spliterator(), false).count());
                        c.output(c.element());
                    }
                }))
                .apply(ToString.elements())
                .apply(ParDo.of(new DoFn<String, String>() {

                    @ProcessElement
                    public void process(ProcessContext c) throws InterruptedException {

                        LOG.info("{}: Value is {} timestamp is {}, real {}",
                                c.pane(), c.element(), c.timestamp().toDateTime().secondOfDay().get(), new Instant().toDateTime().secondOfDay().get());
                        c.output(c.element());
                    }
                }));


        p.run();
    }

    public static class LoopingTimerTransform<KVKeyT, KVValueT> extends PTransform<PCollection<KV<KVKeyT, KVValueT>>, PCollection<KV<KVKeyT, KVValueT>>> {
        @Override
        public PCollection<KV<KVKeyT, KVValueT>> expand(PCollection<KV<KVKeyT, KVValueT>> input) {
            return input
                    .apply(WithKeys.of(0))
                    .apply(ParDo.of(new LoopingTimer<>(
                            Minutes.minutes(15).toStandardSeconds().getSeconds(),
                            Minutes.minutes(1).toStandardSeconds().getSeconds())))
                    .apply(MapElements.via(new SimpleFunction<KV<Integer, KV<KVKeyT, KVValueT>>, KV<KVKeyT, KVValueT>>() {
                        @Override
                        public KV<KVKeyT, KVValueT> apply(KV<Integer, KV<KVKeyT, KVValueT>> input) {
                            return input.getValue();
                        }
                    }));
        }

        public static class LoopingTimer<KVValue> extends DoFn<KV<Integer, KVValue>, KV<Integer, KVValue>> {
            private Logger LOG = LoggerFactory.getLogger(LoopingTimer.class);

            private Integer maxDeltaSec;
            private Integer checkPeriodSec;

            public LoopingTimer(Integer maxDeltaSec, Integer checkPeriodSec) {
                this.maxDeltaSec = maxDeltaSec;
                this.checkPeriodSec = checkPeriodSec;
            }

            @StateId("loopingTimerTime")
            private final StateSpec<ValueState<Long>> loopingTimerTimeSpec =
                    StateSpecs.value(BigEndianLongCoder.of());

            @TimerId("loopingTimer")
            private final TimerSpec loopingTimerSpec =
                    TimerSpecs.timer(TimeDomain.PROCESSING_TIME);

            private Integer deltaSec(Instant lastTime, Instant currentTime) {
                if (lastTime == null || currentTime == null) {
                    return null;
                } else {
                    return Seconds.secondsBetween(lastTime, currentTime).getSeconds();
                }
            }

            private boolean shouldStopPipeline(Integer delta, Integer maxDelta) {
                return delta != null && delta > maxDelta;
            }

            @ProcessElement
            public void process(ProcessContext c,
                                @TimerId("loopingTimer") Timer loopingTimer) {

                LOG.info("Pass trough  LoopingTimer {}, {}, {}, {}", c.element(), c.timestamp(), c.pane().toString(), Thread.currentThread().getId());
                c.output(c.element());
                loopingTimer.offset(Duration.standardSeconds(maxDeltaSec)).setRelative();
            }

            @OnTimer("loopingTimer")
            public void onTimer(
                    OnTimerContext c,
                    @TimerId("loopingTimer") Timer loopingTimer) {
                LOG.info("Timer @ {} fired. STOPPING the pipeline", c.timestamp());
            }
        }

    }
}
