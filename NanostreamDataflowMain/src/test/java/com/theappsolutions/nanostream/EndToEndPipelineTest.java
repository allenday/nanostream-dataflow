package com.theappsolutions.nanostream;

import com.google.inject.Guice;
import com.google.inject.Injector;
import com.theappsolutions.nanostream.aligner.MakeAlignmentViaHttpFn;
import com.theappsolutions.nanostream.aligner.ParseAlignedDataIntoSAMFn;
import com.theappsolutions.nanostream.errorcorrection.ErrorCorrectionFn;
import com.theappsolutions.nanostream.fastq.ParseFastQFn;
import com.theappsolutions.nanostream.injection.MainModule;
import com.theappsolutions.nanostream.kalign.ExtractSequenceFn;
import com.theappsolutions.nanostream.kalign.ProceedKAlignmentFn;
import com.theappsolutions.nanostream.kalign.SequenceOnlyDNACoder;
import com.theappsolutions.nanostream.util.ResourcesHelper;
import com.theappsolutions.nanostream.util.trasform.CombineIterableAccumulatorFn;
import htsjdk.samtools.fastq.FastqRecord;
import japsa.seq.Sequence;
import javafx.util.Pair;
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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
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
        List<Pair<String, String>> expectedResultData;
        try {
            expectedResultData =
                    Arrays.stream(IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("endToEndResult.txt"), UTF_8.name())
                            .split("\n"))
                            .map(entry -> {
                                String[] splittedEntry = entry.split(",");
                                return new Pair<>(splittedEntry[0], splittedEntry[1]);
                            }).collect(Collectors.toList());
        } catch (IOException e) {
            return;
        }

        Injector injector = Guice.createInjector(new MainModule.Builder()
                .setBwaEndpoint("/cgi-bin/bwa.cgi")
                .setServicesUrl("http://35.241.15.140")
                .setBwaDB("genomeDB.fasta")
                .setkAlignEndpoint("/cgi-bin/kalign.cgi").build());

        SequenceOnlyDNACoder sequenceOnlyDNACoder = new SequenceOnlyDNACoder();
        testPipeline.getCoderRegistry()
                .registerCoderForType(sequenceOnlyDNACoder.getEncodedTypeDescriptor(), sequenceOnlyDNACoder);

        PCollection<KV<String, Sequence>> sequenceCollection = testPipeline
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
                .apply("K-Align", ParDo.of(injector.getInstance(ProceedKAlignmentFn.class)))
                .apply("Error correction", ParDo.of(new ErrorCorrectionFn()));

        PAssert.that(sequenceCollection)
                .satisfies((SerializableFunction<Iterable<KV<String, Sequence>>, Void>) input -> {
                    List<KV<String, Sequence>> result = StreamSupport.stream(input.spliterator(), false)
                            .collect(Collectors.toList());
                    Assert.assertNotNull(result);
                    Assert.assertEquals(expectedResultData.size(), result.size());

                    for (int i = 0; i < expectedResultData.size(); i++) {
                        Assert.assertEquals(expectedResultData.get(i).getKey(), result.get(i).getKey());
                        Assert.assertEquals(expectedResultData.get(i).getValue(), result.get(i).getValue().toString());
                    }
                    return null;
                });

        sequenceCollection
                .apply("toString()", ToString.elements())
                .apply("Write to file", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));


        //TODO for debugging
       /* testPipeline.apply(new LoadGeneInfoTransform())
                .apply("toString()", ToString.elements())
                .apply("Write to file", TextIO.write()
                        .withWindowedWrites()
                        .withNumShards(1)
                        .to("result"));*/

        PipelineResult result = testPipeline.run();
        result.waitUntilFinish();
    }
}
