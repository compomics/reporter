package eu.isas.reporter;

import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.myparameters.ReporterPreferences;
import eu.isas.reporter.myparameters.ReporterSettings;
import junit.framework.TestCase;

/**
 * Ratio test.
 * 
 * @author Marc Vaudel
 */
public class RatioTest extends TestCase {

    public void testRatioEystein() {
        try {
            ReporterSettings reporterSettings = new ReporterSettings();
            double[] ratios = new double[]{-0.311194748, -0.311194748, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996};
            double result = RatioEstimator.mEstimate(reporterSettings.getRatioEstimationSettings(), ratios);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
