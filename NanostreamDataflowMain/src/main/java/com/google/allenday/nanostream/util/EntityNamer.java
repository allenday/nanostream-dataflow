package com.google.allenday.nanostream.util;

import com.google.allenday.nanostream.ProcessingMode;
import com.google.allenday.nanostream.other.Configuration;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EntityNamer {

    private final long initialTimestamp;

    public static SimpleDateFormat JOB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ssz");

    private EntityNamer() {
        this.initialTimestamp = System.currentTimeMillis();
    }

    public static EntityNamer initialize() {
        return new EntityNamer();
    }

    public static String generateNameForCollection(String collectionNamePrefix, String bucket) {
        StringBuilder nameBuilder = new StringBuilder();
        if (collectionNamePrefix != null && !collectionNamePrefix.isEmpty()) {
            nameBuilder.append(collectionNamePrefix).append("__");
        }
        nameBuilder.append(Configuration.SEQUENCES_STATISTIC_COLLECTION_NAME_BASE).append("__");
        nameBuilder.append(bucket);
        return nameBuilder.toString();
    }

    public static String generateNameForDocument(String documentNamePrefix, String folder) {
        StringBuilder nameBuilder = new StringBuilder();
        if (documentNamePrefix != null && !documentNamePrefix.isEmpty()) {
            nameBuilder.append(documentNamePrefix).append("__");
        }
        nameBuilder.append(folder.replace("/", "*"));
        return nameBuilder.toString();
    }

    public String generateJobName(ProcessingMode processingMode, String prefix) {
        return generateTimestampedName(addPrefix(processingMode.label, prefix));
    }

    public String generateTimestampedName(String str) {
        return generateTimestampedName(str, new Date(initialTimestamp));
    }

    public static String generateTimestampedName(String str, Date date) {
        return String.format("%s--%s", str, JOB_DATE_FORMAT.format(date)).replace("_", "-");
    }

    public static String addPrefixWithProcessingMode(String base, ProcessingMode processingMode, String prefix) {
        return addPrefix(addPrefix(base, processingMode.label), prefix);
    }

    public static String addPrefix(String base, String prefix) {
        StringBuilder nameBuilder = new StringBuilder();
        if (prefix != null) {
            nameBuilder.append(prefix).append("_");
        }
        nameBuilder.append(base);
        return nameBuilder.toString();
    }

    public long getInitialTimestamp() {
        return initialTimestamp;
    }
}
