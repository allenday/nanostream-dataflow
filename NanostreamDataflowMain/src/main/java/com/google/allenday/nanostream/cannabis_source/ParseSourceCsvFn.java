package com.google.allenday.nanostream.cannabis_source;

import org.apache.beam.sdk.transforms.DoFn;

/**
 */
public class ParseSourceCsvFn extends DoFn<String, CannabisSourceFileMetaData> {

    @ProcessElement
    public void processElement(ProcessContext c) {
        String dataLine = c.element();
        try {
            CannabisSourceFileMetaData.fromCSVLine(dataLine).forEach(c::output);
        } catch (Exception e) {
        }
    }
}