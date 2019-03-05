package com.google.allenday.nanostream.aligner;

import com.google.allenday.nanostream.http.NanostreamHttpService;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
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
public class MakeAlignmentViaHttpFn extends DoFn<KV<GCSSourceData, Iterable<FastqRecord>>, KV<GCSSourceData, String>> {

    private final static String DATABASE_MULTIPART_KEY = "database";
    private final static String FASTQ_DATA_MULTIPART_KEY = "fastq";
    private final static String BWA_ARGUMENTS_MULTIPART_KEY = "args";

    private Logger LOG = LoggerFactory.getLogger(MakeAlignmentViaHttpFn.class);

    private NanostreamHttpService nanostreamHttpService;
    private String database;
    private String endpoint;
    private String bwaArguments;

    public MakeAlignmentViaHttpFn(NanostreamHttpService nanostreamHttpService,
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
        KV<GCSSourceData, Iterable<FastqRecord>> data = c.element();

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

    private String prepareFastQData(Iterable<FastqRecord> data) {
        StringBuilder fastqPostBody = new StringBuilder();
        for (FastqRecord fq : data) {
            fastqPostBody.append(fq.toFastQString()).append("\n");
        }
        return fastqPostBody.toString();
    }
}