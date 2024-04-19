package es.udc.fi.tfg.eval.metrics;

public class MeanMetrics {

    private double sumOfP = 0.0;
    private double sumOfRR = 0.0;
    private double sumOfnDCG = 0.0;
    private double sumOfRP = 0.0;
    private int totalMetrics = 0;

    public void updateMetrics(final double p, final double rr, final double ndcg, final double rp) {
        sumOfP += p;
        sumOfRR += rr;
        sumOfRP += rp;

        if (!Double.isNaN(ndcg)) {
            sumOfnDCG += ndcg;
        }

        totalMetrics++;
    }

    // Mean Precision
    public double getMP() {
        return totalMetrics == 0 ? 0 : sumOfP / totalMetrics;
    }

    // Mean Reciprocal Rank
    public double getMRR() {
        return totalMetrics == 0 ? 0 : sumOfRR / totalMetrics;
    }

    // Mean Normalized Discounted Cumulative Gain
    public double getMnDCG() {
        return totalMetrics == 0 ? 0 : sumOfnDCG / totalMetrics;
    }

    // Mean R-Precision
    public double getMRP() {
        return totalMetrics == 0 ? 0 : sumOfRP / totalMetrics;
    }
}