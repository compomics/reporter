package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import java.util.HashMap;

/**
 * This class contains the different reporter scores.
 *
 * @author Marc Vaudel
 */
public class ItraqScore implements UrParameter {

    /**
     * The reporter scores indexed by the reporter ion index.
     */
    private HashMap<Integer, Double> scores = new HashMap<Integer, Double>();

    /**
     * Constructor.
     */
    public ItraqScore() {
    }

    /**
     * Adds a score.
     *
     * @param ion the reporter ion index
     * @param score the corresponding score
     */
    public void addScore(int ion, double score) {
        scores.put(ion, score);
    }

    /**
     * Returns the score corresponding to the selected ion.
     *
     * @param ion the reporter ion
     * @return the corresponding score
     */
    public Double getScore(int ion) {
        return scores.get(ion);
    }

    /**
     * Returns the minimal score.
     *
     * @return the minimal score
     */
    public double getMinScore() {
        double score = 0;
        for (Double currentScore : scores.values()) {
            if (score == 0 || currentScore < score) {
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
