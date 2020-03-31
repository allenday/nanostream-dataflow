package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.data.PipelineEntity;
import com.google.allenday.nanostream.launcher.data.PipelineRequestParams;
import com.google.allenday.nanostream.launcher.data.ReferenceDb;
import com.google.allenday.nanostream.launcher.exception.BadRequestException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.google.allenday.nanostream.launcher.util.AssertUtil.assertEquals;
import static com.google.allenday.nanostream.launcher.util.AssertUtil.assertNotEmpty;
import static com.google.allenday.nanostream.launcher.util.DateTimeUtil.makeTimestamp;
import static com.google.allenday.nanostream.launcher.util.JsonResponseParser.extractSubscriptionName;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.FIRESTORE_PIPELINES_COLLECTION;
import static java.lang.String.format;
import static java.util.stream.Collectors.toSet;


@Service
public class PipelineCreator extends PipelineBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineCreator.class);
    private static final Pattern NOT_WORD_CHARACTERS = Pattern.compile("[^\\w]+");

    private final JobLauncher jobLauncher;
    private final SubscriptionCreator subscriptionCreator;
    private final PipelineRemover pipelineRemover;

    @Autowired
    public PipelineCreator(JobLauncher jobLauncher,
                           SubscriptionCreator subscriptionCreator,
                           PipelineRemover pipelineRemover
    ) {
        super();
        this.jobLauncher = jobLauncher;
        this.subscriptionCreator = subscriptionCreator;
        this.pipelineRemover = pipelineRemover;
    }

    public PipelineEntity create(PipelineRequestParams pipelineRequestParams) throws IOException, ExecutionException, InterruptedException {
        validateCreatePipelineRequestParams(pipelineRequestParams);

        createSubscription(pipelineRequestParams);
        PipelineEntity pipelineEntity = null;
        try {
            pipelineEntity = tryCreatePipeline(pipelineRequestParams);
            launchJobIfRequired(pipelineEntity, pipelineRequestParams.getPipelineStartImmediately());
        } catch (Exception e) {
            pipelineRemover.remove(pipelineEntity, pipelineRequestParams);
            throw e;
        }
        return pipelineEntity;
    }

    private PipelineEntity tryCreatePipeline(PipelineRequestParams pipelineRequestParams) throws ExecutionException, InterruptedException, IOException {
        PipelineEntity pipelineEntity;
        pipelineEntity = createPipelineEntity(pipelineRequestParams);
        savePipelineEntity(pipelineEntity);
        return pipelineEntity;
    }

    private void createSubscription(PipelineRequestParams pipelineRequestParams) throws IOException {
        assertNotEmpty(pipelineRequestParams.getUploadPubSubTopic(), "Empty topic name not allowed");
        String subscriptionName = extractSubscriptionName(subscriptionCreator.invoke(pipelineRequestParams.getUploadPubSubTopic()));
        pipelineRequestParams.setInputDataSubscription(subscriptionName);
    }

    private PipelineEntity createPipelineEntity(PipelineRequestParams pipelineRequestParams) {
        PipelineEntity pipelineEntity = new PipelineEntity(pipelineRequestParams);
        String now = Instant.now().toString();
        pipelineEntity.setId(makePipelineId());
        pipelineEntity.setOutputCollectionNamePrefix(makeOutputCollectionNamePrefix(pipelineRequestParams.getPipelineName()));
        pipelineEntity.setCreatedAt(now);
        pipelineEntity.setUpdatedAt(now);

        return pipelineEntity;
    }

    private void validateCreatePipelineRequestParams(PipelineRequestParams pipelineRequestParams) {
        assertNotEmpty(pipelineRequestParams.getPipelineName(), "Empty pipeline name not allowed");
        assertNotEmpty(pipelineRequestParams.getInputFolder(), "Empty input folder not allowed");
        assertNotEmpty(pipelineRequestParams.getUploadBucketName(), "Empty upload bucket name not allowed");
        assertNotEmpty(pipelineRequestParams.getProcessingMode(), "Empty processing mode not allowed");
        validateReferenceDbs(pipelineRequestParams.getReferenceDbs());
    }

    private void validateReferenceDbs(List<ReferenceDb> referenceDbs) {
        assertNotEmpty(referenceDbs, "Empty reference db list not allowed");
        for (ReferenceDb referenceDb : referenceDbs) {
            assertNotEmpty(referenceDb.getName(), "Empty reference db name not allowed");
            assertNotEmpty(referenceDb.getFastaUri(), "Empty reference db Fasta Uri not allowed");
            assertNotEmpty(referenceDb.getNcbiTreeUri(), "Empty reference db Ncbi Tree Uri not allowed");
        }
        validateUniqueRefDbName(referenceDbs);
        validateUniqueFastaUri(referenceDbs);
        validateRefDbNameContainsValidChars(referenceDbs);
    }

    private void validateUniqueRefDbName(List<ReferenceDb> referenceDbs) {
        Set<String> set = referenceDbs.stream().map(ReferenceDb::getName).collect(toSet());
        assertEquals(set.size(), referenceDbs.size(), "Duplicated reference db name not allowed");
    }

    private void validateUniqueFastaUri(List<ReferenceDb> referenceDbs) {
        Set<String> set = referenceDbs.stream().map(ReferenceDb::getFastaUri).collect(toSet());
        assertEquals(set.size(), referenceDbs.size(), "Duplicated reference db url not allowed");
    }

    private void validateRefDbNameContainsValidChars(List<ReferenceDb> referenceDbs) {
        referenceDbs.forEach(referenceDb -> {
            if (!referenceDb.getName().matches("[a-zA-Z0-9_\\.-]+")) {
                throw new BadRequestException("INVALID_CHARACTERS", "Reference db name contains not allowed chars. Please use alphanumeric only.");
            }
        });
    }

    private String makePipelineId() {
        return "pl-" + makeTimestamp() + "-" + UUID.randomUUID().toString();
    }

    private String makeOutputCollectionNamePrefix(String pipelineName) {
        String prefix = NOT_WORD_CHARACTERS.matcher(pipelineName).replaceAll("_").toLowerCase();
        return format("%s_%s", prefix, makeTimestamp());
    }

    private void savePipelineEntity(PipelineEntity pipelineEntity) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> future = db.collection(FIRESTORE_PIPELINES_COLLECTION).document(pipelineEntity.getId()).set(pipelineEntity);
        WriteResult writeResult = future.get();
        logger.info("Pipeline '{}' saved to  firestore at {}", pipelineEntity.getPipelineName(), writeResult.getUpdateTime());
    }

    private void launchJobIfRequired(PipelineEntity pipelineEntity, Boolean pipelineStartImmediately) throws ExecutionException, InterruptedException, IOException {
        if (pipelineStartImmediately) {
            List<String> jobIds = jobLauncher.launchById(pipelineEntity.getId());
            if (jobIds.isEmpty()) {
                throw new BadRequestException("UNKNOWN_ERROR", "Cannot start pipeline");
            }
        }
    }

}
