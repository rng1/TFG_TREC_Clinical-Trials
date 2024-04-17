package es.udc.fi.tfg.eval;

import org.apache.lucene.search.similarities.Similarity;

import lombok.Data;

@Data
public class SearchEvalParams {

    private final Similarity model;
    private final String indexPath;
    private final String docsPath;
    private final Integer cut;
}
