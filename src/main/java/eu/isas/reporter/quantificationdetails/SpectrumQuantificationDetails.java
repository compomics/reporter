package eu.isas.reporter.quantificationdetails;

import com.compomics.util.experiment.identification.matches.IonMatch;
import java.util.HashMap;

/**
 * This class contains the quantitative information at the spectrum level.
 *
 * @author Marc Vaudel
 */
public class SpectrumQuantificationDetails {

    /**
     * The reporter ratios.
     */
    private HashMap<String, Double> ratios = null;
    /**
     * The reporter ion matches.
     */
    private HashMap<String, IonMatch> reporterMatches = null;
    /**
     * The deisotoped reporter intensities.
     */
    private HashMap<String, Double> deisotopedIntensities = null;

    /**
     * Sets a ratio.
     *
     * @param sampleIndex the index of the sample
     * @param value the value of the ratio
     */
    public void setRatio(String sampleIndex, double value) {

        if (ratios == null) {
            ratios = new HashMap<String, Double>();
        }

        ratios.put(sampleIndex, value);

    }

    /**
     * Returns the ratio of a given sample.
     *
     * @param reporterIonName the index of sample of interest
     *
     * @return the ratio for this sample, null if not set
     */
    public Double getRatio(String reporterIonName) {

        if (ratios == null) {
            return null;
        }

        return ratios.get(reporterIonName);

    }

    /**
     * Sets the value of a deisotoped intensity.
     *
     * @param reporterIonName the index of the sample
     * @param value the value of the deisotoped intensity
     */
    public void setDeisotopedIntensity(String reporterIonName, double value) {

        if (deisotopedIntensities == null) {
            deisotopedIntensities = new HashMap<String, Double>();
        }

        deisotopedIntensities.put(reporterIonName, value);

    }

    /**
     * Returns the intensity of the given sample.
     *
     * @param reporterIonName the index of the sample
     *
     * @return the intensity
     */
    public Double getDeisotopedIntensity(String reporterIonName) {

        if (deisotopedIntensities == null) {
            return null;
        }

        return deisotopedIntensities.get(reporterIonName);

    }

    /**
     * Sets the value of a reporter ion match.
     *
     * @param reporterIonName the index of the sample
     * @param reporterMatch the reporter ion match
     */
    public void setReporterMatch(String reporterIonName, IonMatch reporterMatch) {

        if (reporterMatches == null) {
            reporterMatches = new HashMap<String, IonMatch>();
        }

        reporterMatches.put(reporterIonName, reporterMatch);

    }

    /**
     * Returns the reporter ion match of the given sample. Null if none.
     *
     * @param reporterIonName the index of the sample
     *
     * @return the reporter ion match
     */
    public IonMatch getRepoterMatch(String reporterIonName) {

        if (reporterMatches == null) {
            return null;
        }

        return reporterMatches.get(reporterIonName);

    }
}
