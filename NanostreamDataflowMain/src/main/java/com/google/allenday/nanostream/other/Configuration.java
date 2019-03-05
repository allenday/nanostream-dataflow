package com.google.allenday.nanostream.other;

public class Configuration {

    public final static int DEFAULT_ALIGNMENT_WINDOW = 60;
    public final static int DEFAULT_STATIC_UPDATING_DELAY = 60;
    public final static int DEFAULT_ALIGNMENT_BATCH_SIZE = 2000;
    public final static String DEFAULT_BWA_ARGUMENTS = "-t 4";

    public final static String SEQUENCES_STATISTIC_COLLECTION_NAME_BASE = "statistic";
    public final static String GENE_CACHE_COLLECTION_NAME_BASE = "gene_cache";

    public final static String SPECIES_GENE_DATA_FILE_NAME = "species_tree.txt";
    public final static String RESISTANCE_GENES_GENE_DATA_FILE_NAME = "resistance_genes_tree.txt";
}
