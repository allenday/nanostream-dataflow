package com.google.allenday.nanostream.util.trasform;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

public class RemoveValueDoFn<K, V> extends DoFn<KV<K, V>, K> {
    @ProcessElement
    public void processElement(ProcessContext c) {
        c.output(c.element().getKey());
    }
}
