package com.google.allenday.nanostream.cannabis_source;

import com.google.allenday.nanostream.gcloud.GCSService;
import org.apache.beam.sdk.transforms.DoFn;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

//TODO

/**
 */
public class ReadSourceCsvFn extends DoFn<String, String> {


    private Logger LOG = LoggerFactory.getLogger(ReadSourceCsvFn.class);

    private GCSService gcsService;
    private static int counter =0;
    @Setup
    public void setup() {
        gcsService = GCSService.initialize();
    }

    @ProcessElement
    public void processElement(ProcessContext c) throws InterruptedException {
        String csvFilename = c.element();
        byte[] content = gcsService.getBlob("nano-stream-cannabis","CannabisGenomics-201703-Sheet1.csv").getContent();
        String data = new String(content);
        for (String dataLine: data.split("\n")){
            counter++;
            LOG.info(String.format("Output line %d: %s", counter, dataLine));
            c.output(dataLine);
        }
    }
}
