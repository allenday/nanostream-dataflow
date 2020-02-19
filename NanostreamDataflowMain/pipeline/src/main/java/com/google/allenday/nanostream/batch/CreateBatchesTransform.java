package com.google.allenday.nanostream.batch;

import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.allenday.genomics.core.model.SampleMetaData;
import com.google.allenday.genomics.core.processing.align.AlignService;
import com.google.allenday.nanostream.fastq.ParseFastQFn;
import com.google.allenday.nanostream.gcs.GetDataFromFastQFileFn;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import htsjdk.samtools.fastq.FastqRecord;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class CreateBatchesTransform extends PTransform<PCollection<KV<GCSSourceData, FileWrapper>>,
        PCollection<KV<SampleMetaData, List<FileWrapper>>>> {

    private GetDataFromFastQFileFn getDataFromFastQFileFn;
    private ParseFastQFn parseFastQFn;
    private SequenceBatchesToFastqFiles sequenceBatchesToFastqFiles;

    private int batchSize;
    private int maxBatchWindowSize;

    public CreateBatchesTransform(GetDataFromFastQFileFn getDataFromFastQFileFn, ParseFastQFn parseFastQFn,
                                  SequenceBatchesToFastqFiles sequenceBatchesToFastqFiles, int batchSize, int maxBatchWindowSize) {
        this.getDataFromFastQFileFn = getDataFromFastQFileFn;
        this.parseFastQFn = parseFastQFn;
        this.sequenceBatchesToFastqFiles = sequenceBatchesToFastqFiles;
        this.batchSize = batchSize;
        this.maxBatchWindowSize = maxBatchWindowSize;
    }

    @Override
    public PCollection<KV<SampleMetaData, List<FileWrapper>>> expand(PCollection<KV<GCSSourceData, FileWrapper>> input) {
        return input
                .apply(ParDo.of(getDataFromFastQFileFn))
                .apply(ParDo.of(parseFastQFn))
                .apply(Window.<KV<GCSSourceData, FastqRecord>>into(new GlobalWindows())
                        .triggering(Repeatedly
                                .forever(AfterFirst.of(
                                        AfterPane.elementCountAtLeast(batchSize),
                                        AfterProcessingTime.pastFirstElementInPane().plusDelayOf(Duration.standardSeconds(maxBatchWindowSize)))))
                        .withAllowedLateness(Duration.ZERO)
                        .discardingFiredPanes())
                .apply(GroupByKey.create())
                .apply(ParDo.of(sequenceBatchesToFastqFiles));
    }

    public static class SequenceBatchesToFastqFiles extends DoFn<KV<GCSSourceData, Iterable<FastqRecord>>, KV<SampleMetaData, List<FileWrapper>>> {

        private final static Logger LOG = LoggerFactory.getLogger(CreateBatchesTransform.class);
        private int batchSize;
        private AlignService.Instrument instrument;

        public SequenceBatchesToFastqFiles(int batchSize, AlignService.Instrument instrument) {
            this.batchSize = batchSize;
            this.instrument = instrument;
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            KV<GCSSourceData, Iterable<FastqRecord>> element = c.element();

            List<FastqRecord> collect = StreamSupport.stream(element.getValue().spliterator(), false).collect(Collectors.toList());
            LOG.info("Size of fastq batch before partition: {}", collect.size());

            List<List<FastqRecord>> choppedList = chopped(collect, batchSize);
            for (List<FastqRecord> list : choppedList) {
                StringBuilder fastqDataBuilder = new StringBuilder();
                for (FastqRecord fastqRecord : list) {
                    fastqDataBuilder.append(fastqRecord.toFastQString()).append("\n");
                }
                SampleMetaData metaData = SampleMetaData.createUnique(element.getKey().toJsonString(),
                        SampleMetaData.LibraryLayout.SINGLE.name(), instrument.name());
                c.output(KV.of(metaData, Collections.singletonList(FileWrapper.fromByteArrayContent(fastqDataBuilder.toString().getBytes(),
                        metaData.getRunId() + ".fastq"))));
            }
        }

        private <T> List<List<T>> chopped(List<T> list, final int batchSize) {
            List<List<T>> parts = new ArrayList<>();
            final int totalSize = list.size();
            for (int i = 0; i < totalSize; i += batchSize) {
                parts.add(new ArrayList<T>(list.subList(i, Math.min(totalSize, i + batchSize))));
            }
            return parts;
        }
    }
}