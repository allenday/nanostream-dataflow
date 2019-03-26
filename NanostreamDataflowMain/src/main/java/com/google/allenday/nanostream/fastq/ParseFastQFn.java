package com.google.allenday.nanostream.fastq;

import com.google.allenday.nanostream.util.FastQUtils;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

/**
 * Creates {@link FastqRecord} instance from .fastq file data
 */
public class ParseFastQFn<T> extends DoFn<KV<T, String>, KV<T, FastqRecord>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        c.output(KV.of(c.element().getKey(), FastQUtils.parseFastqRaw(c.element().getValue())));
    }
}
