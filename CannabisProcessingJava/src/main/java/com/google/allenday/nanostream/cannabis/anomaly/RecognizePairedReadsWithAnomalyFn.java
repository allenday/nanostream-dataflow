package com.google.allenday.nanostream.cannabis.anomaly;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.model.FileWrapper;
import com.google.allenday.genomics.core.model.SampleMetaData;
import com.google.cloud.storage.Blob;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class RecognizePairedReadsWithAnomalyFn extends DoFn<KV<SampleMetaData, List<FileWrapper>>, KV<SampleMetaData, List<FileWrapper>>> {

    private Logger LOG = LoggerFactory.getLogger(RecognizePairedReadsWithAnomalyFn.class);

    private String srcBucket;
    private GCSService gcsService;
    private FileUtils fileUtils;

    public RecognizePairedReadsWithAnomalyFn(String srcBucket, FileUtils fileUtils) {
        this.srcBucket = srcBucket;
        this.fileUtils = fileUtils;
    }

    @Setup
    public void setUp() {
        gcsService = GCSService.initialize(fileUtils);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<SampleMetaData, List<FileWrapper>> input = c.element();
        LOG.info(String.format("RecognizePairedReadsWithAnomalyFn %s", input.toString()));

        List<FileWrapper> originalGeneDataList = input.getValue();
        SampleMetaData geneExampleMetaData = input.getKey();
        try {
            List<FileWrapper> checkedGeneDataList = new ArrayList<>();
            if (originalGeneDataList.size() > 0) {
                BlobId blobId = gcsService.getBlobIdFromUri(originalGeneDataList.get(0).getBlobUri());
                String dirPrefix = blobId.getName().replace(originalGeneDataList.get(0).getFileName(), "");

                boolean hasAnomalyBlobs = false;
                List<Blob> blobs = StreamSupport.stream(gcsService.getListOfBlobsInDir(srcBucket, dirPrefix).iterateAll()
                        .spliterator(), false).collect(Collectors.toList());
                if (!dirPrefix.contains("kannapedia")) {
                    if (blobs.stream().map(b -> {
                        String[] parts = b.getName().split("_");
                        return Integer.parseInt(parts[parts.length - 1].split("\\.")[0]);
                    }).anyMatch(i -> i > 2)) {
                        hasAnomalyBlobs = true;
                        geneExampleMetaData.setComment("There are to much reads in dir");
                    }
                }
                if (hasAnomalyBlobs) {
                    logAnomaly(blobs, geneExampleMetaData);
                } else {
                    for (FileWrapper fileWrapper : originalGeneDataList) {
                        boolean exists = gcsService.isExists(gcsService.getBlobIdFromUri(fileWrapper.getBlobUri()));
                        if (!exists) {
                            LOG.info(String.format("Blob %s doesn't exist. Trying to find blob with other SRA for %s", fileWrapper.getBlobUri(), geneExampleMetaData.toString()));
                            Optional<Blob> blobOpt = blobs.stream().filter(blob -> {

                                String searchIndex = fileWrapper.getFileName().split("\\.")[0].split("_")[1];

                                boolean containsIndex = blob.getName().contains(String.format("_%s", searchIndex));
                                if (blobs.size() == 2) {
                                    return containsIndex;
                                } else {
                                    int runInt = Integer.parseInt(blob.getName()
                                            .substring(blob.getName().lastIndexOf('/') + 1).split("_")[0]
                                            .substring(3));
                                    int serchedRunInt = Integer.parseInt(geneExampleMetaData.getRunId().substring(3));
                                    return Math.abs(serchedRunInt - runInt) == 1 && containsIndex;
                                }
                            }).findFirst();
                            if (blobOpt.isPresent()) {
                                LOG.info(String.format("Found: %s", blobOpt.get().getName()));
                                String fileUri = String.format("gs://%s/%s", blobOpt.get().getBucket(), blobOpt.get().getName());
                                String fileName = fileUtils.getFilenameFromPath(fileUri);
                                checkedGeneDataList.add(FileWrapper.fromBlobUri(fileUri, fileName));
                            } else {
                                logAnomaly(blobs, geneExampleMetaData);
                                geneExampleMetaData.setComment("File not found");
                            }
                        } else {
                            checkedGeneDataList.add(fileWrapper);
                        }
                    }
                }

                if (originalGeneDataList.size() == checkedGeneDataList.size()) {
                    c.output(KV.of(geneExampleMetaData, checkedGeneDataList));
                } else {
                    geneExampleMetaData.setComment(geneExampleMetaData.getComment() + String.format(" (%d/%d)", checkedGeneDataList.size(), originalGeneDataList.size()));
                    c.output(KV.of(geneExampleMetaData, Collections.emptyList()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logAnomaly(List<Blob> blobs, SampleMetaData geneExampleMetaData) {
        LOG.info(String.format("Anomaly: %s, %s, %s, blobs %s",
                geneExampleMetaData.getCenterName(),
                geneExampleMetaData.getSraSample(),
                geneExampleMetaData.getRunId(),
                geneExampleMetaData.getCenterName().toLowerCase().contains("kannapedia") ? "" : blobs.stream().map(BlobInfo::getName).collect(Collectors.joining(", "))));
    }
}
