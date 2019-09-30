package com.google.allenday.nanostream.transforms;

import com.google.allenday.genomics.core.gene.GeneData;
import com.google.allenday.genomics.core.gene.GeneExampleMetaData;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class GroupByPairedReadsAndFilter extends PTransform<PCollection<KV<GeneExampleMetaData, List<GeneData>>>,
        PCollection<KV<GeneExampleMetaData, Iterable<GeneData>>>> {

    private String resultBucket;
    private String anomalyOutputPath;

    public GroupByPairedReadsAndFilter(@Nullable String name, String resultBucket, String anomalyOutputPath) {
        super(name);
        this.resultBucket = resultBucket;
        this.anomalyOutputPath = anomalyOutputPath;
    }

    @Override
    public PCollection<KV<GeneExampleMetaData, Iterable<GeneData>>> expand(PCollection<KV<GeneExampleMetaData, List<GeneData>>> input) {
        input.apply(Filter.by(element -> element.getValue().size() == 0))
                .apply(MapElements.via(new SimpleFunction<KV<GeneExampleMetaData, List<GeneData>>, String>() {
                    @Override
                    public String apply(KV<GeneExampleMetaData, List<GeneData>> input) {
                        return Optional.ofNullable(input.getKey())
                                .map(geneExampleMetaData -> geneExampleMetaData.getComment()+","+geneExampleMetaData.getSrcRawMetaData())
                                .orElse("");
                    }
                }))
                .apply(TextIO.write().withNumShards(1).to(String.format("gs://%s/%s", resultBucket, anomalyOutputPath)));

        return input.apply(Filter.by(element -> element.getValue().size() > 0))
                .apply(MapElements.via(new SimpleFunction<KV<GeneExampleMetaData, List<GeneData>>, KV<String, KV<GeneExampleMetaData, List<GeneData>>>>() {
                    @Override
                    public KV<String, KV<GeneExampleMetaData, List<GeneData>>> apply(KV<GeneExampleMetaData, List<GeneData>> input) {
                        return KV.of(input.getValue().stream().map(GeneData::getBlobUri).collect(Collectors.joining(",")), input);
                    }
                }))
                .apply(GroupByKey.create())
                .apply(MapElements.via(new SimpleFunction<KV<String, Iterable<KV<GeneExampleMetaData, List<GeneData>>>>, KV<GeneExampleMetaData, List<GeneData>>>() {
                    @Override
                    public KV<GeneExampleMetaData, List<GeneData>> apply(KV<String, Iterable<KV<GeneExampleMetaData, List<GeneData>>>> input) {
                        return StreamSupport.stream(input.getValue().spliterator(), false).findFirst().orElse(null);
                    }
                }))
                .apply(MapElements.via(new SimpleFunction<KV<GeneExampleMetaData, List<GeneData>>, KV<GeneExampleMetaData, Iterable<GeneData>>>() {
                    @Override
                    public KV<GeneExampleMetaData, Iterable<GeneData>> apply(KV<GeneExampleMetaData, List<GeneData>> input) {
                        return KV.of(input.getKey(), input.getValue());
                    }
                }));
    }
}
