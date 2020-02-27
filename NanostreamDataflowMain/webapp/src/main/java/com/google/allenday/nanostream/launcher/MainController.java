package com.google.allenday.nanostream.launcher;

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
    private final PipelineOptionsSaver pipelineOptionsSaver;
    private final JobLauncher jobLauncher;
    private final PipelineListFetcher pipelineListFetcher;
    private final PipelineDetailsFetcher pipelineDetailsFetcher;

    @Autowired
    public MainController(JobListFetcher jobListFetcher,
                          SubscriptionCreator subscriptionCreator, PipelineOptionsSaver pipelineOptionsSaver,
                          JobLauncher jobLauncher, PipelineListFetcher pipelineListFetcher,
                          PipelineDetailsFetcher pipelineDetailsFetcher
    ) {
        this.jobListFetcher = jobListFetcher;
        this.subscriptionCreator = subscriptionCreator;
        this.pipelineOptionsSaver = pipelineOptionsSaver;
        this.jobLauncher = jobLauncher;
        this.pipelineListFetcher = pipelineListFetcher;
        this.pipelineDetailsFetcher = pipelineDetailsFetcher;
    }

    @CrossOrigin
    @PostMapping(value = "/options", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineEntity> createPipelineOptions(@RequestBody PipelineRequestParams pipelineRequestParams) throws Exception {
        PipelineEntity pipelineEntity = pipelineOptionsSaver.create(pipelineRequestParams);
        return ResponseEntity.ok(pipelineEntity);
    }

    @CrossOrigin
    @PutMapping(value = "/options", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<PipelineRequestParams> updatePipelineOptions(@RequestBody PipelineRequestParams pipelineRequestParams) throws Exception {
        pipelineOptionsSaver.update(pipelineRequestParams);
        return ResponseEntity.ok(pipelineRequestParams);
    }

    @CrossOrigin
    @PostMapping(value = "/job/launch", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<List<String>> launchJob(@RequestParam String targetInputFolder, @RequestParam String uploadBucketName) throws Exception {
        List<String> jobIds = jobLauncher.launchAutostarted(targetInputFolder, uploadBucketName);
        return ResponseEntity.ok(jobIds);
    }

    @CrossOrigin
    @PostMapping(value = "/stop", produces = APPLICATION_JSON_VALUE)
    public String stop(HttpServletRequest request) throws IOException {
        return new JobStopper(request).invoke();
    }

    @CrossOrigin
    @GetMapping(value = "/jobs", produces = APPLICATION_JSON_VALUE)
    public String jobs() throws IOException {
        return jobListFetcher.invoke();
    }

    @CrossOrigin
    @GetMapping(value = "/pipeline/list", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, List<Map<String, Object>>>> pipelineList() throws ExecutionException, InterruptedException {
        Map<String, List<Map<String, Object>>> response = pipelineListFetcher.invoke();
        return ResponseEntity.ok(response);
    }

    @CrossOrigin
    @GetMapping(value = "/pipeline/details", produces = APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, PipelineEntity>> pipelineDetails(@RequestParam String pipelineId) throws ExecutionException, InterruptedException {
        PipelineEntity pipeline = pipelineDetailsFetcher.invoke(pipelineId);
        Map<String, PipelineEntity> result = new HashMap<>();
        result.put("pipeline", pipeline);
        return ResponseEntity.ok(result);
    }

    @CrossOrigin
    @GetMapping(value = "/info", produces = APPLICATION_JSON_VALUE)
    public String info(HttpServletRequest request) throws IOException {
        return new InfoFetcher(request).invoke();
    }

    @CrossOrigin
    @PostMapping(value = "/subscription/create", produces = APPLICATION_JSON_VALUE)
    public String createSubscription(@RequestParam String topic) throws IOException {
        return subscriptionCreator.invoke(topic);
    }

}
