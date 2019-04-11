package com.google.allenday.nanostream.samtools;

import com.google.allenday.nanostream.cannabis_source.CannabisSourceMetaData;
import com.google.allenday.nanostream.gcloud.GCSService;
import com.google.allenday.nanostream.gcloud.PubSubService;
import com.google.allenday.nanostream.other.Configuration;
import com.google.allenday.nanostream.pubsub.GCSUtils;
import com.google.api.core.ApiFutureCallback;
import com.google.cloud.storage.BlobId;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Objects;

import static com.google.allenday.nanostream.other.Configuration.SAMTOOLS_CHECK_DELAY_DELTA_MILISEC;
import static com.google.allenday.nanostream.other.Configuration.SAMTOOLS_DELAY_MILISEC;

/**
 *
 */
public class SamtoolsViaPubSubDoFn extends DoFn<KV<KV<CannabisSourceMetaData, String>, BlobId>,
        KV<KV<CannabisSourceMetaData, String>, BlobId>> {

    private Logger LOG = LoggerFactory.getLogger(SamtoolsViaPubSubDoFn.class);

    private String topicId;
    private String projectId;

    private PubSubService pubSubService;
    private GCSService gcloudService;
    private String resultBucket;
    private String command;
    private String resultFolder;


    @Setup
    public void setup() {
        pubSubService = PubSubService.initialize();
        gcloudService = GCSService.initialize();
    }

    public SamtoolsViaPubSubDoFn(String projectId,
                                 String topicId,
                                 String resultBucket,
                                 String command,
                                 String resultFolder) {
        this.projectId = projectId;
        this.topicId = topicId;
        this.resultBucket = resultBucket;
        this.command = command;
        this.resultFolder = resultFolder;
    }


    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<KV<CannabisSourceMetaData, String>, BlobId> data = c.element();
        @Nonnull
        CannabisSourceMetaData cannabisSourceMetaData = Objects.requireNonNull(Objects.requireNonNull(data.getKey()).getKey());

        BlobId inputNameBlobId = data.getValue();
        String refDb = Objects.requireNonNull(data.getKey()).getValue();
        BlobId outputNameBlobId = BlobId.of(resultBucket,
                resultFolder
                        + "/"
                        + refDb
                        + "/"
                        + cannabisSourceMetaData.generateFolderAndNameForSampleAndReadGroup() + "_" + refDb + ".fastq");

        try {
            HashMap<String, String> attributes = new HashMap<String, String>() {
                {
                    put("command", command);
                    put("input", GCSUtils.getGCSPathFromBlobId(inputNameBlobId));
                    put("output", GCSUtils.getGCSPathFromBlobId(outputNameBlobId));
                }
            };

            pubSubService.publishMessage(projectId, topicId, attributes, new ApiFutureCallback<String>() {
                @Override
                public void onFailure(Throwable throwable) {
                    LOG.error(throwable.getMessage());
                }

                @Override
                public void onSuccess(String s) {
                    for (int i = 0; i < SAMTOOLS_DELAY_MILISEC; i += SAMTOOLS_CHECK_DELAY_DELTA_MILISEC) {
                        try {
                            Thread.sleep(Configuration.SAMTOOLS_CHECK_DELAY_DELTA_MILISEC);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (gcloudService.isExists(outputNameBlobId)) {
                            c.output(KV.of(data.getKey(), outputNameBlobId));
                            break;
                        }
                    }
                    LOG.error(String.format("Samtools timeout %d ms exceeded. %s", Configuration.ALIGNMENT_DELAY_MILISEC, attributes.toString()));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}