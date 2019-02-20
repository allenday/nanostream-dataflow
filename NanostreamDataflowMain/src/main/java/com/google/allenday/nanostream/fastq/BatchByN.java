package com.google.allenday.nanostream.fastq;

import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupIntoBatches;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollection;

import java.util.Random;

public class BatchByN extends PTransform<PCollection<FastqRecord>, PCollection<Iterable<FastqRecord>>> {
    private static final int DEFAULT_SHARDS_NUMBER = 1;
    private int batchSize;

    public BatchByN(int batchSize) {
        super();
        this.batchSize = batchSize;
    }

    @Override
    public PCollection<Iterable<FastqRecord>> expand(PCollection<FastqRecord> input) {
        return input
                // assign keys, as "GroupIntoBatches" works only with key-value pairs
                .apply(ParDo.of(new AssignRandomKeys(DEFAULT_SHARDS_NUMBER)))
                .apply(GroupIntoBatches.ofSize(batchSize))
                .apply(ParDo.of(new ExtractValues()));
    }

    /**
     * Assigns to elements random integer between zero and shardsNumber
     */
    private static class AssignRandomKeys extends DoFn<FastqRecord, KV<Integer, FastqRecord>> {
        private int shardsNumber;
        private Random random;

        AssignRandomKeys(int shardsNumber) {
            super();
            this.shardsNumber = shardsNumber;
        }

        @Setup
        public void setup() {
            random = new Random();
        }

        @ProcessElement
        public void processElement(ProcessContext c) {
            FastqRecord fastqRecord = c.element();
            KV<Integer, FastqRecord> kv = KV.of(random.nextInt(shardsNumber), fastqRecord);
            c.output(kv);
        }
    }

    /**
     * Extract values from KV
     */
    private static class ExtractValues extends DoFn<KV<Integer, Iterable<FastqRecord>>, Iterable<FastqRecord>> {
        @ProcessElement
        public void processElement(ProcessContext c) {
            KV<Integer, Iterable<FastqRecord>> kv = c.element();
            c.output(kv.getValue());
        }
    }
}
