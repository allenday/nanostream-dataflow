package com.google.allenday.nanostream.launcher;

import com.google.allenday.nanostream.launcher.worker.InfoFetcher;
import com.google.allenday.nanostream.launcher.worker.ListFetcher;
import com.google.allenday.nanostream.launcher.worker.Starter;
import com.google.allenday.nanostream.launcher.worker.Stopper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class MainController {

  @PostMapping(value = "/launch", produces = APPLICATION_JSON_VALUE)
  public String launch(HttpServletRequest request) throws IOException {
    return new Starter(request).invoke();
  }

  @PostMapping(value = "/stop", produces = APPLICATION_JSON_VALUE)
  public String stop(HttpServletRequest request) throws IOException {
    return new Stopper(request).invoke();
  }

  @GetMapping(value = "/jobs", produces = APPLICATION_JSON_VALUE)
  public String jobs(HttpServletRequest request) throws IOException {
    return new ListFetcher(request).invoke();
  }

  @GetMapping(value = "/info", produces = APPLICATION_JSON_VALUE)
  public String info(HttpServletRequest request) throws IOException {
    return new InfoFetcher(request).invoke();
  }
}
