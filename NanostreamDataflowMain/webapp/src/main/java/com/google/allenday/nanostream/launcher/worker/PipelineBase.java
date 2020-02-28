package com.google.allenday.nanostream.launcher.worker;

import com.google.cloud.firestore.Firestore;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.getProjectId;
import static com.google.allenday.nanostream.launcher.util.PipelineUtil.initFirestoreConnection;

abstract public class PipelineBase {
    protected String project;
    protected Firestore db;

    public PipelineBase() {
        project = getProjectId();
        db = initFirestoreConnection();
    }
}
