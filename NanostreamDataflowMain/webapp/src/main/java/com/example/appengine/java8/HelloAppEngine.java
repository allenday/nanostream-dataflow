/*
 * Copyright 2017 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.appengine.java8;

// [START example]

import com.google.allenday.nanostream.NanostreamPipeline;
import com.google.allenday.nanostream.NanostreamPipelineOptions;
import com.google.common.collect.Lists;
import org.apache.beam.sdk.options.PipelineOptionsFactory;
import com.google.api.gax.paging.Page;
import com.google.appengine.api.utils.SystemProperty;
import com.google.cloud.storage.Bucket;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.IOException;
import java.util.Properties;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

// With @WebServlet annotation the webapp/WEB-INF/web.xml is no longer required.
@WebServlet(name = "HelloAppEngine", value = "/hello")
public class HelloAppEngine extends HttpServlet {

    @Override
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String responseText = null;
        responseText = getResponseText();
        response.setContentType("text/plain");
        response.getWriter().println(responseText);
    }

    private String getResponseText() {
        Properties properties = System.getProperties();

        String[] args = "--runner=org.apache.beam.runners.dataflow.DataflowRunner --project=tas-nanostream-test1 --streaming=true --processingMode=species --inputDataSubscription=projects/tas-nanostream-test1/subscriptions/tas-nanostream-test1-upload-subscription --alignmentWindow=20 --statisticUpdatingDelay=30 --servicesUrl=http://10.128.0.3 --bwaEndpoint=/cgi-bin/bwa.cgi --bwaDatabase=DB.fasta --kAlignEndpoint=/cgi-bin/kalign.cgi --outputCollectionNamePrefix=new_scanning --outputDocumentNamePrefix=statistic_document".split("\\s+");
        NanostreamPipelineOptions options = PipelineOptionsFactory.fromArgs(args)
                .withValidation()
                .as(NanostreamPipelineOptions.class);

        new NanostreamPipeline(options).run();

        return "Hello App Engine - Standard using "
                + SystemProperty.version.get() + " Java "
                + properties.get("java.specification.version");
    }

    public static String getInfo() {
        return "Version: " + System.getProperty("java.version")
                + " OS: " + System.getProperty("os.name")
                + " User: " + System.getProperty("user.name");
    }

}
// [END example]
