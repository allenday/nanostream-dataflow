package com.theappsolutions.nanostream.kalign;

import htsjdk.samtools.SAMRecord;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Converts {@link SAMRecord} into {@link Sequence}
 */
public class ExtractSequenceFn extends DoFn<KV<String, Iterable<SAMRecord>>, KV<String, Iterable<Sequence>>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        String ref = c.element().getKey();

        List<Sequence> sequences = StreamSupport.stream(c.element().getValue().spliterator(), false)
                .map(this::generateSequenceFromSam)
                .collect(Collectors.toList());
        c.output(KV.of(ref, sequences));
    }

    private Sequence generateSequenceFromSam(SAMRecord samRecord){
        Alphabet alphabet = Alphabet.DNA();
        String readString = samRecord.getReadString();
        String name = samRecord.getReadName();
        if (samRecord.getReadNegativeStrandFlag()){
            Sequence sequence = Alphabet.DNA.complement(new Sequence(alphabet, readString, name));
            sequence.setName(name);
            return sequence;
        } else {
            return new Sequence(alphabet, readString, name);
        }
    }
}