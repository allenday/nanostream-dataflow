package com.google.allenday.nanostream.cannabis.anomaly;

import com.google.allenday.genomics.core.gene.FileWrapper;
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

public class DetectAnomalyTransform extends PTransform<PCollection<KV<GeneExampleMetaData, List<FileWrapper>>>,
        PCollection<KV<GeneExampleMetaData, List<FileWrapper>>>> {

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
    public PCollection<KV<GeneExampleMetaData, List<FileWrapper>>> expand(PCollection<KV<GeneExampleMetaData, List<FileWrapper>>> input) {
        PCollection<KV<GeneExampleMetaData, List<FileWrapper>>> recognizeAnomaly = input
                .apply("Recognize anomaly", ParDo.of(recognizePairedReadsWithAnomalyFn));
        recognizeAnomaly
                .apply(Filter.by(element -> element.getValue().size() == 0))
                .apply(MapElements.via(new SimpleFunction<KV<GeneExampleMetaData, List<FileWrapper>>, String>() {
                    @Override
                    public String apply(KV<GeneExampleMetaData, List<FileWrapper>> input) {
                        return Optional.ofNullable(input.getKey())
                                .map(geneExampleMetaData -> geneExampleMetaData.getComment() + "," + geneExampleMetaData.getSrcRawMetaData())
                                .orElse("");
                    }
                }))
                .apply(TextIO.write().withNumShards(1).to(String.format("gs://%s/%s", resultBucket, anomalyOutputPath)));

        return recognizeAnomaly.apply(Filter.by(element -> element.getValue().size() > 0))
                .apply(MapElements.via(new SimpleFunction<KV<GeneExampleMetaData, List<FileWrapper>>, KV<String, KV<GeneExampleMetaData, List<FileWrapper>>>>() {
                    @Override
                    public KV<String, KV<GeneExampleMetaData, List<FileWrapper>>> apply(KV<GeneExampleMetaData, List<FileWrapper>> input) {
                        return KV.of(input.getValue().stream().map(FileWrapper::getBlobUri).collect(Collectors.joining(",")), input);
                    }
                }))
                .apply(GroupByKey.create())
                .apply(MapElements.via(new SimpleFunction<KV<String, Iterable<KV<GeneExampleMetaData, List<FileWrapper>>>>, KV<GeneExampleMetaData, List<FileWrapper>>>() {
                    @Override
                    public KV<GeneExampleMetaData, List<FileWrapper>> apply(KV<String, Iterable<KV<GeneExampleMetaData, List<FileWrapper>>>> input) {
                        return StreamSupport.stream(input.getValue().spliterator(), false).findFirst().orElse(null);
                    }
                }))
                .apply(MapElements.via(new SimpleFunction<KV<GeneExampleMetaData, List<FileWrapper>>, KV<GeneExampleMetaData, Iterable<FileWrapper>>>() {
                    @Override
                    public KV<GeneExampleMetaData, Iterable<FileWrapper>> apply(KV<GeneExampleMetaData, List<FileWrapper>> input) {
                        return KV.of(input.getKey(), input.getValue());
                    }
                }))
                .apply("IterToList after anomaly detection", new ValueIterableToValueListTransform<>())
                ;
    }
}
