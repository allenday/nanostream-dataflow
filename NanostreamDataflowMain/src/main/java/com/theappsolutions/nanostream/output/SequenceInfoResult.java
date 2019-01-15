package com.theappsolutions.nanostream.output;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.util.Date;
import java.util.List;

/**
 * Data class for saving Nanostream output in Firestore Database
 */
@DefaultCoder(SerializableCoder.class)
public class SequenceInfoResult {

    public final Date date;
    public final List<SequenceRecord> sequenceRecords;
    public final long calculationTime;

    public SequenceInfoResult(Date date, List<SequenceRecord> sequenceRecords, long calculationTime) {
        this.date = date;
        this.sequenceRecords = sequenceRecords;
        this.calculationTime = calculationTime;
    }

    @DefaultCoder(SerializableCoder.class)
    public static class SequenceRecord {
        public final String id;
        public final String name;
        public final String sequence;
        public final float probe;

        public SequenceRecord(String id, String name, String sequence, float probe) {
            this.id = id;
            this.name = name;
            this.sequence = sequence;
            this.probe = probe;
        }
    }
}
