package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.config.GcpProject;
import com.google.allenday.nanostream.launcher.data.PipelineEntity;
import com.google.allenday.nanostream.launcher.data.PipelineRequestParams;
import com.google.allenday.nanostream.launcher.exception.BadRequestException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

import static com.google.allenday.nanostream.launcher.util.AssertUtil.assertNotEmpty;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.FIRESTORE_PIPELINES_COLLECTION;

@Service
public class PipelineRemover extends PipelineBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineRemover.class);

    private final SubscriptionRemover subscriptionRemover;

    @Autowired
    public PipelineRemover(SubscriptionRemover subscriptionRemover, GcpProject gcpProject) {
        super(gcpProject);
        this.subscriptionRemover = subscriptionRemover;
    }

    public void remove(String pipelineId, PipelineRequestParams pipelineRequestParams) throws ExecutionException, InterruptedException, IOException {
        String inputDataSubscription = pipelineRequestParams.getInputDataSubscription();
        String removeSubscriptionOutput = subscriptionRemover.invoke(inputDataSubscription);
        validateRemoveSubscriptionOutput(removeSubscriptionOutput, inputDataSubscription);
        doRemove(pipelineId);
    }

    public void remove(PipelineEntity pipelineEntity, PipelineRequestParams pipelineRequestParams) throws ExecutionException, InterruptedException, IOException {
        assertNotEmpty(pipelineRequestParams.getInputDataSubscription(), "Missing subscription name");
        subscriptionRemover.invoke(pipelineRequestParams.getInputDataSubscription());
        if (pipelineEntity != null && pipelineEntity.getId() != null) {
            doRemove(pipelineEntity.getId());
        }
    }

    private void doRemove(String pipelineId) throws InterruptedException, ExecutionException {
        ApiFuture<WriteResult> future = db.collection(FIRESTORE_PIPELINES_COLLECTION).document(pipelineId).delete();
        WriteResult writeResult = future.get();
        logger.info("Pipeline '{}' removed at {}", pipelineId, writeResult.getUpdateTime());
    }

    private void validateRemoveSubscriptionOutput(String removeSubscriptionOutput, String inputDataSubscription) {
        if (!"{}".equals(removeSubscriptionOutput.trim())) { // if success response contains an empty json
            String message = "Cannot remove pipeline subscription: " + inputDataSubscription;
            logger.error(message + ": " + removeSubscriptionOutput);
            throw new BadRequestException("REMOVE_PIPELINE_ERROR", message);
        }
    }
}
