package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;

/**
 * This customizable parameter contains all quantification details.
 *
 * @author Marc Vaudel
 */
public class QuantificationDetails implements UrParameter {

    @Override
    public String getFamilyName() {
        return "Reporter";
    }

    @Override
    public int getIndex() {
        return 2;
    }
}
