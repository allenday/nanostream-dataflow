package com.theappsolutions.nanostream;

import com.theappsolutions.nanostream.aligner.AlignerHttpService;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.aligner.ParseAlignedDataIntoSAMFn;
import com.theappsolutions.nanostream.io.WindowedFilenamePolicy;
import com.theappsolutions.nanostream.pubsub.DecodeNotificationJsonMessage;
import com.theappsolutions.nanostream.pubsub.FilterObjectFinalizeMessage;
import com.theappsolutions.nanostream.gcs.GetDataFromFastQFile;
import com.theappsolutions.nanostream.fastq.ParseFastQFn;
import com.theappsolutions.nanostream.util.HttpHelper;
import com.theappsolutions.nanostream.util.trasform.CombineIterableAccumulatorFn;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileBasedSink;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubIO;
import org.apache.beam.sdk.io.gcp.pubsub.PubsubMessage;
import org.apache.beam.sdk.options.*;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;


/**
 * Main class of the Nanostream Dataflow App that provides dataflow pipeline
 * with transformation from PubsubMessage to FasQ data
 */
public class NanostreamApp {

    public interface NanostreamPipelineOptions extends DataflowPipelineOptions {

        @Description("GCP PubSub subscription name to read messages from")
        @Validation.Required
        String getSubscription();

        void setSubscription(String value);


        /*@Description("The window duration in which data will be written. Defaults to 5m. "
                + "Allowed formats are: "
                + "Ns (for seconds, example: 5s), "
                + "Nm (for minutes, example: 12m), "
                + "Nh (for hours, example: 2h).")
        @Default.String("1m")
        String getWindowDuration();

        void setWindowDuration(String value);*/


        @Description("The maximum number of output shards produced when writing.")
        @Default.Integer(1)
        Integer getNumShards();

        void setNumShards(Integer value);

        @Description("The directory to output files to. Must end with a slash.")
        @Validation.Required
        ValueProvider<String> getOutputDirectory();

        void setOutputDirectory(ValueProvider<String> value);

        @Description("The filename prefix of the files to write to.")
        @Default.String("output_file_")
        @Validation.Required
        ValueProvider<String> getOutputFilenamePrefix();

        void setOutputFilenamePrefix(ValueProvider<String> value);

        @Description("The suffix of the files to write.")
        @Default.String("")
        ValueProvider<String> getOutputFilenameSuffix();

        void setOutputFilenameSuffix(ValueProvider<String> value);

        @Description("The shard template of the output file. Specified as repeating sequences "
                + "of the letters 'S' or 'N' (example: SSS-NNN). These are replaced with the "
                + "shard number, or number of shards respectively")
        @Default.String("W-P-SS-of-NN")
        ValueProvider<String> getOutputShardTemplate();

        void setOutputShardTemplate(ValueProvider<String> value);


        @Description("The window duration in which FastQ records will be collected")
        @Default.Integer(60)
        Integer getResistanceGenesWindowTime();

        void setResistanceGenesWindowTime(Integer value);

        @Description("Resistance Genes - Alignment server to use")
        @Validation.Required
        String getResistanceGenesAlignmentServer();

        void setResistanceGenesAlignmentServer(String value);

        @Description("Resistance Genes - Alignment database")
        @Validation.Required
        String getResistanceGenesAlignmentDatabase();

        void setResistanceGenesAlignmentDatabase(String value);

    }

    public static void main(String[] args) {
        NanostreamPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamPipelineOptions.class);

        Pipeline pipeline = Pipeline.create(options);

        PCollection<PubsubMessage> pubsubMessages = pipeline.apply("Reading PubSub", PubsubIO
                .readMessagesWithAttributes()
                .fromSubscription(options.getSubscription()));

        pubsubMessages
                .apply("Filter only ADD FILE", ParDo.of(new FilterObjectFinalizeMessage()))
                .apply("Deserialize messages", ParDo.of(new DecodeNotificationJsonMessage()))
                .apply("Get data from FastQ", ParDo.of(new GetDataFromFastQFile()))
                .apply("Parse FasQ data", ParDo.of(new ParseFastQFn()))
                .apply(
                        options.getResistanceGenesWindowTime() + "s FastQ collect window",
                        Window.into(FixedWindows.of(Duration.standardSeconds(options.getResistanceGenesWindowTime()))))
                .apply("Accumulate to iterable", Combine.globally(new CombineIterableAccumulatorFn<FastqRecord>())
                        .withoutDefaults())
                .apply("Alignment",
                        ParDo.of(new MakeAlignmentViaHttpFn(new AlignerHttpService(new HttpHelper(), options.getResistanceGenesAlignmentDatabase(),
                                options.getResistanceGenesAlignmentServer()))))
                .apply("Generation SAM",
                        ParDo.of(new ParseAlignedDataIntoSAMFn()))

                //TODO temporary output to gcs file for debug
                .apply("Filter only mapped", Filter.by((SerializableFunction<KV<String, SAMRecord>, Boolean>) input -> !input.getValue().getReadUnmappedFlag()))
                .apply("toString()", ParDo.of(new DoFn<KV<String, SAMRecord>, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        SAMRecord data = c.element().getValue();
                        c.output(data.toString() + "\n" + data.getSAMString() + "\n");
                    }
                }))
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
        pipeline.run();
    }
}
