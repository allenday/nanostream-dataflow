package com.google.allenday.nanostream.taxonomy;

import com.google.allenday.genomics.core.io.FileUtils;
import com.google.allenday.genomics.core.io.GCSService;
import com.google.cloud.storage.Blob;
import japsa.bio.phylo.NCBITree;
import org.javatuples.Pair;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;

public class TaxonomyProvider implements Serializable {

    public final static String NCBI_TREE_KEY = "ncbiTreeUri";

    public NCBITree getNcbiTree(GCSService gcsService, FileUtils fileUtils, Blob treeFileBlob) throws IOException {
        String filenameFromPath = fileUtils.getFilenameFromPath(gcsService.getUriFromBlob(treeFileBlob.getBlobId()));
        Pair<String, String> filenameAndExtension = fileUtils.splitFilenameAndExtension(filenameFromPath);
        File tempFile = File.createTempFile(filenameAndExtension.getValue0(), filenameAndExtension.getValue1());
        tempFile.deleteOnExit();

        gcsService.downloadBlobTo(treeFileBlob, tempFile.getAbsolutePath());
        return new NCBITree(tempFile, false);
    }
}
