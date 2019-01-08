package com.theappsolutions.nanostream.kalign;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.http.NanostreamHttpService;
import com.theappsolutions.nanostream.injection.TestModule;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.google.common.base.Charsets.UTF_8;
import static org.mockito.Mockito.*;

/**
 * Set of tests for KAlignment functionality
 */
public class KAlignmentFnTests implements Serializable {

    private final static String TEST_SEQUENCE = "CCCGCTGACGTCGTTCATCCAACCGGTGACTTGCGGGCAAGACAATAAGGCGCGGCCTGACGGCCGCATCG";

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testSingleItemKAlignment() {
        Injector injector = Guice.createInjector(new TestModule.Builder().build());

        String geneId = "test_gene_id";
        String sequesnceName = "test_sequnce_name";
        Sequence testSequence = new Sequence(Alphabet.DNA(), TEST_SEQUENCE, sequesnceName);

        testKAlignment(KV.of(geneId, Collections.singletonList(testSequence)),
                input -> {
                    Assert.assertTrue(input.iterator().hasNext());
                    KV<String, Iterable<Sequence>> resultData = input.iterator().next();
                    Assert.assertEquals(geneId, resultData.getKey());

                    Sequence sequence = resultData.getValue().iterator().next();
                    Assert.assertEquals(TEST_SEQUENCE, sequence.toString());
                    Assert.assertEquals(sequesnceName, sequence.getName());
                    return null;
                }, injector);
    }

    @Test
    public void testMultipleItemKAlignment() {
        Injector injector = Guice.createInjector(new TestModule.Builder().build());

        String geneId = "test_gene_id";
        String sequesnceName = "test_sequnce_name";
        Sequence testSequence = new Sequence(Alphabet.DNA(), TEST_SEQUENCE, sequesnceName);

        try {
            String kAlignmentResult = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("kAlignResult.txt"), UTF_8.name());

            NanostreamHttpService mockHttpService = injector.getInstance(NanostreamHttpService.class);
            when(mockHttpService.generateAlignData(any(), any())).thenReturn(kAlignmentResult);

            testKAlignment(KV.of(geneId, Arrays.asList(testSequence, testSequence)),
                    input -> {
                        Assert.assertTrue(input.iterator().hasNext());
                        KV<String, Iterable<Sequence>> resultData = input.iterator().next();
                        Assert.assertEquals(geneId, resultData.getKey());

                        List<Sequence> sequences = StreamSupport.stream(resultData.getValue().spliterator(), false)
                                .collect(Collectors.toList());
                        Assert.assertEquals(3, sequences.size());
                        Assert.assertEquals(0, sequences.stream().filter(Objects::isNull).count());
                        return null;
                    }, injector);
        } catch (IOException | URISyntaxException e) {
            Assert.fail(e.getMessage());
        }
    }


    private void testKAlignment(KV<String, Iterable<Sequence>> sourceData,
                                SerializableFunction<Iterable<KV<String, Iterable<Sequence>>>, Void> assertFunction,
                                Injector injector) {
        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        testPipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollection<KV<String, Iterable<Sequence>>> parsedFastQ = testPipeline
                .apply(Create.of(sourceData))
                .apply(ParDo.of(injector.getInstance(ProceedKAlignmentFn.class)));
        PAssert.that(parsedFastQ)
                .satisfies(assertFunction);

        testPipeline.run();
    }
}
