package com.google.allenday.nanostream;

import com.google.allenday.genomics.core.pipeline.PipelineSetupUtils;
import com.google.allenday.genomics.core.processing.align.AlignTransform;
import com.google.allenday.genomics.core.reference.ReferenceDatabaseSource;
import com.google.allenday.nanostream.batch.CreateBatchesTransform;
import com.google.allenday.nanostream.coders.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.gcs.GCSSourceData;
import com.google.allenday.nanostream.gcs.ParseGCloudNotification;
import com.google.allenday.nanostream.geneinfo.TaxonData;
import com.google.allenday.nanostream.output.PrepareSequencesStatisticToOutputDbFn;
import com.google.allenday.nanostream.output.WriteDataToFirestoreDbFn;
import com.google.allenday.nanostream.pipeline.LoopingTimerTransform;
import com.google.allenday.nanostream.pipeline.PipelineManagerService;
import com.google.allenday.nanostream.pipeline.transform.FlattenMapToKV;
import com.google.allenday.nanostream.probecalculation.KVCalculationAccumulatorFn;
import com.google.allenday.nanostream.pubsub.DecodeNotificationJsonMessage;
import com.google.allenday.nanostream.pubsub.FilterObjectFinalizeMessage;
import com.google.allenday.nanostream.sam.GetReferencesFromSamDataFn;
import com.google.allenday.nanostream.taxonomy.GetTaxonomyFromTree;
import com.google.allenday.nanostream.taxonomy.resistant_genes.GetResistanceGenesTaxonomyDataFn;
import com.google.allenday.nanostream.taxonomy.resistant_genes.LoadResistantGeneInfoTransform;
import com.google.allenday.nanostream.taxonomy.resistant_genes.ResistantGeneInfo;
import com.google.allenday.nanostream.util.CoderUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.joda.time.Duration;

import java.io.Serializable;
import java.util.Map;

import static com.google.allenday.nanostream.ProcessingMode.RESISTANT_GENES;

public class NanostreamPipeline implements Serializable {

    private NanostreamPipelineOptions options;
    private Injector injector;
    private ProcessingMode processingMode;

    public NanostreamPipeline(NanostreamPipelineOptions options) {
        this.options = options;
        this.injector = Guice.createInjector(new MainModule.Builder().fromOptions(options).build());
        processingMode = ProcessingMode.findByLabel(options.getProcessingMode());
    }

    public void run() {
        PipelineSetupUtils.prepareForInlineAlignment(options);
        Pipeline pipeline = Pipeline.create(options);
        CoderUtils.setupCoders(pipeline, new SequenceOnlyDNACoder());

        Window<KV<KV<GCSSourceData, String>, KV<ReferenceDatabaseSource, TaxonData>>> globalWindowWithTriggering = Window
                .<KV<KV<GCSSourceData, String>, KV<ReferenceDatabaseSource, TaxonData>>>into(new GlobalWindows())
                .triggering(Repeatedly.forever(AfterFirst.of(
                        AfterPane.elementCountAtLeast(options.getStatisticOutputCountTriggerSize()),
                        AfterProcessingTime
                                .pastFirstElementInPane()
                                .plusDelayOf(Duration.standardSeconds(options.getStatisticUpdatingDelay())))))
                .withAllowedLateness(Duration.ZERO)
                .accumulatingFiredPanes();


        pipeline.apply("Reading PubSub", PubsubIO.readMessagesWithAttributes().fromSubscription(options.getInputDataSubscription()))
                .apply("Filter only ADD FILE", ParDo.of(new FilterObjectFinalizeMessage()))
                .apply("Deserialize messages", ParDo.of(new DecodeNotificationJsonMessage()))
                .apply("Parse GCloud notification", ParDo.of(injector.getInstance(ParseGCloudNotification.class)))
                .apply("Create FastQ batches", injector.getInstance(CreateBatchesTransform.class))
                .apply("Alignment", injector.getInstance(AlignTransform.class))
                .apply("Looping timer", new LoopingTimerTransform(
                        options.getAutoStopDelay(),
                        options.getJobNameLabel(),
                        injector.getInstance(PipelineManagerService.class),
                        options.getInitAutoStopOnlyIfDataPassed()))
                .apply("Extract reference name",
                        ParDo.of(injector.getInstance(GetReferencesFromSamDataFn.class)))
                .apply("Get Taxonomy data", getTaxonomyData(pipeline))
                .apply("Global Window with Repeatedly triggering" + options.getStatisticUpdatingDelay(), globalWindowWithTriggering)
                .apply("Accumulate results to Map", Combine.globally(new KVCalculationAccumulatorFn()))
                .apply("Flatten result map", ParDo.of(new FlattenMapToKV<>()))
                .apply("Prepare sequences statistic to output",
                        ParDo.of(injector.getInstance(PrepareSequencesStatisticToOutputDbFn.class)))
                .apply("Write sequences statistic to Firestore",
                        ParDo.of(injector.getInstance(WriteDataToFirestoreDbFn.class)))
        ;

        pipeline.run();
    }

    private ParDo.SingleOutput<KV<KV<GCSSourceData, String>, ReferenceDatabaseSource>, KV<KV<GCSSourceData, String>, KV<ReferenceDatabaseSource, TaxonData>>> getTaxonomyData(Pipeline pipeline) {
        if (processingMode == RESISTANT_GENES) {
            PCollectionView<Map<String, ResistantGeneInfo>> geneInfoMapPCollectionView = pipeline.apply(injector.getInstance(LoadResistantGeneInfoTransform.class))
                    .apply(View.asMap());
            return ParDo.of(injector.getInstance(GetResistanceGenesTaxonomyDataFn.class)
                    .setGeneInfoMapPCollectionView(geneInfoMapPCollectionView))
                    .withSideInputs(geneInfoMapPCollectionView);
        } else {
            return ParDo.of(injector.getInstance(GetTaxonomyFromTree.class));
        }
    }
}
