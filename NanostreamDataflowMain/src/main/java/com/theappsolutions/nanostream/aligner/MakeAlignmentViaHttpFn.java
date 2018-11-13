package com.theappsolutions.nanostream.aligner;

import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.net.URISyntaxException;

/**
 * Makes alignment of fastq data via HTTP server and returns string body of alignment HTTP response
 */
public class MakeAlignmentViaHttpFn extends DoFn<Iterable<FastqRecord>, String> {

    private Logger LOG = LoggerFactory.getLogger(MakeAlignmentViaHttpFn.class);

    private AlignerHttpService alignerHttpService;

    public MakeAlignmentViaHttpFn(AlignerHttpService alignerHttpService) {
        this.alignerHttpService = alignerHttpService;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        Iterable<FastqRecord> data = c.element();
        try {
            @Nonnull
            String responseBody = alignerHttpService.generateAlignData(data);
            c.output(responseBody);
        } catch (URISyntaxException | IOException e) {
            LOG.error(e.getMessage());
        }
    }
}
