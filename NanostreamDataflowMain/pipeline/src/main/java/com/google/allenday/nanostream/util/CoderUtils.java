package com.google.allenday.nanostream.util;

import org.apache.beam.sdk.Pipeline;
import org.apache.beam.sdk.coders.Coder;

import java.util.stream.Stream;

/**
 * Provides methods for working with {@link Coder}
 */
public class CoderUtils {

    public static void setupCoders(Pipeline pipeline, Coder... coders) {
        Stream.of(coders).forEach(coder -> pipeline.getCoderRegistry()
                .registerCoderForType(coder.getEncodedTypeDescriptor(), coder));
    }
}
