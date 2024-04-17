package es.udc.fi.tfg.eval.metrics;

public class TopicMetrics {

    private final int retrieved;
    private final long totalRelevant;

    private int firstPos = Integer.MAX_VALUE;
    private int relevantRetrieved = 0;
    private double sumPrecision = 0.0;

    public TopicMetrics(final int retrieved, final long totalRelevant) {
        this.retrieved = retrieved;
        this.totalRelevant = totalRelevant;
    }

    public void updateMetrics(final int relevance, final int i) {
        if (relevance != 0) {
            firstPos = Math.min(firstPos, i);
            relevantRetrieved++;
            sumPrecision += (double) relevantRetrieved / (i + 1);
        }
    }

    public double getPrecision(final int cut) {
        return (double) relevantRetrieved / Math.min(cut, retrieved);
    }

    public double getRecall() {
        return (double) relevantRetrieved / totalRelevant;
    }

    public double getAveragePrecision() {
        return sumPrecision / totalRelevant;
    }

    public double getReciprocalRank() {
        return firstPos == Integer.MAX_VALUE ? 0.0 : (double) 1 / (firstPos + 1);
    }
}
