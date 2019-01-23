package com.theappsolutions.nanostream.aligner;

import htsjdk.samtools.*;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses aligned data that comes HTTP aligner. Generates {@link KV<String, SAMRecord>>} object that contains
 * SAMRecord name reference-SAMRecord pair
 */
public class ParseAlignedDataIntoSAMFn extends DoFn<String, KV<String, SAMRecord>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        String data = c.element();
        List<SAMRecord> results = parseAlignmentResponse(data);
        results.forEach(sam -> {
            if (!sam.getReferenceName().equals("*")) {
                c.output(KV.of(sam.getReferenceName(), sam));
            }
        });
    }

    private List<SAMRecord> parseAlignmentResponse(String response) {
        List<SAMRecord> results = new ArrayList<>();

        InputStream responseInputStream = new ByteArrayInputStream(response.getBytes());
        SamInputResource resource = SamInputResource.of(responseInputStream);
        //TODO investigate
        SamReaderFactory factory = SamReaderFactory
                .makeDefault()
                .disable(SamReaderFactory.Option.VALIDATE_CRC_CHECKSUMS)
                .validationStringency(ValidationStringency.LENIENT);
        SamReader samReader = factory.open(resource);

        for (SAMRecord samRecord : samReader) {
            results.add(samRecord);
        }
        return results;
    }
}