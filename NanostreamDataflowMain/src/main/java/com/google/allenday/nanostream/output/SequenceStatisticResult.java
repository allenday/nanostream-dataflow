package com.google.allenday.nanostream.output;

import com.google.allenday.nanostream.probecalculation.SequenceCountAndTaxonomyData;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;
import java.util.*;

/**
 * Data class for saving Nanostream output in Firestore Database
 */
@DefaultCoder(SerializableCoder.class)
public class SequenceStatisticResult implements Serializable {

    private Date startDate;
    private Date resultDate;
    private List<SequenceRecord> sequenceRecords;
    private long calculationTime;

    public SequenceStatisticResult(Date startDate, Date resultDate, List<SequenceRecord> sequenceRecords, long calculationTime) {
        this.startDate = startDate;
        this.resultDate = resultDate;
        this.sequenceRecords = sequenceRecords;
        this.calculationTime = calculationTime;
    }

    public Date getStartDate() {
        return startDate;
    }

    public Date getResultDate() {
        return resultDate;
    }

    public List<SequenceRecord> getSequenceRecords() {
        return sequenceRecords;
    }

    public long getCalculationTime() {
        return calculationTime;
    }

    @DefaultCoder(SerializableCoder.class)
    public static class SequenceRecord implements Serializable{
        private String id;
        private String name;
        private String localName;
        private List<String> taxonomy;
        private float probe;

        public SequenceRecord(String id, String name, List<String> taxonomy, float probe) {
            this(id, name, name, taxonomy, probe);
        }

        public SequenceRecord(String id, String name, String localName, List<String> taxonomy, float probe) {
            this.id = id;
            this.name = name;
            this.localName = localName;
            this.taxonomy = taxonomy;
            this.probe = probe;
        }

        public String getId() {
            return id;
        }

        public String getName() {
            return name;
        }

        public List<String> getTaxonomy() {
            return taxonomy;
        }

        public float getProbe() {
            return probe;
        }

        public String getLocalName() {
            return localName;
        }
    }


    public static class Generator {
        public SequenceStatisticResult genereteSequnceInfo(Map<String, SequenceCountAndTaxonomyData> sequenceSourceData, long startTimestamp) {
            Date date = new Date();
            long startTime = System.currentTimeMillis();

            float totalDataListSize = sequenceSourceData.values().stream().mapToInt(it -> Math.toIntExact(it.getCount())).sum();

            List<SequenceStatisticResult.SequenceRecord> sequenceRecords = new LinkedList<>();

            sequenceSourceData.forEach((id, value) -> {
                Set<String> resistantGenesNamesMap = value.getGeneData().getGeneNames();
                SequenceRecord sequenceRecord;
                if (resistantGenesNamesMap != null && resistantGenesNamesMap.size() > 0) {
                    sequenceRecord = new SequenceRecord(UUID.randomUUID().toString(),
                            resistantGenesNamesMap.iterator().next(),
                            id, value.getGeneData().getTaxonomy(), value.getCount() / totalDataListSize);
                } else {
                    sequenceRecord = new SequenceRecord(UUID.randomUUID().toString(),
                            id, value.getGeneData().getTaxonomy(), value.getCount() / totalDataListSize);
                }
                sequenceRecords.add(sequenceRecord);

            });
            long finishTime = System.currentTimeMillis();

            return new SequenceStatisticResult(new Date(startTimestamp), date, sequenceRecords, finishTime - startTime);
        }
    }

}
