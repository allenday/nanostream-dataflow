package com.google.allenday.nanostream.util.trasform;

import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.Map;

/**
 * Flatten PCollection<Map<K, V>> into PCollection<KV<K, V>>
 * @param <K>
 * @param <V>
 */
public class FlattenMapToKV<K, V> extends DoFn<Map<K, V>, KV<K, V>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        c.element().entrySet().forEach(kvEntry -> c.output(KV.of(kvEntry.getKey(), kvEntry.getValue())));
    }
}
