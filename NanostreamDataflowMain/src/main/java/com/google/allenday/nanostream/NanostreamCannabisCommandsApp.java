package com.google.allenday.nanostream;

import com.google.allenday.nanostream.cannabis_source.CannabisSourceFileMetaData;
import com.google.allenday.nanostream.cannabis_source.CannabisSourceMetaData;
import com.google.allenday.nanostream.injection.MainCannabisModule;
import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.other.Configuration;
import com.google.allenday.nanostream.util.CoderUtils;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.ToString;

import java.util.ArrayList;
import java.util.List;

//TODO

/**
 *
 */
public class NanostreamCannabisCommandsApp {

    public static void main(String[] args) {
        String sourceSampleList = "gs://cannabis-3k/CannabisGenomics-201703-Sheet1.csv";
        String sampleToProcess = "SRS190966";

        NanostreamCannabisPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamCannabisPipelineOptions.class);
        Injector injector = Guice.createInjector(new MainCannabisModule.Builder().buildFromOptions(options));

        options.setJobName(injector.getInstance(EntityNamer.class)
                .generateTimestampedName(sampleToProcess));

        Pipeline pipeline = Pipeline.create(options);
        CoderUtils.setupCoders(pipeline, new SequenceOnlyDNACoder());


        pipeline
                .apply("Source file name reading", TextIO.read().from(sourceSampleList))
                .apply("Parse source CSV", ParDo.of(new DoFn<String, Iterable<CannabisSourceFileMetaData>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String dataLine = c.element();
                        try {
                            List<CannabisSourceFileMetaData> cannabisSourceFileMetaData = CannabisSourceFileMetaData.fromCSVLine(dataLine);
                            if (cannabisSourceFileMetaData.size() >0) {
                                c.output(CannabisSourceFileMetaData.fromCSVLine(dataLine));
                            }
                        } catch (Exception e) {
                        }
                    }
                }))
                .apply("Add key as ReadName", ParDo.of(new DoFn<Iterable<CannabisSourceFileMetaData>, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        Iterable<CannabisSourceFileMetaData> data = c.element();

                        CannabisSourceMetaData metaData = data.iterator().next().getCannabisSourceMetaData();

                        StringBuilder builder = new StringBuilder();
                        //builder.append("# ").append(metaData.getRun()).append("\n");

                        List<String> fastqNames = new ArrayList<>();
                        for (CannabisSourceFileMetaData item : data) {
                            String blobName = item.generateGCSBlobName();
                            String[] blobNameParts = blobName.split("/");
                            fastqNames.add(blobNameParts[blobNameParts.length - 1]);
                            builder
                                    .append("gsutil -m cp ")
                                    .append("gs://cannabis-3k/").append(blobName)
                                    .append(" .")
                                    //.append(" \\\n")
                                    .append(" && ");
                        }

                        for (int i = 0; i < Configuration.REF_DB_ARRAY.length; i++) {
                            String dbName = Configuration.REF_DB_ARRAY[i];
                            String resultFileName = String.format("%s_%s", metaData.getRun(), dbName);

                            builder
                                    .append(String.format("minimap2 -ax sr reference/%s/%s.fa ", dbName, dbName));
                            fastqNames.forEach(fastqName -> builder.append(fastqName).append(" "));
                            builder.append("-R").append(" ")
                                    .append(String.format("\'@RG\\tID:%s\\tPL:ILLUMINA\\tPU:NONE\\tSM:%s\'",
                                            metaData.getRun(),
                                            metaData.getSraSample())).append(" ")
                                    .append(String.format("| samtools view -bh - | samtools sort -m 2G -@ 4 > %s.sorted.bam ",
                                            resultFileName))/*.append(" \\\n")*/
                                    .append(String.format("&& gsutil -m mv %s.sorted.bam gs://cannabis-3k-results/manual/bam/%s.sorted.bam",
                                            resultFileName, resultFileName));
                            if (i < Configuration.REF_DB_ARRAY.length - 1) {
                                builder/*.append(" \\\n")*/.append(" && ");
                            }
                        }

                        for (String name: fastqNames){
                            builder.append(String.format(" && rm -f %s", name));
                        }
                        c.output(builder.toString());
                    }
                }))

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
                .apply("Write out", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));

        pipeline.run();
    }
}
