package eu.isas.reporter.compomicsutilitiessettings;

import com.compomics.util.experiment.utils.UrParameter;
import java.util.HashMap;

/**
 *
 * @author Marc
 */
public class RatioLimits implements UrParameter {

    private HashMap<Integer, Double> limitInf = new HashMap<Integer, Double>();
    private HashMap<Integer, Double> limitSup = new HashMap<Integer, Double>();

    public RatioLimits() {
    }

    public void addLimits(int ion, double limitInf, double limitSup) {
        this.limitInf.put(ion, limitInf);
        this.limitSup.put(ion, limitSup);
    }

    public double[] getLimits(int ion) {
        return new double[] {limitInf.get(ion), limitSup.get(ion)};
    }

    public String getFamilyName() {
        return CompomicsKeysFactory.FAMILY_NAME;
    }

    public int getIndex() {
        return CompomicsKeysFactory.RATIO_LIMITS;
    }

}
