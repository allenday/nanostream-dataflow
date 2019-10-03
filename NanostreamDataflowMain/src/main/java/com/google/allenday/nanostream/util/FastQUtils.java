package com.google.allenday.nanostream.util;

import com.google.cloud.ReadChannel;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * Utilities class for manipulations fist FastQ data
 */
public class FastQUtils {

    private final static int BUFFER_SIZE = 64 * 1024;
    private final static String NEW_FASTQ_INDICATION = "@";

    /**
     * Reads FastQ data line by line from GCS. Helps to eliminate Out Of Memory problems with large FastQ files
     */
    public static void readFastqBlob(ReadChannel readChannel, Callback<String> callback) throws IOException {
        StringBuilder fastqBuilder = new StringBuilder();

        ByteBuffer bytes = ByteBuffer.allocate(BUFFER_SIZE);
        while (readChannel.read(bytes) > 0) {
            bytes.flip();
            fastqBuilder.append(StandardCharsets.UTF_8.decode(bytes).toString());
            bytes.clear();

            boolean foundStart, foundEnd;
            do {
                String[] readData = fastqBuilder.toString().trim().split("\n");

                StringBuilder builtFastqBuilder = new StringBuilder();

                foundStart = false;
                foundEnd = false;

                for (String readDatum : readData) {
                    if (readDatum.substring(0, 1).equals(NEW_FASTQ_INDICATION) && !foundEnd) {
                        if (foundStart) {
                            foundEnd = true;
                            fastqBuilder = new StringBuilder();
                        }
                        foundStart = true;
                    }
                    if (foundStart) {
                        StringBuilder builderToAppend = foundEnd ? fastqBuilder : builtFastqBuilder;
                        builderToAppend.append(readDatum).append("\n");
                    }
                }
                if (foundStart && foundEnd) {
                    callback.onResult(builtFastqBuilder.toString());
                }
            } while (foundEnd);
        }
        if (!fastqBuilder.toString().isEmpty()) {
            callback.onResult(fastqBuilder.toString());
        }
    }

    /**
     * Splits multistrand FastQ data into single {@link FastqRecord} objects
     */
    public static void splitMultiStrandFastq(String fastQData, Callback<FastqRecord> callback) {
        List<Integer> fastqStarts = new ArrayList<>();

        int position = 0;
        Scanner scanner = new Scanner(fastQData);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.substring(0, 1).equals(NEW_FASTQ_INDICATION)) {
                fastqStarts.add(position);
            }
            position += line.length() + "\n".length();
        }
        scanner.close();

        IntStream.range(0, fastqStarts.size()).forEach(index -> {
            int startFastqEntityPosition = fastqStarts.get(index);
            int endFastqEntityPosition = (fastqStarts.size() > index + 1)
                    ? fastqStarts.get(index + 1) : fastQData.length();
            String payload = fastQData.substring(startFastqEntityPosition, endFastqEntityPosition);
            String[] payloadAsLines = StringUtils.split(payload, "\n");
            List<String> linesList = Arrays.stream(payloadAsLines)
                    .filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

            String readName = linesList.get(0);
            String readBases = linesList.get(1);
            String qualityHeader = linesList.get(2);
            String baseQualities = linesList.get(3);

            FastqRecord fastQ = new FastqRecord(readName, readBases, qualityHeader, baseQualities);
            callback.onResult(fastQ);
        });
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
