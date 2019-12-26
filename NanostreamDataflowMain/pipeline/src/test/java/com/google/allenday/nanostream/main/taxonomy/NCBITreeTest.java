package com.google.allenday.nanostream.main.taxonomy;

import com.google.allenday.nanostream.util.ResourcesHelper;
import japsa.bio.phylo.NCBITree;
import org.apache.commons.io.IOUtils;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static com.google.allenday.nanostream.other.Configuration.SPECIES_GENE_DATA_FILE_NAME;
import static com.google.common.base.Charsets.UTF_8;

public class NCBITreeTest {

    @Test
    public void testFastQDataParsedCorrectly() throws IOException {
        String treeText = new ResourcesHelper().getFileContent("resistance_genes_tree.txt");
        File temp = File.createTempFile("tree", "txt");

        // Delete temp file when program exits.
        temp.deleteOnExit();

        // Write to temp file
        BufferedWriter out = new BufferedWriter(new FileWriter(temp));
        out.write(treeText);
        out.close();

        NCBITree tree = new NCBITree(temp, false);
        String[][] taxonomy = tree.getTaxonomy("blaHERA");
    }
}
