package com.theappsolutions.nanostream.probecalculation;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;
import java.util.List;

@DefaultCoder(SerializableCoder.class)
public class SequenceCountAndTaxonomyData implements Serializable {

    private long count;
    private List<String> taxonomy;

    public SequenceCountAndTaxonomyData(long count, List<String> taxonomy) {
        this.count = count;
        this.taxonomy = taxonomy;
    }

    public SequenceCountAndTaxonomyData(List<String> taxonomy) {
        this.count = 1L;
        this.taxonomy = taxonomy;
    }

    public void increment() {
        count++;
    }

    public long getCount() {
        return count;
    }

    public List<String> getTaxonomy() {
        return taxonomy;
    }

    @Override
    public String toString() {
        return "SequenceCountAndTaxonomyData{" +
                "count=" + count +
                ", taxonomy=" + taxonomy +
                '}';
    }

    public static SequenceCountAndTaxonomyData merge(SequenceCountAndTaxonomyData data1,
                                               SequenceCountAndTaxonomyData data2) {
        return new SequenceCountAndTaxonomyData(data1.getCount() + data2.getCount(),
                data2.getTaxonomy());
    }
}
