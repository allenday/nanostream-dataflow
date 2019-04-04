package com.google.allenday.nanostream.aligner;

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

import java.util.HashMap;
import java.util.Objects;

import static com.google.allenday.nanostream.other.Configuration.INTERLEAVED_DATA_FOLDER_NAME;
import static com.google.allenday.nanostream.other.Configuration.SAM_SHARDS_DATA_FOLDER_NAME;

/**
 * Makes alignment of fastq data via. Sends GCS path via PubSub and waits for existing result
 */
public class MakeAlignmentViaPubSubDoFn extends DoFn<KV<KV<CannabisSourceMetaData, String>, BlobId>,
        KV<KV<CannabisSourceMetaData, String>, BlobId>> {

    private Logger LOG = LoggerFactory.getLogger(MakeAlignmentViaPubSubDoFn.class);

    private String topicId;
    private String projectId;

    private PubSubService pubSubService;
    private GCSService gcloudService;

    @Setup
    public void setup() {
        pubSubService = PubSubService.initialize();
        gcloudService = GCSService.initialize();
    }

    public MakeAlignmentViaPubSubDoFn(String projectId,
                                      String topicId) {
        this.projectId = projectId;
        this.topicId = topicId;
    }


    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<KV<CannabisSourceMetaData, String>, BlobId> data = c.element();

        BlobId inputNameBlobId = data.getValue();
        String refDbName = Objects.requireNonNull(data.getKey()).getValue();
        BlobId outputNameBlobId = generateOutputName(inputNameBlobId, refDbName);

        try {
            HashMap<String, String> attributes = new HashMap<String, String>() {
                {
                    put("command", "minimap2 -as sr /data/" + refDbName + ".mmi ${input} | grep -v \'^@\'");
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
                    for (int i = 0; i < Configuration.ALIGNMENT_DELAY_MILISEC; i += Configuration.ALIGNMENT_CHECK_DELAY_DELTA_MILISEC) {
                        try {
                            Thread.sleep(Configuration.ALIGNMENT_CHECK_DELAY_DELTA_MILISEC);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                        if (gcloudService.isExists(outputNameBlobId)) {
                            c.output(KV.of(data.getKey(), outputNameBlobId));
                            break;
                        }
                    }
                    LOG.error(String.format("Aligner timeout %d ms exceeded. %s", Configuration.ALIGNMENT_DELAY_MILISEC, attributes.toString()));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private BlobId generateOutputName(BlobId inputNameBlobId, String refDbName) {
        String bucket = inputNameBlobId.getBucket();
        String name = inputNameBlobId.getName()
                .replace(INTERLEAVED_DATA_FOLDER_NAME + "/", SAM_SHARDS_DATA_FOLDER_NAME + "/" + refDbName + "/")
                .replace(".fastq", ".sam");

        return BlobId.of(bucket, name);
    }
}