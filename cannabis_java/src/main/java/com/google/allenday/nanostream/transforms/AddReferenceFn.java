package com.google.allenday.nanostream.transforms;

import com.google.allenday.nanostream.cannabis_parsing.CannabisSourceMetaData;
import org.apache.beam.sdk.transforms.SimpleFunction;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.stream.Collectors;

public class AddReferenceFn extends SimpleFunction<KV<CannabisSourceMetaData, List<String>>,
        Iterable<KV<String, KV<CannabisSourceMetaData, List<String>>>>> {

    private Logger LOG = LoggerFactory.getLogger(AddReferenceFn.class);
    private final static String REFERENCE_PATH_PATTERN = "reference/%s/%s.fa";

    private List<String> references;

    public AddReferenceFn(List<String> references) {
        this.references = references;
    }

    @Override
    public Iterable<KV<String, KV<CannabisSourceMetaData, List<String>>>> apply(KV<CannabisSourceMetaData, List<String>> input) {
        List<KV<String, KV<CannabisSourceMetaData, List<String>>>> collect = references.stream()
                .map(ref -> KV.of(String.format(REFERENCE_PATH_PATTERN, ref, ref), input))
                .collect(Collectors.toList());
        LOG.info(String.format("Add references %s", collect.toString()));
        return collect;
    }
}
