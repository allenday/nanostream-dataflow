package com.theappsolutions.nanostream.fastq;

import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.repackaged.beam_sdks_java_core.org.apache.commons.lang3.StringUtils;
import org.apache.beam.sdk.transforms.DoFn;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Creates {@link FastqRecord} instance from .fastq file data
 */
public class ParseFastQFn extends DoFn<String, FastqRecord> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        String data = c.element();

        String payload = data.trim();
        String[] linesCrud = StringUtils.split(payload, "\n");
        List<String> linesList = Arrays.stream(linesCrud)
                .filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

        String readName = linesList.get(0).replace("XXXX", "").replace("@", "");
        String readBases = linesList.get(1);
        String qualityHeader = linesList.get(2);
        String baseQualities = linesList.get(3);

        FastqRecord fastQ = new FastqRecord(readName, readBases, qualityHeader, baseQualities);
        c.output(fastQ);
    }
}