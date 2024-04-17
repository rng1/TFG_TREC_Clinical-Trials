package es.udc.fi.tfg.util;

import org.apache.commons.cli.ParseException;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;

public class Utility {

    public static Similarity parseModel(final String model) throws ParseException {

        final String[] aux = model.split(" ");

        if (aux.length == 1) {
            throw new ParseException(
                    "Missing parameter for indexing model BM25. Valid options are [bm25 K1]");
        }

        switch (aux[0].toLowerCase()) {
        case "jm" -> {
            return new LMJelinekMercerSimilarity(Float.parseFloat(aux[1]));
        }
        case "bm25" -> {
            return new BM25Similarity(Float.parseFloat(aux[1]), 0.75f);
        }
        default -> throw new ParseException(
                "Unknown indexing model: " + model + ". Valid options are [jm LAMBDA|bm25 K1]");
        }
    }
}
