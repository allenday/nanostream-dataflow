package com.theappsolutions.nanostream.taxonomy;

import com.theappsolutions.nanostream.geneinfo.GeneData;
import com.theappsolutions.nanostream.geneinfo.GeneInfo;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;

import java.util.ArrayList;
import java.util.Map;

/**
 *
 */
public class GetResistanceGenesTaxonomyDataFn extends DoFn<String, KV<String, GeneData>> {

    private final PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView;

    public GetResistanceGenesTaxonomyDataFn(PCollectionView<Map<String, GeneInfo>> geneInfoMapPCollectionView) {
        this.geneInfoMapPCollectionView = geneInfoMapPCollectionView;
    }

    @ProcessElement
    public void processElement(ProcessContext c) {
        String geneName = c.element();
        Map<String, GeneInfo> geneInfoMap = c.sideInput(geneInfoMapPCollectionView);
        GeneData geneData = new GeneData();
        if (geneInfoMap.containsKey(geneName)) {
            GeneInfo geneInfo = geneInfoMap.get(geneName);
            geneData.setGeneNames(geneInfo.getNames());
            geneData.setTaxonomy(new ArrayList<>(geneInfo.getGroups()));
        }
        c.output(KV.of(geneName, geneData));
    }
}
