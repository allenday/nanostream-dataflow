package com.theappsolutions.nanostream.output;

import japsa.seq.Sequence;
import org.apache.beam.sdk.values.KV;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class SequenceInfoGenerator {

    public SequenceInfoResult genereteSequnceInfo(Iterable<KV<String, Sequence>> sequenceSourceData) {
        Date date = new Date();
        long startTime = System.currentTimeMillis();

        float totalDataListSize = StreamSupport.stream(sequenceSourceData.spliterator(), false).count();

        Map<String, List<KV<String, Sequence>>> sourceDataGrouped = StreamSupport.stream(sequenceSourceData.spliterator(), false)
                .collect(Collectors.groupingBy(KV::getKey));
        List<SequenceInfoResult.SequenceRecord> sequenceRecords = new LinkedList<>();

        sourceDataGrouped.forEach((id, value) -> {
            float sequenceCount = value.size();
            String sequenceStr = value.stream().findFirst().map(KV::getValue).map(Sequence::toString).orElse(null);
            sequenceRecords.add(new SequenceInfoResult.SequenceRecord(UUID.randomUUID().toString(), id, sequenceStr, sequenceCount / totalDataListSize));
        });
        long finishTime = System.currentTimeMillis();

        return new SequenceInfoResult(date, sequenceRecords, finishTime - startTime);
    }
}
