package com.google.allenday.nanostream.taxonomy;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.reference.ReferenceDatabaseSource;
import com.google.allenday.nanostream.geneinfo.GeneData;
import com.google.allenday.nanostream.geneinfo.GeneInfo;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import com.google.cloud.storage.Blob;
import japsa.bio.phylo.NCBITree;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.*;

/**
 *
 */
public class GetResistanceGenesTaxonomyDataFn extends DoFn<KV<KV<GCSSourceData, String>, ReferenceDatabaseSource>,
        KV<KV<GCSSourceData, String>, KV<ReferenceDatabaseSource, GeneData>>> {
    private Logger LOG = LoggerFactory.getLogger(GetResistanceGenesTaxonomyDataFn.class);

    private PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView;
    private TaxonomyProvider taxonomyProvider;
    private FileUtils fileUtils;

    private GCSService gcsService;

    public GetResistanceGenesTaxonomyDataFn setGeneInfoMapPCollectionView(
            PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView) {
        this.geneInfoMapPCollectionView = geneInfoMapPCollectionView;
        return this;
    }

    public GetResistanceGenesTaxonomyDataFn(TaxonomyProvider taxonomyProvider, FileUtils fileUtils) {
        this.taxonomyProvider = taxonomyProvider;
        this.fileUtils = fileUtils;
    }

    @Setup
    public void setup() throws IOException {
        gcsService = GCSService.initialize(fileUtils);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<KV<GCSSourceData, String>, ReferenceDatabaseSource> input = c.element();
        String geneName = input.getKey().getValue();
        Map<String, GeneInfo> geneInfoMap = c.sideInput(geneInfoMapPCollectionView);

        ReferenceDatabaseSource referenceDatabaseSource = input.getValue();
        Optional<Blob> ncbiTreeBlob = referenceDatabaseSource.getCustomDatabaseFileByKey(gcsService, TaxonomyProvider.NCBI_TREE_KEY);

        ncbiTreeBlob.ifPresent(blob -> {
            try {
                NCBITree ncbiTree = taxonomyProvider.getNcbiTree(gcsService, fileUtils, blob);
                GeneData geneData = new GeneData();
                if (geneInfoMap.containsKey(geneName)) {
                    GeneInfo geneInfo = geneInfoMap.get(geneName);
                    Set<String> names = geneInfo.getNames();
                    geneData.setGeneNames(names);
                    geneData.setTaxonomy(new ArrayList<>(geneInfo.getGroups()));
                    if (names.size() > 0) {
                        String name = names.iterator().next();

                        String[][] taxonomyAndColor = ncbiTree.getTaxonomy(name.trim());

                        List<String> taxonomy = Arrays.asList(taxonomyAndColor[0]);
                        Collections.reverse(taxonomy);

                        List<String> colors = Arrays.asList(taxonomyAndColor[1]);
                        Collections.reverse(colors);

                        geneData.setColors(colors);
                        geneData.setTaxonomy(taxonomy);
                    }
                }
                c.output(KV.of(input.getKey(), KV.of(referenceDatabaseSource, geneData)));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        });

    }
}
