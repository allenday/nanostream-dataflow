package com.theappsolutions.nanostream;

import com.google.api.core.ApiFuture;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.aligner.ParseAlignedDataIntoSAMFn;
import com.theappsolutions.nanostream.errorcorrection.ErrorCorrectionFn;
import com.theappsolutions.nanostream.fastq.ParseFastQFn;
import com.theappsolutions.nanostream.gcs.GetDataFromFastQFile;
import com.theappsolutions.nanostream.injection.MainModule;
import com.theappsolutions.nanostream.io.WindowedFilenamePolicy;
import com.theappsolutions.nanostream.kalign.ExtractSequenceFn;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.kalign.SequenceOnlyDNACoder;
import com.theappsolutions.nanostream.output.OutputRecord;
import com.theappsolutions.nanostream.pubsub.DecodeNotificationJsonMessage;
import com.theappsolutions.nanostream.pubsub.FilterObjectFinalizeMessage;
import com.theappsolutions.nanostream.util.trasform.CombineIterableAccumulatorFn;
import htsjdk.samtools.fastq.FastqRecord;
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
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.UUID;
import java.util.concurrent.ExecutionException;


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

        FileInputStream serviceAccount =
                null;
        try {
            serviceAccount = new FileInputStream("keys/upwork-nano-stream-firebase.json");


            FirebaseOptions firebaseOptions = new FirebaseOptions.Builder()
                    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                    .setDatabaseUrl("https://upwork-nano-stream.firebaseio.com")
                    .build();

            FirebaseApp.initializeApp(firebaseOptions);
            Firestore db = FirestoreClient.getFirestore();

            ApiFuture<WriteResult> future = db.collection("nanostream_results").document(UUID.randomUUID().toString())
                    .set(new OutputRecord("1", "1", "1", "1", "1"));
            // block on response if required
            System.out.println("Update time : " + future.get().getUpdateTime());
        } catch (IOException | InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        int i = 0;
        while (i < 1) {
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }


        Pipeline pipeline = Pipeline.create(options);
        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        pipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollection<PubsubMessage> pubsubMessages = pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription(options.getSubscription()));

        pubsubMessages
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
                .apply("Error correction", ParDo.of(new ErrorCorrectionFn()))
                //TODO temporary output to gcs file for debug
                .apply("toString()", ToString.elements())
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
