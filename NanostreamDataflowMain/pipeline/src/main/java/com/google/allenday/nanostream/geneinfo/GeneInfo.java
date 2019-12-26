package com.google.allenday.nanostream.geneinfo;

import org.apache.beam.sdk.coders.DefaultCoder;
import org.apache.beam.sdk.coders.SerializableCoder;
import org.apache.commons.lang.StringUtils;

import java.io.Serializable;
import java.util.*;

/**
 * Gene specification data class
 */
@DefaultCoder(SerializableCoder.class)
public class GeneInfo implements Serializable {
    public String sequence;
    public Double score;
    public Long readCounts;
    public Long baseCounts;
    private String names;
    private String groups;

    public GeneInfo() {
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
        return "GeneInfo{" +
                "sequence=" + sequence +
                ", score=" + score +
                ", readCounts=" + readCounts +
                ", baseCounts=" + baseCounts +
                ", names='" + names + '\'' +
                ", groups='" + groups + '\'' +
                '}';
    }
}