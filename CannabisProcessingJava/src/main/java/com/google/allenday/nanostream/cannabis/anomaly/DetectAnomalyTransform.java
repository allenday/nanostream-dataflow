package com.google.allenday.nanostream.cannabis.anomaly;

import com.google.allenday.genomics.core.gene.GeneData;
import com.google.allenday.genomics.core.gene.GeneExampleMetaData;
import com.google.allenday.genomics.core.utils.ValueIterableToValueListTransform;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class DetectAnomalyTransform extends PTransform<PCollection<KV<GeneExampleMetaData, List<GeneData>>>,
        PCollection<KV<GeneExampleMetaData, List<GeneData>>>> {

    private String resultBucket;
    private String anomalyOutputPath;
    private RecognizePairedReadsWithAnomalyFn recognizePairedReadsWithAnomalyFn;

    public DetectAnomalyTransform(String resultBucket, String anomalyOutputPath, RecognizePairedReadsWithAnomalyFn recognizePairedReadsWithAnomalyFn) {
        this.resultBucket = resultBucket;
        this.anomalyOutputPath = anomalyOutputPath;
        this.recognizePairedReadsWithAnomalyFn = recognizePairedReadsWithAnomalyFn;
    }

    public DetectAnomalyTransform(@Nullable String name, String resultBucket, String anomalyOutputPath, RecognizePairedReadsWithAnomalyFn recognizePairedReadsWithAnomalyFn) {
        super(name);
        this.resultBucket = resultBucket;
        this.anomalyOutputPath = anomalyOutputPath;
        this.recognizePairedReadsWithAnomalyFn = recognizePairedReadsWithAnomalyFn;
    }

    @Override
    public PCollection<KV<GeneExampleMetaData, List<GeneData>>> expand(PCollection<KV<GeneExampleMetaData, List<GeneData>>> input) {
        PCollection<KV<GeneExampleMetaData, List<GeneData>>> recognizeAnomaly = input
                .apply("Recognize anomaly", ParDo.of(recognizePairedReadsWithAnomalyFn));
        recognizeAnomaly
                .apply(Filter.by(element -> element.getValue().size() == 0))
                .apply(MapElements.via(new SimpleFunction<KV<GeneExampleMetaData, List<GeneData>>, String>() {
                    @Override
                    public String apply(KV<GeneExampleMetaData, List<GeneData>> input) {
                        return Optional.ofNullable(input.getKey())
                                .map(geneExampleMetaData -> geneExampleMetaData.getComment() + "," + geneExampleMetaData.getSrcRawMetaData())
                                .orElse("");
                    }
                }))
                .apply(TextIO.write().withNumShards(1).to(String.format("gs://%s/%s", resultBucket, anomalyOutputPath)));

        return recognizeAnomaly.apply(Filter.by(element -> element.getValue().size() > 0))
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
                }))
                .apply("IterToList after anomaly detection", new ValueIterableToValueListTransform<>())
                ;
    }
}
