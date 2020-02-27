package com.google.allenday.nanostream.launcher.util;

import java.time.Instant;
import java.util.regex.Pattern;

public final class DateTimeUtil {

    private static final Pattern NOT_ALLOWED_CHARS = Pattern.compile("[-:.]");

    public static String makeTimestamp() {
        return NOT_ALLOWED_CHARS.matcher(Instant.now().toString()).replaceAll("").toLowerCase();
    }

}
