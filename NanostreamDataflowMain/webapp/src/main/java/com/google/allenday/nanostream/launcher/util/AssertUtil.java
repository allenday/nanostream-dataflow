package com.google.allenday.nanostream.launcher.util;

import com.google.allenday.nanostream.launcher.exception.BadRequestException;

public final class AssertUtil {

    public static void assertNotEmpty(String s, String message) {
        if ( s == null || s.length() == 0 ) {
            throw new BadRequestException("EMPTY_PARAMETER", message);
        }
    }

}
