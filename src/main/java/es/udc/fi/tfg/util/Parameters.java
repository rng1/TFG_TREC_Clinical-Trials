package es.udc.fi.tfg.util;

import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public record Parameters() {

    public static final String DOCS_PATH = "C:\\Users\\rnara\\Desktop\\TFG\\data";
    public static final String INDEX_PATH = "C:\\Users\\rnara\\Desktop\\TFG\\index";
    public static final String EVAL_FILENAME = "java_jm_0_9_u.txt";
    public static final String RUN_NAME = "JM_0_9";

    public static final int N_THREADS = Runtime.getRuntime().availableProcessors();
    public static final int TRIALS_PER_TOPIC = 1000;

    public static final Similarity SIMILARITY = new LMJelinekMercerSimilarity(0.9f);

    public static boolean USE_QUERY_FILTER = true;
    public static boolean INDEX_KEYWORDS = true;
}
