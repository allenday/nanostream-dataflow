package com.theappsolutions.nanostream.other;

import com.theappsolutions.nanostream.util.EntityNamer;

public class Constants {

    public final static String SEQUENCES_STATISTIC_DOCUMENT_NAME =
            EntityNamer.generateTimestampedName("resultDocument");
    public final static String SEQUENCES_STATISTIC_COLLECTION_NAME_BASE = "sequences_statistic";
    public final static String SEQUENCES_BODIES_COLLECTION_NAME_BASE = "sequences_bodies";
    public final static String GENE_CACHE_COLLECTION_NAME_BASE = "gene_cache";
}
