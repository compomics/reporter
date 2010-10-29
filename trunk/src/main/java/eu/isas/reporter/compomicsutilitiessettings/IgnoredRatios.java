package eu.isas.reporter.compomicsutilitiessettings;

import com.compomics.util.experiment.utils.UrParameter;
import java.util.HashMap;

/**
 *
 * @author Marc
 */
public class IgnoredRatios implements UrParameter {

    private HashMap<Integer, Boolean> ignoredRatios = new HashMap<Integer, Boolean>();

    public IgnoredRatios() {

    }

    public void ignore(int reporterIon) {
        ignoredRatios.put(reporterIon, true);
    }

    public void account(int reporterIon) {
        ignoredRatios.put(reporterIon, false);
    }

    public boolean isIgnored(int reporterIon) {
        if (ignoredRatios.get(reporterIon) == null) {
            return false;
        } else {
            return ignoredRatios.get(reporterIon);
        }
    }

    public String getFamilyName() {
        return CompomicsKeysFactory.FAMILY_NAME;
    }

    public int getIndex() {
        return CompomicsKeysFactory.IGNORED_RATIOS;
    }
}
