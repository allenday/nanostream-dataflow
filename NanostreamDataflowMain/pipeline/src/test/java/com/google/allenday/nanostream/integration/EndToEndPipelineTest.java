package com.google.allenday.nanostream.integration;

import com.google.allenday.nanostream.ProcessingMode;
import org.apache.beam.sdk.testing.TestPipeline;
import org.junit.Rule;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

/**
 * Tests full pipeline lifecycle in DirectRunner mode
 */
public class EndToEndPipelineTest {

    private final static int FASTQ_GROUPING_WINDOW_TIME_SEC = 20;
    private final static int OUTPUT_TRIGGERING_WINDOW_TIME_SEC = 10;

    private String TEST_BUCKET_NAME = "test_bucket";
    private String TEST_FOLDER_NAME = "/test/folder";

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create();

    public enum Param {
        PROJECT_ID("projectId"),
        RESULT_BUCKET("resultBucket"),
        REFERENCE_NAME_LIST("referenceNamesList"),
        ALL_REFERENCES_GCS_URI("allReferencesDirGcsUri"),
        ALIGNED_OUTPUT_DIR("alignedOutputDir");

        public final String key;

        Param(String key) {
            this.key = key;
        }

        public static String getValueFromMap(Map<String, String> map, Param param) {
            return map.get(param.key);
        }
    }

    @Test
    public void testEndToEndPipelineSpeciesMode() {
        Map<String, String> testParams = new HashMap<>();
        for (Param param : Param.values()) {
            String value = System.getProperty(param.key);
            if (value == null || value.isEmpty()) {
                throw new RuntimeException(String.format("You should provide %s", param.name()));
            }
            testParams.put(param.key, value);
        }
        ProcessingMode processingMode = ProcessingMode.SPECIES;
        //TODO update endToendTEst

        /*Injector injector = Guice.createInjector(new MainModule.Builder()
                .setProjectId(Param.getValueFromMap(testParams, Param.PROJECT_ID))
                .setProcessingMode(processingMode)
                .setAlignerOptions(new GenomicsOptions(Param.getValueFromMap(testParams, Param.RESULT_BUCKET),
                        testPipeline.newProvider(Collections.singletonList(Param.getValueFromMap(testParams, Param.REFERENCE_NAME_LIST))),
                        Param.getValueFromMap(testParams, Param.ALL_REFERENCES_GCS_URI),
                        Param.getValueFromMap(testParams, Param.ALIGNED_OUTPUT_DIR),
                        0
                ))
                .build());

        GCSSourceData gcsSourceData = new GCSSourceData(TEST_BUCKET_NAME, TEST_FOLDER_NAME);

        CoderUtils.setupCoders(testPipeline, new SequenceOnlyDNACoder());

        PCollection<KV<KV<String, String>, SequenceStatisticResult>> sequnceStatisticResultPCollection = testPipeline
                .apply(Create.of(KV.of(gcsSourceData, new ResourcesHelper().getFileContent("testFastQFile.fastq"))))
                .apply("Parse FasQ data", ParDo.of(new DoFn<KV<GCSSourceData, String>,
                        KV<SampleMetaData, List<FileWrapper>>>() {

                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        KV<GCSSourceData, String> element = c.element();
                        FileWrapper fileWrapper =
                                FileWrapper.fromByteArrayContent(element.getValue().getBytes(), "fileName");
                        SampleMetaData geneExampleMetaData = new SampleMetaData();
                        geneExampleMetaData.setSraStudy("TestProject");
                        geneExampleMetaData.setSraSample(SraSampleId.create("testExampleSra"));
                        geneExampleMetaData.setRunId("TestRun");
                        geneExampleMetaData.setLibraryLayout("SINGLE");
                        geneExampleMetaData.setSrcRawMetaData(c.element().getKey().toJsonString());
                        c.output(KV.of(geneExampleMetaData, Collections.singletonList(fileWrapper)));
                    }
                }))
                .apply(FASTQ_GROUPING_WINDOW_TIME_SEC + " Window",
                        Window.into(FixedWindows.of(Duration.standardSeconds(FASTQ_GROUPING_WINDOW_TIME_SEC))))
                .apply("Alignment", injector.getInstance(AlignTransform.class))
                .apply("Extract Sequences",
                        ParDo.of(injector.getInstance(GetReferencesFromSamDataFn.class)))
                .apply("Get Taxonomy data", ParDo.of(injector.getInstance(GetTaxonomyFromTree.class)))
                .apply("Global Window with Repeatedly triggering" + OUTPUT_TRIGGERING_WINDOW_TIME_SEC,
                        Window.<KV<KV<GCSSourceData, String>, GeneData>>into(new GlobalWindows())
                                .triggering(Repeatedly.forever(AfterProcessingTime
                                        .pastFirstElementInPane()
                                        .plusDelayOf(Duration.standardSeconds(OUTPUT_TRIGGERING_WINDOW_TIME_SEC))))
                                .withAllowedLateness(Duration.ZERO)
                                .accumulatingFiredPanes())
                .apply("Accumulate results to Map", Combine.globally(new KVCalculationAccumulatorFn()))
                .apply("Flatten result map", ParDo.of(new FlattenMapToKV<>()))
                .apply("Prepare sequences statistic to output", ParDo.of(injector.getInstance(PrepareSequencesStatisticToOutputDbFn.class)));

        PAssert.that(sequnceStatisticResultPCollection)
                .satisfies((SerializableFunction<Iterable<KV<KV<String, String>, SequenceStatisticResult>>, Void>) input -> {
                    List<KV<KV<String, String>, SequenceStatisticResult>> result = StreamSupport.stream(input.spliterator(), false)
                            .collect(Collectors.toList());
                    Assert.assertNotNull(result);
                    return null;
                });

        PipelineResult result = testPipeline.run();
        result.waitUntilFinish();*/
    }
}
