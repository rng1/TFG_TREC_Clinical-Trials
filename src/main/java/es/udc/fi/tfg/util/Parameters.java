package es.udc.fi.tfg.util;

import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public record Parameters() {

    public static final String DOCS_PATH = "C:\\Users\\rnara\\Desktop\\TFG\\data";
    public static final String INDEX_PATH = "C:\\Users\\rnara\\Desktop\\TFG\\index";
    public static final String EVAL_FILENAME = "java_bm25_1_2_u.txt";
    public static final String RUN_NAME = "BASELINE_JM_0_9";

    public static final int N_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int TRIALS_PER_TOPIC = 1000;

    public static final Similarity SIMILARITY = new LMJelinekMercerSimilarity(0.9f);

    public static boolean USE_QUERY_FILTER = true;
    public static boolean INDEX_KEYWORDS = true;

    // TOPSIS weights
    public static double MAIN_WEIGHT = 0.1;
    public static double INCLUSION_WEIGHT = 0.2;
    public static double EXCLUSION_WEIGHT = 0.8;
}
