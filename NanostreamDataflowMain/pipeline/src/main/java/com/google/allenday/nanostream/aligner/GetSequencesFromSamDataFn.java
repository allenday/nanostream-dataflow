package com.google.allenday.nanostream.aligner;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.io.TransformIoHandler;
import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.allenday.genomics.core.model.ReferenceDatabase;
import com.google.allenday.genomics.core.model.SampleMetaData;
import com.google.allenday.genomics.core.processing.sam.SamBamManipulationService;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Parses aligned data that comes HTTP aligner into {@link SAMRecord}. Generates {@link KV<String, Sequence>>} object that contains
 * name reference-Sequence pair
 */
public class GetSequencesFromSamDataFn extends DoFn<KV<KV<SampleMetaData, ReferenceDatabase>, FileWrapper>, KV<KV<GCSSourceData, String>, Sequence>> {
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
        KV<KV<SampleMetaData, ReferenceDatabase>, FileWrapper> element = c.element();

        SampleMetaData geneSampleMetaData = element.getKey().getKey();
        String workingDir = fileUtils.makeDirByCurrentTimestampAndSuffix(geneSampleMetaData.getRunId());

        try {
            String filePath = ioHandler.handleInputAsLocalFile(gcsService, element.getValue(), workingDir);

            SamReader reader = samBamManipulationService.samReaderFromBamFile(filePath, ValidationStringency.SILENT);
            for (SAMRecord sam : reader) {
                if (!sam.getReferenceName().equals("*")) {
                    GCSSourceData gcsSourceData = GCSSourceData.fromJsonString(geneSampleMetaData.getSrcRawMetaData());
                    c.output(KV.of(KV.of(gcsSourceData, sam.getReferenceName()), generateSequenceFromSam(sam)));
                }
            }
            reader.close();
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