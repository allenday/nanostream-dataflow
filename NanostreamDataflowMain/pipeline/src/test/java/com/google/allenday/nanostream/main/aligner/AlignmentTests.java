package com.google.allenday.nanostream.main.aligner;

import com.google.allenday.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.google.allenday.nanostream.http.NanostreamHttpService;
import com.google.allenday.nanostream.main.injection.TestModule;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.inject.Guice;
import com.google.inject.Injector;
import htsjdk.samtools.fastq.FastqRecord;
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
import java.util.Collections;
import java.util.Iterator;

import static com.google.common.base.Charsets.UTF_8;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

/**
 * Set of tests for fastq data alignment functionality
 */
public class AlignmentTests implements Serializable {

    private final static String SAM_RESULT = "337f59ec-bdd2-49b2-b16a-f5a990f4d85c 4796b unmapped read.";
    private final static String SAM_REFERENCE = "*";

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testFastQHttpAlignment() {
        TestModule testModule = new TestModule.Builder().build();
        Injector injector = Guice.createInjector(testModule);

        try {
            String[] fastqData = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("fasqQSourceData.txt"), UTF_8.name())
                    .split("\n");

            String fastqAlignmentResult = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("fastQAlignmentResult.txt"), UTF_8.name());

            FastqRecord fastqRecord = new FastqRecord(fastqData[0], fastqData[1], fastqData[2], fastqData[3]);
            Iterable<FastqRecord> fastqRecordIterable = Collections.singletonList(fastqRecord);

            NanostreamHttpService mockHttpService = injector.getInstance(NanostreamHttpService.class);
            when(mockHttpService.generateAlignData(any(), any())).thenReturn(fastqAlignmentResult);

            PCollection<KV<GCSSourceData, String>> parsedFastQ = testPipeline
                    .apply(Create.of(KV.of(injector.getInstance(GCSSourceData.class), fastqRecordIterable)))
                    .apply(ParDo.of(injector.getInstance(MakeAlignmentViaHttpFn.class)));
            PAssert.that(parsedFastQ)
                    .satisfies((SerializableFunction<Iterable<KV<GCSSourceData, String>>, Void>) input -> {
                        Iterator<KV<GCSSourceData, String>> dataIterator = input.iterator();
                        Assert.assertTrue(dataIterator.hasNext());
                        KV<GCSSourceData, String> resultData = dataIterator.next();
                        Assert.assertFalse(dataIterator.hasNext());

                        Assert.assertEquals(fastqAlignmentResult, resultData.getValue());
                        return null;
                    });

            testPipeline.run();
        } catch (IOException | URISyntaxException e) {
            Assert.fail(e.getMessage());
        }
    }

    //TODO
    /*@Test
    public void testAlignedDataParsing() {
        try {
            String fastqAlignmentResult = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("fastQAlignmentResult.txt"), UTF_8.name());
            String samStringResult = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("samStringResult.txt"), UTF_8.name());

            PCollection<KV<String, SAMRecord>> parsedFastQ = testPipeline
                    .apply(Create.of(fastqAlignmentResult))
                    .apply(ParDo.of(new GetSequencesFromSamDataFn()));

            PAssert.that(parsedFastQ)
                    .satisfies(input -> {
                        KV<String, SAMRecord> result = input.iterator().next();
                        Assert.assertEquals(1, StreamSupport.stream(input.spliterator(), false)
                                .count());
                        Assert.assertEquals(SAM_REFERENCE, result.getKey());
                        Assert.assertEquals(SAM_RESULT, result.getValue().toString());
                        Assert.assertEquals(samStringResult, result.getValue().getSAMString());
                        return null;
                    });

            testPipeline.run();
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }*/
}
