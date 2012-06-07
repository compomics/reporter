package eu.isas.reporter.identifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class will estimate the FDR.
 *
 * @author Marc Vaudel
 */
public class FdrCalculator {

    /**
     * Map of all the target/decoy hits modeled as Point object (private class)
     * indexed by their e-value.
     */
    private HashMap<Double, Point> hitMap = new HashMap<Double, Point>();
    /**
     * List of the e-values.
     */
    private ArrayList<Double> eValues = new ArrayList<Double>();
    /**
     * The total number of target hits after processing.
     */
    private int nTargetTotal;

    /**
     * Constructor.
     */
    public FdrCalculator() {
    }

    /**
     * Adds a target/decoy hit to the data loaded.
     *
     * @param eValue The e-value
     * @param decoy boolean indicating whether the hit is a target (false) or a
     * decoy (true) hit
     */
    public void addHit(double eValue, boolean decoy) {
        if (!eValues.contains(eValue)) {
            eValues.add(eValue);
            hitMap.put(eValue, new Point(decoy));
        } else {
            if (decoy) {
                hitMap.get(eValue).nDecoy++;
            } else {
                hitMap.get(eValue).nTarget++;
            }
        }
    }

    /**
     * Returns the e-value threshold corresponding to the given FDR limit.
     *
     * @param threshold FDR threshold
     * @return the e-value limit
     * @throws IllegalArgumentException exception thrown when the best hit is a decoy hit
     */
    public Double getEvalueLimit(double threshold) throws IllegalArgumentException {
        
        Collections.sort(eValues);
        double bestEValue = eValues.get(eValues.size() - 1);
        double nTarget = 0.0;
        int nDecoy = 0;
        
        for (double eValue : eValues) {
            
            Point point = hitMap.get(eValue);
            nTarget += point.nTarget;
            nDecoy += point.nDecoy;
            
            try {
                if (nDecoy / nTarget < threshold) {
                    bestEValue = eValue;
                }
            } catch (Exception e) {
                throw new IllegalArgumentException("The best hit is a decoy hit.");
            }
        }
        
        nTargetTotal = (int) nTarget;
        return bestEValue;
    }

    /**
     * Returns the total number of target hits at the last given threshold.
     *
     * @return the total number of target hits at the last given threshold
     */
    public int getNTargetTotal() {
        return nTargetTotal;
    }

    /**
     * Private class to model a target/decoy hit.
     */
    private class Point {

        /**
         * Number of target identifications.
         */
        public int nTarget = 0;
        /**
         * Number of decoy identifications.
         */
        public int nDecoy = 0;

        /**
         * Constructor.
         */
        public Point() {
        }

        /**
         * Constructor for a target/decoy hit.
         *
         * @param decoy boolean indicating if the hit is target (false) or decoy
         * (true)
         */
        public Point(boolean decoy) {
            if (decoy) {
                nDecoy++;
            } else {
                nTarget++;
            }
        }
    }
}
