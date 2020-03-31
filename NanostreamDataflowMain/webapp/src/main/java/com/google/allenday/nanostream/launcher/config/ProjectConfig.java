package com.google.allenday.nanostream.launcher.config;

import com.google.apphosting.api.ApiProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.google.apphosting.api.ApiProxy.getCurrentEnvironment;

@Configuration
public class ProjectConfig {

    private static final Logger logger = LoggerFactory.getLogger(ProjectConfig.class);

    @Bean
    public GcpProject getGcpProject() {
        GcpProject gcpProject = new GcpProject();
        gcpProject.setId(getProjectId());
        return gcpProject;
    }

    private String getProjectId() {
        String projectId;
        projectId = System.getenv("GOOGLE_CLOUD_PROJECT");
        if (projectId != null && !projectId.isEmpty()) {
            logger.info("ProjectId from env var: " + projectId);
        } else { // useful for local environment where GOOGLE_CLOUD_PROJECT not set
            ApiProxy.Environment env = getCurrentEnvironment();
            String appId = env.getAppId();
            // According to docs (https://cloud.google.com/appengine/docs/standard/java/appidentity/)
            // appId should be the same as projectId.
            // In reality appId is prefixed by "s~" chars
            projectId = appId.replaceAll("^s~", "");
            logger.info("ProjectId from api proxy: " + projectId);
        }
        return projectId;
    }

}
