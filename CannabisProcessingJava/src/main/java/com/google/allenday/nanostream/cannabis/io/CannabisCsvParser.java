package com.google.allenday.nanostream.cannabis.io;

import com.google.allenday.genomics.core.model.SampleMetaData;

public class CannabisCsvParser extends SampleMetaData.Parser {

    public CannabisCsvParser() {
        super(Separation.COMMA);
    }

    @Override
    public SampleMetaData processParts(String[] csvLineParts, String csvLine) throws CsvParseException {
        try {
            SampleMetaData geneExampleMetaData = new SampleMetaData(csvLineParts[3], csvLineParts[4], csvLineParts[6], csvLine);
            geneExampleMetaData.setCenterName(csvLineParts[0]);
            geneExampleMetaData.setSraStudy(csvLineParts[1]);
            geneExampleMetaData.setBioProject(csvLineParts[2]);
            return geneExampleMetaData;
        } catch (RuntimeException e) {
            throw new CsvParseException(csvLine);
        }
    }
}
