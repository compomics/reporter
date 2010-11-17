package eu.isas.reporter.compomicsutilitiessettings;

import com.compomics.util.experiment.utils.UrParameter;
import java.util.HashMap;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class ItraqScore implements UrParameter {

    private HashMap<Integer, Double> scores = new HashMap<Integer, Double>();

    /**
     * @TODO: JavaDoc missing
     */
    public ItraqScore() {

    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param ion
     * @param score
     */
    public void addScore(int ion, double score) {
        scores.put(ion, score);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param ion
     * @return
     */
    public Double getScore(int ion) {
        return scores.get(ion);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
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

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public String getFamilyName() {
        return "Reporter";
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public int getIndex() {
        return 1;
    }
}
