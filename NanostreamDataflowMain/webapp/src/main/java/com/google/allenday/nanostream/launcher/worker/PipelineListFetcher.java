package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.config.GcpProject;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.QueryDocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.FIRESTORE_PIPELINES_COLLECTION;

@Service
public class PipelineListFetcher extends PipelineBase {

    private static final Logger logger = LoggerFactory.getLogger(PipelineListFetcher.class);

    @Autowired
    public PipelineListFetcher(GcpProject gcpProject) {
        super(gcpProject);
    }

    public Map<String, List<Map<String, Object>>> invoke() throws ExecutionException, InterruptedException {
        Map<String, List<Map<String, Object>>> result = new HashMap<>();

        List<Map<String, Object>> pipelines = new ArrayList<>();
        ApiFuture<QuerySnapshot> future = db.collection(FIRESTORE_PIPELINES_COLLECTION).get();

        List<QueryDocumentSnapshot> documents = future.get().getDocuments();
        for (QueryDocumentSnapshot document : documents) {
            pipelines.add(document.getData());
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
