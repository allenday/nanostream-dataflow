package com.google.allenday.nanostream;

import com.google.allenday.nanostream.aligner.ComposeAlignedDataDoFn;
import com.google.allenday.nanostream.aligner.MakeAlignmentViaPubSubDoFn;
import com.google.allenday.nanostream.aligner.SaveInterleavedFastQDataToGCSDoFn;
import com.google.allenday.nanostream.cannabis_source.CannabisSourceFileMetaData;
import com.google.allenday.nanostream.cannabis_source.CannabisSourceMetaData;
import com.google.allenday.nanostream.cannabis_source.ParseSourceCsvFn;
import com.google.allenday.nanostream.fastq.ParseFastQFn;
import com.google.allenday.nanostream.gcloud.GetDataFromFastQFileCannabis;
import com.google.allenday.nanostream.injection.MainCannabisModule;
import com.google.allenday.nanostream.io.WindowedFilenamePolicy;
import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.samtools.SamtoolsViaPubSubDoFn;
import com.google.allenday.nanostream.util.CoderUtils;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.FileBasedSink;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;

import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.allenday.nanostream.other.Configuration.BAM_SORTED_DATA_FOLDER_NAME;

//TODO

/**
 *
 */
public class NanostreamCannabisApp {

    public static void main(String[] args) {
        String sourceSampleList = "gs://cannabis-3k/Cannabis Genomics - 201703 - SRA.csv";

        NanostreamCannabisPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamCannabisPipelineOptions.class);
        options.setRunner(DirectRunner.class);
//        Injector injector = Guice.createInjector(new MainCannabisModule.Builder().buildFromOptions(options));

        /*options.setJobName(injector.getInstance(EntityNamer.class)
                .generateTimestampedName(sampleToProcess));
*/
        Pipeline pipeline = Pipeline.create(options);
        CoderUtils.setupCoders(pipeline, new SequenceOnlyDNACoder());

        pipeline
                .apply("Source file name reading", TextIO.read().from(sourceSampleList))
                .apply("Parse source CSV", ParDo.of(new ParseSourceCsvFn()))
                /*.apply("Get data from FastQ", ParDo.of(new GetDataFromFastQFileCannabis()))
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
                            c.output(KV.of(Objects.requireNonNull(valueItem.getKey()).getCannabisSourceMetaData(),
                                    StreamSupport.stream(data.getValue().spliterator(), false)
                                            .map(kv -> KV.of(kv.getValue(), Objects.requireNonNull(kv.getKey()).getPairedIndex()))
                                            .collect(Collectors.toList())));
                        }

                    }
                }))
                .apply("Create batches of " + options.getAlignmentBatchSize() + " FastQ records",
                        GroupIntoBatches.ofSize(options.getAlignmentBatchSize()))
                .apply("Save interleaved fastq data", ParDo.of(injector.getInstance(SaveInterleavedFastQDataToGCSDoFn.class)))
                .apply("Alignment", ParDo.of(injector.getInstance(MakeAlignmentViaPubSubDoFn.class)))
                .apply("Group by reference DB", GroupByKey.create())
                .apply("Compose SAM data", ParDo.of(injector.getInstance(ComposeAlignedDataDoFn.class)))
                .apply("SAM to BAM", ParDo.of(new SamtoolsViaPubSubDoFn(options.getProject(), options.getSamtoolsTopicId(),
                        options.getResultBucket(), "samtools view -b ${input}", BAM_SORTED_DATA_FOLDER_NAME)))*/
                /*.apply("Concat SAM", ParDo.of(new DoFn<KV<CannabisSourceMetaData, String>, KV<CannabisSourceMetaData, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<CannabisSourceMetaData, String> data = c.element();

                        StringBuilder headersBuilder = new StringBuilder();
                        StringBuilder samDataBuilder = new StringBuilder();

                        for (String line : data.getValue().split("\n")) {
                            if (line.substring(0, 1).equals("@")) {
                                headersBuilder.append(line).append("\n");
                            } else {
                                samDataBuilder.append(line).append("\n");
                            }
                        }
                        c.output(headersTag, KV.of(data.getKey(), samDataBuilder.toString()));
                        c.output(KV.of(data.getKey(), samDataBuilder.toString()));
                    }
                }))
                .apply("Group by example read", GroupIntoBatches.ofSize(100))
                .apply("Concat SAM", ParDo.of(new DoFn<KV<CannabisSourceMetaData, Iterable<String>>, KV<CannabisSourceMetaData, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<CannabisSourceMetaData, Iterable<String>> data = c.element();
                        if (StreamSupport.stream(data.getValue().spliterator(), false).count() > 0) {
                            StringBuilder samDataBuilder = new StringBuilder();
                            StreamSupport.stream(data.getValue().spliterator(), false)
                                    .forEach(samDataBuilder::append);
                            c.output(KV.of(data.getKey(), samDataBuilder.toString()));
                        }
                    }
                }))
                .apply("Write to GCS", ParDo.of(new DoFn<KV<CannabisSourceMetaData, String>, KV<CannabisSourceMetaData, String>>() {

                    private GCSService gcsService;

                    @Setup
                    public void setup() {
                        gcsService = GCSService.initialize();
                    }

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<CannabisSourceMetaData, String> data = c.element();
                        Blob blob = gcsService.saveToGcs("cannabis-3k-results",
                                "sam/"+data.getKey().generateUniqueFolderAndNameForSampleAndReadGroup()+".sam", data.getValue().getBytes());
                        c.output(KV.of(data.getKey(), blob.getBlobId().toString()));
                    }
                }))*/
                .apply("toString()", ToString.elements())
                .apply(TextIO.write().to("result-cannabis"));
                /*.apply("Write to GCS", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to(
                                new WindowedFilenamePolicy(
                                        "gs://cannabis-3k-results/logs",
                                        "logs",
                                        "W-P-SS-of-NN",
                                        ""))
                        .withTempDirectory(ValueProvider.NestedValueProvider.of(
                                ValueProvider.StaticValueProvider.of("gs://cannabis-3k-results/logs"),
                                (SerializableFunction<String, ResourceId>) FileBasedSink::convertToFileResourceIfPossible)));*/

        pipeline.run();
    }
}
