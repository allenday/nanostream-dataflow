package com.google.allenday.nanostream.cannabis_source;

import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

//TODO

/**
 *
 */
@DefaultCoder(AvroCoder.class)
public class CannabisSourceMetaData implements Serializable {

    private String project;
    private String projectId;
    private String bioSample;
    private String sraSample;
    private String run;
    private String librarySource;
    private String libraryLayout;
    private long insertSize;
    private long mBases;
    private String sampleName;
    private String libraryName;
    private String cultivar;
    private int readGroup;
    private String leaflyName;
    private String leaflyUrl;
    private String tempSortable;

    public CannabisSourceMetaData() {
    }

    public CannabisSourceMetaData(String project, String projectId, String bioSample, String sraSample, String run,
                                  String librarySource, String libraryLayout, long insertSize, long mBases,
                                  String sampleName, String libraryName, String cultivar, int readGroup,
                                  String leaflyName, String leaflyUrl, String tempSortable) {
        this.project = project;
        this.projectId = projectId;
        this.bioSample = bioSample;
        this.sraSample = sraSample;
        this.run = run;
        this.librarySource = librarySource;
        this.libraryLayout = libraryLayout;
        this.insertSize = insertSize;
        this.mBases = mBases;
        this.sampleName = sampleName;
        this.libraryName = libraryName;
        this.cultivar = cultivar;
        this.readGroup = readGroup;
        this.leaflyName = leaflyName;
        this.leaflyUrl = leaflyUrl;
        this.tempSortable = tempSortable;
    }

    public String generateFolderAndNameForSampleAndReadGroup() {
        String base = sraSample + "_readGroup_" + readGroup;
        return base + "/" + base;
    }

    public String generateUniqueFolderAndNameForSampleAndReadGroup() {
        return generateFolderAndNameForSampleAndReadGroup() + "_" + UUID.randomUUID().toString();
    }

    public boolean isPaired() {
        return libraryLayout.equals("PAIRED");
    }

    public String getProject() {
        return project;
    }

    public String getProjectId() {
        return projectId;
    }

    public String getBioSample() {
        return bioSample;
    }

    public String getSraSample() {
        return sraSample;
    }

    public String getRun() {
        return run;
    }

    public String getLibrarySource() {
        return librarySource;
    }

    public String getLibraryLayout() {
        return libraryLayout;
    }

    public long getInsertSize() {
        return insertSize;
    }

    public long getmBases() {
        return mBases;
    }

    public String getSampleName() {
        return sampleName;
    }

    public String getLibraryName() {
        return libraryName;
    }

    public String getCultivar() {
        return cultivar;
    }

    public int getReadGroup() {
        return readGroup;
    }

    public String getLeaflyName() {
        return leaflyName;
    }

    public String getLeaflyUrl() {
        return leaflyUrl;
    }

    public String getTempSortable() {
        return tempSortable;
    }

    @Override
    protected CannabisSourceMetaData clone() {
        return new CannabisSourceMetaData(project, projectId, bioSample, sraSample, run, librarySource, libraryLayout,
                insertSize, mBases, sampleName, libraryName, cultivar, readGroup, leaflyName, leaflyUrl, tempSortable);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        CannabisSourceMetaData that = (CannabisSourceMetaData) o;
        return insertSize == that.insertSize &&
                mBases == that.mBases &&
                readGroup == that.readGroup &&
                Objects.equals(project, that.project) &&
                Objects.equals(projectId, that.projectId) &&
                Objects.equals(bioSample, that.bioSample) &&
                Objects.equals(sraSample, that.sraSample) &&
                Objects.equals(run, that.run) &&
                Objects.equals(librarySource, that.librarySource) &&
                Objects.equals(libraryLayout, that.libraryLayout) &&
                Objects.equals(sampleName, that.sampleName) &&
                Objects.equals(libraryName, that.libraryName) &&
                Objects.equals(cultivar, that.cultivar) &&
                Objects.equals(leaflyName, that.leaflyName) &&
                Objects.equals(leaflyUrl, that.leaflyUrl) &&
                Objects.equals(tempSortable, that.tempSortable);
    }

    @Override
    public int hashCode() {
        return Objects.hash(project, projectId, bioSample, sraSample, run, librarySource, libraryLayout, insertSize,
                mBases, sampleName, libraryName, cultivar, readGroup, leaflyName, leaflyUrl, tempSortable);
    }

    @Override
    public String toString() {
        return "CannabisSourceFileMetaData{" +
                "project='" + project + '\'' +
                ", projectId='" + projectId + '\'' +
                ", bioSample='" + bioSample + '\'' +
                ", sraSample='" + sraSample + '\'' +
                ", run='" + run + '\'' +
                ", librarySource='" + librarySource + '\'' +
                ", libraryLayout='" + libraryLayout + '\'' +
                ", insertSize=" + insertSize +
                ", mBases=" + mBases +
                ", sampleName='" + sampleName + '\'' +
                ", libraryName='" + libraryName + '\'' +
                ", cultivar='" + cultivar + '\'' +
                ", readGroup=" + readGroup +
                ", leaflyName='" + leaflyName + '\'' +
                ", leaflyUrl='" + leaflyUrl + '\'' +
                ", tempSortable='" + tempSortable + '\'' +
                '}';
    }
}

