package com.google.allenday.nanostream.pubsub;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;

/**
 * Object class for Cloud Pub/Sub Notifications for Cloud Storage
 * See <a href="https://cloud.google.com/storage/docs/object-change-notification">Object Change Notification</a>
 * documentation
 */
public class GCloudNotification implements Serializable {

    @SerializedName("kind")
    @Expose
    private String kind;
    @SerializedName("id")
    @Expose
    private String id;
    @SerializedName("selfLink")
    @Expose
    private String selfLink;
    @SerializedName("name")
    @Expose
    private String name;
    @SerializedName("bucket")
    @Expose
    private String bucket;
    @SerializedName("generation")
    @Expose
    private String generation;
    @SerializedName("metageneration")
    @Expose
    private String metageneration;
    @SerializedName("contentType")
    @Expose
    private String contentType;
    @SerializedName("timeCreated")
    @Expose
    private String timeCreated;
    @SerializedName("updated")
    @Expose
    private String updated;
    @SerializedName("storageClass")
    @Expose
    private String storageClass;
    @SerializedName("timeStorageClassUpdated")
    @Expose
    private String timeStorageClassUpdated;
    @SerializedName("size")
    @Expose
    private String size;
    @SerializedName("md5Hash")
    @Expose
    private String md5Hash;
    @SerializedName("mediaLink")
    @Expose
    private String mediaLink;
    @SerializedName("contentLanguage")
    @Expose
    private String contentLanguage;
    @SerializedName("metadata")
    @Expose
    private Metadata metadata;
    @SerializedName("crc32c")
    @Expose
    private String crc32c;
    @SerializedName("etag")
    @Expose
    private String etag;

    public String getKind() {
        return kind;
    }

    public String getId() {
        return id;
    }

    public String getSelfLink() {
        return selfLink;
    }

    public String getName() {
        return name;
    }

    public String getBucket() {
        return bucket;
    }

    public String getGeneration() {
        return generation;
    }

    public String getMetageneration() {
        return metageneration;
    }

    public String getContentType() {
        return contentType;
    }

    public String getTimeCreated() {
        return timeCreated;
    }

    public String getUpdated() {
        return updated;
    }

    public String getStorageClass() {
        return storageClass;
    }

    public String getTimeStorageClassUpdated() {
        return timeStorageClassUpdated;
    }

    public String getSize() {
        return size;
    }

    public String getMd5Hash() {
        return md5Hash;
    }

    public String getMediaLink() {
        return mediaLink;
    }

    public String getContentLanguage() {
        return contentLanguage;
    }

    public Metadata getMetadata() {
        return metadata;
    }

    public String getCrc32c() {
        return crc32c;
    }

    public String getEtag() {
        return etag;
    }

    @DefaultCoder(SerializableCoder.class)
    public class Metadata implements Serializable{

        @SerializedName("goog-reserved-file-atime")
        @Expose
        private String googReservedFileAtime;
        @SerializedName("goog-reserved-file-mtime")
        @Expose
        private String googReservedFileMtime;
        @SerializedName("goog-reserved-posix-gid")
        @Expose
        private String googReservedPosixGid;
        @SerializedName("goog-reserved-posix-mode")
        @Expose
        private String googReservedPosixMode;
        @SerializedName("goog-reserved-posix-uid")
        @Expose
        private String googReservedPosixUid;

        public String getGoogReservedFileAtime() {
            return googReservedFileAtime;
        }

        public String getGoogReservedFileMtime() {
            return googReservedFileMtime;
        }

        public String getGoogReservedPosixGid() {
            return googReservedPosixGid;
        }

        public String getGoogReservedPosixMode() {
            return googReservedPosixMode;
        }

        public String getGoogReservedPosixUid() {
            return googReservedPosixUid;
        }
    }
}