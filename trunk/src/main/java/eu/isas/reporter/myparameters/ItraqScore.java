package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import java.util.HashMap;

/**
 * This class contains the different reporter scores
 *
 * @author Marc Vaudel
 */
public class ItraqScore implements UrParameter {

    /**
     * the reporter scores indexed by the reporter ion index
     */
    private HashMap<Integer, Double> scores = new HashMap<Integer, Double>();

    /**
     * constructor
     */
    public ItraqScore() {

    }

    /**
     * adds a score
     *
     * @param ion   the reporter ion index
     * @param score the corresponding score
     */
    public void addScore(int ion, double score) {
        scores.put(ion, score);
    }

    /**
     * returns the score corresponding to the selected ion
     *
     * @param ion the reporter ion
     * @return the corresponding score
     */
    public Double getScore(int ion) {
        return scores.get(ion);
    }

    /**
     * returns the minimal score
     *
     * @return the minimal score
     */
    public Double getMinScore() {
        Double score = null;
        for (Double currentScore : scores.values()) {
            if (score == null || currentScore < score) {
                score = currentScore;
            }
        }
        return score;
    }

    @Override
    public String getFamilyName() {
        return "Reporter";
    }

    @Override
    public int getIndex() {
        return 1;
    }
}
