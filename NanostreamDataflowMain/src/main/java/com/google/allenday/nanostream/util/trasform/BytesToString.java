package com.google.allenday.nanostream.util.trasform;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

public class BytesToString<K> extends DoFn<KV<K, byte[]>, KV<K, String>> {
    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<K, byte[]> kv = c.element();
        c.output(KV.of(kv.getKey(), new String(kv.getValue())));
    }
}