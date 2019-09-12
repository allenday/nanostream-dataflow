package com.google.allenday.nanostream.transforms;

import com.google.allenday.nanostream.cannabis_parsing.CannabisSourceMetaData;
import com.google.allenday.nanostream.gcloud.GCSService;
import com.google.allenday.nanostream.utils.BamFilesMerger;
import com.google.allenday.nanostream.utils.FileUtils;
import com.google.cloud.storage.Blob;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MergeBamQFn extends DoFn<KV<KV<String, String>, Iterable<KV<CannabisSourceMetaData, KV<String, String>>>>,
        KV<CannabisSourceMetaData, KV<String, String>>> {

    private Logger LOG = LoggerFactory.getLogger(MergeBamQFn.class);

    private GCSService gcsService;
    private String resultsBucket;
    private BamFilesMerger bamFilesMerger;
    private String jobTime;

    public final static String RESULT_SORTED_MERGED_DIR_NAME_PATTERN = "cannabis_processing_output/%s/result_sorted_merged_bam/";
    private final static String MERGE_SORTED_FILE_PREFIX = ".merged.sorted.bam";

    public MergeBamQFn(String resultsBucket, BamFilesMerger bamFilesMerger, String jobTime) {
        this.resultsBucket = resultsBucket;
        this.bamFilesMerger = bamFilesMerger;
        this.jobTime = jobTime;
    }

    @Setup
    public void setUp() throws Exception {
        gcsService = GCSService.initialize();
    }

    private boolean isNeedToMerge(List<KV<CannabisSourceMetaData, KV<String, String>>> bamListStream) {
        return bamListStream.size() > 1;
    }

    private List<String> donloadBamFiles(List<KV<CannabisSourceMetaData, KV<String, String>>> bamListStream) {
        List<String> localBamPaths = new ArrayList<>();
        bamListStream.stream().map(KV::getValue).forEach(paths -> {
            Blob blob = gcsService.getBlob(paths.getKey(), paths.getValue());
            String localPath = "/bam_src/" + blob.getName();
            if (Files.exists(Paths.get(localPath))) {
                throw new RuntimeException(String.format("Duplication of %s", localPath));
            }
            gcsService.downloadBlobTo(blob, localPath);
            localBamPaths.add(localPath);
        });
        return localBamPaths;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        LOG.info(String.format("MergeDoFnInput: %s", c.element().toString()));

        KV<KV<String, String>, Iterable<KV<CannabisSourceMetaData, KV<String, String>>>> element = c.element();
        List<KV<CannabisSourceMetaData, KV<String, String>>> bamList = StreamSupport.stream(element.getValue().spliterator(), false)
                .collect(Collectors.toList());
        String reference = element.getKey().getValue();
        bamList.stream().findFirst().ifPresent(kv -> {
            KV<String, String> srcFileData = kv.getValue();
            Optional<CannabisSourceMetaData> metaDataOpt = Optional.ofNullable(kv.getKey());

            metaDataOpt.ifPresent(metaData -> {
                String destFileName = String.format(RESULT_SORTED_MERGED_DIR_NAME_PATTERN, jobTime) + metaData.getSraSample() + "_" + reference + MERGE_SORTED_FILE_PREFIX;
                KV<String, String> destData = KV.of(resultsBucket, destFileName);

                String filesDirPath;
                if (!isNeedToMerge(bamList)) {
                    gcsService.copy(srcFileData.getKey(), srcFileData.getValue(), destData.getKey(), destData.getValue());
                    c.output(KV.of(metaData, destData));
                } else {
                    LOG.info("To merge: " + bamList.stream().map(el -> Objects.requireNonNull(el.getKey()).getDirPrefix())
                            .collect(Collectors.joining(". ")));

                    filesDirPath = '/' + System.currentTimeMillis() + "_" + metaData.getSraSample() + "/";
                    FileUtils.mkdir(filesDirPath);

                    String filePrefix = filesDirPath + metaData.getSraSample();

                    String outputPath = filePrefix + MERGE_SORTED_FILE_PREFIX;

                    List<String> localBamPaths = donloadBamFiles(bamList);
                    bamFilesMerger.merge(localBamPaths.stream().map(el -> Paths.get(el)).collect(Collectors.toList()), outputPath);
                    try {
                        gcsService.saveToGcs(resultsBucket, destData.getValue(), Files.readAllBytes(Paths.get(outputPath)));

                        c.output(KV.of(metaData, destData));
                    } catch (IOException e) {
                        e.printStackTrace();
                    } finally {
                        FileUtils.deleteDir(filesDirPath);
                    }
                }
            });

        });

    }
}
