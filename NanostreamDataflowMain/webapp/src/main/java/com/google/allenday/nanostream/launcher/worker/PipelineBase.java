package com.google.allenday.nanostream.launcher.worker;

import com.google.allenday.nanostream.launcher.config.GcpProject;
import com.google.cloud.firestore.Firestore;

import static com.google.allenday.nanostream.launcher.util.PipelineUtil.initFirestoreConnection;

abstract public class PipelineBase {
    protected String project;
    protected Firestore db;

    public PipelineBase(GcpProject gcpProject) {
        project = gcpProject.getId();
        db = initFirestoreConnection(project);
    }
}
