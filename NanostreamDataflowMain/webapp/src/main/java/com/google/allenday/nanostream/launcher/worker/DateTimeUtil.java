package com.google.allenday.nanostream.launcher.worker;

import java.time.Instant;

public final class DateTimeUtil {

    public static String makeTimestamp() {
        return Instant.now().toString().replaceAll("[-:.]", "").toLowerCase();
    }

}
