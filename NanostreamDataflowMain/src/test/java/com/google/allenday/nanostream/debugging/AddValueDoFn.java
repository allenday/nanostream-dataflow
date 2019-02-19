package com.google.allenday.nanostream.debugging;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.IntStream;

@DefaultCoder(SerializableCoder.class)
public class AddValueDoFn<K> extends DoFn<K, KV<K, List<String>>> implements Serializable {
    @ProcessElement
    public void processElement(ProcessContext c) {

        ArrayList<String> builder = new ArrayList<>();
        IntStream.range(0, 10).forEach(index -> builder.add(UUID.randomUUID().toString()));
        c.output(KV.of(c.element(), builder));
    }
}
