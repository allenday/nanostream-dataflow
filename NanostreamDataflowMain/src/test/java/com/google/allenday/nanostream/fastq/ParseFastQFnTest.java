package com.google.allenday.nanostream.fastq;

import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Iterator;
import java.util.stream.IntStream;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Tests process of fastq file parsing into FastqRecord object
 */
public class ParseFastQFnTest {

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testFastQDataParsedCorrectly() throws IOException {
        testFastQDataParsedCorrectly("testFastQFile.fastq", 1);
    }

    @Test
    public void testFastQDataParsedCorrectlyWithoutTags() throws IOException {
        testFastQDataParsedCorrectly("testFastQFileWithoutTags.fastq", 1);
    }

    @Test
    public void testFastQDataParsedCorrectlyMultiEntity() throws IOException {
        testFastQDataParsedCorrectly("testMultiEntityFastQFile.fastq", 2);
    }


    private void testFastQDataParsedCorrectly(String sourceDataFilename, int outputFastqListSize) throws IOException {
        String data = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream(sourceDataFilename), UTF_8.name());
        String[] assertData = IOUtils.toString(
                getClass().getClassLoader().getResourceAsStream("fasqQOutputData.txt"), UTF_8.name())
                .split("\n");

        PCollection<FastqRecord> parsedFastQ = testPipeline
                .apply(Create.of(data))
                .apply(ParDo.of(new ParseFastQFn()));

        PAssert.that(parsedFastQ)
                .satisfies((SerializableFunction<Iterable<FastqRecord>, Void>) input -> {
                    Iterator<FastqRecord> fastqRecordIterator = input.iterator();
                    IntStream.range(0, outputFastqListSize).forEach(index -> {
                        FastqRecord fastqRecord = fastqRecordIterator.next();
                        Assert.assertEquals(assertData[0],
                                fastqRecord.getReadName());
                        Assert.assertEquals(assertData[1],
                                fastqRecord.getReadString());
                        Assert.assertEquals(assertData[2],
                                fastqRecord.getBaseQualityHeader());
                        Assert.assertEquals(assertData[3],
                                fastqRecord.getBaseQualityString());
                    });
                    return null;
                });

        testPipeline.run();
    }
}
