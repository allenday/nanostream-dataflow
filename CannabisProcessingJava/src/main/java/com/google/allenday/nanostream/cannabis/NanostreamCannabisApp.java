package com.google.allenday.nanostream.cannabis;

import com.google.allenday.genomics.core.csv.ParseSourceCsvTransform;
import com.google.allenday.genomics.core.model.ReferenceDatabase;
import com.google.allenday.genomics.core.pipeline.PipelineSetupUtils;
import com.google.allenday.genomics.core.processing.AlignAndPostProcessTransform;
import com.google.allenday.genomics.core.processing.other.DeepVariantFn;
import com.google.allenday.genomics.core.processing.vcf_to_bq.VcfToBqFn;
import com.google.allenday.genomics.core.utils.NameProvider;
import com.google.allenday.nanostream.cannabis.vcf_to_bq.DvAndVcfToBqConnector;
import com.google.inject.Guice;
import com.google.inject.Injector;
import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.MapElements;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import java.util.ArrayList;

public class NanostreamCannabisApp {

    private final static String JOB_NAME_PREFIX = "nanostream-cannabis--";

    public static void main(String[] args) {

        NanostreamCannabisPipelineOptions pipelineOptions = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamCannabisPipelineOptions.class);
        PipelineSetupUtils.prepareForInlineAlignment(pipelineOptions);

        Injector injector = Guice.createInjector(new NanostreamCannabisModule.Builder()
                .setFromOptions(pipelineOptions)
                .build());

        NameProvider nameProvider = injector.getInstance(NameProvider.class);
        pipelineOptions.setJobName(nameProvider.buildJobName(JOB_NAME_PREFIX, pipelineOptions.getSraSamplesToFilter()));

        Pipeline pipeline = Pipeline.create(pipelineOptions);

        PCollection<KV<ReferenceDatabase, String>> vcfResults = pipeline
//                .apply("Parse data", injector.getInstance(ParseSourceCsvTransform.class))
//                .apply("Align reads and prepare for DV", injector.getInstance(AlignAndPostProcessTransform.class))
//                .apply("Variant Calling", ParDo.of(injector.getInstance(DeepVariantFn.class)))
//                .apply("Prepare to VcfToBq transform", MapElements.via(new DvAndVcfToBqConnector()));
        .apply(Create.of(KV.of(new ReferenceDatabase("AGQN03", new ArrayList<>()), "gs://cannabis-3k-results/cannabis_processing_output__test/2020-01-03--13-23-04-UTC/result_dv/SRS1107977_AGQN03/SRS1107977_AGQN03.vcf")));
        if (pipelineOptions.getExportVcfToBq()) {
            vcfResults
                    .apply("Export to BigQuery", ParDo.of(injector.getInstance(VcfToBqFn.class)));
        }

        pipeline.run();
    }
}
