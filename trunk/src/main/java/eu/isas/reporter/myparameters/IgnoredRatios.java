package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import java.util.HashMap;

/**
 * This class will be used to flag ignored ratios.
 *
 * @author Marc Vaudel
 */
public class IgnoredRatios implements UrParameter {

    /**
     * List of ignored ratios.
     */
    private HashMap<Integer, Boolean> ignoredRatios = new HashMap<Integer, Boolean>();

    /**
     * Constructor.
     */
    public IgnoredRatios() {
    }

    /**
     * Method used to ignore a specific ratio.
     *
     * @param reporterIon the reporter ion on which the calculation was done
     */
    public void ignore(int reporterIon) {
        ignoredRatios.put(reporterIon, true);
    }

    /**
     * Method used to account a specific ratio.
     *
     * @param reporterIon the reporter ion on which the calculation was done
     */
    public void account(int reporterIon) {
        ignoredRatios.put(reporterIon, false);
    }

    /**
     * Method indicating whether a reporter ion should be ignored for the
     * calculation.
     *
     * @param reporterIon the reporter ion of interest
     * @return a boolean indicating whether the ratio should be ignored
     */
    public boolean isIgnored(int reporterIon) {
        if (ignoredRatios.get(reporterIon) == null) {
            return false;
        } else {
            return ignoredRatios.get(reporterIon);
        }
    }

    @Override
    public String getFamilyName() {
        return "Reporter";
    }

    @Override
    public int getIndex() {
        return 0;
    }
}
