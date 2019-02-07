package com.theappsolutions.nanostream.fastq;

import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.repackaged.beam_sdks_java_core.org.apache.commons.lang3.StringUtils;
import org.apache.beam.sdk.transforms.DoFn;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

/**
 * Creates {@link FastqRecord} instance from .fastq file data
 */
public class ParseFastQFn extends DoFn<String, FastqRecord> {

    private final static String NEW_FASTQ_INDICATION = "runid";

    @ProcessElement
    public void processElement(ProcessContext c) {
        List<Integer> fastqStarts = new ArrayList<>();

        String data = c.element();

        int position = 0;
        Scanner scanner = new Scanner(data);
        while (scanner.hasNextLine()) {
            String line = scanner.nextLine();
            if (line.contains(NEW_FASTQ_INDICATION)) {
                fastqStarts.add(position);
            }
            position += line.length()+"\n".length();
        }
        scanner.close();

        IntStream.range(0, fastqStarts.size()).forEach(index -> {
            int startFastqEntityPosition = fatqStarts.get(index);
            int endFastqEntityPosition = (fastqStarts.size() > index + 1)
                    ? fastqStarts.get(index + 1) : data.length();
            String payload = data.substring(startFastqEntityPosition, endFastqEntityPosition);
            String[] payloadAsLines = StringUtils.split(payload, "\n");
            List<String> linesList = Arrays.stream(payloadAsLines)
                    .filter(line -> !line.trim().isEmpty()).collect(Collectors.toList());

            String readName = clearReadNameFromTags(linesList.get(0));
            String readBases = linesList.get(1);
            String qualityHeader = linesList.get(2);
            String baseQualities = linesList.get(3);

            FastqRecord fastQ = new FastqRecord(readName, readBases, qualityHeader, baseQualities);
            c.output(fastQ);
        });
    }

    /**
     * Clears original readName data form special tags, namely "XXXX" and "@"
     */
    private String clearReadNameFromTags(String readName){
        return readName.replace("XXXX", "").replace("@", "");
    }
}
