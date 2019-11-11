package com.google.allenday.nanostream.launcher;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@WebServlet(name = "StopDataflow", value = "/stop")
public class StopDataflow extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new Pipeline.Stopper(req, resp).invoke();
    }
}
