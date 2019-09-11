package com.google.allenday.nanostream.transforms;

import com.google.allenday.nanostream.cannabis_parsing.CannabisSourceMetaData;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import java.util.List;
import java.util.stream.StreamSupport;

public class GroupByPairedReadsAndFilter extends PTransform<PCollection<KV<CannabisSourceMetaData, List<String>>>,
        PCollection<KV<CannabisSourceMetaData, List<String>>>> {

    @Override
    public PCollection<KV<CannabisSourceMetaData, List<String>>> expand(PCollection<KV<CannabisSourceMetaData, List<String>>> input) {
        return input.apply(MapElements.via(new SimpleFunction<KV<CannabisSourceMetaData, List<String>>, KV<String, KV<CannabisSourceMetaData, List<String>>>>() {
            @Override
            public KV<String, KV<CannabisSourceMetaData, List<String>>> apply(KV<CannabisSourceMetaData, List<String>> input) {
                return KV.of(String.join(",", input.getValue()), input);
            }
        }))
                .apply(GroupByKey.create())
                .apply(MapElements.via(new SimpleFunction<KV<String, Iterable<KV<CannabisSourceMetaData, List<String>>>>, KV<CannabisSourceMetaData, List<String>>>() {
                    @Override
                    public KV<CannabisSourceMetaData, List<String>> apply(KV<String, Iterable<KV<CannabisSourceMetaData, List<String>>>> input) {
                        return StreamSupport.stream(input.getValue().spliterator(), false).findFirst().orElse(null);
                    }
                }))
                .apply(Filter.by(element -> element.getValue().size() > 0));
    }
}
