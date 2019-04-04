package com.google.allenday.nanostream.aligner;

import com.google.allenday.nanostream.cannabis_source.CannabisSourceMetaData;
import com.google.allenday.nanostream.gcloud.GCSService;
import com.google.allenday.nanostream.other.Configuration;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import htsjdk.samtools.fastq.FastqRecord;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.stream.Stream;

import static com.google.allenday.nanostream.other.Configuration.INTERLEAVED_DATA_FOLDER_NAME;

/**
 * Prepares interleaved FastQ data and saves it to Google Cloud Storage
 */
public class SaveInterleavedFastQDataToGCSDoFn extends DoFn<KV<CannabisSourceMetaData, Iterable<Iterable<KV<FastqRecord, Integer>>>>,
        KV<KV<CannabisSourceMetaData, String>, BlobId>> {

    private GCSService gcsService;
    private String resultBucket;

    public SaveInterleavedFastQDataToGCSDoFn(String resultBucket) {
        this.resultBucket = resultBucket;
    }

    @Setup
    public void setup() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<CannabisSourceMetaData, Iterable<Iterable<KV<FastqRecord, Integer>>>> data = c.element();

        @Nonnull
        CannabisSourceMetaData cannabisSourceMetaData = Objects.requireNonNull(data.getKey());

        String fastQData = prepareFastQData(data.getValue());
        Blob blob = gcsService.saveToGcs(resultBucket,
                INTERLEAVED_DATA_FOLDER_NAME + "/" + cannabisSourceMetaData.generateUniqueFolderAndNameForSampleAndReadGroup() + ".fastq",
                fastQData.getBytes());

        Stream.of(Configuration.REF_DB_ARRAY).forEach(redDb -> c.output(KV.of(KV.of(cannabisSourceMetaData, redDb), blob.getBlobId())));
    }

    private String prepareFastQData(Iterable<Iterable<KV<FastqRecord, Integer>>> data) {
        StringBuilder fastqPostBody = new StringBuilder();
        for (Iterable<KV<FastqRecord, Integer>> pair : data) {
            for (KV<FastqRecord, Integer> element : pair) {
                fastqPostBody.append(Objects.requireNonNull(element.getKey()).toFastQString()).append("\n");
            }
        }
        return fastqPostBody.toString();
    }
}