package eu.isas.reporter.compomicsutilitiessettings;

import com.compomics.util.experiment.utils.UrParameter;

/**
 *
 * @author Marc
 */
public class CompomicsKeysFactory {

    public final static String FAMILY_NAME = "Reporter Parameters";
    public final static int IGNORED_RATIOS = 0;
    public final static int RATIO_LIMITS = 1;
    public final static int SCORE = 2;

    private static CompomicsKeysFactory instance = null;

    private CompomicsKeysFactory() {

    }

    public static CompomicsKeysFactory getInstance() {
        if (instance == null) {
            instance = new CompomicsKeysFactory();
        }
        return instance;
    }

    public UrParameter getKey(int index) {
        switch (index) {
            case IGNORED_RATIOS:
                return new IgnoredRatios();
            case RATIO_LIMITS:
                return new RatioLimits();
            case SCORE:
                return new ItraqScore();
            default:
                return null;
        }
    }
}
