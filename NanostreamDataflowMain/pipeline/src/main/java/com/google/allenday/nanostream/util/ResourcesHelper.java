package com.google.allenday.nanostream.util;

import org.apache.commons.io.IOUtils;

import java.io.IOException;

import static com.google.common.base.Charsets.UTF_8;

/**
 * Provides methods for working with resources/ files
 */
public class ResourcesHelper {

    /**
     * Extracts content of given file by file name
     */
    public String getFileContent(String fileName) {

        String result = "";

        ClassLoader classLoader = getClass().getClassLoader();
        try {
            result = IOUtils.toString(classLoader.getResourceAsStream(fileName), UTF_8.name());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }
}
