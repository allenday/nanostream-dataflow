package com.google.allenday.nanostream.taxonomy;

import com.google.allenday.nanostream.geneinfo.GeneData;
import com.google.allenday.nanostream.geneinfo.GeneInfo;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PCollectionView;

import java.util.ArrayList;
import java.util.Map;
import java.util.Set;

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
            Set<String> names = geneInfo.getNames();
            geneData.setGeneNames(names);
            geneData.setTaxonomy(new ArrayList<>(geneInfo.getGroups()));
            if (names.size() > 0) {
                geneData.getTaxonomy().add(names.iterator().next());
            }
        }
        c.output(KV.of(geneName, geneData));
    }
}
