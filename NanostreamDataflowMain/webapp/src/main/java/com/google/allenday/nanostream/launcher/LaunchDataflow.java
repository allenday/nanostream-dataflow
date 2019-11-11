package com.google.allenday.nanostream.launcher;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;

@WebServlet(name = "LaunchDataflow", value = "/launch")
public class LaunchDataflow extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new Pipeline.Starter(req, resp).invoke();
    }
}
