/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;

/**
 * this customizable parameter contains all quantification details
 *
 * @author marc
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
