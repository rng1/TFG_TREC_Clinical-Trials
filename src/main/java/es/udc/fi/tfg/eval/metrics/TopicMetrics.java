package es.udc.fi.tfg.eval.metrics;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TopicMetrics {

    private final List<Integer> dcgRelevances = new ArrayList<>();

    private int firstRelevantPos = Integer.MAX_VALUE;
    private int relevantRetrieved = 0;

    public TopicMetrics() {
    }

    public void updateMetrics(final int relevance, final int i) {
        if (relevance == 2) {
            firstRelevantPos = Math.min(firstRelevantPos, i);
            relevantRetrieved++;
        }

        dcgRelevances.add(relevance);
    }

    // Precision at cut
    public double getP(final int cut) {
        return (double) relevantRetrieved / cut;
    }

    // Reciprocal Rank
    public double getRR() {
        return firstRelevantPos == Integer.MAX_VALUE ? 0.0 : (double) 1 / (firstRelevantPos + 1);
    }

    // Discounted Cumulative Gain at cut
    public double getDCG(final int cut) {
        double dcg = 0.0;
        for (int i = 0; i < cut; i++) {
            dcg += (Math.pow(2, dcgRelevances.get(i)) - 1) / log2(i + 2);
        }
        return dcg;
    }

    // Ideal Discounted Cumulative Gain at cut
    public double getIDCG(final int cut) {
        final List<Integer> sortedRelevances = new ArrayList<>(dcgRelevances);
        sortedRelevances.sort(Collections.reverseOrder());

        double idcg = 0.0;
        for (int i = 0; i < cut; i++) {
            idcg += (Math.pow(2, sortedRelevances.get(i)) - 1) / log2(i + 2);
        }
        return idcg;
    }

    private double log2(final double x) {
        return Math.log(x) / Math.log(2);
    }
}
