package com.google.allenday.nanostream.taxonomy;

import com.google.allenday.nanostream.geneinfo.GeneData;
import com.google.allenday.nanostream.pubsub.GCSSourceData;
import japsa.bio.phylo.NCBITree;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class GetTaxonomyFromTree extends DoFn<KV<GCSSourceData, String>, KV<KV<GCSSourceData, String>, GeneData>> {
    private String treeText;
    private NCBITree tree;

    public GetTaxonomyFromTree(String treeText) {
        this.treeText = treeText;
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

        // convert names like "gi|564911138|ref|NC_023018.1|" to "NC_023018.1"
        if (geneName.split("\\|").length > 3) {
            geneName = geneName.split("\\|")[3];
        }

        String[][] taxonomyAndColor = tree.getTaxonomy(geneName.trim());

        List<String> taxonomy = Arrays.asList(taxonomyAndColor[0]);
        Collections.reverse(taxonomy);

        List<String> colors = Arrays.asList(taxonomyAndColor[1]);
        Collections.reverse(colors);

        c.output(KV.of(gcsSourceDataStringKV, new GeneData(taxonomy, colors)));
    }
}
