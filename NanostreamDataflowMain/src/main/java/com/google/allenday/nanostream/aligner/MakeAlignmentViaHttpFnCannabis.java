package com.google.allenday.nanostream.aligner;

import com.google.allenday.nanostream.cannabis.CannabisSourceMetaData;
import com.google.allenday.nanostream.http.NanostreamHttpService;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Makes alignment of fastq data via HTTP server and returnes string body of alignment HTTP response
 */
public class MakeAlignmentViaHttpFnCannabis extends DoFn<KV<CannabisSourceMetaData, Iterable<Iterable<KV<FastqRecord, Integer>>>>, KV<CannabisSourceMetaData, String>> {

    private final static String DATABASE_MULTIPART_KEY = "database";
    private final static String FASTQ_DATA_MULTIPART_KEY = "fastq";
    private final static String BWA_ARGUMENTS_MULTIPART_KEY = "args";

    private Logger LOG = LoggerFactory.getLogger(MakeAlignmentViaHttpFnCannabis.class);

    private NanostreamHttpService nanostreamHttpService;
    private String database;
    private String endpoint;
    private String bwaArguments;

    public MakeAlignmentViaHttpFnCannabis(NanostreamHttpService nanostreamHttpService,
                                          String database,
                                          String endpoint,
                                          String bwaArguments) {
        this.nanostreamHttpService = nanostreamHttpService;
        this.database = database;
        this.endpoint = endpoint;
        this.bwaArguments = bwaArguments;
    }

    @ProcessElement
    public void processElement(ProcessContext c) throws IOException, URISyntaxException {
        KV<CannabisSourceMetaData, Iterable<Iterable<KV<FastqRecord, Integer>>>> data = c.element();

        Map<String, String> content = new HashMap<>();
        content.put(DATABASE_MULTIPART_KEY, database);
        content.put(FASTQ_DATA_MULTIPART_KEY, prepareFastQData(data.getValue()));
        content.put(BWA_ARGUMENTS_MULTIPART_KEY, bwaArguments);

        try {
            LOG.info(String.format("Sending Alignment request (#%s) with %d elements...", content.hashCode(),
                    StreamSupport.stream(data.getValue().spliterator(), false).count()));

            String responseBody = nanostreamHttpService.generateAlignData(endpoint, content);

            if (responseBody == null) {
                LOG.info(String.format("Receive NULL Alignment response (#%s)", content.hashCode()));
            } else if (responseBody.length() == 0) {
                LOG.info(String.format("Receive EMPTY Alignment response (#%s)", content.hashCode()));
            } else {
                LOG.info(String.format("Receive Alignment response (#%s) with %d length", content.hashCode(), responseBody.length()));
                c.output(KV.of(data.getKey(), responseBody));
            }
        } catch (URISyntaxException | IOException e) {
            LOG.error(e.getMessage());
        }
    }

    private String prepareFastQData(Iterable<Iterable<KV<FastqRecord, Integer>>> data) {
        StringBuilder fastqPostBody = new StringBuilder();
        for (Iterable<KV<FastqRecord, Integer>> pair : data) {
            for (KV<FastqRecord, Integer> element : pair) {
                fastqPostBody.append(element.getKey().toFastQString()).append("\n");
            }
        }
        return fastqPostBody.toString();
    }
}