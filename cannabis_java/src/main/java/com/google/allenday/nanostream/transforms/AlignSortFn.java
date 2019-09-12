package com.google.allenday.nanostream.transforms;

import com.google.allenday.nanostream.NanostreamCannabisApp;
import com.google.allenday.nanostream.cannabis_parsing.CannabisSourceMetaData;
import com.google.allenday.nanostream.cmd.CmdExecutor;
import com.google.allenday.nanostream.cmd.WorkerSetupService;
import com.google.allenday.nanostream.gcloud.GCSService;
import com.google.allenday.nanostream.utils.FileUtils;
import com.google.cloud.storage.Blob;
import htsjdk.samtools.*;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class AlignSortFn extends DoFn<KV<CannabisSourceMetaData, List<String>>, KV<KV<String, String>, KV<CannabisSourceMetaData, KV<String, String>>>> {

    private Logger LOG = LoggerFactory.getLogger(NanostreamCannabisApp.class);
    private GCSService gcsService;

    private CmdExecutor cmdExecutor;
    private WorkerSetupService workerSetupService;
    private String srcBucket;
    private String resultsBucket;
    private List<String> referenceNames;
    private String jobTime;

    public AlignSortFn(CmdExecutor cmdExecutor,
                       WorkerSetupService workerSetupService,
                       String srcBucket,
                       String resultsBucket,
                       List<String> referenceNames,
                       String jobTime) {
        this.cmdExecutor = cmdExecutor;
        this.workerSetupService = workerSetupService;
        this.srcBucket = srcBucket;
        this.resultsBucket = resultsBucket;
        this.referenceNames = referenceNames;
        this.jobTime = jobTime;
    }

    @Setup
    public void setUp() throws Exception {
        gcsService = GCSService.initialize();
        workerSetupService.setupMinimap2();
    }

    private final static String RESULT_SORTED_DIR_NAME_PATTERN = "cannabis_processing_output/%s/result_sorted_bam/";
    private final static String GCS_REFERENCE_DIR = "reference/";
    private final static String ALIGN_COMMAND_PATTERN = "./minimap2-2.17_x64-linux/minimap2" +
            " -ax sr %s %s" +
            " -R '@RG\tID:minimap2\tPL:ILLUMINA\tPU:NONE\tSM:RSP11055' " +
            "> %s";
    private final static String SORTED_BAM_FILE_PREFIX = ".sorted.bam";
    private final static String SAM_FILE_PREFIX = ".sam";

    private void downloadReferencesIfNeeded() {
        gcsService.getAllBlobsIn(srcBucket, GCS_REFERENCE_DIR)
                .stream()
                .filter(blob -> referenceNames.stream().anyMatch(ref -> blob.getName().contains(ref)))
                .forEach(blob -> {
                    String filePath = "/" + blob.getName();
                    if (!Files.exists(Paths.get(filePath))) {
                        gcsService.downloadBlobTo(blob, filePath);
                    } else {
                        LOG.info(String.format("Reference %s already exists", blob.getName()));
                    }
                });
    }

    private List<String> donloadFastqFiles(List<String> fastqPaths) {
        List<String> localFastqPaths = new ArrayList<>();

        fastqPaths.forEach(paths -> {
            Blob blob = gcsService.getBlob("cannabis-3k", paths);
            String localPath = "/fastq_src/" + blob.getName();
            if (Files.exists(Paths.get(localPath))) {
                throw new RuntimeException(String.format("Duplication of %s", localPath));
            }
            gcsService.downloadBlobTo(blob, localPath);
            localFastqPaths.add(localPath);
        });
        return localFastqPaths;
    }

    private String alignFastq(List<String> localFastqPaths, String filePrefix, String reference) {
        String alignedSamPath = filePrefix + "_" + reference + SAM_FILE_PREFIX;

        String referencePath = String.format("reference/%s/%s.fa", reference, reference);
        String minimapCommand = String.format(ALIGN_COMMAND_PATTERN, referencePath,
                String.join(" ", localFastqPaths), alignedSamPath);

        boolean success = cmdExecutor.executeCommand(minimapCommand);
        if (!success) {
            throw new RuntimeException("Align command failed");
        }
        cmdExecutor.executeCommand("free -m");
        /* Total amount of free memory available to the JVM */
        LOG.info("Free memory (bytes): " +
                Runtime.getRuntime().freeMemory());

        /* This will return Long.MAX_VALUE if there is no preset limit */
        long maxMemory = Runtime.getRuntime().maxMemory();
        /* Maximum amount of memory the JVM will attempt to use */
        LOG.info("Maximum memory (bytes): " +
                (maxMemory == Long.MAX_VALUE ? "no limit" : maxMemory));

        /* Total memory currently in use by the JVM */
        LOG.info("Total memory (bytes): " +
                Runtime.getRuntime().totalMemory());
        return alignedSamPath;
    }


    private String sortFastq(String alignedSamPath, String filePrefix, String reference) throws IOException {
        String alignedSortedBamPath = filePrefix + "_" + reference + SORTED_BAM_FILE_PREFIX;
        final SamReader reader = SamReaderFactory.makeDefault().open(new File(alignedSamPath));
        reader.getFileHeader().setSortOrder(SAMFileHeader.SortOrder.coordinate);

        SAMFileWriter samFileWriter = new SAMFileWriterFactory()
                .makeBAMWriter(reader.getFileHeader(), false, new File(alignedSortedBamPath));

        for (SAMRecord record : reader) {
            samFileWriter.addAlignment(record);
        }
        samFileWriter.close();
        reader.close();
        FileUtils.deleteFile(alignedSamPath);

        cmdExecutor.executeCommand("ls " + alignedSortedBamPath);
        return alignedSortedBamPath;
    }

    private Blob uploadResultToGcs(String alignedSortedBamPath) throws IOException {
        String gcsFileName = String.format(RESULT_SORTED_DIR_NAME_PATTERN, jobTime) +
                alignedSortedBamPath.split("/")[alignedSortedBamPath.split("/").length - 1];
        return gcsService.saveToGcs(resultsBucket, gcsFileName,
                Files.readAllBytes(Paths.get(alignedSortedBamPath)));
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        LOG.info(String.format("Start of align -> view -> sort with input: %s", c.element().toString()));
        downloadReferencesIfNeeded();

        KV<CannabisSourceMetaData, List<String>> data = c.element();

        if (data.getValue().size() > 0) {

            CannabisSourceMetaData cannabisSourceMetaData = data.getKey();
            try {
                List<String> localFastqPaths = donloadFastqFiles(data.getValue());

                String run = cannabisSourceMetaData.getRun();
                String filesDirPath = '/' + System.currentTimeMillis() + "_" + run + "/";
                FileUtils.mkdir(filesDirPath);

                String filePrefix = filesDirPath + run;

                for (String reference : referenceNames) {
                    long startMainPart = System.currentTimeMillis();

                    String alignedSamPath = alignFastq(localFastqPaths, filePrefix, reference);
                    String alignedSortedBamPath = sortFastq(alignedSamPath, filePrefix, reference);
                    Blob blob = uploadResultToGcs(alignedSortedBamPath);
                    FileUtils.deleteFile(alignedSortedBamPath);

                    c.output(KV.of(KV.of(cannabisSourceMetaData.getSraSample(), reference), KV.of(cannabisSourceMetaData,
                            KV.of(blob.getBucket(), blob.getName()))));
                    LOG.info(String.format("Finished align-sort of %s with reference %s in %d s",
                            String.join(" ", localFastqPaths), reference, (System.currentTimeMillis() - startMainPart) / 1000));
                }
                FileUtils.deleteDir(filesDirPath);

            } catch (Exception e) {
                LOG.error(e.getMessage());
            }
        }

    }
}
