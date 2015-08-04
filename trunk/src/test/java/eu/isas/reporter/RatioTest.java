package eu.isas.reporter;

import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.myparameters.ReporterPreferences;
import junit.framework.TestCase;

/**
 *
 * @author Marc
 */
public class RatioTest extends TestCase {
    
    public void testRatioEystein() {
        try {
        ReporterPreferences reporterPreferences = ReporterPreferences.getUserPreferences();
        double[] ratios = new double[]{-0.311194748, -0.311194748, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996, -0.301029996};
             double result = RatioEstimator.mEstimate(reporterPreferences.getRatioEstimationSettings(), ratios);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
