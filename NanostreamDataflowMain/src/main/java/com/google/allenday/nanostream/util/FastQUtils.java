package com.google.allenday.nanostream.util;

import com.google.cloud.ReadChannel;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.repackaged.beam_sdks_java_core.org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    private static Logger LOG = LoggerFactory.getLogger(FastQUtils.class);

    private final static int BUFFER_SZIE = 64 * 1024;
    private final static String NEW_FASTQ_INDICATION = "@";
    private static int publishCount = 0;
    private static int readCount = 0;

    /**
     * Reads FastQ data line by line from GCS. Helps to eliminate Out Of Memory problems with large FastQ files
     */
    public static void readFastqBlob(ReadChannel readChannel, Callback<String> callback) throws IOException {
        StringBuilder fastqStrandBuilder = new StringBuilder();

        ByteBuffer bytes = ByteBuffer.allocate(BUFFER_SZIE);
        while (readChannel.read(bytes) > 0) {
            readCount++;

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
                    /*if (readDatum.substring(0, 1).equals(NEW_FASTQ_INDICATION)) {
                        if (!fastqStrandBuilder.toString().isEmpty()) {
                            publishResult(fastqStrandBuilder.toString(), callback);
                        }
                        fastqStrandBuilder = new StringBuilder();
                    }*/

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
        publishCount++;
        LOG.info(String.format("publishResultLength: %d; publishCount, %d; readCount: %d",
                result.length(), publishCount, readCount));
        callback.onResult(result);
    }

    /**
     * Splits multistrand FastQ data into single {@link FastqRecord} objects
     */
    public static void splitMultiStrandFastq(String fastQData, Callback<FastqRecord> callback) {
        /*List<Integer> fastqStarts = new ArrayList<>();

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
        });*/


        String[] payloadAsLines = StringUtils.split(fastQData, "\n");
        List<String> linesList = Arrays.stream(payloadAsLines)
                .filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

        String readName = linesList.get(0);
        String readBases = linesList.get(1);
        String qualityHeader = linesList.get(2);
        String baseQualities = linesList.get(3);

        FastqRecord fastQ = new FastqRecord(readName, readBases, qualityHeader, baseQualities);
        callback.onResult(fastQ);
    }

    public interface Callback<T> {
        void onResult(T result);
    }
}
