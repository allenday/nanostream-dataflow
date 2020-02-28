package com.google.allenday.nanostream.launcher.worker;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.allenday.nanostream.launcher.data.PipelineRequestParams;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.FIRESTORE_PIPELINES_COLLECTION;

@Service
public class PipelineUpdater extends PipelineBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineUpdater.class);

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
