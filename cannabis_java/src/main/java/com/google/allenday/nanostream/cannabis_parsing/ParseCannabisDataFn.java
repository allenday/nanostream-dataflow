package com.google.allenday.nanostream.cannabis_parsing;

import com.google.allenday.genomics.core.gene.GeneData;
import com.google.allenday.genomics.core.gene.GeneExampleMetaData;
import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
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

public class ParseCannabisDataFn extends DoFn<String, KV<GeneExampleMetaData, List<GeneData>>> {

    private String srcBucket;
    private Logger LOG = LoggerFactory.getLogger(ParseCannabisDataFn.class);
    private GCSService gcsService;
    private FileUtils fileUtils;

    public ParseCannabisDataFn(String srcBucket, FileUtils fileUtils) {
        this.srcBucket = srcBucket;
        this.fileUtils = fileUtils;
    }

    @Setup
    public void setUp() {
        gcsService = GCSService.initialize(fileUtils);
    }

    private GeneExampleMetaData generateGeneExampleMetaDataFromCSVLine(String input) {
        String[] parts = input.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        return new GeneExampleMetaData(parts[0], parts[1], parts[2], parts[3], parts[4], input);
    }

    private boolean isPaired(String input) {
        String[] parts = input.split(",(?=([^\"]*\"[^\"]*\")*[^\"]*$)");
        return parts[6].toLowerCase().equals("paired");
    }

    private List<GeneData> generateGeneDataFromGeneExampleMetaData(GeneExampleMetaData geneExampleMetaData, boolean isPaired) {
        String uriPrefix;
        if (geneExampleMetaData.getProject().toLowerCase().equals("Kannapedia".toLowerCase())) {
            uriPrefix = String.format("gs://%s/kannapedia/", srcBucket);
        } else {
            uriPrefix = String.format("gs://%s/sra/%s/%s/", srcBucket, geneExampleMetaData.getProjectId(),
                    geneExampleMetaData.getSraSample());
        }
        String fileNameForward = geneExampleMetaData.getRun() + "_1.fastq";
        List<GeneData> geneDataList =
                new ArrayList<>(Collections.singletonList(GeneData.fromBlobUri(uriPrefix + fileNameForward, fileNameForward)));
        if (isPaired) {
            String fileNameBack = geneExampleMetaData.getRun() + "_2.fastq";
            geneDataList.add(GeneData.fromBlobUri(uriPrefix + fileNameBack, fileNameBack));
        }
        return geneDataList;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        String input = c.element();
        LOG.info(String.format("Parse %s", input));
        try {
            GeneExampleMetaData geneExampleMetaData = generateGeneExampleMetaDataFromCSVLine(input);

            boolean isPaired = isPaired(input);
            List<GeneData> originalGeneDataList = generateGeneDataFromGeneExampleMetaData(geneExampleMetaData, isPaired);
            List<GeneData> checkedGeneDataList = new ArrayList<>();

            if (originalGeneDataList.size() > 0) {
                BlobId blobId = gcsService.getBlobIdFromUri(originalGeneDataList.get(0).getBlobUri());
                String dirPrefix = blobId.getName().replace(originalGeneDataList.get(0).getFileName(), "");

                boolean hasAnomalyBlobs = false;
                List<Blob> blobs = StreamSupport.stream(gcsService.getListOfBlobsInDir(srcBucket, dirPrefix).iterateAll()
                        .spliterator(), false).collect(Collectors.toList());
                if (!dirPrefix.contains("kannapedia")) {
                    Long reduce = blobs.stream().map(BlobInfo::getSize).reduce((long) 0, Long::sum);
                    if ((reduce / (1024 * 1024) > 10000)) {
                        hasAnomalyBlobs = true;
                        geneExampleMetaData.setComment(String.format("Read group to large (%d MB)", reduce / (1024 * 1024)));
                    }
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
                    for (GeneData geneData : originalGeneDataList) {
                        boolean exists = gcsService.isExists(gcsService.getBlobIdFromUri(geneData.getBlobUri()));
                        if (!exists) {
                            LOG.info(String.format("Blob %s doesn't exist. Trying to find blob with other SRA for %s", geneData.getBlobUri(), geneExampleMetaData.toString()));
                            Optional<Blob> blobOpt = blobs.stream().filter(blob -> {

                                String searchIndex = geneData.getFileName().split("\\.")[0].split("_")[1];

                                boolean containsIndex = blob.getName().contains(String.format("_%s", searchIndex));
                                if (blobs.size() == 2) {
                                    return containsIndex;
                                } else {
                                    int runInt = Integer.parseInt(blob.getName()
                                            .substring(blob.getName().lastIndexOf('/') + 1).split("_")[0]
                                            .substring(3));
                                    int serchedRunInt = Integer.parseInt(geneExampleMetaData.getRun().substring(3));
                                    return Math.abs(serchedRunInt - runInt) == 1 && containsIndex;
                                }
                            }).findFirst();
                            if (blobOpt.isPresent()) {
                                LOG.info(String.format("Found: %s", blobOpt.get().getName()));
                                String fileUri = String.format("gs://%s/%s", blobOpt.get().getBucket(), blobOpt.get().getName());
                                String fileName = fileUtils.getFilenameFromPath(fileUri);
                                checkedGeneDataList.add(GeneData.fromBlobUri(fileUri, fileName));
                            } else {
                                logAnomaly(blobs, geneExampleMetaData);
                                geneExampleMetaData.setComment("File not found");
                            }
                        } else {
                            checkedGeneDataList.add(geneData);
                        }
                    }
                }

                if (originalGeneDataList.size() == checkedGeneDataList.size()) {
                    c.output(KV.of(geneExampleMetaData, checkedGeneDataList));
                } else {
                    c.output(KV.of(geneExampleMetaData, Collections.emptyList()));
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void logAnomaly(List<Blob> blobs, GeneExampleMetaData geneExampleMetaData) {
        LOG.info(String.format("Anomaly: %s, %s, %s, blobs %s",
                geneExampleMetaData.getProject(),
                geneExampleMetaData.getSraSample(),
                geneExampleMetaData.getRun(),
                blobs.stream().map(BlobInfo::getName).collect(Collectors.joining(", "))));
    }
}
