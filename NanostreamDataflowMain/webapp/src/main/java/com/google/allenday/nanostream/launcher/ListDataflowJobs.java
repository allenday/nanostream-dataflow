package com.google.allenday.nanostream.launcher;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "ListDataflowJobs", value = "/jobs")
public class ListDataflowJobs extends HttpServlet {
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        // read jobs metadata from database/API?
            // request API
            // parse JSON
            //
        // render template
        resp.getWriter().write("List of messages");
    }
}