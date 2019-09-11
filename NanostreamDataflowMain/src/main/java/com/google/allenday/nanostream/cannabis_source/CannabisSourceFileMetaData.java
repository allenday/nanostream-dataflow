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


    public static List<CannabisSourceFileMetaData> fromCSVLine(String csvLine) throws Exception{
        String[] parts = csvLine.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        CannabisSourceMetaData cannabisSourceFileMetaData = new CannabisSourceMetaData(
                parts[0],
                parts[1],
                parts[2],
                parts[3],
                parts[4],
                parts[5],
                parts[6],
                isNumeric(parts[7]) ? Long.parseLong(parts[7]) : 0,
                isNumeric(parts[8]) ? Long.parseLong(parts[8]) : 0,
                parts[9],
                parts[10],
                parts[11],
                isNumeric(parts[12]) ?Integer.parseInt(parts[12]) : 0,
                parts[14],
                parts[15],
                parts[16]);

        List<CannabisSourceFileMetaData> results = new ArrayList<>();
        results.add(new CannabisSourceFileMetaData(cannabisSourceFileMetaData, 1));

        if (cannabisSourceFileMetaData.isPaired()) {
            results.add(new CannabisSourceFileMetaData(cannabisSourceFileMetaData.clone(), 2));

        }
        return results;
    }

    public static boolean isNumeric(String str) {
        return str.matches("-?\\d+(\\.\\d+)?");  //match a number with optional '-' and decimal.
    }

    public String generateGCSBlobName() {
        String runName = cannabisSourceMetaData.getRun();
        runName += "_" + pairedIndex;
        runName += ".fastq";
        if (cannabisSourceMetaData.getProject().toLowerCase().equals("Kannapedia".toLowerCase())) {
            return "kannapedia/" + runName;
        } else {
            return "sra/" + cannabisSourceMetaData.getProjectId() + "/" + cannabisSourceMetaData.getSraSample() + "/" + runName;
        }
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

