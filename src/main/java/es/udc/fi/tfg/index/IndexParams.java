package es.udc.fi.tfg.index;

import org.apache.lucene.search.similarities.Similarity;

import lombok.Data;

@Data
public final class IndexParams {

    private final String indexPath;
    private final String docsPath;
    private final Similarity model;
    private final int numThreads;
}