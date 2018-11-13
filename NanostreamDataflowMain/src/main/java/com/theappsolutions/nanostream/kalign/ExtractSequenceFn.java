package com.theappsolutions.nanostream.kalign;

import htsjdk.samtools.SAMRecord;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.ArrayList;
import java.util.List;

public class ExtractSequenceFn extends DoFn<KV<String, Iterable<SAMRecord>>, KV<String, Iterable<Sequence>>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        String ref = c.element().getKey();
        Iterable<SAMRecord> samIt = c.element().getValue();
        List<Sequence> seqs = new ArrayList<>();
        for (SAMRecord sam : samIt) {
            String alignedBases = sam.getReadString().substring(sam.getAlignmentStart(), sam.getAlignmentEnd());
            Sequence seq = new Sequence(Alphabet.DNA(), alignedBases, sam.getReadName());

            if (sam.getReadNegativeStrandFlag()) {
                seq = Alphabet.DNA.complement(seq);
                seq.setName(sam.getReadName());
            }
            seqs.add(seq);
        }
        c.output(KV.of(ref, seqs));
    }
}