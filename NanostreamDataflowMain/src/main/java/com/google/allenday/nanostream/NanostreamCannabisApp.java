package com.google.allenday.nanostream;

import com.google.allenday.nanostream.aligner.GetSequencesFromSamDataFnCannabis;
import com.google.allenday.nanostream.aligner.MakeAlignmentViaHttpFnCannabis;
import com.google.allenday.nanostream.cannabis_source.CannabisSourceFileMetaData;
import com.google.allenday.nanostream.cannabis_source.CannabisSourceMetaData;
import com.google.allenday.nanostream.cannabis_source.ParseSourceCsvFn;
import com.google.allenday.nanostream.fastq.ParseFastQFn;
import com.google.allenday.nanostream.gcs.GetDataFromFastQFileCannabis;
import com.google.allenday.nanostream.injection.MainCannabisModule;
import com.google.allenday.nanostream.io.WindowedFilenamePolicy;
import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.util.CoderUtils;
import com.google.inject.Guice;
import com.google.inject.Injector;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileBasedSink;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

//TODO

/**
 *
 */
public class NanostreamCannabisApp {

    public static void main(String[] args) {
        NanostreamCannabisPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamCannabisPipelineOptions.class);
        Injector injector = Guice.createInjector(new MainCannabisModule.Builder().buildFromOptions(options));

        Pipeline pipeline = Pipeline.create(options);
        CoderUtils.setupCoders(pipeline, new SequenceOnlyDNACoder());
        PCollection<String> source_file_reading = pipeline
                .apply("Source file name reading", TextIO.read().from("gs://nano-stream-cannabis/CannabisGenomics-201703-Sheet1.csv"));
        source_file_reading.
                apply("Parse source CSV", ParDo.of(new ParseSourceCsvFn()))
                .apply("Get data from FastQ", ParDo.of(new GetDataFromFastQFileCannabis()))
                .apply("Parse FastQ data", ParDo.of(new ParseFastQFn<>()))
                .apply("Add key as ReadName", (ParDo.of(new DoFn<KV<CannabisSourceFileMetaData, FastqRecord>, KV<String, KV<CannabisSourceFileMetaData, FastqRecord>>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<CannabisSourceFileMetaData, FastqRecord> data = c.element();
                        c.output(KV.of(data.getValue().getReadName(), data));
                    }
                })))
                .apply("Group by ReadName", GroupByKey.create())
                .apply("Reorganize data", ParDo.of(new DoFn<KV<String, Iterable<KV<CannabisSourceFileMetaData, FastqRecord>>>,
                        KV<CannabisSourceMetaData, Iterable<KV<FastqRecord, Integer>>>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<String, Iterable<KV<CannabisSourceFileMetaData, FastqRecord>>> data = c.element();
                        KV<CannabisSourceFileMetaData, FastqRecord> valueItem = data.getValue().iterator().next();
                        if (valueItem != null) {
                            c.output(KV.of(valueItem.getKey().getCannabisSourceMetaData(),
                                    StreamSupport.stream(data.getValue().spliterator(), false)
                                            .map(kv -> KV.of(kv.getValue(), kv.getKey().getPairedIndex()))
                                            .collect(Collectors.toList())));
                        }

                    }
                }))
                .apply("Create batches of " + options.getAlignmentBatchSize() + " FastQ records",
                        GroupIntoBatches.ofSize(options.getAlignmentBatchSize()))
                .apply("Alignment", ParDo.of(injector.getInstance(MakeAlignmentViaHttpFnCannabis.class)))
                .apply("Extract Sequences",
                        ParDo.of(new GetSequencesFromSamDataFnCannabis<>()))
                .apply(ParDo.of(new DoFn<KV<KV<CannabisSourceMetaData, String>, String>, KV<CannabisSourceMetaData, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        c.output(KV.of(c.element().getKey().getKey(), c.element().getValue()));
                    }
                }))

                .apply("Group by CannabisSourceMetaData", GroupByKey.create())
                .apply("Summarize data", ParDo.of(new DoFn<KV<CannabisSourceMetaData, Iterable<String>>, KV<CannabisSourceMetaData, Long>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<CannabisSourceMetaData, Iterable<String>> element = c.element();
                        c.output(KV.of(element.getKey(), StreamSupport.stream(element.getValue().spliterator(), false).count()));

                    }
                }))
                .apply("toString()", ToString.elements())
                .apply("Write to GCS", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to(
                                new WindowedFilenamePolicy(
                                        "gs://nano-stream-cannabis/output_full",
                                        "output_full",
                                        "W-P-SS-of-NN",
                                        ""))
                        .withTempDirectory(ValueProvider.NestedValueProvider.of(
                                ValueProvider.StaticValueProvider.of("gs://nano-stream-cannabis/output_full"),
                                (SerializableFunction<String, ResourceId>) FileBasedSink::convertToFileResourceIfPossible)));

        pipeline.run();
    }
}
