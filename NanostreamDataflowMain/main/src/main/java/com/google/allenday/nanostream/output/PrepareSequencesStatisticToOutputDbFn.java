package com.google.allenday.nanostream.output;

import com.google.allenday.nanostream.probecalculation.SequenceCountAndTaxonomyData;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.util.EntityNamer;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import javax.annotation.Nonnull;
import java.util.Map;


/**
 * Prepare {@link SequenceStatisticResult} data to output
 */
public class PrepareSequencesStatisticToOutputDbFn extends DoFn<KV<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>>,
        KV<KV<String, String>, SequenceStatisticResult>> {

    private String collectionNamePrefix;
    private String documentNamePrefix;
    private long startTimestamp;

    public PrepareSequencesStatisticToOutputDbFn(String collectionNamePrefix, String documentNamePrefix, long startTimestamp) {
        this.collectionNamePrefix = collectionNamePrefix;
        this.documentNamePrefix = documentNamePrefix;
        this.startTimestamp = startTimestamp;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<GCSSourceData, Map<String, SequenceCountAndTaxonomyData>> gcsSourceDataMapKV = c.element();
        SequenceStatisticResult.Generator sequenceStatisticGenerator = new SequenceStatisticResult.Generator();

        @Nonnull
        GCSSourceData gcsSourceData = gcsSourceDataMapKV.getKey();
        SequenceStatisticResult sequenceStatisticResult =
                sequenceStatisticGenerator.genereteSequnceInfo(gcsSourceDataMapKV.getValue(), gcsSourceDataMapKV.getKey(), startTimestamp);
        c.output(KV.of(KV.of(EntityNamer.generateNameForCollection(collectionNamePrefix, gcsSourceData.getBucket()),
                EntityNamer.generateNameForDocument(documentNamePrefix, gcsSourceData.getFolder())), sequenceStatisticResult));
    }
}
