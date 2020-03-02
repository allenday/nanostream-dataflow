package com.google.allenday.nanostream.launcher.util;

import com.google.allenday.nanostream.launcher.exception.BadRequestException;

import java.util.List;

public final class AssertUtil {

    public static void assertNotEmpty(String s, String message) {
        if ( s == null || s.length() == 0 ) {
            throw new BadRequestException("EMPTY_PARAMETER", message);
        }
    }

    public static <T> void assertNotEmpty(List<T> s, String message) {
        if ( s == null || s.size() == 0 ) {
            throw new BadRequestException("EMPTY_PARAMETER", message);
        }
    }

}