package com.theappsolutions.nanostream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.aligner.GetSequencesFromSamDataFn;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.errorcorrection.ErrorCorrectionFn;
import com.theappsolutions.nanostream.fastq.BatchByN;
import com.theappsolutions.nanostream.fastq.ParseFastQFn;
import com.theappsolutions.nanostream.injection.MainModule;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.kalign.SequenceOnlyDNACoder;
import com.theappsolutions.nanostream.output.PrepareSequencesBodiesToOutputDbFn;
import com.theappsolutions.nanostream.output.PrepareSequencesStatisticToOutputDbFn;
import com.theappsolutions.nanostream.output.SequenceBodyResult;
import com.theappsolutions.nanostream.output.SequenceStatisticResult;
import com.theappsolutions.nanostream.probecalculation.KVCalculationAccumulatorFn;
import com.theappsolutions.nanostream.taxonomy.GetSpeciesTaxonomyDataFn;
import com.theappsolutions.nanostream.util.ResourcesHelper;
import com.theappsolutions.nanostream.util.trasform.RemoveValueDoFn;
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

import java.util.List;
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

    @Test
    public void testEndToEndPipelineSpeciesMode() {
        Injector injector = Guice.createInjector(new MainModule.Builder()
                .setProjectId("upwork-nano-stream")
                .setBwaEndpoint("/cgi-bin/bwa.cgi")
                .setServicesUrl("http://35.241.15.140")
                .setBwaDB("genomeDB.fasta")
                .setkAlignEndpoint("/cgi-bin/kalign.cgi")
                .setOutputFirestoreGeneCacheCollection("gene_cache")
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
                .apply("Get Taxonomy data", ParDo.of(injector.getInstance(GetSpeciesTaxonomyDataFn.class)))
                .apply(Window.<KV<String, List<String>>>into(new GlobalWindows())
                        .triggering(Repeatedly.forever(AfterProcessingTime
                                .pastFirstElementInPane()
                                .plusDelayOf(Duration.standardSeconds(OUTPUT_TRIGGERING_WINDOW_TIME_SEC))))
                        .withAllowedLateness(Duration.ZERO)
                        .accumulatingFiredPanes())
                .apply("Accumulate results to Map", Combine.globally(new KVCalculationAccumulatorFn()))
                .apply("Prepare sequences statistic to output",
                        ParDo.of(new PrepareSequencesStatisticToOutputDbFn()));


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
