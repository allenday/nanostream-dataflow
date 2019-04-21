package com.google.allenday.nanostream.output;

import com.google.allenday.nanostream.probecalculation.SequenceCountAndTaxonomyData;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
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
    private String bucket;
    private String folder;
    private List<SequenceRecord> sequenceRecords;
    private long calculationTime;

    public SequenceStatisticResult(Date startDate, Date resultDate, String bucket, String folder, List<SequenceRecord> sequenceRecords, long calculationTime) {
        this.startDate = startDate;
        this.resultDate = resultDate;
        this.bucket = bucket;
        this.folder = folder;
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

    public String getBucket() {
        return bucket;
    }

    public String getFolder() {
        return folder;
    }

    @DefaultCoder(SerializableCoder.class)
    public static class SequenceRecord implements Serializable{
        private String id;
        private String name;
        private String localName;
        private List<String> taxonomy;
        private List<String> colors;
        private float probe;

        public SequenceRecord(String id, String name, String localName, List<String> taxonomy, List<String> colors, float probe) {
            this.id = id;
            this.name = name;
            this.localName = localName;
            this.taxonomy = taxonomy;
            this.colors = colors;
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

        public List<String> getColors() {
            return colors;
        }
    }


    public static class Generator {

        public SequenceStatisticResult genereteSequnceInfo(Map<String, SequenceCountAndTaxonomyData> sequenceSourceData,
                                                           GCSSourceData gcsSourceData, long startTimestamp) {
            Date date = new Date();
            long startTime = System.currentTimeMillis();

            float totalDataListSize = sequenceSourceData.values().stream().mapToInt(it -> Math.toIntExact(it.getCount())).sum();

            List<SequenceStatisticResult.SequenceRecord> sequenceRecords = new LinkedList<>();

            sequenceSourceData.forEach((id, value) -> {
                Set<String> resistantGenesNamesMap = value.getGeneData().getGeneNames();
                String localName;
                if (resistantGenesNamesMap != null && resistantGenesNamesMap.size() > 0) {
                    localName = resistantGenesNamesMap.iterator().next();
                } else {
                    localName = id;
                }
                SequenceRecord sequenceRecord = new SequenceRecord(UUID.randomUUID().toString(),
                        localName, id, value.getGeneData().getTaxonomy(), value.getGeneData().getColors(),
                        value.getCount() / totalDataListSize);
                sequenceRecords.add(sequenceRecord);

            });
            long finishTime = System.currentTimeMillis();

            return new SequenceStatisticResult(new Date(startTimestamp), date, gcsSourceData.getBucket(),
                    gcsSourceData.getFolder(), sequenceRecords, finishTime - startTime);
        }
    }

}
