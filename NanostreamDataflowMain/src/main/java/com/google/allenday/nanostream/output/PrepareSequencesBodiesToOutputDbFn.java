package com.google.allenday.nanostream.output;

import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

/**
 * Prepare {@link Sequence} data to output
 */
public class PrepareSequencesBodiesToOutputDbFn extends DoFn<KV<String, Sequence>, KV<String, SequenceBodyResult>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<String, Sequence> sequenceKV = c.element();
        c.output(KV.of(sequenceKV.getKey(), new SequenceBodyResult(sequenceKV.getValue().toString())));
    }
}
