package com.google.allenday.nanostream.gcs;

import com.google.allenday.genomics.core.model.FileWrapper;
import org.apache.beam.sdk.options.ValueProvider;
import org.apache.beam.sdk.transforms.Filter;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import java.util.Optional;

/**
 * Filters files from specific GCS directory
 */
public class FilterGcsDirectoryTransform extends PTransform<PCollection<KV<GCSSourceData, FileWrapper>>, PCollection<KV<GCSSourceData, FileWrapper>>> {

    private ValueProvider<String> inputDir;

    public FilterGcsDirectoryTransform(ValueProvider<String> inputDir) {
        this.inputDir = inputDir;
    }

    @Override
    public PCollection<KV<GCSSourceData, FileWrapper>> expand(PCollection<KV<GCSSourceData, FileWrapper>> input) {
        return input.apply(Filter.by(kv -> Optional.ofNullable(kv.getKey()).map(GCSSourceData::getFolder)
                .map(dir -> dir.startsWith(inputDir.get()) || dir.startsWith("/" + inputDir.get())).orElse(false)));
    }
}
