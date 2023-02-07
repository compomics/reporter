package eu.isas.reporter.calculation;

import com.compomics.util.math.BasicMathFunctions;
import eu.isas.reporter.settings.RatioEstimationSettings;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.math.util.FastMath;

/**
 * This class estimates ratios at the peptide and protein level.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class RatioEstimator {

    /**
     * Estimate the ratio resulting from the compilation of several ratios.
     *
     * @param ratioEstimationSettings the ratio estimation settings
     * @param ratios The input ratios
     * @return the resulting ratio
     */
    public static Double estimateRatios(
            RatioEstimationSettings ratioEstimationSettings,
            ArrayList<Double> ratios
    ) {

        if (ratios == null || ratios.isEmpty()) {
            return 0.0;
        }
        if (ratios.size() < 6) {
            return BasicMathFunctions.median(ratios);
        }

        Collections.sort(ratios);
        int nZeros = 0;
        Double ratioMin = null, ratioMax = null;

        for (double ratio : ratios) {

            if (ratio == 0) {

                nZeros++;

            } else {

                if (ratioMin == null || ratioMin > ratio) {
                    ratioMin = ratio;
                }

                if (ratioMax == null || ratioMax < ratio) {
                    ratioMax = ratio;
                }

            }

        }

        if (nZeros == ratios.size()) {
            return 0.0;
        }

        if (ratioMin.equals(ratioMax)) {
            return ratioMin;
        }

        int nLeft = ratios.size() - 2 * nZeros;

        if (nLeft < 6) {
            return BasicMathFunctions.median(ratios);
        }

        double[] logRatios = new double[nLeft];
        int index = nZeros;
        ratioMin = null;
        ratioMax = null;

        for (int i = 0; i < nLeft; i++, index++) {

            double ratio = ratios.get(index);
            double logRatio = FastMath.log10(ratio);

            if (ratioMin == null || logRatio < ratioMin) {
                ratioMin = logRatio;
            }

            if (ratioMax == null || logRatio > ratioMax) {
                ratioMax = logRatio;
            }

            logRatios[i] = logRatio;

        }

        if (ratioMax - ratioMin <= ratioEstimationSettings.getRatioResolution()) {
            return BasicMathFunctions.median(ratios);
        }

        double logResult = mEstimate(ratioEstimationSettings, logRatios);
        double result = FastMath.pow(10, logResult);

        return result;
    }

    /**
     * Returns the compilation of various ratios using a redescending
     * M-estimator.
     *
     * @param ratioEstimationSettings the ratio estimation settings
     * @param ratios various input ratios
     *
     * @return the resulting ratio
     */
    public static Double mEstimate(
            RatioEstimationSettings ratioEstimationSettings,
            double[] ratios
    ) {

        double complement = (100 - ratioEstimationSettings.getPercentile()) / 200;

        if (complement < 0 || complement > 100) {
            throw new IllegalArgumentException(
                    "Incorrect complement window size of " + complement + "."
            );
        }

        double percentileLow = BasicMathFunctions.percentile(ratios, complement);
        double percentileHigh = BasicMathFunctions.percentile(ratios, 1 - complement);
        double window = percentileHigh - percentileLow;
        double halfWindow = window / 2;
        double resolution = ratioEstimationSettings.getRatioResolution();

        if (window == 0) {
            return BasicMathFunctions.median(ratios);
        }

        // Check how many ratios we can get in the window
        double lastTest = ratios[0] - halfWindow;
        int nRatios, nRatiosMax = 0;
        double step = Math.min(0.01 * window, resolution);

        for (double ratioRef : ratios) {

            if (ratioRef + halfWindow > lastTest) {

                double start = Math.max(lastTest, ratioRef - halfWindow);
                lastTest = ratioRef + halfWindow;

                for (double r0 = start; r0 <= lastTest; r0 += step) {

                    nRatios = 0;

                    for (double ratio : ratios) {
                        if (Math.abs(ratio - r0) <= halfWindow) {
                            nRatios++;
                        }
                    }

                    if (nRatios > nRatiosMax) {
                        nRatiosMax = nRatios;
                    }

                }
            }
        }

        double bestIntegral = -1;
        ArrayList<Double> bestRatios = new ArrayList<>();

        lastTest = ratios[0] - halfWindow;

        for (double ratioRef : ratios) {

            if (ratioRef + halfWindow > lastTest) {

                double start = Math.max(lastTest, ratioRef - halfWindow);
                lastTest = ratioRef + halfWindow;

                for (double r0 = start; r0 <= lastTest; r0 += step) {

                    double integral = 0;
                    nRatios = 0;

                    for (double r : ratios) {

                        if (Math.abs(r - r0) <= halfWindow) {
                            nRatios++;
                            integral += (r - r0) * Math.pow(1 - Math.pow((r - r0) / window, 2), 2);
                        }

                    }

                    if (nRatios > 0.9 * nRatiosMax) {

                        integral = Math.abs(integral);

                        if (integral == bestIntegral || bestIntegral == -1) {

                            bestRatios.add(r0);

                        } else if (integral < bestIntegral) {

                            bestIntegral = integral;
                            bestRatios = new ArrayList<Double>();
                            bestRatios.add(r0);

                        }

                    }
                }
            }
        }
        if (bestRatios.isEmpty()) {

            throw new IllegalArgumentException(
                    "Best ratio not found for the given set of ratios."
            );

        } else if (bestRatios.size() == 1) {

            return bestRatios.get(0);

        } else {

            double summ = 0;

            for (double ratio : bestRatios) {
                summ += ratio;
            }

            summ = summ / bestRatios.size();

            return summ;

        }
    }
}
