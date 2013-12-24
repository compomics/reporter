/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.reporter.calculation;

import com.compomics.util.experiment.identification.matches.IonMatch;
import java.util.HashMap;

/**
 * This class contains the quantification results details for a given match
 *
 * @author Marc
 */
public class MatchQuantificationDetails {

    /**
     * The reporter ratios
     */
    private HashMap<Integer, Double> ratios = null;
    /**
     * The reporter ion matches
     */
    private HashMap<Integer, IonMatch> reporterMatches = null;
    /**
     * The deisotoped reporter intensities
     */
    private HashMap<Integer, Double> deisotopedIntensities = null;

    /**
     * Sets a ratio.
     *
     * @param sampleIndex the index of the sample
     * @param value the value of the ratio
     */
    public void setRatio(int sampleIndex, double value) {
        if (ratios == null) {
            ratios = new HashMap<Integer, Double>();
        }
        ratios.put(sampleIndex, value);
    }

    /**
     * Returns the ratio of a given sample.
     *
     * @param sampleIndex the index of sample of interest
     *
     * @return the ratio for this sample, null if not set
     */
    public Double getRatio(int sampleIndex) {
        if (ratios == null) {
            return null;
        }
        return ratios.get(sampleIndex);
    }

    /**
     * Sets the value of a deisotoped intensity.
     *
     * @param index the index of the sample
     * @param value the value of the deisotoped intensity
     */
    public void setDeisotopedIntensity(Integer index, double value) {
        if (deisotopedIntensities == null) {
            deisotopedIntensities = new HashMap<Integer, Double>();
        }
        deisotopedIntensities.put(index, value);
    }

    /**
     * Returns the intensity of the given sample.
     *
     * @param index the index of the sample
     *
     * @return the intensity
     */
    public Double getDeisotopedIntensity(Integer index) {
        if (deisotopedIntensities == null) {
            return null;
        }
        return deisotopedIntensities.get(index);
    }

    /**
     * Sets the value of a reporter ion match.
     *
     * @param index the index of the sample
     * @param reporterMatch the reporter ion match
     */
    public void setReporterMatch(Integer index, IonMatch reporterMatch) {
        if (reporterMatches == null) {
            reporterMatches = new HashMap<Integer, IonMatch>();
        }
        reporterMatches.put(index, reporterMatch);
    }

    /**
     * Returns the reporter ion match of the given sample. Null if none.
     *
     * @param index the index of the sample
     *
     * @return the reporter ion match
     */
    public IonMatch getRepoterMatch(Integer index) {
        if (reporterMatches == null) {
            return null;
        }
        return reporterMatches.get(index);
    }
}
