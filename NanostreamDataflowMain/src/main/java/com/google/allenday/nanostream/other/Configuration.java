package com.google.allenday.nanostream.other;

import java.util.concurrent.TimeUnit;

public class Configuration {

    public final static int DEFAULT_ALIGNMENT_WINDOW = 60;
    public final static int DEFAULT_STATIC_UPDATING_DELAY = 60;
    public final static int DEFAULT_ALIGNMENT_BATCH_SIZE = 2000;
    public final static String DEFAULT_BWA_ARGUMENTS = "-t 4";

    public final static String SEQUENCES_STATISTIC_COLLECTION_NAME_BASE = "statistic";
    public final static String GENE_CACHE_COLLECTION_NAME_BASE = "gene_cache";

    public final static String SPECIES_GENE_DATA_FILE_NAME = "species_tree.txt";
    public final static String RESISTANCE_GENES_GENE_DATA_FILE_NAME = "resistance_genes_tree.txt";

    public final static String INTERLEAVED_DATA_FOLDER_NAME = "interleaved";
    public final static String SAM_SHARDS_DATA_FOLDER_NAME = "sam_shards";
    public final static String SAM_COLLECTED_DATA_FOLDER_NAME = "sam_collected";
    public static final long ALIGNMENT_DELAY_MILISEC = TimeUnit.SECONDS.toMillis(300);
    public static final int ALIGNMENT_CHECK_DELAY_DELTA_MILISEC = 500;

    public static final String[] REF_DB_ARRAY = {"AGQN03", "LKUA01", "LKUB01", "MNPR01", "MXBD01", "QKVJ02", "QVPT02", "UZAU01"};
}
