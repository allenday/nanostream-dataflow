package com.google.allenday.nanostream.launcher.data;

public class ReferenceDb {

    private String name;
    private String fastaUri;
    private String ncbiTreeUri;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFastaUri() {
        return fastaUri;
    }

    public void setFastaUri(String fastaUri) {
        this.fastaUri = fastaUri;
    }

    public String getNcbiTreeUri() {
        return ncbiTreeUri;
    }

    public void setNcbiTreeUri(String ncbiTreeUri) {
        this.ncbiTreeUri = ncbiTreeUri;
    }
}
