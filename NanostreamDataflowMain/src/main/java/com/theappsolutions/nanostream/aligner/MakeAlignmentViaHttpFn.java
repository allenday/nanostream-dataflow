package com.theappsolutions.nanostream.aligner;

import com.theappsolutions.nanostream.http.NanostreamHttpService;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.transforms.DoFn;
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
public class MakeAlignmentViaHttpFn extends DoFn<Iterable<FastqRecord>, String> {

    private final static String DATABASE_MULTIPART_KEY = "database";
    private final static String FASTQ_DATA_MULTIPART_KEY = "fastq";

    private Logger LOG = LoggerFactory.getLogger(MakeAlignmentViaHttpFn.class);

    private NanostreamHttpService nanostreamHttpService;
    private String database;
    private String endpoint;

    public MakeAlignmentViaHttpFn(NanostreamHttpService nanostreamHttpService,
                                  String database,
                                  String endpoint) {
        this.nanostreamHttpService = nanostreamHttpService;
        this.database = database;
        this.endpoint = endpoint;
    }

    @ProcessElement
    public void processElement(ProcessContext c) throws IOException, URISyntaxException {
        Iterable<FastqRecord> data = c.element();

        Map<String, String> content = new HashMap<>();
        content.put(DATABASE_MULTIPART_KEY, database);
        content.put(FASTQ_DATA_MULTIPART_KEY, prepareFastQData(data));


        LOG.info(String.format("Sending Alignment request (#%s) with %d elements...", content.hashCode(),
                StreamSupport.stream(data.spliterator(), false).count()));

        String responseBody = nanostreamHttpService.generateAlignData(endpoint, content);


        if (responseBody == null) {
            LOG.info(String.format("Receive NULL Alignment response (#%s)", content.hashCode()));
        } else if (responseBody.length() == 0) {
            LOG.info(String.format("Receive EMPTY Alignment response (#%s)", content.hashCode()));
        } else {
            LOG.info(String.format("Receive Alignment response (#%s) with %d length", content.hashCode(), responseBody.length()));
            c.output(responseBody);
        }
    }

    private String prepareFastQData(Iterable<FastqRecord> data) {
        StringBuilder fastqPostBody = new StringBuilder();
        for (FastqRecord fq : data) {
            fastqPostBody.append(fq.toFastQString()).append("\n");
        }
        return fastqPostBody.toString();
    }
}