package eu.isas.reporter.identifications;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class FdrCalculator {

    private HashMap<Integer, HashMap<Double, Point>> hitMap = new HashMap<Integer, HashMap<Double, Point>>();
    private HashMap<Integer, ArrayList<Double>> eValues = new HashMap<Integer, ArrayList<Double>>();

    /**
     * @TODO: JavaDoc missing
     */
    public FdrCalculator() {

    }

    /**
     * @TODO: JavaDoc missing
     * 
     * @param searchEngine
     * @param eValue
     * @param decoy
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
     * @TODO: JavaDoc missing
     * 
     * @param limit
     * @return
     * @throws Exception
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
     * @TODO: JavaDoc missing
     */
    private class Point {

        public int nTarget = 0;
        public int nDecoy = 0;

        /**
         * @TODO: JavaDoc missing
         */
        public Point() {

        }

        /**
         * @TODO: JavaDoc missing
         * 
         * @param decoy
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
