package com.google.allenday.nanostream.cannabis.io;

import com.google.allenday.genomics.core.model.GeneExampleMetaData;

public class CannabisCsvParser extends GeneExampleMetaData.Parser {

    @Override
    public GeneExampleMetaData processParts(String[] csvLineParts, String csvLine) {
        GeneExampleMetaData geneExampleMetaData = new GeneExampleMetaData(csvLineParts[3], csvLineParts[4], csvLineParts[6], csvLine);
        geneExampleMetaData.setCenterName(csvLineParts[0]);
        geneExampleMetaData.setSraStudy(csvLineParts[1]);
        geneExampleMetaData.setBioProject(csvLineParts[2]);
        return geneExampleMetaData;
    }
}
