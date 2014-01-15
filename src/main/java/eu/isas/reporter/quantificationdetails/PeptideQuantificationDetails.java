/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.reporter.quantificationdetails;

import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import java.util.HashMap;

/**
 * This class contains the quantitative information at the peptide level.
 *
 * @author Marc
 */
public class PeptideQuantificationDetails {

    /**
     * The reporter raw (not normalised) ratios
     */
    private HashMap<Integer, Double> rawRatios = null;

    /**
     * Returns the ratio of a given sample normalized if the given reporter ion quantification has normalisation factors. null if not found.
     *
     * @param sampleIndex the index of sample of interest
     * @param reporterIonQuantification the quantification object containing the
     * normalisation factors
     *
     * @return the ratio for this sample, null if not set
     */
    public Double getRatio(int sampleIndex, ReporterIonQuantification reporterIonQuantification) {
        if (rawRatios == null) {
            return null;
        }
        Double ratio = rawRatios.get(sampleIndex);
        if (reporterIonQuantification.hasNormalisationFactors()
                && ratio != null
                && ratio != Double.NaN) {
            ratio /= reporterIonQuantification.getNormalisationFactor(sampleIndex);
        }
        return ratio;
    }

    /**
     * Sets a raw (not normalised) normalized ratio.
     *
     * @param sampleIndex the index of the sample
     * @param value the value of the raw (not normalised) ratio
     */
    public void setRawRatio(int sampleIndex, double value) {
        if (rawRatios == null) {
            rawRatios = new HashMap<Integer, Double>();
        }
        rawRatios.put(sampleIndex, value);
    }

    /**
     * Returns the raw (not normalised) ratio of a given sample. null if not
     * found.
     *
     * @param sampleIndex the index of sample of interest
     *
     * @return the raw (not normalised) ratio for this sample, null if not set
     */
    public Double getRawRatio(int sampleIndex) {
        if (rawRatios == null) {
            return null;
        }
        return rawRatios.get(sampleIndex);
    }
}
