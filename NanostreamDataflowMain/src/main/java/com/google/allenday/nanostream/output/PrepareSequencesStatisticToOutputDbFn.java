package com.google.allenday.nanostream.output;

import com.google.allenday.nanostream.probecalculation.SequenceCountAndTaxonomyData;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.Map;


/**
 * Prepare {@link SequenceStatisticResult} data to output
 */
public class PrepareSequencesStatisticToOutputDbFn extends DoFn<Map<String, SequenceCountAndTaxonomyData>,
        KV<String, SequenceStatisticResult>> {

    private String statisticDocumentName;
    private long startTimestamp;

    public PrepareSequencesStatisticToOutputDbFn(String statisticDocumentName, long startTimestamp) {
        this.statisticDocumentName = statisticDocumentName;
        this.startTimestamp = startTimestamp;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        Map<String, SequenceCountAndTaxonomyData> sequenceIterableKV = c.element();
        SequenceStatisticResult.Generator sequenceStatisticGenerator = new SequenceStatisticResult.Generator();

        SequenceStatisticResult sequenceStatisticResult = sequenceStatisticGenerator.genereteSequnceInfo(sequenceIterableKV, startTimestamp);
        c.output(KV.of(statisticDocumentName, sequenceStatisticResult));
    }
}
