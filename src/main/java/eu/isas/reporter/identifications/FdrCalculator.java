package eu.isas.reporter.identifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class will estimate the FDR
 *
 * @author Marc Vaudel
 */
public class FdrCalculator {

    /**
     * Map of all the target/decoy hits modeled as Point object (private class) indexed by their e-value and search engine (indexed by their compomics index)
     */
    private HashMap<Integer, HashMap<Double, Point>> hitMap = new HashMap<Integer, HashMap<Double, Point>>();
    /**
     * list of the e-values for each search engine
     */
    private HashMap<Integer, ArrayList<Double>> eValues = new HashMap<Integer, ArrayList<Double>>();

    /**
     * constructor
     */
    public FdrCalculator() {

    }

    /**
     * Adds a target/decoy hit to the data loaded
     * 
     * @param searchEngine  The search engine being used
     * @param eValue        The e-value
     * @param decoy         boolean indicating whether the hit is a target (false) or a decoy (true) hit
     */
    public void addHit(int searchEngine, double eValue, boolean decoy) {
        if (hitMap.get(searchEngine)==null) {
            hitMap.put(searchEngine, new HashMap<Double, Point>());
            eValues.put(searchEngine, new ArrayList<Double>());
        }
        if (!eValues.get(searchEngine).contains(eValue)) {
            eValues.get(searchEngine).add(eValue);
            hitMap.get(searchEngine).put(eValue, new Point(decoy));
        } else {
            if (decoy) {
                hitMap.get(searchEngine).get(eValue).nDecoy++;
            } else {
                hitMap.get(searchEngine).get(eValue).nTarget++;
            }
        }
    }

    /**
     * returns the e-values thresholds corresponding to the given FDR limit
     * 
     * @param limit         FDR threshold
     * @return  map of the e-value limits indexed by their search engines
     * @throws Exception    exception thrown when the best hit is a decoy hit
     */
    public HashMap<Integer, Double> getEvalueLimits(double limit) throws Exception {
        HashMap<Integer, Double> results = new HashMap<Integer, Double>();
        for (int searchEngine : eValues.keySet()) {
            Collections.sort(eValues.get(searchEngine));
        }
        for (int searchEngine : eValues.keySet()) {
            double bestEValue = 100;
            int nTarget = 0;
            int nDecoy = 0;
            Point point;
            for (double eValue : eValues.get(searchEngine)) {
                point = hitMap.get(searchEngine).get(eValue);
                nTarget += point.nTarget;
                nDecoy += point.nDecoy;
                try {
                    if (nDecoy/nTarget < limit) {
                        bestEValue = eValue;
                    }
                } catch (Exception e) {
                    throw new Exception("The best hit is a decoy hit.");
                }
            }
            results.put(searchEngine, bestEValue);
        }
        return results;
    }

    /**
     * private class to model a target/decoy hit
     */
    private class Point {

        /**
         * Number of target identifications
         */
        public int nTarget = 0;
        /**
         * Number of decoy identifications
         */
        public int nDecoy = 0;

        /**
         * constructor
         */
        public Point() {

        }

        /**
         * constructor for a target/decoy hit
         * 
         * @param decoy boolean indicating if the hit is target (false) or decoy (true)
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
