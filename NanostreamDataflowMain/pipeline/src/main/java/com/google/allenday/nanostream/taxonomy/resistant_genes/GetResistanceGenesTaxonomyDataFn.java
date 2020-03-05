package com.google.allenday.nanostream.taxonomy.resistant_genes;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.allenday.genomics.core.reference.ReferenceDatabaseSource;
import com.google.allenday.nanostream.gcs.GCSSourceData;
import com.google.allenday.nanostream.geneinfo.TaxonData;
import com.google.allenday.nanostream.taxonomy.TaxonomyProvider;
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
        KV<KV<GCSSourceData, String>, KV<ReferenceDatabaseSource, TaxonData>>> {
    private Logger LOG = LoggerFactory.getLogger(GetResistanceGenesTaxonomyDataFn.class);

    private PCollectionView<Map<String, ResistantGeneInfo>> geneInfoMapPCollectionView;
    private TaxonomyProvider taxonomyProvider;
    private FileUtils fileUtils;

    private GCSService gcsService;

    public GetResistanceGenesTaxonomyDataFn(TaxonomyProvider taxonomyProvider, FileUtils fileUtils) {
        this.taxonomyProvider = taxonomyProvider;
        this.fileUtils = fileUtils;
    }

    public GetResistanceGenesTaxonomyDataFn setGeneInfoMapPCollectionView(
            PCollectionView<Map<String, ResistantGeneInfo>> geneInfoMapPCollectionView) {
        this.geneInfoMapPCollectionView = geneInfoMapPCollectionView;
        return this;
    }

    @Setup
    public void setup() throws IOException {
        gcsService = GCSService.initialize(fileUtils);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<KV<GCSSourceData, String>, ReferenceDatabaseSource> input = c.element();
        String geneName = input.getKey().getValue();
        Map<String, ResistantGeneInfo> geneInfoMap = c.sideInput(geneInfoMapPCollectionView);

        ReferenceDatabaseSource referenceDatabaseSource = input.getValue();
        Optional<Blob> ncbiTreeBlob = referenceDatabaseSource.getCustomDatabaseFileByKey(gcsService, TaxonomyProvider.NCBI_TREE_KEY);

        ncbiTreeBlob.ifPresent(blob -> {
            try {
                NCBITree ncbiTree = taxonomyProvider.getNcbiTree(gcsService, fileUtils, blob);
                TaxonData taxonData = new TaxonData();
                if (geneInfoMap.containsKey(geneName)) {
                    ResistantGeneInfo geneInfo = geneInfoMap.get(geneName);
                    Set<String> names = geneInfo.getNames();
                    taxonData.setGeneNames(names);
                    taxonData.setTaxonomy(new ArrayList<>(geneInfo.getGroups()));
                    if (names.size() > 0) {
                        String name = names.iterator().next();

                        String[][] taxonomyAndColor = ncbiTree.getTaxonomy(name.trim());

                        List<String> taxonomy = Arrays.asList(taxonomyAndColor[0]);
                        Collections.reverse(taxonomy);

                        List<String> colors = Arrays.asList(taxonomyAndColor[1]);
                        Collections.reverse(colors);

                        taxonData.setColors(colors);
                        taxonData.setTaxonomy(taxonomy);
                    }
                }
                c.output(KV.of(input.getKey(), KV.of(referenceDatabaseSource, taxonData)));
            } catch (IOException e) {
                LOG.error(e.getMessage(), e);
            }
        });

    }
}
