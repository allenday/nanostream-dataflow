package com.google.allenday.nanostream.probecalculation;

import com.google.allenday.nanostream.geneinfo.TaxonData;
import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;

@DefaultCoder(SerializableCoder.class)
public class SequenceCountAndTaxonomyData implements Serializable {

    private long count;
    private TaxonData taxonData;

    public SequenceCountAndTaxonomyData(long count, TaxonData taxonData) {
        this.count = count;
        this.taxonData = taxonData;
    }

    public SequenceCountAndTaxonomyData(TaxonData taxonData) {
        this.count = 1L;
        this.taxonData = taxonData;
    }

    public static SequenceCountAndTaxonomyData merge(SequenceCountAndTaxonomyData data1,
                                                     SequenceCountAndTaxonomyData data2) {
        return new SequenceCountAndTaxonomyData(data1.getCount() + data2.getCount(),
                data2.getTaxonData());
    }

    public void increment() {
        count++;
    }

    public long getCount() {
        return count;
    }

    public TaxonData getTaxonData() {
        return taxonData;
    }

    @Override
    public String toString() {
        return "SequenceCountAndTaxonomyData{" +
                "count=" + count +
                ", taxonomy=" + taxonData +
                '}';
    }
}
