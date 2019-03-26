package com.google.allenday.nanostream.cannabis_source;

import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

//TODO

/**
 *
 */
@DefaultCoder(AvroCoder.class)
public class CannabisSourceFileMetaData implements Serializable {

    private CannabisSourceMetaData cannabisSourceMetaData;
    private int pairedIndex;

    public CannabisSourceFileMetaData() {
    }

    public CannabisSourceFileMetaData(CannabisSourceMetaData sourceMetaData, int pairedIndex) {

        this.cannabisSourceMetaData = sourceMetaData;
        this.pairedIndex = pairedIndex;
    }

    public static List<CannabisSourceFileMetaData> fromCSVLine(String csvLine) {
        String[] parts = csvLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        CannabisSourceMetaData cannabisSourceFileMetaData = new CannabisSourceMetaData(parts[0], parts[1], parts[2], parts[3], parts[4], parts[5], parts[6],
                Long.parseLong(parts[7]), Long.parseLong(parts[8]), parts[9], parts[10], parts[11],
                Integer.parseInt(parts[12]), parts[14], parts[15], parts[16]);

        List<CannabisSourceFileMetaData> results = new ArrayList<>();
        results.add(new CannabisSourceFileMetaData(cannabisSourceFileMetaData, 1));

        if (cannabisSourceFileMetaData.isPaired()) {
            results.add(new CannabisSourceFileMetaData(cannabisSourceFileMetaData.clone(), 2));

        }
        return results;
    }

    public String generateGCSBlobName() {
        String runName = cannabisSourceMetaData.getRun();
        if (cannabisSourceMetaData.isPaired()) {
            runName += "_" + pairedIndex;
        }
        runName += ".fastq";
        return "sra_medium/" + cannabisSourceMetaData.getProjectId() + "/" + cannabisSourceMetaData.getSraSample() + "/" + runName;
    }

    public CannabisSourceMetaData getCannabisSourceMetaData() {
        return cannabisSourceMetaData;
    }

    public int getPairedIndex() {
        return pairedIndex;
    }

    @Override
    protected CannabisSourceFileMetaData clone() {
        return new CannabisSourceFileMetaData(cannabisSourceMetaData.clone(), pairedIndex);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CannabisSourceFileMetaData that = (CannabisSourceFileMetaData) o;
        return pairedIndex == that.pairedIndex &&
                Objects.equals(cannabisSourceMetaData, that.cannabisSourceMetaData);
    }

    @Override
    public int hashCode() {
        return Objects.hash(cannabisSourceMetaData, pairedIndex);
    }

    @Override
    public String toString() {
        return "CannabisSourceFileMetaData{" +
                "cannabisSourceMetaData=" + cannabisSourceMetaData +
                ", pairedIndex=" + pairedIndex +
                '}';
    }
}

