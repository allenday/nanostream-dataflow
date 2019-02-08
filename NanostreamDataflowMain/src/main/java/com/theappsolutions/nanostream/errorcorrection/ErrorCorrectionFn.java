package com.theappsolutions.nanostream.errorcorrection;

import japsa.bio.np.ErrorCorrection;
import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.ArrayList;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Provides sequence merging operation that creates one final sequence from multiple parts
 */
public class ErrorCorrectionFn extends DoFn<KV<String, Iterable<Sequence>>, KV<String, Sequence>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        ArrayList<Sequence> seqList = StreamSupport.stream(c.element().getValue().spliterator(), false)
                .collect(Collectors.toCollection(ArrayList::new));

        if (seqList.size() == 1) {
            c.output(KV.of(c.element().getKey(), seqList.get(0)));
            return;
        }
        Sequence consensusSequence = ErrorCorrection.getConsensus(seqList);
        c.output(KV.of(c.element().getKey(), consensusSequence));
    }
}