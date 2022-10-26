package eu.isas.reporter.quantificationdetails;

import com.compomics.util.experiment.normalization.NormalizationFactors;
import java.util.HashMap;

/**
 * This class contains the quantitative information at the PSM level.
 *
 * @author Marc Vaudel
 */
public class PsmQuantificationDetails {

    /**
     * The reporter raw (not normalized) ratios.
     */
    private HashMap<String, Double> rawRatios = null;

    /**
     * Returns the ratio of a given sample normalized if the given reporter ion
     * quantification has normalization factors. Null if not found.
     *
     * @param reporterIonName the index of sample of interest
     * @param normalizationFactors the normalization factors
     *
     * @return the ratio for this sample, null if not set
     */
    public Double getRatio(String reporterIonName, NormalizationFactors normalizationFactors) {
        if (rawRatios == null) {
            return null;
        }
        Double ratio = rawRatios.get(reporterIonName);
        if (normalizationFactors.hasPsmNormalisationFactors()
                && ratio != null
                && ratio != Double.NaN) {
            ratio /= normalizationFactors.getPsmNormalisationFactor(reporterIonName);
        }
        return ratio;
    }

    /**
     * Sets a raw (not normalized) normalized ratio.
     *
     * @param reporterIonName the index of the sample
     * @param value the value of the raw (not normalized) ratio
     */
    public void setRawRatio(String reporterIonName, double value) {
        if (rawRatios == null) {
            rawRatios = new HashMap<String, Double>();
        }
        rawRatios.put(reporterIonName, value);
    }

    /**
     * Returns the raw (not normalized) ratio of a given sample. null if not
     * found.
     *
     * @param reporterIonName the index of sample of interest
     *
     * @return the raw (not normalized) ratio for this sample, null if not set
     */
    public Double getRawRatio(String reporterIonName) {
        if (rawRatios == null) {
            return null;
        }
        return rawRatios.get(reporterIonName);
    }
}
