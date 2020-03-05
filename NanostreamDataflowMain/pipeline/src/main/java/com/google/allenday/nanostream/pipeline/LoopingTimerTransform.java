package com.google.allenday.nanostream.pipeline;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.state.TimeDomain;
import org.apache.beam.sdk.state.Timer;
import org.apache.beam.sdk.state.TimerSpec;
import org.apache.beam.sdk.state.TimerSpecs;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.DefaultTrigger;
import org.apache.beam.sdk.transforms.windowing.GlobalWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionList;
import org.apache.beam.sdk.values.TimestampedValue;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoopingTimerTransform<KVKeyT, KVValueT> extends PTransform<PCollection<KV<KVKeyT, KVValueT>>, PCollection<KV<KVKeyT, KVValueT>>> {
    private Logger LOG = LoggerFactory.getLogger(LoopingTimerTransform.class);

    private ValueProvider<Integer> maxDeltaSec;
    private ValueProvider<String> jobNameLabel;
    private PipelineManagerService pipelineManagerService;

    public LoopingTimerTransform(ValueProvider<Integer> maxDeltaSec, ValueProvider<String> jobNameLabel,
                                 PipelineManagerService pipelineManagerService) {
        this.maxDeltaSec = maxDeltaSec;
        this.pipelineManagerService = pipelineManagerService;
        this.jobNameLabel = jobNameLabel;
    }

    @Override
    public PCollection<KV<KVKeyT, KVValueT>> expand(PCollection<KV<KVKeyT, KVValueT>> input) {
        Instant nowTime = Instant.now();
        LOG.info("Starter time: {}", nowTime.toString());
        PCollection<KV<Integer, KV<KVKeyT, KVValueT>>> starterCollection = input.getPipeline()
                .apply(Create.<KV<KVKeyT, KVValueT>>timestamped(TimestampedValue.of(KV.of(null, null), nowTime)))
                .apply(WithKeys.of(1));

        PCollection<KV<Integer, KV<KVKeyT, KVValueT>>> mainCollection = input
                .apply(Window.<KV<KVKeyT, KVValueT>>into(new GlobalWindows())
                        .triggering(DefaultTrigger.of()).withAllowedLateness(Duration.ZERO)
                        .discardingFiredPanes())
                .apply(WithKeys.of(0));

        return PCollectionList.of(starterCollection).and(mainCollection)
                .apply(Flatten.pCollections())
                .apply(ParDo.of(new LoopingTimer<>(
                        pipelineManagerService,
                        maxDeltaSec,
                        jobNameLabel)))
                .apply(MapElements.via(new SimpleFunction<KV<Integer, KV<KVKeyT, KVValueT>>, KV<KVKeyT, KVValueT>>() {
                    @Override
                    public KV<KVKeyT, KVValueT> apply(KV<Integer, KV<KVKeyT, KVValueT>> input) {
                        return input.getValue();
                    }
                }));
    }

    public static class LoopingTimer<KVValue> extends DoFn<KV<Integer, KVValue>, KV<Integer, KVValue>> {
        @TimerId("loopingTimer")
        private final TimerSpec loopingTimerSpec =
                TimerSpecs.timer(TimeDomain.PROCESSING_TIME);
        private Logger LOG = LoggerFactory.getLogger(LoopingTimer.class);
        private ValueProvider<Integer> maxDeltaSec;
        private ValueProvider<String> jobNameLabel;
        private PipelineManagerService pipelineManagerService;

        LoopingTimer(PipelineManagerService pipelineManagerService, ValueProvider<Integer> maxDeltaSec,
                     ValueProvider<String> jobNameLabel) {
            this.maxDeltaSec = maxDeltaSec;
            this.pipelineManagerService = pipelineManagerService;
            this.jobNameLabel = jobNameLabel;
        }

        private Integer deltaSec(Instant lastTime, Instant currentTime) {
            if (lastTime == null || currentTime == null) {
                return null;
            } else {
                return Seconds.secondsBetween(lastTime, currentTime).getSeconds();
            }
        }

        @ProcessElement
        public void process(ProcessContext c,
                            @TimerId("loopingTimer") Timer loopingTimer) {

            LOG.info("Pass trough  LoopingTimer {}, {}, {}. Set timer: {}", c.element(), c.timestamp(), c.pane().toString(),
                    Instant.now().plus(Duration.standardSeconds(maxDeltaSec.get())).toString());

            Duration timerOffset = null;
            if (c.element().getKey().equals(0)) {
                timerOffset = Duration.standardSeconds(maxDeltaSec.get());
                c.output(c.element());
            } else {
                Instant startTime = c.timestamp();
                long diff = Instant.now().getMillis() - startTime.getMillis();
                timerOffset = diff > 0 ? Duration.millis(diff) : Duration.ZERO;
            }
            loopingTimer.offset(timerOffset).setRelative();
        }

        @OnTimer("loopingTimer")
        public void onTimer(
                OnTimerContext c,
                PipelineOptions pipelineOptions,
                @TimerId("loopingTimer") Timer loopingTimer) {
            LOG.info("Timer @ {} fired. STOPPING the pipeline", c.timestamp());

            DataflowPipelineOptions opt = pipelineOptions.as(DataflowPipelineOptions.class);
            try {
                pipelineManagerService.sendStopPipelineCommand(opt.getProject(), jobNameLabel.get());
            } catch (IOException e) {
                LOG.error(e.getMessage());
            }
        }
    }

}