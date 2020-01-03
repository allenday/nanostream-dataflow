package com.google.allenday.nanostream.cannabis.vcf_to_bq;

import com.google.allenday.genomics.core.model.ReadGroupMetaData;
import com.google.allenday.genomics.core.model.ReferenceDatabase;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;

public class DvAndVcfToBqConnector extends SimpleFunction<KV<KV<ReadGroupMetaData, ReferenceDatabase>, String>, KV<ReferenceDatabase, String>> {
    @Override
    public KV<ReferenceDatabase, String> apply(KV<KV<ReadGroupMetaData, ReferenceDatabase>, String> input) {
        return KV.of(input.getKey().getValue(), input.getValue());
    }
}