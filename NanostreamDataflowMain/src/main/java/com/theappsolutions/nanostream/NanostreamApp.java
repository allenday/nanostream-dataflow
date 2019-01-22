package com.theappsolutions.nanostream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.aligner.ParseAlignedDataIntoSAMFn;
import com.theappsolutions.nanostream.errorcorrection.ErrorCorrectionFn;
import com.theappsolutions.nanostream.fastq.ParseFastQFn;
import com.theappsolutions.nanostream.gcs.GetDataFromFastQFile;
import com.theappsolutions.nanostream.injection.MainModule;
import com.theappsolutions.nanostream.kalign.ExtractSequenceFn;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.kalign.SequenceOnlyDNACoder;
import com.theappsolutions.nanostream.output.WriteSequencesBodyToFirestoreDbFn;
import com.theappsolutions.nanostream.output.WriteSequencesStatisticToFirestoreDbFn;
import com.theappsolutions.nanostream.probecalculation.KVKalculationAccumulatorFn;
import com.theappsolutions.nanostream.probecalculation.SequenceCountAndTaxonomyData;
import com.theappsolutions.nanostream.pubsub.DecodeNotificationJsonMessage;
import com.theappsolutions.nanostream.pubsub.FilterObjectFinalizeMessage;
import com.theappsolutions.nanostream.taxonomy.GetTaxonomyDataFn;
import com.theappsolutions.nanostream.util.trasform.CombineIterableAccumulatorFn;
import com.theappsolutions.nanostream.util.trasform.RemoveValueDoFn;
import htsjdk.samtools.fastq.FastqRecord;
import japsa.seq.Sequence;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

import java.util.List;
import java.util.Map;

/**
 * Main class of the Nanostream Dataflow App that provides dataflow pipeline
 * with transformation from PubsubMessage to FasQ data
 */
public class NanostreamApp {

    public static void main(String[] args) {
        NanostreamPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamPipelineOptions.class);
        Injector injector = Guice.createInjector(new MainModule.Builder().buildFromOptions(options));

        Pipeline pipeline = Pipeline.create(options);
        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        pipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollection<PubsubMessage> pubsubMessages = pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription(options.getInputDataSubscription()));

        PCollection<KV<String, Sequence>> errorCorrectedCollection = pubsubMessages
                .apply("Filter only ADD FILE", ParDo.of(new FilterObjectFinalizeMessage()))
                .apply("Deserialize messages", ParDo.of(new DecodeNotificationJsonMessage()))
                .apply("Get data from FastQ", ParDo.of(new GetDataFromFastQFile()))
                .apply("Parse FasQ data", ParDo.of(new ParseFastQFn()))
                .apply(
                        options.getDataCollectionWindow() + "s FastQ collect window",
                        Window.into(FixedWindows.of(Duration.standardSeconds(options.getDataCollectionWindow()))))
                .apply("Accumulate to iterable", Combine.globally(new CombineIterableAccumulatorFn<FastqRecord>())
                        .withoutDefaults())
                .apply("Alignment", ParDo.of(injector.getInstance(MakeAlignmentViaHttpFn.class)))
                .apply("Generation SAM",
                        ParDo.of(new ParseAlignedDataIntoSAMFn()))
                .apply("Group by SAM reference", GroupByKey.create())
                .apply("Extract Sequences",
                        ParDo.of(new ExtractSequenceFn()))
                .apply("K-Align", ParDo.of(injector.getInstance(ProceedKAlignmentFn.class)))
                .apply("Error correction", ParDo.of(new ErrorCorrectionFn()));

        errorCorrectedCollection
                .apply("Remove Sequence part", ParDo.of(new RemoveValueDoFn<>()))
                .apply("Get Taxonomy data", ParDo.of(injector.getInstance(GetTaxonomyDataFn.class)))
                .apply("Accumulating", Window.<KV<String, List<String>>>into(new GlobalWindows())
                        .triggering(Repeatedly.forever(AfterProcessingTime
                                .pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(60))))
                        .withAllowedLateness(Duration.ZERO)
                        .accumulatingFiredPanes())
                .apply("Accumulate results to Map", Combine.globally(new KVKalculationAccumulatorFn()))
                .apply("Take only last pane", Window.<Map<String, SequenceCountAndTaxonomyData>>into(new GlobalWindows())
                        .triggering(Repeatedly.forever(AfterProcessingTime
                                .pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(10))))
                        .withAllowedLateness(Duration.ZERO)
                        .discardingFiredPanes())
                .apply("Write to sequence statistic to Firestore", ParDo.of(injector.getInstance(WriteSequencesStatisticToFirestoreDbFn.class)));

        errorCorrectedCollection.apply("Write to sequences to Firestore", ParDo.of(injector.getInstance(WriteSequencesBodyToFirestoreDbFn.class)));


        PipelineResult result = pipeline.run();
        result.waitUntilFinish();
    }
}
