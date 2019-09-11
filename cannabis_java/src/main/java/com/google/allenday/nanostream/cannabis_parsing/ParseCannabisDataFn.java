package com.google.allenday.nanostream.cannabis_parsing;

import com.google.allenday.nanostream.gcloud.GCSService;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class ParseCannabisDataFn extends DoFn<String, KV<CannabisSourceMetaData, List<String>>> {

    private String srcBucket;
    private Logger LOG = LoggerFactory.getLogger(ParseCannabisDataFn.class);
    private GCSService gcsService;

    public ParseCannabisDataFn(String srcBucket) {
        this.srcBucket = srcBucket;
    }

    @Setup
    public void setUp() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        String input = c.element();
        LOG.info(String.format("Parse %s", input));
        try {
            List<CannabisSourceFileMetaData> cannabisSourceFileMetaData = CannabisSourceFileMetaData.fromCSVLine(input);
            List<String> fileNames = new ArrayList<>();

            cannabisSourceFileMetaData.stream().findFirst()
                    .map(CannabisSourceFileMetaData::getCannabisSourceMetaData).ifPresent(metaData -> {

                String dirPrefix = metaData.getDirPrefix();
                List<Blob> blobs = StreamSupport.stream(gcsService.getListOfBlobsInDir(srcBucket, dirPrefix).iterateAll()
                        .spliterator(), false).collect(Collectors.toList());

                boolean hasAnomalyBlobs = blobs.stream().map(b -> {
                    String[] parts = b.getName().split("_");
                    return Integer.parseInt(parts[parts.length - 1].split("\\.")[0]);
                }).anyMatch(i -> i > 2);
                if (hasAnomalyBlobs) {
                    logAnomaly(blobs, metaData);
                } else {
                    for (CannabisSourceFileMetaData fileMetaData : cannabisSourceFileMetaData) {
                        boolean exists = gcsService.isExists(BlobId.of(srcBucket, fileMetaData.generateGCSBlobName()));
                        if (exists) {
                            fileNames.add(fileMetaData.generateGCSBlobName());
                        } else {
                            Optional<Blob> blobOpt = blobs.stream().filter(blob -> {
                                boolean containsIndex = blob.getName()
                                        .contains(String.format("_%s", fileMetaData.getPairedIndex()));
                                if (blobs.size() == 2) {
                                    return containsIndex;
                                } else {
                                    int runInt = Integer.parseInt(blob.getName()
                                            .substring(blob.getName().lastIndexOf('/') + 1).split("_")[0]
                                            .substring(3));
                                    int serchedRunInt = Integer.parseInt(fileMetaData.getCannabisSourceMetaData().getRun().substring(3));
                                    return Math.abs(serchedRunInt - runInt) == 1 && containsIndex;
                                }
                            })
                                    .findFirst();
                            if (blobOpt.isPresent()) {
                                fileNames.add(blobOpt.get().getName());
                            } else {
                                logAnomaly(blobs, metaData);
                            }
                        }
                    }
                }
                if (fileNames.size() == cannabisSourceFileMetaData.size()) {
                    c.output(KV.of(metaData, fileNames));
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logAnomaly(List<Blob> blobs, CannabisSourceMetaData metaData) {
        LOG.info(String.format("Anomaly: %s, %s, %s, blobs %s",
                metaData.getProject(),
                metaData.getSraSample(),
                metaData.getRun(),
                blobs.stream().map(BlobInfo::getName).collect(Collectors.joining(", "))));
    }
}
