package com.google.allenday.nanostream.launcher;

import com.google.allenday.nanostream.launcher.worker.InfoFetcher;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "GetJobInfo", value = "/info")
public class GetJobInfo extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new InfoFetcher(req, resp).invoke();
    }
}
