package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import java.util.HashMap;

/**
 * This class will contain the limits of the considered ratios.
 *
 * @author Marc Vaudel
 */
public class RatioLimits implements UrParameter {

    /**
     * Lower limits indexed by the reporter ion index.
     */
    private HashMap<Integer, Double> limitInf = new HashMap<Integer, Double>();
    /**
     * Higher limit indexed by the reporter ion index.
     */
    private HashMap<Integer, Double> limitSup = new HashMap<Integer, Double>();

    /**
     * Constructor.
     */
    public RatioLimits() {
    }

    /**
     * Adds limits for the considered reporter ion.
     *
     * @param ion the reporter ion
     * @param limitInf the lower limit
     * @param limitSup the upper limit
     */
    public void addLimits(int ion, double limitInf, double limitSup) {
        this.limitInf.put(ion, limitInf);
        this.limitSup.put(ion, limitSup);
    }

    /**
     * Returns the limits for the considered reporter ion.
     *
     * @param ion the reporter ion
     * @return the lower and upper limit
     */
    public double[] getLimits(int ion) {
        try {
            return new double[]{limitInf.get(ion), limitSup.get(ion)};
        } catch (Exception e) {
            return new double[]{0.1, 10};
        }
    }

    @Override
    public String getFamilyName() {
        return "Reporter";
    }

    @Override
    public int getIndex() {
        return 2;
    }
}
