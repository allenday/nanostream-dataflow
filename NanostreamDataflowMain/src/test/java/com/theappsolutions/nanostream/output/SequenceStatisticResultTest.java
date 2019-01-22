package com.theappsolutions.nanostream.output;

import japsa.seq.Sequence;
import javafx.util.Pair;
import org.apache.beam.sdk.values.KV;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SequenceStatisticResultTest {

    @Test
    public void outputRecordCreationTest() {
        int sequenceMock1Quantity = 2;
        int sequenceMock2Quantity = 4;

        Pair<String, String> sequenceMock1TestData = new Pair<>("sequenceMock1Name", "sequenceMock1TestValue");
        Pair<String, String> sequenceMock2TestData = new Pair<>("sequenceMock2Name", "sequenceMock2TestValue");

        Sequence sequenceMock1 = mock(Sequence.class);
        when(sequenceMock1.toString()).thenReturn(sequenceMock1TestData.getKey());

        Sequence sequenceMock2 = mock(Sequence.class);
        when(sequenceMock2.toString()).thenReturn(sequenceMock2TestData.getKey());

        List<KV<String, Sequence>> srcCollection = new ArrayList<>();
        IntStream.range(0, sequenceMock1Quantity).forEach(index -> srcCollection.add(KV.of(sequenceMock1TestData.getKey(), sequenceMock1)));
        IntStream.range(0, sequenceMock2Quantity).forEach(index -> srcCollection.add(KV.of(sequenceMock2TestData.getKey(), sequenceMock2)));
        Collections.shuffle(srcCollection);

        //TODO finish
       /* Date approximateDate = new Date();
        SequenceStatisticResult sequenceInfoResult = new SequenceInfoGenerator().genereteSequnceInfo(srcCollection);
        Assert.assertThat(sequenceInfoResult.date.getTime(), allOf(greaterThanOrEqualTo(approximateDate.getTime()),
                lessThan(approximateDate.getTime() + 10)));
        Assert.assertThat(sequenceInfoResult.calculationTime, greaterThan(0L));
        Assert.assertEquals(2, sequenceInfoResult.sequenceRecords.size());
        Assert.assertTrue(sequenceInfoResult.sequenceRecords.stream()
                .anyMatch(sequenceRecord -> sequenceRecord.name.equals(sequenceMock1TestData.getKey())));
        Assert.assertTrue(sequenceInfoResult.sequenceRecords.stream()
                .anyMatch(sequenceRecord -> sequenceRecord.name.equals(sequenceMock2TestData.getKey())));

        Assert.assertEquals(1, sequenceInfoResult.sequenceRecords.stream()
                .filter(sequenceRecord -> sequenceRecord.name.equals(sequenceMock1TestData.getKey())).count());
        Assert.assertEquals(1, sequenceInfoResult.sequenceRecords.stream()
                .filter(sequenceRecord -> sequenceRecord.name.equals(sequenceMock2TestData.getKey())).count());
        Assert.assertEquals("Wrong probe calculation", Float.valueOf((float) sequenceMock1Quantity / (sequenceMock1Quantity + sequenceMock2Quantity)),
                sequenceInfoResult.sequenceRecords.stream()
                        .filter(sequenceRecord -> sequenceRecord.name.equals(sequenceMock1TestData.getKey())).findFirst()
                        .map(record -> record.probe).orElse(-1f));
        Assert.assertEquals("Wrong probe calculation", Float.valueOf((float) sequenceMock2Quantity / (sequenceMock1Quantity + sequenceMock2Quantity)),
                sequenceInfoResult.sequenceRecords.stream()
                        .filter(sequenceRecord -> sequenceRecord.name.equals(sequenceMock2TestData.getKey())).findFirst()
                        .map(record -> record.probe).orElse(-1f));*/
    }
}
