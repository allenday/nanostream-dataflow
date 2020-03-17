package com.google.allenday.nanostream.geneinfo;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Gene data class
 */
@DefaultCoder(SerializableCoder.class)
public class TaxonData implements Serializable {

    private Set<String> geneNames;
    private List<String> taxonomy;
    private List<String> colors;

    public TaxonData(List<String> taxonomy) {
        this.taxonomy = taxonomy;
    }

    public TaxonData(List<String> taxonomy, List<String> colors) {
        this.taxonomy = taxonomy;
        this.colors = colors;
    }

    public TaxonData() {

    }

    public List<String> getColors() {
        return colors;
    }

    public void setColors(List<String> colors) {
        this.colors = colors;
    }

    public Set<String> getGeneNames() {
        return geneNames;
    }

    public void setGeneNames(Set<String> geneNames) {
        this.geneNames = geneNames;
    }

    public List<String> getTaxonomy() {
        return taxonomy;
    }

    public void setTaxonomy(List<String> taxonomy) {
        this.taxonomy = taxonomy;
    }

    @Override
    public String toString() {
        return "TaxonData{" +
                "geneNames=" + geneNames +
                ", taxonomy=" + taxonomy +
                '}';
    }
}