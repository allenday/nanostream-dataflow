package com.google.allenday.nanostream.launcher.worker;

import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.FIRESTORE_PIPELINES_COLLECTION;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.getProjectId;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.initFirestoreConnection;
import static java.lang.String.format;

@Service
public class PipelineListFetcher {

    private static final Logger logger = LoggerFactory.getLogger(PipelineListFetcher.class);

    private String project;
    private Firestore db;

    public PipelineListFetcher() {
        project = getProjectId();
        db = initFirestoreConnection();
    }

    public Map<String, List<Map<String, Object>>> invoke() throws ExecutionException, InterruptedException {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        List<Map<String, Object>> pipelines = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = db.collection(FIRESTORE_PIPELINES_COLLECTION).get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            pipelines.add(document.getData());
//            logger.info("Document data: " + document.getData());
//            System.out.println("Document data: " + document.getData());
//            System.out.println(document.getId() + " => " + document.toObject(City.class));
        }
        pipelines.sort(byCreatedAtDesc());

        result.put("pipelines", pipelines);
        return result;
    }

    private Comparator<Map<String, Object>> byCreatedAtDesc() {
        return (o1, o2) -> {
            String createdAt1 = (String)o1.get("createdAt");
            String createdAt2 = (String)o2.get("createdAt");
            if (createdAt1.equals(createdAt2)) {
                return 0;
            }
            if (Instant.parse(createdAt1).getEpochSecond() > Instant.parse(createdAt2).getEpochSecond()) {
                return -1;
            } else {
                return 1;
            }
        };
    }

}
