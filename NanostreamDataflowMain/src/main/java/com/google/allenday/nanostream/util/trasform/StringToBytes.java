package com.google.allenday.nanostream.util.trasform;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

public class StringToBytes<K> extends DoFn<KV<K, String>, KV<K, byte[]>> {
    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<K, String> kv = c.element();
        c.output(KV.of(kv.getKey(), kv.getValue().getBytes()));
    }
}