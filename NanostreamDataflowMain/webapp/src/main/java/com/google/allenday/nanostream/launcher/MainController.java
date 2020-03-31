package com.google.allenday.nanostream.launcher;

import com.google.allenday.nanostream.launcher.data.PipelineEntity;
import com.google.allenday.nanostream.launcher.data.PipelineRequestParams;
import com.google.allenday.nanostream.launcher.worker.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    private final JobLauncher jobLauncher;
    private final JobStopper jobStopper;
    private final JobInfoFetcher jobInfoFetcher;
    private final PipelineCreator pipelineCreator;
    private final PipelineUpdater pipelineUpdater;
    private final PipelineRemover pipelineRemover;
    private final PipelineListFetcher pipelineListFetcher;
    private final PipelineDetailsFetcher pipelineDetailsFetcher;

    @Autowired
    public MainController(JobListFetcher jobListFetcher, JobLauncher jobLauncher, JobStopper jobStopper,
                          JobInfoFetcher jobInfoFetcher, PipelineCreator pipelineCreator, PipelineUpdater pipelineUpdater,
                          PipelineRemover pipelineRemover, PipelineListFetcher pipelineListFetcher,
                          PipelineDetailsFetcher pipelineDetailsFetcher
    ) {
        this.jobListFetcher = jobListFetcher;
        this.jobLauncher = jobLauncher;
        this.jobStopper = jobStopper;
        this.jobInfoFetcher = jobInfoFetcher;
        this.pipelineCreator = pipelineCreator;
        this.pipelineUpdater = pipelineUpdater;
        this.pipelineRemover = pipelineRemover;
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
    public String stopJob(@RequestParam String location, @RequestParam String jobId) throws IOException {
        return jobStopper.invoke(location, jobId);
    }

    @CrossOrigin
    @GetMapping(value = "/jobs", produces = APPLICATION_JSON_VALUE)
    public String getJobList() throws IOException {
        return jobListFetcher.invoke();
    }

    @CrossOrigin
    @GetMapping(value = "/jobs/info", produces = APPLICATION_JSON_VALUE)
    public String getJobInfo(@RequestParam String location, @RequestParam String jobId) throws IOException {
        return jobInfoFetcher.invoke(location, jobId);
    }

}
