package eu.isas.reporter.compomicsutilitiessettings;

import com.compomics.util.experiment.utils.UrParameter;
import java.util.HashMap;

/**
 *
 * @author Marc
 */
public class ItraqScore implements UrParameter {

    private HashMap<Integer, Double> scores = new HashMap<Integer, Double>();

    public ItraqScore() {

    }

    public void addScore(int ion, double score) {
        scores.put(ion, score);
    }

    public Double getScore(int ion) {
        return scores.get(ion);
    }

    public Double getMinScore() {
        Double score = null;
        for (Double currentScore : scores.values()) {
            if (score == null || currentScore < score) {
                score = currentScore;
            }
        }
        return score;
    }

    public String getFamilyName() {
        return "Reporter";
    }

    public int getIndex() {
        return 1;
    }
}
