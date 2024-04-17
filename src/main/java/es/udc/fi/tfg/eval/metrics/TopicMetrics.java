package es.udc.fi.tfg.eval.metrics;

public class TopicMetrics {

    private final int totalRetrieved;
    private final long totalRelevant;

    private int firstRelevantPos = Integer.MAX_VALUE;
    private int relevantRetrieved = 0;
    private double sumOfPrecision = 0.0;

    public TopicMetrics(final int totalRetrieved, final long totalRelevant) {
        this.totalRetrieved = totalRetrieved;
        this.totalRelevant = totalRelevant;
    }

    public void updateMetrics(final int relevance, final int i) {
        if (relevance == 2) {
            firstRelevantPos = Math.min(firstRelevantPos, i);
            relevantRetrieved++;
            sumOfPrecision += (double) relevantRetrieved / (i + 1);
        }
    }

    public double getPrecision(final int cut) {
        return (double) relevantRetrieved / Math.min(cut, totalRetrieved);
    }

    public double getRecall() {
        return (double) relevantRetrieved / totalRelevant;
    }

    public double getAveragePrecision() {
        return sumOfPrecision / totalRelevant;
    }

    public double getReciprocalRank() {
        return firstRelevantPos == Integer.MAX_VALUE ? 0.0 : (double) 1 / (firstRelevantPos + 1);
    }
}
