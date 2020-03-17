package com.google.allenday.nanostream.batch;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.allenday.genomics.core.model.SampleMetaData;
import com.google.allenday.genomics.core.processing.align.AlignService;
import com.google.allenday.nanostream.gcs.GCSSourceData;
import com.google.allenday.nanostream.util.FastQUtils;
import com.google.cloud.storage.BlobId;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.windowing.*;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;
import org.joda.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CreateBatchesTransform extends PTransform<PCollection<KV<GCSSourceData, FileWrapper>>,
        PCollection<KV<SampleMetaData, List<FileWrapper>>>> {

    private SequenceBatchesToFastqFiles sequenceBatchesToFastqFiles;

    private int batchSize;
    private int maxBatchWindowSize;

    public CreateBatchesTransform(SequenceBatchesToFastqFiles sequenceBatchesToFastqFiles, int batchSize, int maxBatchWindowSize) {
        this.sequenceBatchesToFastqFiles = sequenceBatchesToFastqFiles;
        this.batchSize = batchSize;
        this.maxBatchWindowSize = maxBatchWindowSize;
    }

    @Override
    public PCollection<KV<SampleMetaData, List<FileWrapper>>> expand(PCollection<KV<GCSSourceData, FileWrapper>> input) {
        return input
                .apply(Window.<KV<GCSSourceData, FileWrapper>>into(new GlobalWindows())
                        .triggering(Repeatedly
                                .forever(AfterFirst.of(
                                        AfterPane.elementCountAtLeast(batchSize),
                                        AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(maxBatchWindowSize)))))
                        .withAllowedLateness(Duration.ZERO)
                        .discardingFiredPanes())
                .apply(GroupByKey.create())
                .apply(ParDo.of(sequenceBatchesToFastqFiles));
    }

    public static class SequenceBatchesToFastqFiles extends DoFn<KV<GCSSourceData, Iterable<FileWrapper>>, KV<SampleMetaData, List<FileWrapper>>> {

        private final static Logger LOG = LoggerFactory.getLogger(CreateBatchesTransform.class);
        private int batchSize;
        private AlignService.Instrument instrument;
        private GCSService gcsService;
        private FileUtils fileUtils;


        public SequenceBatchesToFastqFiles(FileUtils fileUtils, int batchSize, AlignService.Instrument instrument) {
            this.batchSize = batchSize;
            this.instrument = instrument;
            this.fileUtils = fileUtils;
        }

        @Setup
        public void setup() {
            gcsService = GCSService.initialize(fileUtils);
        }


        private KV<SampleMetaData, List<FileWrapper>> buildOutputKV(GCSSourceData gcsSourceData, List<String> outputList) {
            SampleMetaData metaData = SampleMetaData.createUnique(gcsSourceData.toJsonString(),
                    SampleMetaData.LibraryLayout.SINGLE.name(), instrument.name());
            String filename = metaData.getRunId() + ".fastq";
            byte[] byteContent = String.join("", outputList).getBytes();
            return KV.of(metaData, Collections.singletonList(FileWrapper.fromByteArrayContent(byteContent, filename)));

        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            KV<GCSSourceData, Iterable<FileWrapper>> element = c.element();
            GCSSourceData gcsSourceData = element.getKey();

            List<FileWrapper> collect = StreamSupport.stream(element.getValue().spliterator(), false).collect(Collectors.toList());
            LOG.info("Size of fastq files batch before partition: {}", collect.size());

            final List<String> outputList = new ArrayList<>();
            if (gcsSourceData != null) {
                for (FileWrapper fileWrapper : collect) {
                    BlobId blobId = gcsService.getBlobIdFromUri(fileWrapper.getBlobUri());
                    try {
                        FastQUtils.readFastqBlob(gcsService.getBlobReaderByGCloudNotificationData(blobId.getBucket(), blobId.getName()),
                                fastqString -> {
                                    FastQUtils.splitMultiStrandFastq(fastqString, fastqRecord -> {
                                        outputList.add(fastqRecord.toFastQString() + "\n");
                                        if (outputList.size() == batchSize) {
                                            c.output(buildOutputKV(gcsSourceData, outputList));
                                            outputList.clear();
                                        }
                                    });
                                });
                    } catch (IOException e) {
                        e.printStackTrace();
                        LOG.error(e.getMessage());
                    }
                }
                if (outputList.size() > 0) {
                    c.output(buildOutputKV(gcsSourceData, outputList));
                }
            }
        }
    }
}