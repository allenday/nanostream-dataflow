package com.google.allenday.nanostream.taxonomy;

import com.google.allenday.nanostream.geneinfo.GeneData;
import com.google.allenday.nanostream.geneinfo.GeneInfo;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import japsa.bio.phylo.NCBITree;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 *
 */
public class GetResistanceGenesTaxonomyDataFn extends DoFn<KV<GCSSourceData, String>, KV<KV<GCSSourceData, String>, GeneData>> {

    private PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView;
    private String treeText;
    private NCBITree tree;

    public GetResistanceGenesTaxonomyDataFn(String treeText) {
        this.treeText = treeText;
    }

    public GetResistanceGenesTaxonomyDataFn setGeneInfoMapPCollectionView(
            PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView) {
        this.geneInfoMapPCollectionView = geneInfoMapPCollectionView;
        return this;
    }

    @Setup
    public void setup() throws IOException {
        File temp = File.createTempFile("tree", "txt");

        // Delete temp file when program exits.
        temp.deleteOnExit();

        // Write to temp file
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(treeText);
        out.close();

        tree = new NCBITree(temp, false);
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        KV<GCSSourceData, String> gcsSourceDataStringKV = c.element();
        String geneName = gcsSourceDataStringKV.getValue();
        Map<String, GeneInfo> geneInfoMap = c.sideInput(geneInfoMapPCollectionView);

        GeneData geneData = new GeneData();
        if (geneInfoMap.containsKey(geneName)) {
            GeneInfo geneInfo = geneInfoMap.get(geneName);
            Set<String> names = geneInfo.getNames();
            geneData.setGeneNames(names);
            geneData.setTaxonomy(new ArrayList<>(geneInfo.getGroups()));
            if (names.size() > 0) {
                String name = names.iterator().next();

                String[][] taxonomyAndColor = tree.getTaxonomy(name.trim());

                List<String> taxonomy = Arrays.asList(taxonomyAndColor[0]);
                Collections.reverse(taxonomy);

                List<String> colors = Arrays.asList(taxonomyAndColor[1]);
                Collections.reverse(colors);

                geneData.setColors(colors);
                geneData.setTaxonomy(taxonomy);
            }
        }
        c.output(KV.of(gcsSourceDataStringKV, geneData));
    }
}
