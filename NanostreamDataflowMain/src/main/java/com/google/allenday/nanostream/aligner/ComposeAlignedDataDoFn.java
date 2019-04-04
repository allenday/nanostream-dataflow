package com.google.allenday.nanostream.aligner;

import com.google.allenday.nanostream.cannabis_source.CannabisSourceMetaData;
import com.google.allenday.nanostream.gcloud.GCSService;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import javax.annotation.Nonnull;
import java.util.Objects;

import static com.google.allenday.nanostream.other.Configuration.SAM_COLLECTED_DATA_FOLDER_NAME;

/**
 *
 */
public class ComposeAlignedDataDoFn extends DoFn<KV<KV<CannabisSourceMetaData, String>, Iterable<BlobId>>,
        KV<KV<CannabisSourceMetaData, String>, BlobId>> {

    private GCSService gcsService;
    private String resultBucket;
    private String srсBucket;
    private String samHeadersPath;

    public ComposeAlignedDataDoFn(String resultBucket, String srсBucket, String samHeadersPath) {
        this.resultBucket = resultBucket;
        this.srсBucket = srсBucket;
        this.samHeadersPath = samHeadersPath;
    }

    @Setup
    public void setup() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<KV<CannabisSourceMetaData, String>, Iterable<BlobId>> data = c.element();
        @Nonnull
        CannabisSourceMetaData cannabisSourceMetaData = Objects.requireNonNull(Objects.requireNonNull(data.getKey()).getKey());

        Iterable<BlobId> samShards = data.getValue();
        String refDb = Objects.requireNonNull(data.getKey()).getValue();
        BlobId destBlob = BlobId.of(resultBucket,
                SAM_COLLECTED_DATA_FOLDER_NAME
                        + "/"
                        + refDb
                        + "/"
                        + cannabisSourceMetaData.generateFolderAndNameForSampleAndReadGroup() + "_" + refDb + ".fastq");
        BlobId headersBlob = BlobId.of(srсBucket, String.format(samHeadersPath, refDb));
        Blob blob = gcsService.composeBlobs(samShards, headersBlob, destBlob);

        c.output(KV.of(data.getKey(), blob.getBlobId()));
    }
}