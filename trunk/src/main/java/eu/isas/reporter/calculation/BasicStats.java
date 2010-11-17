package eu.isas.reporter.calculation;

import java.util.Arrays;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class BasicStats {

    /**
     * @TODO: JavaDoc missing
     *
     * @param ratios
     * @return
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
     * @TODO: JavaDoc missing
     *
     * @param ratios
     * @return
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
