package com.google.allenday.nanostream.pipeline;

import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.allenday.genomics.core.model.SampleMetaData;
import com.google.allenday.genomics.core.reference.ReferenceDatabaseSource;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.options.PipelineOptions;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.state.TimeDomain;
import org.apache.beam.sdk.state.Timer;
import org.apache.beam.sdk.state.TimerSpec;
import org.apache.beam.sdk.state.TimerSpecs;
import org.apache.beam.sdk.transforms.*;
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

public class LoopingTimerTransform extends PTransform<PCollection<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>>,
        PCollection<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>>> {
    private Logger LOG = LoggerFactory.getLogger(LoopingTimerTransform.class);

    private ValueProvider<Integer> maxDeltaSec;
    private ValueProvider<String> jobNameLabel;
    private boolean initAutoStopOnlyIfDataPassed;
    private PipelineManagerService pipelineManagerService;

    public LoopingTimerTransform(ValueProvider<Integer> maxDeltaSec, ValueProvider<String> jobNameLabel,
                                 PipelineManagerService pipelineManagerService, boolean initAutoStopOnlyIfDataPassed) {
        this.maxDeltaSec = maxDeltaSec;
        this.pipelineManagerService = pipelineManagerService;
        this.jobNameLabel = jobNameLabel;
        this.initAutoStopOnlyIfDataPassed = initAutoStopOnlyIfDataPassed;
    }

    @Override
    public PCollection<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>> expand(PCollection<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>> input) {
        PCollection<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>> flattenedCollection;
        if (initAutoStopOnlyIfDataPassed) {
            flattenedCollection = input;
        } else {
            Instant nowTime = Instant.now();
            LOG.info("Starter time: {}", nowTime.toString());

            PCollection<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>> starterCollection = input.getPipeline()
                    .apply(Create.<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>>timestamped(
                            TimestampedValue.of(KV.of(SampleMetaData.createUnique("", "", ""),
                                    KV.of(new ReferenceDatabaseSource.Explicit(), FileWrapper.empty())), nowTime)))
                    .apply(Window.<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>>into(new GlobalWindows())
                            .triggering(input.getWindowingStrategy().getTrigger()).withAllowedLateness(Duration.ZERO).discardingFiredPanes());

            flattenedCollection = PCollectionList.of(starterCollection).and(input)
                    .apply(Flatten.pCollections());
        }

        return flattenedCollection
                .apply(WithKeys.of(0))
                .apply(ParDo.of(new LoopingTimer(pipelineManagerService, maxDeltaSec, jobNameLabel)))
                .apply(MapElements.via(new SimpleFunction<KV<Integer, KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>>, KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>>() {
                    @Override
                    public KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>> apply(KV<Integer, KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>> input) {
                        return input.getValue();
                    }
                }));
    }

    public static class LoopingTimer extends DoFn<KV<Integer, KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>>,
            KV<Integer, KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>>> {

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

            Duration timerOffset = Duration.standardSeconds(maxDeltaSec.get());
            if (c.element().getValue().getValue().getValue().getDataType() != FileWrapper.DataType.EMPTY) {
                c.output(c.element());
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