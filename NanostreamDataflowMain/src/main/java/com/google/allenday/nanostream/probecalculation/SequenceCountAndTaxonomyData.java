package com.google.allenday.nanostream.probecalculation;

import com.google.allenday.nanostream.geneinfo.GeneData;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;

@DefaultCoder(SerializableCoder.class)
public class SequenceCountAndTaxonomyData implements Serializable {

    private long count;
    private GeneData geneData;

    public SequenceCountAndTaxonomyData(long count, GeneData geneData) {
        this.count = count;
        this.geneData = geneData;
    }

    public SequenceCountAndTaxonomyData(GeneData geneData) {
        this.count = 1L;
        this.geneData = geneData;
    }

    public void increment() {
        count++;
    }

    public long getCount() {
        return count;
    }

    public GeneData getGeneData() {
        return geneData;
    }

    @Override
    public String toString() {
        return "SequenceCountAndTaxonomyData{" +
                "count=" + count +
                ", taxonomy=" + geneData +
                '}';
    }

    public static SequenceCountAndTaxonomyData merge(SequenceCountAndTaxonomyData data1,
                                               SequenceCountAndTaxonomyData data2) {
        return new SequenceCountAndTaxonomyData(data1.getCount() + data2.getCount(),
                data2.getGeneData());
    }
}
