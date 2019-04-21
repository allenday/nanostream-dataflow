package com.google.allenday.nanostream.util.trasform;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.PCollection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Debugging {@link PTransform} that logs all passing data
 */
public class LoggerTransform<T> extends PTransform<PCollection<T>, PCollection<T>> {

    private Logger LOG = LoggerFactory.getLogger(LoggerTransform.class);

    @Override
    public PCollection<T> expand(PCollection<T> input) {

        return input.apply(
                ParDo.of(new DoFn<T, T>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        LOG.info(c.element().toString());
                        c.output(c.element());
                    }
                })
        );
    }
}





