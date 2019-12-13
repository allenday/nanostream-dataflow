package com.google.allenday.nanostream.launcher;

import com.google.allenday.nanostream.launcher.worker.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@RestController
public class MainController {

    private final ListFetcher listFetcher;
    private final Starter starter;

    @Autowired
    public MainController(ListFetcher listFetcher, Starter starter) {
        this.listFetcher = listFetcher;
        this.starter = starter;
    }

    @CrossOrigin
    @PostMapping(value = "/launch", produces = APPLICATION_JSON_VALUE)
    public String launch(HttpServletRequest request) throws IOException {
        return starter.invoke(new LaunchParams(request));
    }

    @CrossOrigin
    @PostMapping(value = "/stop", produces = APPLICATION_JSON_VALUE)
    public String stop(HttpServletRequest request) throws IOException {
        return new Stopper(request).invoke();
    }

    @CrossOrigin
    @GetMapping(value = "/jobs", produces = APPLICATION_JSON_VALUE)
    public String jobs() throws IOException {
        return listFetcher.invoke();
    }

    @CrossOrigin
    @GetMapping(value = "/info", produces = APPLICATION_JSON_VALUE)
    public String info(HttpServletRequest request) throws IOException {
        return new InfoFetcher(request).invoke();
    }
}
