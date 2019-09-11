package com.google.allenday.nanostream;

import com.google.allenday.nanostream.cannabis_source.CannabisSourceFileMetaData;
import com.google.allenday.nanostream.cannabis_source.CannabisSourceMetaData;
import com.google.allenday.nanostream.gcloud.GCSService;
import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.other.Configuration;
import com.google.allenday.nanostream.util.CoderUtils;
import com.google.cloud.storage.BlobId;
import org.apache.beam.runners.dataflow.options.DataflowPipelineOptions;
import org.apache.beam.runners.direct.DirectRunner;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.ToString;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

//TODO

/**
 *
 */
public class NanostreamCannabisCommandsNew {
    private static int counter = 0;

    public static void main(String[] args) {
        String sourceSampleList = "gs://cannabis-3k/Cannabis Genomics - 201703 - SRA.csv";
        String sampleToProcess = "SRS190966";

        DataflowPipelineOptions opt = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(DataflowPipelineOptions.class);
        Pipeline pipeline = Pipeline.create();
        opt.setProject("cannabis-3k");
        opt.setRunner(DirectRunner.class);
        CoderUtils.setupCoders(pipeline, new SequenceOnlyDNACoder());


        pipeline
                .apply("Source file name reading", TextIO.read().from(sourceSampleList))
                .apply("Parse source CSV", ParDo.of(new DoFn<String, Iterable<CannabisSourceFileMetaData>>() {

                    private Logger LOG = LoggerFactory.getLogger(NanostreamCannabisCommandsNew.class);

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String dataLine = c.element();
                        try {
                            List<CannabisSourceFileMetaData> cannabisSourceFileMetaData = CannabisSourceFileMetaData.fromCSVLine(dataLine);
                            c.output(CannabisSourceFileMetaData.fromCSVLine(dataLine));
                        } catch (Exception e) {
                            LOG.error(e.getMessage());
                        }
                    }
                }))
                .apply("Add key as ReadName", ParDo.of(new DoFn<Iterable<CannabisSourceFileMetaData>, String>() {
                    private Logger LOG = LoggerFactory.getLogger(NanostreamCannabisCommandsNew.class);

                    private GCSService gcsService;

                    @Setup
                    public void setup() {
                        gcsService = GCSService.initialize();
                    }

                    @ProcessElement
                    public void processElement(ProcessContext c) {


                        Iterable<CannabisSourceFileMetaData> data = c.element();

                        CannabisSourceMetaData metaData = data.iterator().next().getCannabisSourceMetaData();

                        counter++;
                        LOG.info(String.format("Element: %d;\n Data: %s", counter,
                                metaData));

                        StringBuilder builder = new StringBuilder();
                        builder.append("# ").append(metaData.getRun()).append("\n");
                        boolean allPresent = true;

                        List<String> fastqNames = new ArrayList<>();
                        for (CannabisSourceFileMetaData item : data) {
                            String blobName = item.generateGCSBlobName();
                            String[] blobNameParts = blobName.split("/");
                            fastqNames.add(blobNameParts[blobNameParts.length - 1]);
                            builder
                                    .append("gsutil -m cp ")
                                    .append("gs://cannabis-3k/").append(blobName)
                                    .append(" .")
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
                                            resultFileName))
                                    .append(String.format("&& gsutil -m mv %s.sorted.bam gs://cannabis-3k-results/manual/bam/%s.sorted.bam",
                                            resultFileName, resultFileName));
                            if (i < Configuration.REF_DB_ARRAY.length - 1) {
                                builder.append(" && ");
                            }

                            if (allPresent) {
                                String gsOutput = String.format("gs://cannabis-3k-results/manual/bam/%s.sorted.bam", resultFileName);
                                String mainPart = gsOutput.split("//")[1];
                                String bucketName = mainPart.split("/")[0];
                                String blobName = mainPart.replace(bucketName + "/", "");
                                BlobId blobId = BlobId.of(bucketName, blobName);
                                if (!gcsService.isExists(blobId)) {
                                    allPresent = false;
                                }
                            }
                        }

                        for (String name : fastqNames) {
                            builder.append(String.format(" && rm -f %s", name));
                        }
                        if (!allPresent) {
                            c.output(builder.toString());
                        }
                    }
                }))
                .apply("toString()", ToString.elements())
                .apply("Write out", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("NanostreamCannabisCommandsNew_result"));

        pipeline.run();
    }
}
