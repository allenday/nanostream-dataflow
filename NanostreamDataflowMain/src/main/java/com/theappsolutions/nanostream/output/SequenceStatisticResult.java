package com.theappsolutions.nanostream.output;

import com.theappsolutions.nanostream.probecalculation.SequenceCountAndTaxonomyData;
import com.theappsolutions.nanostream.util.EntityNamer;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;
import java.util.*;

/**
 * Data class for saving Nanostream output in Firestore Database
 */
@DefaultCoder(SerializableCoder.class)
public class SequenceStatisticResult implements Serializable {

    public final static String SEQUENCE_STATISTIC_DOCUMENT_NAME = EntityNamer.generateName("resultDocument");

    private Date date;
    private List<SequenceRecord> sequenceRecords;
    private long calculationTime;

    public SequenceStatisticResult(Date date, List<SequenceRecord> sequenceRecords, long calculationTime) {
        this.date = date;
        this.sequenceRecords = sequenceRecords;
        this.calculationTime = calculationTime;
    }

    public Date getDate() {
        return date;
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
        private List<String> taxonomy;
        private float probe;

        public SequenceRecord(String id, String name, List<String> taxonomy, float probe) {
            this.id = id;
            this.name = name;
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
    }


    public static class Generator {
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

}
