package com.google.allenday.nanostream.taxonomy.resistant_genes;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Gene specification data class
 */
@DefaultCoder(SerializableCoder.class)
public class ResistantGeneInfo implements Serializable {
    public String sequence;
    public Double score;
    public Long readCounts;
    public Long baseCounts;
    private String names;
    private String groups;

    public ResistantGeneInfo() {
        names = "";
        groups = "";
        sequence = "";
        score = -999999D;
        readCounts = 0L;
        baseCounts = 0L;
    }

    //have to do custom serialize sets - incompatibility with collections and AvroCoder
    public Set<String> getNames() {
        Set<String> res = new HashSet<>(Arrays.asList(names.split(",")));
        return res;
    }

    public void setNames(Set<String> set) {
        List<String> toks = new ArrayList<>(set);
        names = StringUtils.join(toks, ",");
    }

    public Set<String> getGroups() {
        Set<String> res = new HashSet<>(Arrays.asList(groups.split(",")));
        return res;
    }

    public void setGroups(Set<String> set) {
        List<String> toks = new ArrayList<>(set);
        groups = StringUtils.join(toks, ",");
    }

    @Override
    public String toString() {
        return "ResistantGeneInfo{" +
                "sequence=" + sequence +
                ", score=" + score +
                ", readCounts=" + readCounts +
                ", baseCounts=" + baseCounts +
                ", names='" + names + '\'' +
                ", groups='" + groups + '\'' +
                '}';
    }
}