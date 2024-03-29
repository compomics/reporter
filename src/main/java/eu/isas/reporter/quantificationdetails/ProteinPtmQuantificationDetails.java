package eu.isas.reporter.quantificationdetails;

import java.util.HashMap;

/**
 * This class contains the quantitative information at the GO level.
 *
 * @author Marc Vaudel
 */
public class ProteinPtmQuantificationDetails {

    /**
     * The reporter ratios.
     */
    private HashMap<String, Double> ratios = null;

    /**
     * Sets a ratio.
     *
     * @param reporterIonName the index of the sample
     * @param value the value of the ratio
     */
    public void setRatio(String reporterIonName, double value) {

        if (ratios == null) {
            ratios = new HashMap<String, Double>();
        }

        ratios.put(reporterIonName, value);

    }

    /**
     * Returns the ratio of a given sample. null if not found.
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
}
