package com.google.allenday.nanostream.aligner;

import com.google.allenday.genomics.core.align.SamBamManipulationService;
import com.google.allenday.genomics.core.gene.GeneData;
import com.google.allenday.genomics.core.gene.GeneExampleMetaData;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.io.TransformIoHandler;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.allenday.nanostream.util.ObjectSizeFetcher;
import htsjdk.samtools.*;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

/**
 * Parses aligned data that comes HTTP aligner into {@link SAMRecord}. Generates {@link KV<String, Sequence>>} object that contains
 * name reference-Sequence pair
 */
public class GetSequencesFromSamDataFn extends DoFn<KV<GeneExampleMetaData, GeneData>, KV<KV<GCSSourceData, String>, Sequence>> {
    private Logger LOG = LoggerFactory.getLogger(GetSequencesFromSamDataFn.class);

    private FileUtils fileUtils;
    private TransformIoHandler ioHandler;
    private GCSService gcsService;
    private final SamBamManipulationService samBamManipulationService;

    public GetSequencesFromSamDataFn(FileUtils fileUtils, TransformIoHandler ioHandler, SamBamManipulationService samBamManipulationService) {
        this.fileUtils = fileUtils;
        this.ioHandler = ioHandler;
        this.samBamManipulationService = samBamManipulationService;
    }

    @Setup
    public void setUp() {
        gcsService = GCSService.initialize(fileUtils);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<GeneExampleMetaData, GeneData> element = c.element();
        String workingDir = fileUtils.makeUniqueDirWithTimestampAndSuffix(element.getKey().getRunId());

        try {
            String filePath = ioHandler.handleInputAsLocalFile(gcsService, element.getValue(), workingDir);

            List<SAMRecord> results = samBamManipulationService.samRecordsFromBamFile(filePath);
            LOG.info(String.format("List SAMRecords size: %d", ObjectSizeFetcher.sizeOf(results)));
            LOG.info(String.format("List SAMRecords item size: %d", results.size()));
            results.forEach(sam -> {
                if (!sam.getReferenceName().equals("*")) {
                    GCSSourceData gcsSourceData = GCSSourceData.fromJsonString(element.getKey().getSrcRawMetaData());
                    c.output(KV.of(KV.of(gcsSourceData, sam.getReferenceName()), generateSequenceFromSam(sam)));
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileUtils.deleteDir(workingDir);
    }

    private Sequence generateSequenceFromSam(SAMRecord samRecord) {
        Alphabet alphabet = Alphabet.DNA();
        String readString = samRecord.getReadString();
        String name = samRecord.getReadName();
        if (samRecord.getReadNegativeStrandFlag()) {
            Sequence sequence = Alphabet.DNA.complement(new Sequence(alphabet, readString, name));
            sequence.setName(name);
            return sequence;
        } else {
            return new Sequence(alphabet, readString, name);
        }
    }
}