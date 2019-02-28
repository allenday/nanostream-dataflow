package com.google.allenday.nanostream.kalign;

import com.google.allenday.nanostream.http.NanostreamHttpService;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import japsa.seq.Alphabet;
import japsa.seq.FastaReader;
import japsa.seq.Sequence;
import japsa.seq.SequenceReader;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;

/**
 * Makes K-Align transformation of {@link Sequence} via HTTP server*
 * See <a href="https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-6-298">K-Align</a>
 * information
 */
public class ProceedKAlignmentFn extends DoFn<KV<KV<GCSSourceData, String>, Iterable<Sequence>>, KV<KV<GCSSourceData, String>, Iterable<Sequence>>> {

    private final static String FASTA_DATA_MULTIPART_KEY = "fasta";

    private Logger LOG = LoggerFactory.getLogger(ProceedKAlignmentFn.class);

    private NanostreamHttpService nanostreamHttpService;
    private String endpoint;

    public ProceedKAlignmentFn(NanostreamHttpService nanostreamHttpService,
                               String endpoint) {
        this.nanostreamHttpService = nanostreamHttpService;
        this.endpoint = endpoint;
    }


    @ProcessElement
    public void processElement(ProcessContext c) {
        Iterable<Sequence> sequenceIterable = c.element().getValue();
        long sequenceIterableSize = StreamSupport.stream(sequenceIterable.spliterator(), false)
                .count();
        if (sequenceIterableSize <= 1) {
            c.output(c.element());
            return;
        }

        Map<String, String> content = new HashMap<>();
        content.put(FASTA_DATA_MULTIPART_KEY, prepareFastAData(sequenceIterable));

        try {
            LOG.info(String.format("Sending K-Align request with %d elements...", sequenceIterableSize));
            @Nonnull
            String responseBody = nanostreamHttpService.generateAlignData(endpoint, content);

            List<Sequence> seqList = new ArrayList<>();
            SequenceReader fastaReader = FastaReader.getReader(new ByteArrayInputStream(responseBody.getBytes()));

            if (fastaReader == null) {
                return;
            }
            Sequence nSeq;
            while ((nSeq = fastaReader.nextSequence(Alphabet.DNA())) != null) {
                seqList.add(nSeq);
            }
            fastaReader.close();


            c.output(KV.of(c.element().getKey(), seqList));
        } catch (URISyntaxException | IOException e) {
            LOG.error(e.getMessage());
        }

    }

    private String prepareFastAData(Iterable<Sequence> sequenceIterable) {
        StringBuilder fasta = new StringBuilder();
        for (Sequence s : sequenceIterable) {
            fasta.append(">").append(s.getName()).append("\n").append(s.toString()).append("\n");
        }
        return fasta.toString();
    }
}