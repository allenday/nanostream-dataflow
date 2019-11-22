package com.google.allenday.nanostream.launcher;

import com.google.allenday.nanostream.launcher.worker.Stopper;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@WebServlet(name = "StopDataflow", value = "/stop")
public class StopDataflow extends HttpServlet {

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        new Stopper(req, resp).invoke();
    }
}
