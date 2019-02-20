package com.google.allenday.nanostream.integration;

import com.google.allenday.nanostream.NanostreamApp;
import com.google.allenday.nanostream.taxonomy.GetTaxonomyFromTree;
import com.google.inject.Guice;
import com.google.inject.Injector;
import com.google.allenday.nanostream.aligner.GetSequencesFromSamDataFn;
import com.google.allenday.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.google.allenday.nanostream.errorcorrection.ErrorCorrectionFn;
import com.google.allenday.nanostream.fastq.BatchByN;
import com.google.allenday.nanostream.fastq.ParseFastQFn;
import com.google.allenday.nanostream.geneinfo.GeneData;
import com.google.allenday.nanostream.injection.MainModule;
import com.google.allenday.nanostream.kalign.ProceedKAlignmentFn;
import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.output.PrepareSequencesBodiesToOutputDbFn;
import com.google.allenday.nanostream.output.PrepareSequencesStatisticToOutputDbFn;
import com.google.allenday.nanostream.output.SequenceBodyResult;
import com.google.allenday.nanostream.output.SequenceStatisticResult;
import com.google.allenday.nanostream.probecalculation.KVCalculationAccumulatorFn;
import com.google.allenday.nanostream.taxonomy.GetSpeciesTaxonomyDataFn;
import com.google.allenday.nanostream.util.ResourcesHelper;
import com.google.allenday.nanostream.util.trasform.RemoveValueDoFn;
import com.google.inject.Guice;
import com.google.inject.Injector;
import japsa.seq.Sequence;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Tests full pipeline lifecycle in DirectRunner mode
 */
public class EndToEndPipelineTest {

    private final static int FASTQ_GROUPING_WINDOW_TIME_SEC = 20;
    private static final int FASTQ_GROUPING_BATCH_SIZE = 10;
    private final static int OUTPUT_TRIGGERING_WINDOW_TIME_SEC = 10;

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create();

    public enum Param {
        PROJECT_ID("projectId"),
        BWA_ENDPOINT("bwaEndpoint"),
        SERVICES_URL("servicesUrl"),
        BWA_DB("bwaDB"),
        K_ALIGN_ENDPOINT("kAlignEndpoint");

        public final String key;

        Param(String key) {
            this.key = key;
        }

        public static String getValueFromMap(Map<String, String> map, Param param){
            return map.get(param.key);
        }
    }

    @Test
    public void testEndToEndPipelineSpeciesMode() {
        Map<String, String> testParams = new HashMap<>();
        for (Param param: Param.values()){
            String value = System.getProperty(param.key);
            if (value == null || value.isEmpty()){
                throw new RuntimeException(String.format("You should provide %s", param.name()));
            }
            testParams.put(param.key, value);
        }

        NanostreamApp.ProcessingMode processingMode = NanostreamApp.ProcessingMode.SPECIES;

        Injector injector = Guice.createInjector(new MainModule.Builder()
                .setProjectId(Param.getValueFromMap(testParams, Param.PROJECT_ID))
                .setBwaEndpoint(Param.getValueFromMap(testParams, Param.BWA_ENDPOINT))
                .setServicesUrl(Param.getValueFromMap(testParams, Param.SERVICES_URL))
                .setBwaDB(Param.getValueFromMap(testParams, Param.BWA_DB))
                .setkAlignEndpoint(Param.getValueFromMap(testParams, Param.K_ALIGN_ENDPOINT))
                .setProcessingMode(processingMode)
                .build());

        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        testPipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollection<KV<String, Sequence>> errorCorrectedCollection = testPipeline
                .apply(Create.of(new ResourcesHelper().getFileContent("testFastQFile.fastq")))
                .apply("Parse FasQ data", ParDo.of(new ParseFastQFn()))
                .apply(FASTQ_GROUPING_WINDOW_TIME_SEC + " Window",
                        Window.into(FixedWindows.of(Duration.standardSeconds(FASTQ_GROUPING_WINDOW_TIME_SEC))))
                .apply("Create batches of "+ FASTQ_GROUPING_BATCH_SIZE +" FastQ records", new BatchByN(FASTQ_GROUPING_BATCH_SIZE))
                .apply("Alignment", ParDo.of(injector.getInstance(MakeAlignmentViaHttpFn.class)))
                .apply("Extract Sequences",
                        ParDo.of(new GetSequencesFromSamDataFn()))
                .apply("Group by SAM reference", GroupByKey.create())
                .apply("K-Align", ParDo.of(injector.getInstance(ProceedKAlignmentFn.class)))
                .apply("Error correction", ParDo.of(new ErrorCorrectionFn()));

        PCollection<KV<String, SequenceStatisticResult>> sequnceStatisticResultPCollection = errorCorrectedCollection
                .apply("Remove Sequence part", ParDo.of(new RemoveValueDoFn<>()))
                .apply("Get Taxonomy data", ParDo.of(new GetTaxonomyFromTree(
                        new ResourcesHelper().getFileContent("common_tree.txt"))))
                .apply("Global Window with Repeatedly triggering" + OUTPUT_TRIGGERING_WINDOW_TIME_SEC,
                        Window.<KV<String, GeneData>>into(new GlobalWindows())
                                .triggering(Repeatedly.forever(AfterProcessingTime
                                        .pastFirstElementInPane()
                                        .plusDelayOf(Duration.standardSeconds(OUTPUT_TRIGGERING_WINDOW_TIME_SEC))))
                                .withAllowedLateness(Duration.ZERO)
                                .accumulatingFiredPanes())
                .apply("Accumulate results to Map", Combine.globally(new KVCalculationAccumulatorFn()))
                .apply("Prepare sequences statistic to output", ParDo.of(injector.getInstance(PrepareSequencesStatisticToOutputDbFn.class)));


        PCollection<KV<String, SequenceBodyResult>> sequnceBodyResultpCollection = errorCorrectedCollection
                .apply("Prepare sequences bodies to output",
                        ParDo.of(new PrepareSequencesBodiesToOutputDbFn()));


        PAssert.that(sequnceBodyResultpCollection)
                .satisfies((SerializableFunction<Iterable<KV<String, SequenceBodyResult>>, Void>) input -> {
                    List<KV<String, SequenceBodyResult>> result = StreamSupport.stream(input.spliterator(), false)
                            .collect(Collectors.toList());
                    Assert.assertNotNull(result);
                    return null;
                });

        PAssert.that(sequnceStatisticResultPCollection)
                .satisfies((SerializableFunction<Iterable<KV<String, SequenceStatisticResult>>, Void>) input -> {
                    List<KV<String, SequenceStatisticResult>> result = StreamSupport.stream(input.spliterator(), false)
                            .collect(Collectors.toList());
                    Assert.assertNotNull(result);
                    return null;
                });

        PipelineResult result = testPipeline.run();
        result.waitUntilFinish();
    }
}
