package com.theappsolutions.nanostream.output;

public class OutputRecord {

    public final String date;
    public final String name;
    public final String sequence;
    public final String error;
    public final String parts;

    public OutputRecord(String date, String name, String sequence, String error, String parts) {
        this.date = date;
        this.name = name;
        this.sequence = sequence;
        this.error = error;
        this.parts = parts;
    }
}
