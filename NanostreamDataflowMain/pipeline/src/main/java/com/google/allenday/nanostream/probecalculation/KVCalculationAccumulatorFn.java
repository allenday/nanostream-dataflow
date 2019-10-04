package com.google.allenday.nanostream.probecalculation;

import com.google.allenday.nanostream.geneinfo.GeneData;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.values.KV;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 *
 */
public class KVCalculationAccumulatorFn extends Combine.CombineFn<
        KV<KV<GCSSourceData, String>, GeneData>,
        Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>>,
        Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>>> {


    @Override
    public Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>> createAccumulator() {
        return new HashMap<>();
    }

    @Override
    public Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>> addInput(
            Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>> accumulator,
            KV<KV<GCSSourceData, String>, GeneData> input) {
        @Nonnull
        KV<GCSSourceData, String> gcsSourceDataStringKV = input.getKey();
        if (!accumulator.containsKey(gcsSourceDataStringKV.getKey()) || accumulator.get(gcsSourceDataStringKV.getKey()) == null) {
            accumulator.put(gcsSourceDataStringKV.getKey(), new HashMap<>());
        }

        Map<String, SequenceCountAndTaxonomyData> stringSequenceCountAndTaxonomyDataMap =
                accumulator.get(gcsSourceDataStringKV.getKey());
        if (stringSequenceCountAndTaxonomyDataMap.containsKey(gcsSourceDataStringKV.getValue())
                && stringSequenceCountAndTaxonomyDataMap.get(gcsSourceDataStringKV.getValue()) != null) {
            stringSequenceCountAndTaxonomyDataMap.get(gcsSourceDataStringKV.getValue()).increment();
        } else {
            stringSequenceCountAndTaxonomyDataMap.put(gcsSourceDataStringKV.getValue(), new SequenceCountAndTaxonomyData(input.getValue()));
        }

        return accumulator;
    }

    @Override
    public Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>> mergeAccumulators(
            Iterable<Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>>> accumulators) {

        return StreamSupport.stream(accumulators.spliterator(), false)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                Map.Entry::getValue,
                                (stringSequenceCountAndTaxonomyDataMap, stringSequenceCountAndTaxonomyDataMap2) ->
                                        Stream.of(stringSequenceCountAndTaxonomyDataMap, stringSequenceCountAndTaxonomyDataMap2)
                                                .map(Map::entrySet)
                                                .flatMap(Collection::stream)
                                                .collect(
                                                        Collectors.toMap(
                                                                Map.Entry::getKey,
                                                                Map.Entry::getValue,
                                                                SequenceCountAndTaxonomyData::merge
                                                        )
                                                )
                        )
                );
    }

    @Override
    public Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>> extractOutput(Map<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>> accumulator) {
        return accumulator;
    }
}
