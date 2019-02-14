package com.theappsolutions.nanostream.output;

import com.theappsolutions.nanostream.probecalculation.SequenceCountAndTaxonomyData;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.util.Map;

import static com.theappsolutions.nanostream.other.Constants.SEQUENCES_STATISTIC_DOCUMENT_NAME;

/**
 * Prepare {@link SequenceStatisticResult} data to output
 */
public class PrepareSequencesStatisticToOutputDbFn extends DoFn<Map<String, SequenceCountAndTaxonomyData>,
        KV<String, SequenceStatisticResult>> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        Map<String, SequenceCountAndTaxonomyData> sequenceIterableKV = c.element();
        SequenceStatisticResult.Generator sequenceStatisticGenerator = new SequenceStatisticResult.Generator();

        SequenceStatisticResult sequenceStatisticResult = sequenceStatisticGenerator.genereteSequnceInfo(sequenceIterableKV);
        c.output(KV.of(SEQUENCES_STATISTIC_DOCUMENT_NAME, sequenceStatisticResult));
    }
}
