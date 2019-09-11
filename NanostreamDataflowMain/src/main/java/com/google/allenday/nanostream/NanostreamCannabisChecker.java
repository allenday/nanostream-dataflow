package com.google.allenday.nanostream;

import com.google.allenday.nanostream.cannabis_source.CannabisSourceFileMetaData;
import com.google.allenday.nanostream.cannabis_source.CannabisSourceMetaData;
import com.google.allenday.nanostream.gcloud.GCSService;
import com.google.allenday.nanostream.injection.MainCannabisModule;
import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.other.Configuration;
import com.google.allenday.nanostream.pubsub.GCSUtils;
import com.google.allenday.nanostream.util.CoderUtils;
import com.google.allenday.nanostream.util.EntityNamer;
import com.google.cloud.storage.BlobId;
import com.google.inject.Guice;
import com.google.inject.Injector;
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
public class NanostreamCannabisChecker {

    public static void main(String[] args) {
        String sourceSampleList = "gs://cannabis-3k-results/nanostream_cannabis_commands_merge_and_sort_with_SRP064442.txt";
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
                .apply(ParDo.of(new DoFn<String, KV<String, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String dataLine = c.element();
                        String[] s = dataLine.split(" ");
                       /* Stream.of(s).filter(item -> item.contains(".sorted.merged.bam") && item.contains("gs://")).findFirst()
                                .ifPresent(item -> c.output(KV.of(item, dataLine)));*/
                        Stream.of(s).filter(item -> item.contains(".sorted.bam") && item.contains("gs://")).
                                forEach(item -> c.output(KV.of(item, dataLine)));
                    }
                }))
                .apply(ParDo.of(new DoFn<KV<String, String>, String>() {
                    private Logger LOG = LoggerFactory.getLogger(NanostreamCannabisUtilsApp.class);

                    private GCSService gcsService;

                    @Setup
                    public void setup() {
                        gcsService = GCSService.initialize();
                    }

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<String, String> data = c.element();

                        String mainPart = data.getKey().split("//")[1];
                        String bucketName = mainPart.split("/")[0];
                        String blobName = mainPart.replace(bucketName + "/", "");
                        BlobId blobId = BlobId.of(bucketName, blobName);
                        if (!gcsService.isExists(blobId)) {
                            LOG.info(data.toString() + " NOT exists");
                            c.output(data.toString());
                        } else {
                            LOG.info(blobId.toString() + " exists");
                        }
                    }
                }))
                .apply("toString()", ToString.elements())
                .apply("Write out", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));

        pipeline.run();
    }
}
