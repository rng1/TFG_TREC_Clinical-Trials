package es.udc.fi.tfg.eval.metrics;

public class MeanMetrics {

    private double sumOfPrecision = 0.0;
    private double sumOfRecall = 0.0;
    private double sumOfAveragePrecision = 0.0;
    private double sumOfReciprocalRank = 0.0;
    private int numberOfMetrics = 0;

    public void updateMetrics(final double precision, final double recall, final double averagePrecision,
            final double reciprocalRank) {

        if (precision != 0 || recall != 0 || averagePrecision != 0 || reciprocalRank != 0) {
            sumOfPrecision += precision;
            sumOfRecall += recall;
            sumOfAveragePrecision += averagePrecision;
            sumOfReciprocalRank += reciprocalRank;
            numberOfMetrics++;
        }
    }

    public double getMeanPrecision() {
        return numberOfMetrics == 0 ? 0 : sumOfPrecision / numberOfMetrics;
    }

    public double getMeanRecall() {
        return numberOfMetrics == 0 ? 0 : sumOfRecall / numberOfMetrics;
    }

    public double getMeanAveragePrecision() {
        return numberOfMetrics == 0 ? 0 : sumOfAveragePrecision / numberOfMetrics;
    }

    public double getMeanReciprocalRank() {
        return numberOfMetrics == 0 ? 0 : sumOfReciprocalRank / numberOfMetrics;
    }
}