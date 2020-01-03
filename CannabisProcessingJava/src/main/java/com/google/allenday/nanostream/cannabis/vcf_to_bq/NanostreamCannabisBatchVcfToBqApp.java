package com.google.allenday.nanostream.cannabis.vcf_to_bq;

import com.google.allenday.genomics.core.pipeline.GenomicsOptions;
import com.google.allenday.genomics.core.pipeline.PipelineSetupUtils;
import com.google.allenday.genomics.core.processing.vcf_to_bq.VcfToBqBatchTransform;
import com.google.allenday.genomics.core.utils.NameProvider;
import com.google.cloud.storage.BlobId;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NanostreamCannabisBatchVcfToBqApp {
    private static Logger LOG = LoggerFactory.getLogger(NanostreamCannabisBatchVcfToBqApp.class);

    private final static String JOB_NAME_PREFIX_PATTERN = "nanostream-cannabis--batch-vcf-to-bq-%s";

    public static void main(String[] args) {

        NanostreamCannabisPipelineVcfToBqOptions pipelineOptions = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamCannabisPipelineVcfToBqOptions.class);
        PipelineSetupUtils.prepareForInlineAlignment(pipelineOptions);

        Injector injector = Guice.createInjector(new NanostreamCannabisVcfToBqModule.Builder()
                .setFromOptions(pipelineOptions)
                .build());

        NameProvider nameProvider = injector.getInstance(NameProvider.class);
        for (String refName : pipelineOptions.getReferenceNamesList()) {
            String jobNamePrefix = String.format(JOB_NAME_PREFIX_PATTERN, refName);

            pipelineOptions.setJobName(nameProvider.buildJobName(jobNamePrefix, null));
            Pipeline pipeline = Pipeline.create(pipelineOptions);

            pipeline
                    .apply("Read export arguments", Create.of(KV.of(BlobId.of(pipelineOptions.getSrcBucket(), String.format(pipelineOptions.getVcfPathPattern(), refName)), refName)))
                    .apply("Export VCF to BigQuery", injector.getInstance(VcfToBqBatchTransform.class))
            ;

            pipeline.run();
        }
    }
}
