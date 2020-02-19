package com.google.allenday.nanostream.launcher.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

import static com.google.allenday.nanostream.launcher.util.AssertUtil.assertNotEmpty;
import static com.google.allenday.nanostream.launcher.util.DateTimeUtil.makeTimestamp;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;
import static java.lang.String.format;


@Service
public class PipelineOptionsSaver {

    private static final Logger logger = LoggerFactory.getLogger(PipelineOptionsSaver.class);

    private final JobLauncher jobLauncher;

    private String project;
    private Firestore db;

    @Autowired
    public PipelineOptionsSaver(JobLauncher jobLauncher) {
        this.jobLauncher = jobLauncher;

        project = getProjectId();
        db = initFirestoreConnection();
    }

    public PipelineEntity create(PipelineRequestParams pipelineRequestParams) throws IOException, ExecutionException, InterruptedException {
        PipelineEntity pipelineEntity = createOptionsForNewPipeline(pipelineRequestParams);
        savePipelineConfiguration(pipelineEntity);
        launchJobIfRequired(pipelineEntity);
        return pipelineEntity;
    }

    private PipelineEntity createOptionsForNewPipeline(PipelineRequestParams pipelineRequestParams) {
        assertNotEmpty(pipelineRequestParams.getPipelineName(), "Empty pipeline name not allowed");
        assertNotEmpty(pipelineRequestParams.getInputFolder(), "Empty input folder not allowed");
        assertNotEmpty(pipelineRequestParams.getUploadBucketName(), "Empty upload bucket name not allowed");
        assertNotEmpty(pipelineRequestParams.getReferenceNameList(), "Empty reference name list not allowed");
        assertNotEmpty(pipelineRequestParams.getProcessingMode(), "Empty processing mode not allowed");
        assertNotEmpty(pipelineRequestParams.getInputDataSubscription(), "Empty input data subscription not allowed");

        PipelineEntity pipelineEntity = new PipelineEntity(pipelineRequestParams);
        String now = Instant.now().toString();
        pipelineEntity.setId(makePipelineId());
        pipelineEntity.setOutputCollectionNamePrefix(makeOutputCollectionNamePrefix(pipelineRequestParams.getPipelineName()));
        pipelineEntity.setCreatedAt(now);
        pipelineEntity.setUpdatedAt(now);


        return pipelineEntity;
    }

    private String makePipelineId() {
        return "pl-" + makeTimestamp() + "-" + UUID.randomUUID().toString();
    }

    private String makeOutputCollectionNamePrefix(String pipelineName) {
        String prefix = pipelineName.replaceAll("[^\\w]+", "_").toLowerCase();
        return format("%s_%s", prefix, makeTimestamp());
    }


    private PipelineEntity createOptionsForExistingPipeline(PipelineRequestParams pipelineRequestParams) {
        PipelineEntity pipelineEntity = new PipelineEntity(pipelineRequestParams);
        String now = Instant.now().toString();
        pipelineEntity.setUpdatedAt(now);
        return pipelineEntity;
    }

    private void savePipelineConfiguration(PipelineEntity pipelineEntity) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> future = db.collection(FIRESTORE_PIPELINES_COLLECTION).document(pipelineEntity.getId()).set(pipelineEntity);
        WriteResult writeResult = future.get();
        logger.info("Pipeline '{}' saved to  firestore at {}", pipelineEntity.getPipelineName(), writeResult.getUpdateTime());
    }

    private void launchJobIfRequired(PipelineEntity pipelineEntity) throws ExecutionException, InterruptedException, IOException {
        if (pipelineEntity.getPipelineStartImmediately()) {
            jobLauncher.launchById(pipelineEntity.getId());
        }
    }

    public void update(PipelineRequestParams pipelineRequestParams) throws ExecutionException, InterruptedException {
        ObjectMapper objectMapper = new ObjectMapper();
        Map<String, Object> map = objectMapper.convertValue(pipelineRequestParams, new TypeReference<Map<String, Object>>() {});
        map.values().removeIf(Objects::isNull);
        map.put("updatedAt", Instant.now().toString());
        logger.info(map.toString());
        ApiFuture<WriteResult> future = db.collection(FIRESTORE_PIPELINES_COLLECTION).document(pipelineRequestParams.getId()).update(map);
        WriteResult writeResult = future.get();
        logger.info("Pipeline '{}' updated at {}", pipelineRequestParams.getId(), writeResult.getUpdateTime());
    }

}
