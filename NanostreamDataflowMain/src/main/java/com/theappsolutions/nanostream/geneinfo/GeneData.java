package com.theappsolutions.nanostream.geneinfo;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;

import java.io.Serializable;
import java.util.List;
import java.util.Set;

/**
 * Gene data class
 */
@DefaultCoder(SerializableCoder.class)
public class GeneData implements Serializable {

    private Set<String> geneNames;
    private List<String> taxonomy;

    public GeneData(List<String> taxonomy) {
        this.taxonomy = taxonomy;
    }

    public GeneData() {

    }

    public void setGeneNames(Set<String> geneNames) {
        this.geneNames = geneNames;
    }

    public void setTaxonomy(List<String> taxonomy) {
        this.taxonomy = taxonomy;
    }

    public Set<String> getGeneNames() {
        return geneNames;
    }

    public List<String> getTaxonomy() {
        return taxonomy;
    }

    @Override
    public String toString() {
        return "GeneData{" +
                "geneNames=" + geneNames +
                ", taxonomy=" + taxonomy +
                '}';
    }
}