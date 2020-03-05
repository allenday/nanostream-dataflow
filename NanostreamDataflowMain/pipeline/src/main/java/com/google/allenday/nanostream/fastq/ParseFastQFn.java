package com.google.allenday.nanostream.fastq;

import com.google.allenday.nanostream.gcs.GCSSourceData;
import com.google.allenday.nanostream.util.FastQUtils;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

/**
 * Creates {@link FastqRecord} instance from .fastq file data
 */
public class ParseFastQFn extends DoFn<KV<GCSSourceData, String>, KV<GCSSourceData, FastqRecord>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        FastQUtils.splitMultiStrandFastq(c.element().getValue(), fastQ -> c.output(KV.of(c.element().getKey(), fastQ)));
    }
}
