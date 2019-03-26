package com.google.allenday.nanostream.cannabis_source;

import org.apache.beam.sdk.transforms.DoFn;

//TODO
/**
 */
public class ParseSourceCsvFn extends DoFn<String, CannabisSourceFileMetaData> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        String dataLine = c.element();
        if (dataLine.split(",")[3].equals("SRS190966")) {
            CannabisSourceFileMetaData.fromCSVLine(dataLine).forEach(c::output);
        }
    }
}
