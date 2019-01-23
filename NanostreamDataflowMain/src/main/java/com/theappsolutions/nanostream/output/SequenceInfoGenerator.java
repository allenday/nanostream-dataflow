package com.theappsolutions.nanostream.output;

import com.theappsolutions.nanostream.probecalculation.SequenceCountAndTaxonomyData;

import java.util.*;

public class SequenceInfoGenerator {

    public SequenceStatisticResult genereteSequnceInfo(Map<String, SequenceCountAndTaxonomyData> sequenceSourceData) {
        Date date = new Date();
        long startTime = System.currentTimeMillis();

        float totalDataListSize = sequenceSourceData.values().stream().mapToInt(it -> Math.toIntExact(it.getCount())).sum();

        List<SequenceStatisticResult.SequenceRecord> sequenceRecords = new LinkedList<>();

        sequenceSourceData.forEach((id, value) -> {
            sequenceRecords.add(new SequenceStatisticResult.SequenceRecord(UUID.randomUUID().toString(), id, value.getTaxonomy(), value.getCount() / totalDataListSize));
        });
        long finishTime = System.currentTimeMillis();

        return new SequenceStatisticResult(date, sequenceRecords, finishTime - startTime);
    }
}
