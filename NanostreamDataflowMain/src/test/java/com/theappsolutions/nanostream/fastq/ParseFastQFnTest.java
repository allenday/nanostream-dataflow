package com.theappsolutions.nanostream.fastq;

import com.theappsolutions.nanostream.fastq.ParseFastQFn;
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

import static com.google.common.base.Charsets.UTF_8;

/**
 * Tests process of fastq file parsing into FastqRecord object
 */
public class ParseFastQFnTest {

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testFastQDataParsedCorrectly() {
        try {
            // TODO: I think we need 2 cases with incorrect fastq file(containing garbage like "XXXX"), and with correct file
            String data = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("testFastQFile.fastq"), UTF_8.name());

            String[] assertData = IOUtils.toString(
                    getClass().getClassLoader().getResourceAsStream("fasqQOutputData.txt"), UTF_8.name())
                    .split("\n");

            PCollection<FastqRecord> parsedFastQ = testPipeline
                    .apply(Create.of(data))
                    .apply(ParDo.of(new ParseFastQFn()));

            PAssert.that(parsedFastQ)
                    .satisfies((SerializableFunction<Iterable<FastqRecord>, Void>) input -> {
                        FastqRecord fastqRecord = input.iterator().next();

                        Assert.assertEquals(assertData[0],
                                fastqRecord.getReadName());
                        Assert.assertEquals(assertData[1],
                                fastqRecord.getReadString());
                        Assert.assertEquals(assertData[2],
                                fastqRecord.getBaseQualityHeader());
                        Assert.assertEquals(assertData[3],
                                fastqRecord.getBaseQualityString());
                        return null;
                    });
        } catch (IOException e) {
            e.printStackTrace();
        }

        // TODO: I think there is no sense in running a test pipeline if an exception has been already thrown
        // better don't try to catch an exception in the test, leave it to test runner
        testPipeline.run();
    }
}
