package com.theappsolutions.nanostream.util;

import com.theappsolutions.nanostream.NanostreamApp;

import java.text.SimpleDateFormat;
import java.util.Date;

public class EntityNamer {
    public static SimpleDateFormat JOB_DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH-mm-ssz");

    public static String generateJobName(NanostreamApp.ProcessingMode processingMode, String prefix) {
        StringBuilder nameBuilder = new StringBuilder();
        if (prefix != null) {
            nameBuilder.append(prefix).append("_");
        }
        nameBuilder.append(processingMode.label);
        return generateName(nameBuilder.toString());
    }

    public static String generateName(String str) {
        return String.format("%s--%s", str, JOB_DATE_FORMAT.format(new Date())).replace("_", "-");
    }
}
