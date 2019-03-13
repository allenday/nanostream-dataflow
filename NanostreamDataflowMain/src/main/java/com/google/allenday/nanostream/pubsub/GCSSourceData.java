package com.google.allenday.nanostream.pubsub;

import org.apache.beam.sdk.coders.AvroCoder;
import org.apache.beam.sdk.coders.DefaultCoder;

import javax.annotation.Nonnull;
import java.io.Serializable;
import java.util.Objects;

/**
 * Data class with information of bucket and folder of source FastQ file
 */
@DefaultCoder(AvroCoder.class)
public class GCSSourceData implements Serializable {

    private String bucket;
    private String folder;
    private String filename;

    public GCSSourceData() {
    }

    public void setBucket(String bucket) {
        this.bucket = bucket;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public GCSSourceData(@Nonnull String bucket, @Nonnull String folder, @Nonnull String filename) {
        this.bucket = bucket;
        this.folder = folder;
        this.filename = filename;
    }

    public static GCSSourceData fromGCloudNotification(GCloudNotification gCloudNotification) {
        String folder = "/";
        String filename;

        String name = gCloudNotification.getName();
        int index = name.lastIndexOf("/");
        if (index >= 0) {
            folder += name.substring(0, index + 1);
            filename = name.substring(index + 1, name.length());
        } else {
            filename = name;
        }

        return new GCSSourceData(gCloudNotification.getBucket(), folder, filename);
    }

    public String getBucket() {
        return bucket;
    }

    public String getFolder() {
        return folder;
    }

    public String getFilename() {
        return filename;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        GCSSourceData that = (GCSSourceData) o;
        return bucket.equals(that.bucket) &&
                folder.equals(that.folder) &&
                filename.equals(that.filename);
    }

    @Override
    public int hashCode() {
        return Objects.hash(bucket, folder, filename);
    }

    @Override
    public String toString() {
        return "GCSSourceData{" +
                "bucket='" + bucket + '\'' +
                ", folder='" + folder + '\'' +
                ", folder='" + filename + '\'' +
                '}';
    }
}

