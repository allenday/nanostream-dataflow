package com.google.allenday.nanostream.probecalculation;

import com.google.allenday.genomics.core.reference.ReferenceDatabaseSource;
import com.google.allenday.nanostream.gcs.GCSSourceData;
import com.google.allenday.nanostream.geneinfo.TaxonData;
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
        KV<KV<GCSSourceData, String>, KV<ReferenceDatabaseSource, TaxonData>>,
        Map<KV<GCSSourceData, String>, Map<String, SequenceCountAndTaxonomyData>>,
        Map<KV<GCSSourceData, String>, Map<String, SequenceCountAndTaxonomyData>>> {


    @Override
    public Map<KV<GCSSourceData, String>, Map<String, SequenceCountAndTaxonomyData>> createAccumulator() {
        return new HashMap<>();
    }

    @Override
    public Map<KV<GCSSourceData, String>, Map<String, SequenceCountAndTaxonomyData>> addInput(
            Map<KV<GCSSourceData, String>, Map<String, SequenceCountAndTaxonomyData>> accumulator,
            KV<KV<GCSSourceData, String>, KV<ReferenceDatabaseSource, TaxonData>> input) {
        @Nonnull
        KV<GCSSourceData, String> gcsSourceDataStringKV = input.getKey();
        @Nonnull
        KV<ReferenceDatabaseSource, TaxonData> refAndGeneData = input.getValue();

        KV<GCSSourceData, String> accumKey = KV.of(gcsSourceDataStringKV.getKey(), refAndGeneData.getKey().getName());
        if (!accumulator.containsKey(accumKey) || accumulator.get(accumKey) == null) {
            accumulator.put(accumKey, new HashMap<>());
        }

        Map<String, SequenceCountAndTaxonomyData> accumValue = accumulator.get(accumKey);
        if (accumValue.containsKey(gcsSourceDataStringKV.getValue())
                && accumValue.get(gcsSourceDataStringKV.getValue()) != null) {
            accumValue.get(gcsSourceDataStringKV.getValue()).increment();
        } else {
            accumValue.put(gcsSourceDataStringKV.getValue(), new SequenceCountAndTaxonomyData(refAndGeneData.getValue()));
        }

        return accumulator;
    }

    @Override
    public Map<KV<GCSSourceData, String>, Map<String, SequenceCountAndTaxonomyData>> mergeAccumulators(
            Iterable<Map<KV<GCSSourceData, String>, Map<String, SequenceCountAndTaxonomyData>>> accumulators) {

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
    public Map<KV<GCSSourceData, String>, Map<String, SequenceCountAndTaxonomyData>> extractOutput(Map<KV<GCSSourceData, String>,
            Map<String, SequenceCountAndTaxonomyData>> accumulator) {
        return accumulator;
    }
}
