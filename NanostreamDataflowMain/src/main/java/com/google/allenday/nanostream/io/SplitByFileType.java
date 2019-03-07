package com.google.allenday.nanostream.io;

import com.google.allenday.nanostream.pubsub.GCSSourceData;
import org.apache.beam.sdk.transforms.Partition;
import org.apache.beam.sdk.values.KV;

/**
 * Naively splits by file extension. By default expects FastQ files
 */
public class SplitByFileType implements Partition.PartitionFn<KV<GCSSourceData, byte[]>> {

    public static final int FASTQ_FILE = 0;
    public static final int SAM_FILE = 1;
    public static final int BAM_FILE = 2;
    public static final int NUMBER_OF_PARTITIONS = 3;

    @Override
    public int partitionFor(KV<GCSSourceData, byte[]> file, int numPartitions) {
        GCSSourceData gcsSourceData = file.getKey();
        String filenameLower = gcsSourceData.getFilename().toLowerCase();

        if (filenameLower.endsWith(".bam")) {
            return BAM_FILE;
        }

        if (filenameLower.endsWith(".sam")) {
            return SAM_FILE;
        }

        return FASTQ_FILE;
    }
}
