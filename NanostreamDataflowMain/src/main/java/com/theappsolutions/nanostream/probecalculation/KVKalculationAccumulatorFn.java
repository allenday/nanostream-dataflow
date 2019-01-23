package com.theappsolutions.nanostream.probecalculation;

import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.values.KV;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 */
public class KVKalculationAccumulatorFn extends Combine.CombineFn<
        KV<String, List<String>>,
        Map<String, SequenceCountAndTaxonomyData>,
        Map<String, SequenceCountAndTaxonomyData>> {

    @Override
    public Map<String, SequenceCountAndTaxonomyData> createAccumulator() {
        return new HashMap<>();
    }

    @Override
    public Map<String, SequenceCountAndTaxonomyData> addInput(Map<String, SequenceCountAndTaxonomyData> accumulator, KV<String, List<String>> input) {
        if (accumulator.containsKey(input.getKey()) && accumulator.get(input.getKey()) != null) {
            accumulator.get(input.getKey()).increment();
        } else {
            accumulator.put(input.getKey(), new SequenceCountAndTaxonomyData(input.getValue()));
        }
        return accumulator;
    }

    @Override
    public Map<String, SequenceCountAndTaxonomyData> mergeAccumulators(Iterable<Map<String, SequenceCountAndTaxonomyData>> accumulators) {
        return StreamSupport.stream(accumulators.spliterator(), false)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(
                        Collectors.toMap(        // collects into a map
                                Map.Entry::getKey,   // where each entry is based
                                Map.Entry::getValue, // on the entries in the stream
                                (sequenceCountAndTaxonomyData, sequenceCountAndTaxonomyData2) ->
                                        Stream.of(sequenceCountAndTaxonomyData, sequenceCountAndTaxonomyData2).findFirst().orElse(null)
                        )
                );
    }

    @Override
    public Map<String, SequenceCountAndTaxonomyData> extractOutput(Map<String, SequenceCountAndTaxonomyData> accumulator) {
        return accumulator;
    }
}
