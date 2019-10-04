package com.google.allenday.nanostream.util.trasform;

import com.google.allenday.nanostream.util.ObjectSizeFetcher;
import org.apache.beam.sdk.transforms.Combine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * Combine collection of {@link T} objects into single {@link Iterable<T>}
 * @param <T> type of input values
 */
public class CombineIterableAccumulatorFn<T> extends Combine.CombineFn<T, List<T>, Iterable<T>> {

    private Logger LOG = LoggerFactory.getLogger(CombineIterableAccumulatorFn.class);

    @Override
    public List<T> createAccumulator() {
        return new ArrayList<>();
    }

    @Override
    public List<T> addInput(List<T> accumulator, T input) {
        accumulator.add(input);
        return accumulator;
    }

    @Override
    public List<T> mergeAccumulators(Iterable<List<T>> accumulators) {
        return StreamSupport.stream(accumulators.spliterator(), false)
                .flatMap(List::stream)
                .collect(Collectors.toList());
    }

    @Override
    public Iterable<T> extractOutput(List<T> accumulator) {
        if (accumulator.size() > 0) {
            LOG.info("CombineIterableAccumulatorFn extractOutput: " + accumulator.get(0).getClass().getName());
        }
        LOG.info("CombineIterableAccumulatorFn extractOutput: " + ObjectSizeFetcher.sizeOf(accumulator));
        return accumulator;
    }
}
