package com.theappsolutions.nanostream.output;

import java.util.Date;

/**
 * Data class for saving Nanostream output in Firestore Database
 */
public class OutputRecord {

    public final Date date;
    public final String name;
    public final String sequence;
    public final Float error;
    public final Float probe;

    public OutputRecord(Date date, String name, String sequence, Float error, Float probe) {
        this.date = date;
        this.name = name;
        this.sequence = sequence;
        this.error = error;
        this.probe = probe;
    }
}
