/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.reporter.quantificationdetails;

import java.util.HashMap;

/**
 * This class contains the quantitative information at the PSM level.
 *
 * @author Marc
 */
public class PsmQuantificationDetails {
    
    /**
     * The reporter ratios
     */
    private HashMap<Integer, Double> ratios = null;
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
     * Returns the ratio of a given sample. null if not found. null if not found.
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
}
