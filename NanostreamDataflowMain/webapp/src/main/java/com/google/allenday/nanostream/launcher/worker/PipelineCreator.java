package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.data.PipelineEntity;
import com.google.allenday.nanostream.launcher.data.PipelineRequestParams;
import com.google.allenday.nanostream.launcher.data.ReferenceDb;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;

import static com.google.allenday.nanostream.launcher.util.AssertUtil.assertNotEmpty;
import static com.google.allenday.nanostream.launcher.util.DateTimeUtil.makeTimestamp;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.FIRESTORE_PIPELINES_COLLECTION;
import static java.lang.String.format;


@Service
public class PipelineCreator extends PipelineBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineCreator.class);
    private static final Pattern NOT_WORD_CHARACTERS = Pattern.compile("[^\\w]+");

    private final JobLauncher jobLauncher;

    @Autowired
    public PipelineCreator(JobLauncher jobLauncher) {
        super();
        this.jobLauncher = jobLauncher;
    }

    public PipelineEntity create(PipelineRequestParams pipelineRequestParams) throws IOException, ExecutionException, InterruptedException {
        PipelineEntity pipelineEntity = createOptionsForNewPipeline(pipelineRequestParams);
        savePipelineConfiguration(pipelineEntity);
        launchJobIfRequired(pipelineEntity, pipelineRequestParams.getPipelineStartImmediately());
        return pipelineEntity;
    }

    private PipelineEntity createOptionsForNewPipeline(PipelineRequestParams pipelineRequestParams) {
        validateCreatePipelineRequestParams(pipelineRequestParams);

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
        assertNotEmpty(pipelineRequestParams.getReferenceNameList(), "Empty reference name list not allowed");
        assertNotEmpty(pipelineRequestParams.getProcessingMode(), "Empty processing mode not allowed");
        assertNotEmpty(pipelineRequestParams.getInputDataSubscription(), "Empty input data subscription not allowed");
        validateReferenceDbs(pipelineRequestParams.getReferenceDbs());
    }

    private void validateReferenceDbs(List<ReferenceDb> referenceDbs) {
        assertNotEmpty(referenceDbs, "Empty reference db list allowed");
        for (ReferenceDb referenceDb : referenceDbs) {
            assertNotEmpty(referenceDb.getName(), "Empty reference db name not allowed");
            assertNotEmpty(referenceDb.getFastaUri(), "Empty reference db Fasta Uri not allowed");
            assertNotEmpty(referenceDb.getNcbiTreeUri(), "Empty reference db Ncbi Tree Uri not allowed");
        }
    }

    private String makePipelineId() {
        return "pl-" + makeTimestamp() + "-" + UUID.randomUUID().toString();
    }

    private String makeOutputCollectionNamePrefix(String pipelineName) {
        String prefix = NOT_WORD_CHARACTERS.matcher(pipelineName).replaceAll("_").toLowerCase();
        return format("%s_%s", prefix, makeTimestamp());
    }

    private void savePipelineConfiguration(PipelineEntity pipelineEntity) throws ExecutionException, InterruptedException {
        ApiFuture<WriteResult> future = db.collection(FIRESTORE_PIPELINES_COLLECTION).document(pipelineEntity.getId()).set(pipelineEntity);
        WriteResult writeResult = future.get();
        logger.info("Pipeline '{}' saved to  firestore at {}", pipelineEntity.getPipelineName(), writeResult.getUpdateTime());
    }

    private void launchJobIfRequired(PipelineEntity pipelineEntity, Boolean pipelineStartImmediately) throws ExecutionException, InterruptedException, IOException {
        if (pipelineStartImmediately) {
            jobLauncher.launchById(pipelineEntity.getId());
        }
    }

}
