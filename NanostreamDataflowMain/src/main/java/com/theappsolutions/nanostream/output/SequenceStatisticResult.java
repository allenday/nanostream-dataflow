package com.theappsolutions.nanostream.output;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.util.Date;
import java.util.List;

/**
 * Data class for saving Nanostream output in Firestore Database
 */
@DefaultCoder(SerializableCoder.class)
public class SequenceStatisticResult {

    public final static String SEQUENCE_STATISTIC_DOCUMENT_NAME = "resultDocument";

    public final Date date;
    public final List<SequenceRecord> sequenceRecords;
    public final long calculationTime;

    public SequenceStatisticResult(Date date, List<SequenceRecord> sequenceRecords, long calculationTime) {
        this.date = date;
        this.sequenceRecords = sequenceRecords;
        this.calculationTime = calculationTime;
    }

    @DefaultCoder(SerializableCoder.class)
    public static class SequenceRecord {
        public final String id;
        public final String name;
        public final List<String> taxonomy;
        public final float probe;

        public SequenceRecord(String id, String name, List<String> taxonomy, float probe) {
            this.id = id;
            this.name = name;
            this.taxonomy = taxonomy;
            this.probe = probe;
        }
    }
}
