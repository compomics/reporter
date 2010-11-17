package eu.isas.reporter.compomicsutilitiessettings;

import com.compomics.util.experiment.utils.UrParameter;
import java.util.HashMap;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class RatioLimits implements UrParameter {

    private HashMap<Integer, Double> limitInf = new HashMap<Integer, Double>();
    private HashMap<Integer, Double> limitSup = new HashMap<Integer, Double>();

    /**
     * @TODO: JavaDoc missing
     */
    public RatioLimits() {
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param ion
     * @param limitInf
     * @param limitSup
     */
    public void addLimits(int ion, double limitInf, double limitSup) {
        this.limitInf.put(ion, limitInf);
        this.limitSup.put(ion, limitSup);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param ion
     * @return
     */
    public double[] getLimits(int ion) {
        return new double[] {limitInf.get(ion), limitSup.get(ion)};
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
        return 2;
    }
}
