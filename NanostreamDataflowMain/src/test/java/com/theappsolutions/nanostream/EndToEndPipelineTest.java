package com.theappsolutions.nanostream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.aligner.ParseAlignedDataIntoSAMFn;
import com.theappsolutions.nanostream.fastq.ParseFastQFn;
import com.theappsolutions.nanostream.injection.MainModule;
import com.theappsolutions.nanostream.kalign.ExtractSequenceFn;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.kalign.SequenceOnlyDNACoder;
import com.theappsolutions.nanostream.util.ResourcesHelper;
import com.theappsolutions.nanostream.util.trasform.CombineIterableAccumulatorFn;
import htsjdk.samtools.fastq.FastqRecord;
import japsa.seq.Sequence;
import org.apache.beam.sdk.PipelineResult;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.windowing.FixedWindows;
import org.apache.beam.sdk.transforms.windowing.Window;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.io.IOUtils;
import org.joda.time.Duration;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.stream.StreamSupport;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Tests full pipeline lifecycle in DirectRunner mode
 */
public class EndToEndPipelineTest {

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create();

    @Test
    public void testEndToEndPipeline() {
        String[] resultData;
        try {
            resultData = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("endToEndResult.txt"), UTF_8.name())
                    .split("\n");
        } catch (IOException e) {
            return;
        }

        Injector injector = Guice.createInjector(new MainModule.Builder()
                .setBwaEndpoint("/cgi-bin/bwa.cgi")
                .setBaseUrl("http://35.241.15.140")
                .setBwaDb("genomeDB.fasta")
                .setKalignEndpoint("/cgi-bin/kalign.cgi").build());

        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        testPipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollection<KV<String, Iterable<Sequence>>> sequenceCollection = testPipeline
                .apply(Create.of(new ResourcesHelper().getFileContent("testFastQFile.fastq")))
                .apply("Parse FasQ data", ParDo.of(new ParseFastQFn()))
                .apply(
                        "30s FastQ collect window",
                        Window.into(FixedWindows.of(Duration.standardSeconds(10))))
                .apply("Accumulate to iterable", Combine.globally(new CombineIterableAccumulatorFn<FastqRecord>())
                        .withoutDefaults())
                .apply("Alignment", ParDo.of((injector.getInstance(MakeAlignmentViaHttpFn.class))))
                .apply("Generation SAM",
                        ParDo.of(new ParseAlignedDataIntoSAMFn()))
                .apply("Group by SAM referance", GroupByKey.create())
                .apply("Extract Sequences",
                        ParDo.of(new ExtractSequenceFn()))
                .apply("Consensus", ParDo.of(injector.getInstance(ProceedKAlignmentFn.class)));
        PAssert.that(sequenceCollection)
                .satisfies((SerializableFunction<Iterable<KV<String, Iterable<Sequence>>>, Void>) input -> {
                    KV<String, Iterable<Sequence>> result = input.iterator().next();
                    Assert.assertNotNull(result);

                    Assert.assertEquals(1, StreamSupport.stream(input.spliterator(), false).count());
                    Assert.assertEquals(resultData[0], result.getKey());
                    Assert.assertEquals(3, StreamSupport.stream(result.getValue().spliterator(), false).count());
                    Assert.assertEquals(resultData[1], result.getValue().toString());
                    return null;
                });

        sequenceCollection.apply("toString()", ToString.elements())
                .apply("Write to file", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));

        PipelineResult result = testPipeline.run();
        result.waitUntilFinish();
    }
}
