package es.udc.fi.tfg.eval.metrics;

public class MeanMetrics {
    private double sumP = 0.0;
    private double sumR = 0.0;
    private double sumAP = 0.0;
    private double sumRR = 0.0;
    private int count = 0;

    public void updateMetrics(final double precision, final double recall, final double averagePrecision,
            final double reciprocalRank) {
        if (precision != 0 || recall != 0 || averagePrecision != 0 || reciprocalRank != 0) {
            sumP += precision;
            sumR += recall;
            sumAP += averagePrecision;
            sumRR += reciprocalRank;
            count++;
        }
    }

    public double getMeanPrecision() {
        return count == 0 ? 0 : sumP / count;
    }

    public double getMeanRecall() {
        return count == 0 ? 0 : sumR / count;
    }

    public double getMeanAveragePrecision() {
        return count == 0 ? 0 : sumAP / count;
    }

    public double getMeanReciprocalRank() {
        return count == 0 ? 0 : sumRR / count;
    }
}