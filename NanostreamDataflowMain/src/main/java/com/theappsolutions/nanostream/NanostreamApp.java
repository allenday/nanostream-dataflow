package com.theappsolutions.nanostream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.aligner.GetSequencesFromSamDataFn;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.errorcorrection.ErrorCorrectionFn;
import com.theappsolutions.nanostream.fastq.BatchByN;
import com.theappsolutions.nanostream.fastq.ParseFastQFn;
import com.theappsolutions.nanostream.gcs.GetDataFromFastQFile;
import com.theappsolutions.nanostream.gcs.ParseGCloudNotification;
import com.theappsolutions.nanostream.geneinfo.GeneData;
import com.theappsolutions.nanostream.geneinfo.GeneInfo;
import com.theappsolutions.nanostream.geneinfo.LoadGeneInfoTransform;
import com.theappsolutions.nanostream.injection.MainModule;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.kalign.SequenceOnlyDNACoder;
import com.theappsolutions.nanostream.output.PrepareSequencesBodiesToOutputDbFn;
import com.theappsolutions.nanostream.output.PrepareSequencesStatisticToOutputDbFn;
import com.theappsolutions.nanostream.output.WriteSequencesBodiesToFirestoreDbFn;
import com.theappsolutions.nanostream.output.WriteSequencesStatisticToFirestoreDbFn;
import com.theappsolutions.nanostream.probecalculation.KVCalculationAccumulatorFn;
import com.theappsolutions.nanostream.pubsub.DecodeNotificationJsonMessage;
import com.theappsolutions.nanostream.pubsub.FilterObjectFinalizeMessage;
import com.theappsolutions.nanostream.taxonomy.GetResistanceGenesTaxonomyDataFn;
import com.theappsolutions.nanostream.taxonomy.GetSpeciesTaxonomyDataFn;
import com.theappsolutions.nanostream.util.EntityNamer;
import com.theappsolutions.nanostream.util.trasform.RemoveValueDoFn;
import japsa.seq.Sequence;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.View;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.joda.time.Duration;

import java.util.Map;
import java.util.stream.Stream;

/**
 * Main class of the Nanostream Dataflow App that provides dataflow pipeline
 * with transformation from PubsubMessage to Sequences Statistic and Sequences Bodies
 */
public class NanostreamApp {

    private static final int FASTQ_GROUPING_BATCH_SIZE = 200;

    public enum ProcessingMode {
        SPECIES("species"),
        RESISTANT_GENES("resistance_genes");

        public final String label;

        ProcessingMode(String label) {
            this.label = label;
        }

        public static ProcessingMode findByLabel(String label) {
            return Stream.of(ProcessingMode.values()).filter(
                    processingMode -> processingMode.label.equals(label))
                    .findFirst().orElse(SPECIES);
        }
    }

    public static void main(String[] args) {
        NanostreamPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamPipelineOptions.class);
        final ProcessingMode processingMode = ProcessingMode.findByLabel(options.getProcessingMode());
        Injector injector = Guice.createInjector(new MainModule.Builder().buildFromOptions(options));

        options.setJobName(injector.getInstance(EntityNamer.class)
                .generateJobName(processingMode, options.getOutputFirestoreCollectionNamePrefix()));
        Pipeline pipeline = Pipeline.create(options);
        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        pipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView = null;
        if (processingMode == ProcessingMode.RESISTANT_GENES) {
            PCollection<KV<String, GeneInfo>> geneInfoMapPCollection = pipeline.apply(injector.getInstance(LoadGeneInfoTransform.class));
            geneInfoMapPCollectionView = geneInfoMapPCollection.apply(View.asMap());
        }

        PCollection<PubsubMessage> pubsubMessages = pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription(options.getInputDataSubscription()));

        PCollection<KV<String, Sequence>> errorCorrectedCollection = pubsubMessages
                .apply("Filter only ADD FILE", ParDo.of(new FilterObjectFinalizeMessage()))
                .apply("Deserialize messages", ParDo.of(new DecodeNotificationJsonMessage()))
                .apply("Parse GCloud notification", ParDo.of(new ParseGCloudNotification()))

                .apply("Get data from FastQ", ParDo.of(new GetDataFromFastQFile()))
                .apply("Parse FastQ data", ParDo.of(new ParseFastQFn()))
                .apply(options.getAlignmentWindow() + "s FastQ collect window",
                        Window.into(FixedWindows.of(Duration.standardSeconds(options.getAlignmentWindow()))))
                .apply("Create batches of "+ FASTQ_GROUPING_BATCH_SIZE +" FastQ records",
                        new BatchByN(FASTQ_GROUPING_BATCH_SIZE))
                .apply("Alignment", ParDo.of(injector.getInstance(MakeAlignmentViaHttpFn.class)))
                .apply("Extract Sequences",
                        ParDo.of(new GetSequencesFromSamDataFn()))
                .apply("Group by SAM reference", GroupByKey.create())
                .apply("K-Align", ParDo.of(injector.getInstance(ProceedKAlignmentFn.class)))
                .apply("Error correction", ParDo.of(new ErrorCorrectionFn()));

        errorCorrectedCollection
                .apply("Remove Sequence part", ParDo.of(new RemoveValueDoFn<>()))
                .apply("Get Taxonomy data", processingMode == ProcessingMode.RESISTANT_GENES
                        ? ParDo.of(new GetResistanceGenesTaxonomyDataFn(geneInfoMapPCollectionView))
                        .withSideInputs(geneInfoMapPCollectionView)
                        : ParDo.of(injector.getInstance(GetSpeciesTaxonomyDataFn.class)))
                .apply("Global Window with Repeatedly triggering" + options.getStatisticUpdatingDelay(),
                        Window.<KV<String, GeneData>>into(new GlobalWindows())
                        .triggering(Repeatedly.forever(AfterProcessingTime
                                .pastFirstElementInPane()
                                .plusDelayOf(Duration.standardSeconds(options.getStatisticUpdatingDelay()))))
                        .withAllowedLateness(Duration.ZERO)
                        .accumulatingFiredPanes())
                .apply("Accumulate results to Map", Combine.globally(new KVCalculationAccumulatorFn()))
                .apply("Prepare sequences statistic to output",
                        ParDo.of(injector.getInstance(PrepareSequencesStatisticToOutputDbFn.class)))
                .apply("Write sequences statistic to Firestore",
                        ParDo.of(injector.getInstance(WriteSequencesStatisticToFirestoreDbFn.class)));

        errorCorrectedCollection
                .apply("Prepare sequences bodies to output",
                        ParDo.of(new PrepareSequencesBodiesToOutputDbFn()))
                .apply("Write sequences bodies to Firestore",
                        ParDo.of(injector.getInstance(WriteSequencesBodiesToFirestoreDbFn.class)));


        pipeline.run();
    }
}
