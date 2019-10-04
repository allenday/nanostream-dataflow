package com.google.allenday.nanostream.geneinfo;

import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.DoFn;
import org.apache.beam.sdk.transforms.GroupByKey;
import org.apache.beam.sdk.transforms.PTransform;
import org.apache.beam.sdk.transforms.ParDo;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.KV;
import org.apache.beam.sdk.values.PBegin;
import org.apache.beam.sdk.values.PCollection;
import org.apache.beam.sdk.values.TupleTag;

import java.util.HashSet;
import java.util.Set;

/**
 * Generate {@link GeneInfo} data collection from gene list files
 */
public class LoadGeneInfoTransform extends PTransform<PBegin, PCollection<KV<String, GeneInfo>>> {

    private String genesListFilePath;

    public LoadGeneInfoTransform(String genesListFilePath) {
        this.genesListFilePath = genesListFilePath;
    }

    @Override
    public PCollection<KV<String, GeneInfo>> expand(PBegin input) {

        PCollection<String> geneListData = input.getPipeline()
                .apply("Read gene list file", TextIO.read().from(genesListFilePath));


        PCollection<KV<String, String>> geneIdGeneGroupKVcollection = geneListData.apply(
                "GeneId => GeneGroup",
                ParDo.of(new DoFn<String, KV<String, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String line = c.element();
                        String[] toks = line.trim().split(" ");
                        if (toks.length < 3)
                            return;
                        String geneID = toks[0];
                        String geneGroup = toks[2];
                        c.output(KV.of(geneID, geneGroup));
                    }
                })
        );

        PCollection<KV<String, String>> geneIdGeneNameKVcollection = geneListData.apply(
                "GeneId => GeneName",
                ParDo.of(new DoFn<String, KV<String, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String line = c.element();
                        String[] toks = line.trim().split(" ");
                        if (toks.length < 3)
                            return;
                        String geneID = toks[0];
                        String geneName = toks[1];
                        c.output(KV.of(geneID, geneName));
                    }
                })
        );


        PCollection<KV<String, Iterable<String>>> groupedGeneGroups = geneIdGeneGroupKVcollection.apply("geneID => Iterable<geneGroup>", GroupByKey.create());
        PCollection<KV<String, Iterable<String>>> groupedGeneNames = geneIdGeneNameKVcollection.apply("geneID => Iterable<geneName>", GroupByKey.create());


        PCollection<KV<String, Set<String>>> geneGroups = groupedGeneGroups.apply(
                "GeneId => Set<GeneGroup>",
                ParDo.of(new DoFn<KV<String, Iterable<String>>, KV<String, Set<String>>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        Set<String> vals = new HashSet<>();
                        for (String v : c.element().getValue())
                            vals.add(v);

                        c.output(KV.of(c.element().getKey(), vals));
                    }
                })
        );

        PCollection<KV<String, Set<String>>> geneNames = groupedGeneNames.apply(
                "GeneId => Set<GeneName>",
                ParDo.of(new DoFn<KV<String, Iterable<String>>, KV<String, Set<String>>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        Set<String> vals = new HashSet<String>();
                        for (String v : c.element().getValue())
                            vals.add(v);
                        c.output(KV.of(c.element().getKey(), vals));
                    }
                })
        );

        TupleTag<Set<String>> geneGroupTag = new TupleTag<>();
        TupleTag<Set<String>> geneNameTag = new TupleTag<>();

        PCollection<KV<String, CoGbkResult>> kvCollection = KeyedPCollectionTuple
                .of(geneGroupTag, geneGroups)
                .and(geneNameTag, geneNames)
                .apply(CoGroupByKey.create());

        return kvCollection.apply(
                "GeneId => GeneInfo",
                ParDo.of(new DoFn<KV<String, CoGbkResult>, KV<String, GeneInfo>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String geneID = c.element().getKey();

                        if (geneID == null)
                            return;

                        CoGbkResult gbk = c.element().getValue();

                        Set<String> gg = gbk.getOnly(geneGroupTag, new HashSet<>());
                        Set<String> gn = gbk.getOnly(geneNameTag, new HashSet<>());

                        GeneInfo info = new GeneInfo();
                        info.setGroups(gg);
                        info.setNames(gn);

                        c.output(KV.of(geneID, info));
                    }
                })
        );
    }
}





