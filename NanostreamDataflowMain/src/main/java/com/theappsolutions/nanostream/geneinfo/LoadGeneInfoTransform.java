package com.theappsolutions.nanostream.geneinfo;

import japsa.seq.Alphabet;
import japsa.seq.Sequence;
import org.apache.beam.sdk.io.TextIO;
import org.apache.beam.sdk.transforms.*;
import org.apache.beam.sdk.transforms.join.CoGbkResult;
import org.apache.beam.sdk.transforms.join.CoGroupByKey;
import org.apache.beam.sdk.transforms.join.KeyedPCollectionTuple;
import org.apache.beam.sdk.values.*;

import java.util.HashSet;
import java.util.Set;

/**
 * Generate {@link GeneInfo} data collection from fasta and gene list files
 */
public class LoadGeneInfoTransform extends PTransform<PBegin, PCollection<KV<String, GeneInfo>>> {

    private final static String GENE_GROUP_PREFIX ="dg=";
    private final static String GENE_ID_PREFIX ="geneID==";

    private String fastaFilePath, genesListFilePath;

    public LoadGeneInfoTransform(String fastaFilePath, String genesListFilePath) {
        this.fastaFilePath = fastaFilePath;
        this.genesListFilePath = genesListFilePath;
    }

    @Override
    public PCollection<KV<String, GeneInfo>> expand(PBegin input) {

        PCollection<String> fastaData = input.getPipeline()
                .apply("Read fasta file", TextIO.read().from(fastaFilePath));
        PCollection<String> geneListData = input.getPipeline()
                .apply("Read gene list file", TextIO.read().from(genesListFilePath));

        PCollection<Sequence> fastaDataAsSequnces = fastaData
                .apply("Parse Sequnce from fasta", ParDo.of(new DoFn<String, Sequence>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String element = c.element();
                        String[] parts = element.split("\t");

                        String header = parts[0];
                        String bases = parts[1];

                        header = header.replaceFirst("^>", "");
                        String[] headerParts = header.split("\\s+", 2);

                        Sequence sequence;
                        if (headerParts.length > 1) {
                            sequence = new Sequence(Alphabet.DNA(), bases, headerParts[0]);
                            sequence.setDesc(headerParts[1]);
                        } else {
                            sequence = new Sequence(Alphabet.DNA(), bases, header);
                        }
                        c.output(sequence);
                    }
                }));

        PCollection<KV<String, Sequence>> sequenceNameSequenceKVcollection = fastaDataAsSequnces.apply(
                "SequenceName => Sequence",
                ParDo.of(new DoFn<Sequence, KV<String, Sequence>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        Sequence seq = c.element();
                        c.output(KV.of(seq.getName(), seq));
                    }
                })
        );

        PCollection<KV<String, String>> sequenceNameGeneGroupKVcollection = fastaDataAsSequnces.apply(
                "Sequence_name => GeneGroup",
                ParDo.of(new DoFn<Sequence, KV<String, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        Sequence seq = c.element();
                        String name = seq.getName();
                        String[] toks = seq.getDesc().split(";");
                        for (String tok : toks) {
                            if (tok.startsWith(GENE_GROUP_PREFIX)) {
                                String geneGroup = tok.substring(GENE_GROUP_PREFIX.length());
                                c.output(KV.of(name, geneGroup));
                            }
                        }
                    }
                })
        );

        PCollection<KV<String, String>> sequenceNameProteinIdKVcollection = fastaDataAsSequnces.apply(
                "SequenceName => ProteinId",
                ParDo.of(new DoFn<Sequence, KV<String, String>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        Sequence seq = c.element();
                        String name = seq.getName();
                        String[] toks = seq.getDesc().split(";");
                        for (String tok : toks) {
                            if (tok.startsWith(GENE_ID_PREFIX)) {
                                String proteinID = tok.substring(GENE_ID_PREFIX.length());
                                c.output(KV.of(name, proteinID));
                            }
                        }
                    }
                })
        );
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

        PCollection<KV<String, String>> flattenedGeneGroups = PCollectionList.of(sequenceNameGeneGroupKVcollection)
                .and(geneIdGeneGroupKVcollection).apply(Flatten.pCollections());
        PCollection<KV<String, String>> flattenedGeneNames = PCollectionList.of(sequenceNameProteinIdKVcollection)
                .and(geneIdGeneNameKVcollection).apply(Flatten.pCollections());

        PCollection<KV<String, Iterable<String>>> groupedGeneGroups = flattenedGeneGroups.apply("geneID => Iterable<geneGroup>", GroupByKey.<String, String>create());
        PCollection<KV<String, Iterable<String>>> groupedGeneNames = flattenedGeneNames.apply("geneID => Iterable<geneName>", GroupByKey.<String, String>create());


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

        TupleTag<Sequence> geneSequenceTag = new TupleTag<Sequence>();
        TupleTag<Set<String>> geneGroupTag = new TupleTag<Set<String>>();
        TupleTag<Set<String>> geneNameTag = new TupleTag<Set<String>>();

        PCollection<KV<String, CoGbkResult>> kvCollection = KeyedPCollectionTuple
                .of(geneGroupTag, geneGroups)
                .and(geneNameTag, geneNames)
                .and(geneSequenceTag, sequenceNameSequenceKVcollection)
                .apply(CoGroupByKey.create());

        PCollection<KV<String, GeneInfo>> geneInfo = kvCollection.apply(
                "GeneId => GeneInfo",
                ParDo.of(new DoFn<KV<String, CoGbkResult>, KV<String, GeneInfo>>() {
                    @ProcessElement
                    public void processElement(ProcessContext c) {
                        String geneID = c.element().getKey();

                        if (geneID == null)
                            return;

                        CoGbkResult gbk = c.element().getValue();

                        Sequence seq = gbk.getOnly(geneSequenceTag, null);
                        Set<String> gg = gbk.getOnly(geneGroupTag, new HashSet<>());
                        Set<String> gn = gbk.getOnly(geneNameTag, new HashSet<>());

                        if (seq == null)
                            return;

                        GeneInfo info = new GeneInfo();
                        info.sequence = seq;
                        info.setGroups(gg);
                        info.setNames(gn);

                        c.output(KV.of(geneID, info));
                    }
                })
        );
        return geneInfo;
    }
}





