/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.google.allenday.nanostream.utils;

import org.apache.beam.sdk.io.FileSystems;
import org.apache.beam.sdk.io.fs.ResolveOptions.StandardResolveOptions;
import org.apache.beam.sdk.io.fs.ResourceId;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Utilities for dealing with movement of files from object stores and workers.
 */
public class FileUtils {

    private static final Logger LOG = LoggerFactory.getLogger(FileUtils.class);

    public static ResourceId getFileResourceId(String directory, String fileName) {
        ResourceId resourceID = FileSystems.matchNewResource(directory, true);
        return resourceID.getCurrentDirectory().resolve(fileName, StandardResolveOptions.RESOLVE_FILE);
    }

    public static String toStringParams(ProcessBuilder builder) {
        return String.join(",", builder.command());
    }

    public static String copyFile(ResourceId sourceFile, ResourceId destinationFile)
            throws IOException {

        try (WritableByteChannel writeChannel = FileSystems.create(destinationFile, "text/plain")) {
            try (ReadableByteChannel readChannel = FileSystems.open(sourceFile)) {

                final ByteBuffer buffer = ByteBuffer.allocateDirect(16 * 1024);
                while (readChannel.read(buffer) != -1) {
                    buffer.flip();
                    writeChannel.write(buffer);
                    buffer.compact();
                }
                buffer.flip();
                while (buffer.hasRemaining()) {
                    writeChannel.write(buffer);
                }
            }
        }

        return destinationFile.toString();
    }

    public static String readLineOfLogFile(Path path) {

        try (BufferedReader br = Files.newBufferedReader(Paths.get(path.toString()), UTF_8)) {
            return br.readLine();
        } catch (IOException e) {
            LOG.error("Error reading the first line of file", e);
        }

        // `return empty string rather than NULL string as this data is often used in further logging
        return "";
    }

    public static boolean mkdir(String path) {
        Path dir;
        if (path.charAt(path.length() - 1) != '/') {
            dir = Paths.get(path).getParent();
        } else {
            dir = Paths.get(path);
        }
        try {
            if (!Files.exists(dir)) {
                Files.createDirectories(dir);
                LOG.info(String.format("Dir %s created", dir.toString()));
            }
            return true;
        } catch (IOException e) {
            LOG.error(e.getMessage());
            return false;
        }
    }

    public static void deleteFile(String filePath){
        File fileToDelete = new File(filePath);
        if (fileToDelete.exists()){
            boolean delete = fileToDelete.delete();
            if (delete) {
                LOG.info(String.format("File %s deleted", filePath));
            }
        }
    }

    public static void deleteDir(String dirPath){
        try {
            org.apache.commons.io.FileUtils.deleteDirectory(new File(dirPath));
            LOG.info(String.format("Dir %s deleted", dirPath));
        } catch (IOException e) {
            LOG.error(e.getMessage());
        }
    }
}
