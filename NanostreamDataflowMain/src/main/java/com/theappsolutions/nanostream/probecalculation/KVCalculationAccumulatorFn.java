package com.theappsolutions.nanostream.probecalculation;

import com.theappsolutions.nanostream.geneinfo.GeneData;
import com.theappsolutions.nanostream.util.ObjectSizeFetcher;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 *
 */
public class KVCalculationAccumulatorFn extends Combine.CombineFn<
        KV<String, GeneData>,
        Map<String, SequenceCountAndTaxonomyData>,
        Map<String, SequenceCountAndTaxonomyData>> {


    private Logger LOG = LoggerFactory.getLogger(KVCalculationAccumulatorFn.class);

    @Override
    public Map<String, SequenceCountAndTaxonomyData> createAccumulator() {
        LOG.info("createAccumulator");
        return new HashMap<>();
    }

    @Override
    public Map<String, SequenceCountAndTaxonomyData> addInput(Map<String, SequenceCountAndTaxonomyData> accumulator, KV<String, GeneData> input) {
        if (accumulator.containsKey(input.getKey()) && accumulator.get(input.getKey()) != null) {
            accumulator.get(input.getKey()).increment();
        } else {
            accumulator.put(input.getKey(), new SequenceCountAndTaxonomyData(input.getValue()));
        }
        LOG.info("addInput, accum size->" + (long) accumulator.entrySet().size());
        return accumulator;
    }

    @Override
    public Map<String, SequenceCountAndTaxonomyData> mergeAccumulators(Iterable<Map<String, SequenceCountAndTaxonomyData>> accumulators) {
        LOG.info("mergeAccumulators");
        StreamSupport.stream(accumulators.spliterator(), false).forEach(accum -> {
            LOG.info("mergeAccumulators, accumSize ->" + (long) accum.entrySet().size());
        });

        return StreamSupport.stream(accumulators.spliterator(), false)
                .map(Map::entrySet)
                .flatMap(Collection::stream)
                .collect(
                        Collectors.toMap(        // collects into a map
                                Map.Entry::getKey,   // where each entry is based
                                Map.Entry::getValue, // on the entries in the stream
                                SequenceCountAndTaxonomyData::merge
                        )
                );
    }

    @Override
    public Map<String, SequenceCountAndTaxonomyData> extractOutput(Map<String, SequenceCountAndTaxonomyData> accumulator) {
        LOG.info("KVCalculationAccumulatorFn extractOutput" + ObjectSizeFetcher.sizeOf(accumulator));
        return accumulator;
    }
}
