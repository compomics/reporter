package eu.isas.reporter.quantificationdetails;

import com.compomics.util.experiment.normalization.NormalizationFactors;
import java.util.HashMap;

/**
 * This class contains the quantitative information at the protein level.
 *
 * @author Marc Vaudel
 */
public class ProteinQuantificationDetails {
    
    /**
     * The reporter raw (not normalized) ratios.
     */
    private HashMap<String, Double> rawRatios = null;

    /**
     * The reporter raw (not normalized) ratios obtained using peptides unique to a group.
     */
    private HashMap<String, Double> uniqueRawRatios = null;

    /**
     * The reporter raw (not normalized) ratios obtained using peptides shared by groups.
     */
    private HashMap<String, Double> sharedRawRatios = null;

    /**
     * Returns the ratio of a given sample normalized if the given reporter ion
     * quantification has normalization factors. null if not found.
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
        if (normalizationFactors.hasProteinNormalisationFactors()
                && ratio != null
                && ratio != Double.NaN) {
            ratio /= normalizationFactors.getProteinNormalisationFactor(reporterIonName);
        }
        return ratio;
    }

    /**
     * Returns the ratio of a given sample normalized if the given reporter ion
     * quantification has normalization factors. null if not found.
     *
     * @param reporterIonName the index of sample of interest
     * @param normalizationFactors the normalization factors
     *
     * @return the ratio for this sample, null if not set
     */
    public Double getUniqueRatio(String reporterIonName, NormalizationFactors normalizationFactors) {
        if (uniqueRawRatios == null) {
            return null;
        }
        Double ratio = uniqueRawRatios.get(reporterIonName);
        if (normalizationFactors.hasProteinNormalisationFactors()
                && ratio != null
                && ratio != Double.NaN) {
            ratio /= normalizationFactors.getProteinNormalisationFactor(reporterIonName);
        }
        return ratio;
    }

    /**
     * Returns the ratio of a given sample normalized if the given reporter ion
     * quantification has normalization factors. null if not found.
     *
     * @param reporterIonName the index of sample of interest
     * @param normalizationFactors the normalization factors
     *
     * @return the ratio for this sample, null if not set
     */
    public Double getSharedRatio(String reporterIonName, NormalizationFactors normalizationFactors) {
        if (sharedRawRatios == null) {
            return null;
        }
        Double ratio = sharedRawRatios.get(reporterIonName);
        if (normalizationFactors.hasProteinNormalisationFactors()
                && ratio != null
                && ratio != Double.NaN) {
            ratio /= normalizationFactors.getProteinNormalisationFactor(reporterIonName);
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

    /**
     * Sets a unique raw (not normalized) normalized ratio.
     *
     * @param reporterIonName the index of the sample
     * @param value the value of the raw (not normalized) ratio
     */
    public void setUniqueRawRatio(String reporterIonName, double value) {
        if (uniqueRawRatios == null) {
            uniqueRawRatios = new HashMap<String, Double>();
        }
        uniqueRawRatios.put(reporterIonName, value);
    }

    /**
     * Returns the unique raw (not normalized) ratio of a given sample. null if not
     * found.
     *
     * @param reporterIonName the index of sample of interest
     *
     * @return the raw (not normalized) ratio for this sample, null if not set
     */
    public Double getUniqueRawRatio(String reporterIonName) {
        if (uniqueRawRatios == null) {
            return null;
        }
        return uniqueRawRatios.get(reporterIonName);
    }

    /**
     * Sets a shared raw (not normalized) normalized ratio.
     *
     * @param reporterIonName the index of the sample
     * @param value the value of the raw (not normalized) ratio
     */
    public void setSharedRawRatio(String reporterIonName, double value) {
        if (sharedRawRatios == null) {
            sharedRawRatios = new HashMap<String, Double>();
        }
        sharedRawRatios.put(reporterIonName, value);
    }

    /**
     * Returns the shared raw (not normalized) ratio of a given sample. null if not
     * found.
     *
     * @param reporterIonName the index of sample of interest
     *
     * @return the raw (not normalized) ratio for this sample, null if not set
     */
    public Double getSharedRawRatio(String reporterIonName) {
        if (sharedRawRatios == null) {
            return null;
        }
        return sharedRawRatios.get(reporterIonName);
    }
}
