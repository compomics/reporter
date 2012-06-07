package eu.isas.reporter.calculation;

import java.util.Arrays;

/**
 * This class provides basic statistical methods.
 *
 * @author Marc Vaudel
 */
public class BasicStats {

    /**
     * Method to estimate the median.
     *
     * @param ratios array of double
     * @return median of the input
     */
    public static double median(double[] ratios) {
        Arrays.sort(ratios);
        int length = ratios.length;
        if (ratios.length == 1) {
            return ratios[0];
        }
        if (length % 2 == 1) {
            return ratios[(length - 1) / 2];
        } else {
            return (ratios[length / 2] + ratios[(length) / 2 - 1]) / 2;
        }
    }

    /**
     * Method estimating the median absolute deviation.
     *
     * @param ratios array of doubles
     * @return the mad of the input
     */
    public static double mad(double[] ratios) {
        double[] deviations = new double[ratios.length];
        double med = median(ratios);
        for (int i = 0; i < ratios.length; i++) {
            deviations[i] = Math.abs(ratios[i] - med);
        }
        return median(deviations);
    }
}
