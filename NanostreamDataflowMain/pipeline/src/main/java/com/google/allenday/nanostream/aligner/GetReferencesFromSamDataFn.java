package com.google.allenday.nanostream.aligner;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.io.TransformIoHandler;
import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.allenday.genomics.core.model.SampleMetaData;
import com.google.allenday.genomics.core.processing.sam.SamBamManipulationService;
import com.google.allenday.genomics.core.reference.ReferenceDatabaseSource;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import htsjdk.samtools.SAMRecord;
import htsjdk.samtools.SamReader;
import htsjdk.samtools.ValidationStringency;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

/**
 * Parses aligned data that comes HTTP aligner into {@link SAMRecord}. Generates {@link KV<KV<GCSSourceData, String>, Sequence>>} object that contains
 * name reference-Sequence pair
 */
public class GetReferencesFromSamDataFn extends DoFn<KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>>,
        KV<KV<GCSSourceData, String>, ReferenceDatabaseSource>> {
    private Logger LOG = LoggerFactory.getLogger(GetReferencesFromSamDataFn.class);

    private FileUtils fileUtils;
    private TransformIoHandler ioHandler;
    private GCSService gcsService;
    private final SamBamManipulationService samBamManipulationService;

    public GetReferencesFromSamDataFn(FileUtils fileUtils, TransformIoHandler ioHandler, SamBamManipulationService samBamManipulationService) {
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
        KV<SampleMetaData, KV<ReferenceDatabaseSource, FileWrapper>> element = c.element();

        SampleMetaData geneSampleMetaData = element.getKey();
        FileWrapper fileWrapper = element.getValue().getValue();
        ReferenceDatabaseSource referenceDatabaseSource = element.getValue().getKey();
        String workingDir = fileUtils.makeDirByCurrentTimestampAndSuffix(geneSampleMetaData.getRunId());

        try {
            String filePath = ioHandler.handleInputAsLocalFile(gcsService, fileWrapper, workingDir);

            SamReader reader = samBamManipulationService.samReaderFromBamFile(filePath, ValidationStringency.SILENT);
            for (SAMRecord sam : reader) {
                if (!sam.getReferenceName().equals("*")) {
                    GCSSourceData gcsSourceData = GCSSourceData.fromJsonString(geneSampleMetaData.getSrcRawMetaData());
                    c.output(KV.of(KV.of(gcsSourceData, sam.getReferenceName()), referenceDatabaseSource));
                }
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        fileUtils.deleteDir(workingDir);
    }
}