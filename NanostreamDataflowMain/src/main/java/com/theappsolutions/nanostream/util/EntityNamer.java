package com.theappsolutions.nanostream.util;

import com.theappsolutions.nanostream.NanostreamApp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EntityNamer {
    public static SimpleDateFormat JOB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ssz");

    public static String generateJobName(NanostreamApp.ProcessingMode processingMode, String prefix) {
        return generateTimestampedName(addPrefix(processingMode.label, prefix));
    }

    public static String generateTimestampedName(String str) {
        return String.format("%s--%s", str, JOB_DATE_FORMAT.format(new Date())).replace("_", "-");
    }

    public static String addPrefixWithProcessingMode(String base, NanostreamApp.ProcessingMode processingMode, String prefix) {
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
}
