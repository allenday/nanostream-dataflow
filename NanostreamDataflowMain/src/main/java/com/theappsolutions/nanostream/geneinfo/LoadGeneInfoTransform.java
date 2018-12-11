package com.theappsolutions.nanostream.geneinfo;

import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;

public class LoadGeneInfoTransform extends PTransform<PBegin, PCollection<String>> {

    @Override
    public PCollection<String> expand(PBegin input) {

        return input.getPipeline()
                .apply("Read GS", TextIO.read().from("gs://nano-stream-test/ResistanceGenes/resFinder/DB.fasta"))
                .apply("Count", ParDo.of(new DoFn<String, String>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        c.output(c.element()+" ---> "+c.element().length());
                    }
                }));
    }
}
