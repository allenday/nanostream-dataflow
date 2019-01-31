package com.theappsolutions.nanostream.taxonomy;

import com.theappsolutions.nanostream.geneinfo.GeneInfo;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
public class GetResistantGenesTaxonomyDataFn extends DoFn<String, KV<String, List<String>>> {

    private final PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView;

    public GetResistantGenesTaxonomyDataFn(PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView) {
        this.geneInfoMapPCollectionView = geneInfoMapPCollectionView;
    }

    @Setup
    public void setup() {
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        String geneName = c.element();
        Map<String, GeneInfo> geneInfoMap = c.sideInput(geneInfoMapPCollectionView);
        List<String> taxonomy = new ArrayList<>();
        if (geneInfoMap.containsKey(geneName)) {
            taxonomy.addAll(geneInfoMap.get(geneName).getGroups());
        }
        c.output(KV.of(geneName, taxonomy));
    }
}
