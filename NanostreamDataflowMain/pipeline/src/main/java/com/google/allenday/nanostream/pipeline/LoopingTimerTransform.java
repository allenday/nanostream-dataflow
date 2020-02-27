package com.google.allenday.nanostream.pipeline;

import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.state.TimeDomain;
import org.apache.beam.sdk.state.Timer;
import org.apache.beam.sdk.state.TimerSpec;
import org.apache.beam.sdk.state.TimerSpecs;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;
import org.joda.time.Instant;
import org.joda.time.Seconds;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class LoopingTimerTransform<KVKeyT, KVValueT> extends PTransform<PCollection<KV<KVKeyT, KVValueT>>, PCollection<KV<KVKeyT, KVValueT>>> {

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
        return input
                .apply(WithKeys.of(0))
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

        @ProcessElement
        public void process(ProcessContext c,
                            @TimerId("loopingTimer") Timer loopingTimer) {

            ;
            LOG.info("Pass trough  LoopingTimer {}, {}, {}. Set timer: {}", c.element(), c.timestamp(), c.pane().toString(),
                    Instant.now().plus(Duration.standardSeconds(maxDeltaSec.get())).toString());
            c.output(c.element());
            loopingTimer.offset(Duration.standardSeconds(maxDeltaSec.get())).setRelative();
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