package com.google.allenday.nanostream.taxonomy;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.reference.ReferenceDatabaseSource;
import com.google.allenday.nanostream.gcs.GCSSourceData;
import com.google.allenday.nanostream.geneinfo.TaxonData;
import com.google.cloud.storage.Blob;
import japsa.bio.phylo.NCBITree;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

public class GetTaxonomyFromTree extends DoFn<KV<KV<GCSSourceData, String>, ReferenceDatabaseSource>,
        KV<KV<GCSSourceData, String>, KV<ReferenceDatabaseSource, TaxonData>>> {
    private Logger LOG = LoggerFactory.getLogger(GetTaxonomyFromTree.class);

    private TaxonomyProvider taxonomyProvider;
    private FileUtils fileUtils;

    private GCSService gcsService;

    public GetTaxonomyFromTree(TaxonomyProvider taxonomyProvider, FileUtils fileUtils) {
        this.taxonomyProvider = taxonomyProvider;
        this.fileUtils = fileUtils;
    }

    @Setup
    public void setup() throws IOException {
        gcsService = GCSService.initialize(fileUtils);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<KV<GCSSourceData, String>, ReferenceDatabaseSource> gcsSourceDataStringKV = c.element();

        ReferenceDatabaseSource referenceDatabaseSource = gcsSourceDataStringKV.getValue();
        Optional<Blob> ncbiTreeBlob = referenceDatabaseSource.getCustomDatabaseFileByKey(gcsService, TaxonomyProvider.NCBI_TREE_KEY);

        ncbiTreeBlob.ifPresent(blob -> {
            try {
                NCBITree ncbiTree = taxonomyProvider.getNcbiTree(gcsService, fileUtils, blob);

                String geneName = gcsSourceDataStringKV.getKey().getValue();

                // convert names like "gi|564911138|ref|NC_023018.1|" to "NC_023018.1"
                if (geneName.split("\\|").length > 3) {
                    geneName = geneName.split("\\|")[3];
                }

                String[][] taxonomyAndColor = ncbiTree.getTaxonomy(geneName.trim());

                List<String> taxonomy = Arrays.asList(taxonomyAndColor[0]);
                Collections.reverse(taxonomy);

                List<String> colors = Arrays.asList(taxonomyAndColor[1]);
                Collections.reverse(colors);

                c.output(KV.of(gcsSourceDataStringKV.getKey(), KV.of(referenceDatabaseSource, new TaxonData(taxonomy, colors))));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }

        });
    }
}
