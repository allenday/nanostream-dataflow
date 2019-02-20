package com.google.allenday.nanostream.main.util.transform;

import com.google.allenday.nanostream.util.trasform.CombineIterableAccumulatorFn;
import org.apache.beam.sdk.testing.PAssert;
import org.apache.beam.sdk.testing.TestPipeline;
import org.apache.beam.sdk.transforms.Combine;
import org.apache.beam.sdk.transforms.Create;
import org.apache.beam.sdk.transforms.SerializableFunction;
import org.apache.beam.sdk.values.PCollection;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.StreamSupport;


/**
 * Tests of {@link CombineIterableAccumulatorFn} correct combining
 */
public class CombineIterableAccumulatorFnTest {

    @Rule
    public final transient TestPipeline testPipeline = TestPipeline.create().enableAbandonedNodeEnforcement(true);

    @Test
    public void testCorrectCombiningToIterable() throws IOException {
        List<String> elements = Arrays.asList("Element 1", "Element 2", "Element 3");

        PCollection<Iterable<String>> parsedFastQ = testPipeline
                .apply(Create.of(elements))
                .apply(Combine.globally(new CombineIterableAccumulatorFn<>()));

        PAssert.that(parsedFastQ)
                .satisfies((SerializableFunction<Iterable<Iterable<String>>, Void>) input -> {
                    List<String> resultData = StreamSupport.stream(input.iterator().next().spliterator(), false)
                            .collect(Collectors.toList());

                    Assert.assertEquals(elements.size(),
                            resultData.size());
                    IntStream.range(0, elements.size())
                            .forEach(index -> Assert.assertTrue(resultData.contains(elements.get(index))));
                    return null;
                });

        testPipeline.run();
    }
}
