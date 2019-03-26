package com.google.allenday.nanostream.util;

import com.google.cloud.ReadChannel;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.repackaged.beam_sdks_java_core.org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilities class for manipulations fist FastQ data
 */
public class FastQUtils {
    private final static int BUFFER_SIZE = 64 * 1024;

    /**
     * Reads FastQ data line by line from GCS. Helps to eliminate Out Of Memory problems with large FastQ files
     */
    public static void readFastqBlob(ReadChannel readChannel, Callback<String> callback) throws IOException {
        StringBuilder fastqStrandBuilder = new StringBuilder();
        ByteBuffer bytes = ByteBuffer.allocate(BUFFER_SIZE);
        while (readChannel.read(bytes) > 0) {
            bytes.flip();
            String data = fastqStrandBuilder.toString() + StandardCharsets.UTF_8.decode(bytes).toString();
            boolean isCutByLine = data.substring(data.length() - 1).equals("\n");
            fastqStrandBuilder = new StringBuilder();
            bytes.clear();

            String[] readData = data.trim().split("\n");
            for (int i = 0; i < readData.length; i++) {
                String readDatum = readData[i];
                if (readDatum != null && !readDatum.isEmpty()) {
                    if (fastqStrandBuilder.toString().split("\n").length >= 4) {
                        publishResult(fastqStrandBuilder.toString(), callback);
                        fastqStrandBuilder = new StringBuilder();
                    }
                    fastqStrandBuilder.append(readDatum);
                    if (i < readData.length - 1 || isCutByLine) {
                        fastqStrandBuilder.append("\n");
                    }
                }
            }
        }
        if (!fastqStrandBuilder.toString().isEmpty()) {
            publishResult(fastqStrandBuilder.toString(), callback);
        }
    }


    private static void publishResult(String result, Callback<String> callback) {
        callback.onResult(result);
    }

    public static FastqRecord parseFastqRaw(String fastQData) {
        String[] payloadAsLines = StringUtils.split(fastQData, "\n");
        List<String> linesList = Arrays.stream(payloadAsLines)
                .filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

        String readName = clearReadNameFromTags(linesList.get(0));
        String readBases = linesList.get(1);
        String qualityHeader = linesList.get(2);
        String baseQualities = linesList.get(3);

        return new FastqRecord(readName, readBases, qualityHeader, baseQualities);
    }


    /**
     * Clears original readName data form special tags, namely "XXXX" and "@"
     */
    private static String clearReadNameFromTags(String readName) {
        return readName.replace("XXXX", "").replace("@", "");
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
