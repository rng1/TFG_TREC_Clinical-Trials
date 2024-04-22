package es.udc.fi.tfg.util;

import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.Similarity;

public record Parameters() {

    public static final String BRANCH_NAME = "";
    public static final String DOCS_PATH = "";
    public static final String INDEX_PATH = "" + BRANCH_NAME;

    public static final int N_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int TRIALS_PER_TOPIC = 1000;
    public static final int CUT = 10;

    public static final Similarity SIMILARITY = new BM25Similarity(1.2f, 0.75f);

    // Index options
    public static boolean INDEX_KEYWORDS = true;

    // Search options
    public static boolean USE_QUERY_FILTER = true;

    public static double MAIN_WEIGHT = 0.4;
    public static double INCLUSION_WEIGHT = 0.4;
    public static double EXCLUSION_WEIGHT = 0.6;
}
