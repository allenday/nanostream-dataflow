package com.google.allenday.nanostream.main.errorcorrection;

import com.google.allenday.nanostream.errorcorrection.ErrorCorrectionFn;
import com.google.allenday.nanostream.kalign.SequenceOnlyDNACoder;
import com.google.allenday.nanostream.main.injection.TestModule;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.inject.Guice;
import com.google.inject.Injector;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

/**
 * Tests process of {@link Sequence} merging and error correction in {@link ErrorCorrectionFn}
 */
public class ErrorCorrectionFnTest {

    private final static String TEST_SEQUENCE_1 = "-AA-C-CGGTGG--TTCGCGG-G";
    private final static String TEST_SEQUENCE_2 = "GAA-CGCGGTGAACTT-GCGG-G";
    private final static String TEST_SEQUENCE_3 = "-AATC-TGGTG--CT----GGTG";

    private final static String MERGED_SEQUENCE = "AACCGGTGACTTGCGGG";

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testSingleItemErrorCorrection() {
        Injector injector = Guice.createInjector(new TestModule.Builder().build());

        GCSSourceData mockGCSSourceData = injector.getInstance(GCSSourceData.class);

        String geneId = "test_gene_id";
        String sequesnceName = "test_sequnce_name";
        Sequence testSequence1 = new Sequence(Alphabet.DNA(), TEST_SEQUENCE_1, sequesnceName);

        testErrorCorrection(KV.of(KV.of(mockGCSSourceData, geneId), Collections.singletonList(testSequence1)),
                input -> {
                    Assert.assertTrue(input.iterator().hasNext());
                    KV<KV<GCSSourceData, String>, Sequence> resultData = input.iterator().next();
                    Assert.assertEquals(geneId, resultData.getKey().getValue());

                    Sequence sequence = resultData.getValue();
                    Assert.assertEquals(TEST_SEQUENCE_1, sequence.toString());
                    Assert.assertEquals(sequesnceName, sequence.getName());
                    return null;
                }, injector);
    }

    @Test
    public void testMultipleItemErrorCorrection() {
        Injector injector = Guice.createInjector(new TestModule.Builder().build());
        GCSSourceData mockGCSSourceData = injector.getInstance(GCSSourceData.class);

        String geneId = "test_gene_id";
        String sequesnceName = "test_sequnce_name";
        Sequence testSequence1 = new Sequence(Alphabet.DNA(), TEST_SEQUENCE_1, sequesnceName);
        Sequence testSequence2 = new Sequence(Alphabet.DNA(), TEST_SEQUENCE_2, sequesnceName);
        Sequence testSequence3 = new Sequence(Alphabet.DNA(), TEST_SEQUENCE_3, sequesnceName);

        testErrorCorrection(KV.of(KV.of(mockGCSSourceData, geneId), Arrays.asList(testSequence1, testSequence2, testSequence3)),
                input -> {
                    Assert.assertTrue(input.iterator().hasNext());
                    KV<KV<GCSSourceData, String>, Sequence> resultData = input.iterator().next();
                    Assert.assertEquals(geneId, resultData.getKey().getValue());

                    Sequence sequence = resultData.getValue();
                    Assert.assertEquals(MERGED_SEQUENCE, sequence.toString());
                    return null;
                }, injector);
    }


    private void testErrorCorrection(KV<KV<GCSSourceData, String>, Iterable<Sequence>> sourceData,
                                     SerializableFunction<Iterable<KV<KV<GCSSourceData, String>, Sequence>>, Void> assertFunction,
                                     Injector injector) {
        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        testPipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollection<KV<KV<GCSSourceData, String>, Sequence>> parsedFastQ = testPipeline
                .apply(Create.of(sourceData))
                .apply(ParDo.of(injector.getInstance(ErrorCorrectionFn.class)));
        PAssert.that(parsedFastQ)
                .satisfies(assertFunction);

        testPipeline.run();
    }
}
