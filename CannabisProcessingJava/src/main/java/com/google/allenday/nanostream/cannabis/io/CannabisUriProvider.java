package com.google.allenday.nanostream.cannabis.io;

import com.google.allenday.genomics.core.io.UriProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class CannabisUriProvider extends UriProvider {

    public CannabisUriProvider(String srcBucket, ProviderRule providerRule) {
        super(srcBucket, providerRule);
    }

    public static CannabisUriProvider withDefaultProviderRule(String srcBucket) {
        return new CannabisUriProvider(srcBucket, (ProviderRule) (geneExampleMetaData, bucket) -> {
            boolean isKannapedia = geneExampleMetaData.getSraStudy().toLowerCase().equals("Kannapedia".toLowerCase());
            String uriPrefix = isKannapedia
                    ? String.format("gs://%s/kannapedia/", bucket)
                    : String.format("gs://%s/sra/%s/%s/", bucket, geneExampleMetaData.getSraStudy(),
                    geneExampleMetaData.getSraSample());
            String fileNameForward = geneExampleMetaData.getRunId() + "_1.fastq";
            List<String> urisList =
                    new ArrayList<>(Collections.singletonList(uriPrefix + fileNameForward));
            if (geneExampleMetaData.isPaired()) {
                String fileNameBack = geneExampleMetaData.getRunId() + "_2.fastq";
                urisList.add(uriPrefix + fileNameBack);
            }
            return urisList;
        });
    }
}