package eu.isas.reporter.compomicsutilitiessettings;

import com.compomics.util.experiment.utils.UrParameter;
import java.util.HashMap;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class IgnoredRatios implements UrParameter {

    private HashMap<Integer, Boolean> ignoredRatios = new HashMap<Integer, Boolean>();

    /**
     * @TODO: JavaDoc missing
     */
    public IgnoredRatios() {

    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param reporterIon
     */
    public void ignore(int reporterIon) {
        ignoredRatios.put(reporterIon, true);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param reporterIon
     */
    public void account(int reporterIon) {
        ignoredRatios.put(reporterIon, false);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param reporterIon
     * @return
     */
    public boolean isIgnored(int reporterIon) {
        if (ignoredRatios.get(reporterIon) == null) {
            return false;
        } else {
            return ignoredRatios.get(reporterIon);
        }
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
        return 0;
    }
}
