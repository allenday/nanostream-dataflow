package com.google.allenday.nanostream.kalign;

import com.google.allenday.genomics.core.align.KAlignService;
import com.google.allenday.genomics.core.cmd.CmdExecutor;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import japsa.seq.Alphabet;
import japsa.seq.FastaReader;
import japsa.seq.Sequence;
import japsa.seq.SequenceReader;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.StreamSupport;

/**
 * Makes K-Align transformation of {@link Sequence} via HTTP server*
 * See <a href="https://bmcbioinformatics.biomedcentral.com/articles/10.1186/1471-2105-6-298">K-Align</a>
 * information
 */
public class ProceedKAlignmentFn extends DoFn<KV<KV<GCSSourceData, String>, Iterable<Sequence>>, KV<KV<GCSSourceData, String>, Iterable<Sequence>>> {

    private Logger LOG = LoggerFactory.getLogger(ProceedKAlignmentFn.class);

    private FileUtils fileUtils;
    private KAlignService kAlignService;

    public ProceedKAlignmentFn(FileUtils fileUtils, KAlignService kAlignService) {
        this.fileUtils = fileUtils;
        this.kAlignService = kAlignService;
    }

    @Setup
    public void setUp() {
        kAlignService.setupKAlign2();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        Iterable<Sequence> sequenceIterable = c.element().getValue();
        long sequenceIterableSize = StreamSupport.stream(sequenceIterable.spliterator(), false)
                .count();
        LOG.info(c.element().getKey().toString());
        LOG.info(String.format("Fasta size %d", sequenceIterableSize));
        if (sequenceIterableSize <= 1) {
            c.output(c.element());
            return;
        }
        String refName = c.element().getKey().getValue().replace(".","_");
        String workingDir = fileUtils.makeUniqueDirWithTimestampAndSuffix(refName);

        String currentTimestamp = String.valueOf(System.currentTimeMillis());
        try {

            try {
                String fastaFilePath = prepareFastaFile(sequenceIterable,
                        workingDir + refName + "_" + currentTimestamp + KAlignService.FASTA_FILE_EXTENSION);
                GCSService gcsService = GCSService.initialize(new FileUtils());
                gcsService.writeToGcs("nanostream-clinic",fastaFilePath, fastaFilePath);

                String kalignedFilePath = kAlignService.kAlignFasta(fastaFilePath, workingDir,
                        refName + "_" + "kalined", currentTimestamp);

                List<Sequence> seqList = new ArrayList<>();
                SequenceReader fastaReader = FastaReader.getReader(fileUtils.getInputStreamFromFile(kalignedFilePath));

                if (fastaReader == null) {
                    return;
                }
                Sequence nSeq;
                while ((nSeq = fastaReader.nextSequence(Alphabet.DNA())) != null) {
                    seqList.add(nSeq);
                }
                fastaReader.close();


                c.output(KV.of(c.element().getKey(), seqList));

            } catch (IOException e) {
                e.printStackTrace();
            }

        } catch (RuntimeException e) {
            LOG.error(e.getMessage());
            e.printStackTrace();
        }
        fileUtils.deleteDir(workingDir);
    }

    private String prepareFastaFile(Iterable<Sequence> sequenceIterable, String filePath) throws IOException {
        FileWriter fileWriter = new FileWriter(filePath);
        for (Sequence s : sequenceIterable) {
            fileWriter.write(">" + s.getName() + "\n" + s.toString() + "\n");
        }
        fileWriter.flush();
        fileWriter.close();
        return filePath;
    }
}