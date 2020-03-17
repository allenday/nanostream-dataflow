package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.data.PipelineEntity;
import com.google.allenday.nanostream.launcher.exception.BadRequestException;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.Firestore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.concurrent.ExecutionException;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.*;

@Service
public class PipelineDetailsFetcher {

    private static final Logger logger = LoggerFactory.getLogger(PipelineDetailsFetcher.class);

    private String project;
    private Firestore db;

    public PipelineDetailsFetcher() {
        project = getProjectId();
        db = initFirestoreConnection();
    }

    public PipelineEntity invoke(String pipelineId) throws ExecutionException, InterruptedException {
        ApiFuture<DocumentSnapshot> future = db.collection(FIRESTORE_PIPELINES_COLLECTION).document(pipelineId).get();
        DocumentSnapshot document = future.get();

        if (document.exists()) {
            return document.toObject(PipelineEntity.class);
        } else {
            throw new BadRequestException("PIPELINE_NOT_FOUND", "Pipeline not found");
        }
    }
}
