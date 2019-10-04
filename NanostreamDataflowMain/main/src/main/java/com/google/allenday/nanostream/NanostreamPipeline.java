package com.google.allenday.nanostream;

import com.google.allenday.nanostream.aligner.GetSequencesFromSamDataFn;
import com.google.allenday.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.google.allenday.nanostream.errorcorrection.ErrorCorrectionFn;
import com.google.allenday.nanostream.fastq.ParseFastQFn;
import com.google.allenday.nanostream.gcs.GetDataFromFastQFile;
import com.google.allenday.nanostream.gcs.ParseGCloudNotification;
import com.google.allenday.nanostream.geneinfo.GeneData;
import com.google.allenday.nanostream.geneinfo.GeneInfo;
import com.google.allenday.nanostream.geneinfo.LoadGeneInfoTransform;
import com.google.allenday.nanostream.injection.MainModule;
import com.google.allenday.nanostream.kalign.ProceedKAlignmentFn;
import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.output.PrepareSequencesStatisticToOutputDbFn;
import com.google.allenday.nanostream.output.WriteDataToFirestoreDbFn;
import com.google.allenday.nanostream.probecalculation.KVCalculationAccumulatorFn;
import com.google.allenday.nanostream.pubsub.DecodeNotificationJsonMessage;
import com.google.allenday.nanostream.pubsub.FilterObjectFinalizeMessage;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.taxonomy.GetResistanceGenesTaxonomyDataFn;
import com.google.allenday.nanostream.taxonomy.GetTaxonomyFromTree;
import com.google.allenday.nanostream.util.CoderUtils;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.allenday.nanostream.util.trasform.FlattenMapToKV;
import com.google.allenday.nanostream.util.trasform.RemoveValueDoFn;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.joda.time.Duration;

import java.util.Map;

public class NanostreamPipeline {

    private NanostreamPipelineOptions options;

    public NanostreamPipeline(NanostreamPipelineOptions options) {
        this.options = options;
    }

    public void run() {
        final ProcessingMode processingMode = ProcessingMode.findByLabel(options.getProcessingMode());
        Injector injector = Guice.createInjector(new MainModule.Builder().buildFromOptions(options));

        options.setJobName(injector.getInstance(EntityNamer.class)
                .generateJobName(processingMode, options.getOutputCollectionNamePrefix()));
        Pipeline pipeline = Pipeline.create(options);
        CoderUtils.setupCoders(pipeline, new SequenceOnlyDNACoder());

        PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView = null;
        if (processingMode == ProcessingMode.RESISTANT_GENES) {
            PCollection<KV<String, GeneInfo>> geneInfoMapPCollection = pipeline.apply(injector.getInstance(LoadGeneInfoTransform.class));
            geneInfoMapPCollectionView = geneInfoMapPCollection.apply(View.asMap());
        }

        PCollection<PubsubMessage> pubsubMessages = pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription(options.getInputDataSubscription()));

        pubsubMessages
                .apply("Filter only ADD FILE", ParDo.of(new FilterObjectFinalizeMessage()))
                .apply("Deserialize messages", ParDo.of(new DecodeNotificationJsonMessage()))
                .apply("Parse GCloud notification", ParDo.of(new ParseGCloudNotification()))

                .apply("Get data from FastQ", ParDo.of(new GetDataFromFastQFile()))
                .apply("Parse FastQ data", ParDo.of(new ParseFastQFn()))
                .apply(options.getAlignmentWindow() + "s FastQ collect window",
                        Window.into(FixedWindows.of(Duration.standardSeconds(options.getAlignmentWindow()))))
                .apply("Create batches of "+ options.getAlignmentBatchSize() +" FastQ records",
                        GroupIntoBatches.ofSize(options.getAlignmentBatchSize()))
                .apply("Alignment", ParDo.of(injector.getInstance(MakeAlignmentViaHttpFn.class)))
                .apply("Extract Sequences",
                        ParDo.of(new GetSequencesFromSamDataFn()))
                .apply("Group by SAM reference", GroupByKey.create())
                .apply("K-Align", ParDo.of(injector.getInstance(ProceedKAlignmentFn.class)))
                .apply("Error correction", ParDo.of(new ErrorCorrectionFn()))

                .apply("Remove Sequence part", ParDo.of(new RemoveValueDoFn<>()))
                .apply("Get Taxonomy data", processingMode == ProcessingMode.RESISTANT_GENES
                        ? ParDo.of(injector.getInstance(GetResistanceGenesTaxonomyDataFn.class)
                        .setGeneInfoMapPCollectionView(geneInfoMapPCollectionView))
                        .withSideInputs(geneInfoMapPCollectionView)
                        : ParDo.of(injector.getInstance(GetTaxonomyFromTree.class)))
                .apply("Global Window with Repeatedly triggering" + options.getStatisticUpdatingDelay(),
                        Window.<KV<KV<GCSSourceData, String>, GeneData>>into(new GlobalWindows())
                                .triggering(Repeatedly.forever(AfterProcessingTime
                                        .pastFirstElementInPane()
                                        .plusDelayOf(Duration.standardSeconds(options.getStatisticUpdatingDelay()))))
                                .withAllowedLateness(Duration.ZERO)
                                .accumulatingFiredPanes())
                .apply("Accumulate results to Map", Combine.globally(new KVCalculationAccumulatorFn()))
                .apply("Flatten result map", ParDo.of(new FlattenMapToKV<>()))
                .apply("Prepare sequences statistic to output",
                        ParDo.of(injector.getInstance(PrepareSequencesStatisticToOutputDbFn.class)))
                .apply("Write sequences statistic to Firestore",
                        ParDo.of(injector.getInstance(WriteDataToFirestoreDbFn.class)));

        pipeline.run();
    }
}
