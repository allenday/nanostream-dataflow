package com.google.allenday.nanostream.launcher;

import com.google.allenday.nanostream.launcher.data.PipelineEntity;
import com.google.allenday.nanostream.launcher.data.PipelineRequestParams;
import com.google.allenday.nanostream.launcher.worker.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
@RequestMapping("/api/v1")
public class MainController {

    private final JobListFetcher jobListFetcher;
    private final SubscriptionCreator subscriptionCreator;
    private final PipelineCreator pipelineCreator;
    private final PipelineUpdater pipelineUpdater;
    private final PipelineRemover pipelineRemover;
    private final JobLauncher jobLauncher;
    private final PipelineListFetcher pipelineListFetcher;
    private final PipelineDetailsFetcher pipelineDetailsFetcher;

    @Autowired
    public MainController(JobListFetcher jobListFetcher,
                          SubscriptionCreator subscriptionCreator, PipelineCreator pipelineCreator,
                          PipelineUpdater pipelineUpdater, PipelineRemover pipelineRemover, JobLauncher jobLauncher,
                          PipelineListFetcher pipelineListFetcher,
                          PipelineDetailsFetcher pipelineDetailsFetcher
    ) {
        this.jobListFetcher = jobListFetcher;
        this.subscriptionCreator = subscriptionCreator;
        this.pipelineCreator = pipelineCreator;
        this.pipelineUpdater = pipelineUpdater;
        this.pipelineRemover = pipelineRemover;
        this.jobLauncher = jobLauncher;
        this.pipelineListFetcher = pipelineListFetcher;
        this.pipelineDetailsFetcher = pipelineDetailsFetcher;
    }

    @CrossOrigin
    @PostMapping(value = "/pipelines", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineEntity> createPipeline(@RequestBody PipelineRequestParams pipelineRequestParams) throws Exception {
        PipelineEntity pipelineEntity = pipelineCreator.create(pipelineRequestParams);
        return ResponseEntity.ok(pipelineEntity);
    }

    @CrossOrigin
    @PutMapping(value = "/pipelines", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineRequestParams> updatePipeline(@RequestBody PipelineRequestParams pipelineRequestParams) throws Exception {
        pipelineUpdater.update(pipelineRequestParams);
        return ResponseEntity.ok(pipelineRequestParams);
    }

    @CrossOrigin
    @GetMapping(value = "/pipelines", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<Map<String, Object>>>> pipelineList() throws ExecutionException, InterruptedException {
        Map<String, List<Map<String, Object>>> response = pipelineListFetcher.invoke();
        return ResponseEntity.ok(response);
    }

    @CrossOrigin
    @GetMapping(value = "/pipelines/{pipelineId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, PipelineEntity>> pipelineDetails(@PathVariable("pipelineId") String pipelineId) throws ExecutionException, InterruptedException {
        PipelineEntity pipeline = pipelineDetailsFetcher.invoke(pipelineId);
        Map<String, PipelineEntity> result = new HashMap<>();
        result.put("pipeline", pipeline);
        return ResponseEntity.ok(result);
    }

    @CrossOrigin
    @DeleteMapping(value = "/pipelines/{pipelineId}", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<String> deletePipeline(@PathVariable("pipelineId") String pipelineId, @RequestBody PipelineRequestParams pipelineRequestParams) throws Exception {
        pipelineRemover.remove(pipelineId, pipelineRequestParams);
        return ResponseEntity.ok("{}");
    }

    @CrossOrigin
    @PostMapping(value = "/jobs/launch", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> launchJob(@RequestParam String targetInputFolder, @RequestParam String uploadBucketName) throws Exception {
        List<String> jobIds = jobLauncher.launchAutostarted(targetInputFolder, uploadBucketName);
        return ResponseEntity.ok(jobIds);
    }

    @CrossOrigin
    @PostMapping(value = "/jobs/stop", produces = APPLICATION_JSON_VALUE)
    public String stopJob(HttpServletRequest request) throws IOException {
        return new JobStopper(request).invoke();
    }

    @CrossOrigin
    @GetMapping(value = "/jobs", produces = APPLICATION_JSON_VALUE)
    public String getJobList() throws IOException {
        return jobListFetcher.invoke();
    }

    @CrossOrigin
    @GetMapping(value = "/jobs/info", produces = APPLICATION_JSON_VALUE)
    public String getJobInfo(HttpServletRequest request) throws IOException {
        return new JobInfoFetcher(request).invoke();
    }

    @CrossOrigin
    @PostMapping(value = "/subscription/create", produces = APPLICATION_JSON_VALUE)
    public String createSubscription(@RequestParam String topic) throws IOException {
        return subscriptionCreator.invoke(topic);
    }

}
