package com.google.allenday.nanostream.launcher.util;

import java.time.Instant;

public final class DateTimeUtil {

    public static String makeTimestamp() {
        return Instant.now().toString().replaceAll("[-:.]", "").toLowerCase();
    }

}
