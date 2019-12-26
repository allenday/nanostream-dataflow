package com.google.allenday.nanostream.genebank;

import java.util.Date;
import java.util.List;

public class GeneTaxonomyInfo {

    private String geneNameNanostream;
    private String geneLocusNCBI;
    private String searchQuery;
    private List<String> taxonomy;
    private Date updatedAt;
    private String definition;


    public GeneTaxonomyInfo() {
    }


    public GeneTaxonomyInfo(String geneNameNanostream, String geneLocusNCBI, String searchQuery, List<String> taxonomy, String definition) {
        this.geneNameNanostream = geneNameNanostream;
        this.geneLocusNCBI = geneLocusNCBI;
        this.taxonomy = taxonomy;
        this.searchQuery = searchQuery;
        this.updatedAt = new Date();
        this.definition = definition;
    }

    public String getGeneNameNanostream() {
        return geneNameNanostream;
    }

    public String getGeneLocusNCBI() {
        return geneLocusNCBI;
    }

    public List<String> getTaxonomy() {
        return taxonomy;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public String getDefinition() {
        return definition;
    }
}
