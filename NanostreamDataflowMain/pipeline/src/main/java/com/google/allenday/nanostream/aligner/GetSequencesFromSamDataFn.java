package com.google.allenday.nanostream.aligner;

import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.util.ObjectSizeFetcher;
import htsjdk.samtools.*;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Parses aligned data that comes HTTP aligner into {@link SAMRecord}. Generates {@link KV<String, Sequence>>} object that contains
 * name reference-Sequence pair
 */
public class GetSequencesFromSamDataFn extends DoFn<KV<GCSSourceData, String>, KV<KV<GCSSourceData, String>, Sequence>> {
    private Logger LOG = LoggerFactory.getLogger(GetSequencesFromSamDataFn.class);

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<GCSSourceData, String> data = c.element();
        List<SAMRecord> results = parseAlignmentResponse(data.getValue());
        LOG.info(String.format("List SAMRecords size: %d", ObjectSizeFetcher.sizeOf(results)));
        LOG.info(String.format("List SAMRecords item size: %d", results.size()));
        results.forEach(sam -> {
            if (!sam.getReferenceName().equals("*")) {
                c.output(KV.of(KV.of(data.getKey(), sam.getReferenceName()), generateSequenceFromSam(sam)));
            }
        });
    }

    private List<SAMRecord> parseAlignmentResponse(String response) {
        List<SAMRecord> results = new ArrayList<>();

        InputStream responseInputStream = new ByteArrayInputStream(response.getBytes());
        SamInputResource resource = SamInputResource.of(responseInputStream);

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

    private Sequence generateSequenceFromSam(SAMRecord samRecord) {
        Alphabet alphabet = Alphabet.DNA();
        String readString = samRecord.getReadString();
        String name = samRecord.getReadName();
        if (samRecord.getReadNegativeStrandFlag()) {
            Sequence sequence = Alphabet.DNA.complement(new Sequence(alphabet, readString, name));
            sequence.setName(name);
            return sequence;
        } else {
            return new Sequence(alphabet, readString, name);
        }
    }
}