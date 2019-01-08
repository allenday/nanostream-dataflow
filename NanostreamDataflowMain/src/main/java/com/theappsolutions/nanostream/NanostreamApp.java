package com.theappsolutions.nanostream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.aligner.ParseAlignedDataIntoSAMFn;
import com.theappsolutions.nanostream.errorcorrection.ErrorCorrectionFn;
import com.theappsolutions.nanostream.fastq.ParseFastQFn;
import com.theappsolutions.nanostream.gcs.GetDataFromFastQFile;
import com.theappsolutions.nanostream.geneinfo.GeneInfo;
import com.theappsolutions.nanostream.geneinfo.LoadGeneInfoTransform;
import com.theappsolutions.nanostream.injection.MainModule;
import com.theappsolutions.nanostream.io.WindowedFilenamePolicy;
import com.theappsolutions.nanostream.kalign.ExtractSequenceFn;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.kalign.SequenceOnlyDNACoder;
import com.theappsolutions.nanostream.output.WriteToFirestoreDbFn;
import com.theappsolutions.nanostream.pubsub.DecodeNotificationJsonMessage;
import com.theappsolutions.nanostream.pubsub.FilterObjectFinalizeMessage;
import com.theappsolutions.nanostream.util.trasform.CombineIterableAccumulatorFn;
import htsjdk.samtools.fastq.FastqRecord;
import japsa.seq.Sequence;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.FileBasedSink;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.PCollectionView;
import org.joda.time.Duration;

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
        Injector injector = Guice.createInjector(new MainModule.Builder().buildWithPipelineOptions(options));

        Pipeline pipeline = Pipeline.create(options);
        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        pipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollection<PubsubMessage> pubsubMessages = pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription(options.getSubscription()));

        PCollectionView<Map<String, GeneInfo>> geneInfo = pipeline.apply(injector.getInstance(LoadGeneInfoTransform.class))
                .apply(
                        View.asMap()
                );

        PCollection<KV<String, Sequence>> mainWorkflow = pubsubMessages
                .apply("Filter only ADD FILE", ParDo.of(new FilterObjectFinalizeMessage()))
                .apply("Deserialize messages", ParDo.of(new DecodeNotificationJsonMessage()))
                .apply("Get data from FastQ", ParDo.of(new GetDataFromFastQFile()))
                .apply("Parse FasQ data", ParDo.of(new ParseFastQFn()))
                .apply(
                        options.getWindowTime() + "s FastQ collect window",
                        Window.into(FixedWindows.of(Duration.standardSeconds(options.getWindowTime()))))
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

        mainWorkflow.apply("Write to Datastore", ParDo.of(injector.getInstance(WriteToFirestoreDbFn.class)));
                //TODO temporary output to gcs file for debug

        mainWorkflow.apply("toString()", ToString.elements())
                .apply("Write to GCS", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(options.getNumShards())
                        .to(
                                new WindowedFilenamePolicy(
                                        options.getOutputDirectory(),
                                        options.getOutputFilenamePrefix(),
                                        options.getOutputShardTemplate(),
                                        options.getOutputFilenameSuffix()))
                        .withTempDirectory(ValueProvider.NestedValueProvider.of(
                                options.getOutputDirectory(),
                                (SerializableFunction<String, ResourceId>) FileBasedSink::convertToFileResourceIfPossible)));

        PipelineResult result = pipeline.run();
        result.waitUntilFinish();
    }
}
