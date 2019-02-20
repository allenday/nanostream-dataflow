package com.google.allenday.nanostream.taxonomy;

import com.google.allenday.nanostream.geneinfo.GeneData;
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

public class GetTaxonomyFromTree extends DoFn<String, KV<String, GeneData>> {
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
        String geneName = c.element();

        // convert names like "gi|564911138|ref|NC_023018.1|" to "NC_023018.1"
        if (geneName.split("\\|").length > 3) {
            geneName = geneName.split("\\|")[3];
        }

        String[][] taxonomyAndColor = tree.getTaxonomy(geneName.trim());

        List<String> taxonomy = Arrays.asList(taxonomyAndColor[0]);
        Collections.reverse(taxonomy);

        c.output(KV.of(geneName, new GeneData(taxonomy)));
    }
}
