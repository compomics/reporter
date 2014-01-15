/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.reporter.myparameters.ReporterPreferences;
import java.io.IOException;
import java.sql.SQLException;

/**
 * This class indicates whether identification features or ratios are valid
 * according to the user settings
 *
 * @author Marc
 */
public class QuantificationFilter {

    /**
     * Filters out NaN and 0 ratios
     *
     * @param reporterPreferences the user quantification preferences
     * @param ratio the ratio of interest
     *
     * @return true if the ratio is not NaN and should be accounted for
     * according to the user settings
     */
    public static boolean isRatioValid(ReporterPreferences reporterPreferences, Double ratio) {
        return ratio != Double.NaN && (!reporterPreferences.isIgnoreNullIntensities() || ratio > 0);
    }

    /**
     * Filters the psms to be used for quantification according to the user
     * quantification preferences
     *
     * @param reporterPreferences the user quantification preferences
     * @param identification the identification where to get the information
     * from
     * @param matchKey the key of the match of interest
     *
     * @return true if the PSM can be used for quantification
     * 
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public static boolean isPsmValid(ReporterPreferences reporterPreferences, Identification identification, String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getSpectrumMatchParameter(matchKey, psParameter);
        if (psParameter.getMatchValidationLevel().getIndex() < reporterPreferences.getPsmValidationLevel().getIndex()) {
            return false;
        }
        return true;
    }

    /**
     * Filters the peptide to be used for quantification according to the user
     * quantification preferences
     *
     * @param reporterPreferences the user quantification preferences
     * @param identification the identification where to get the information
     * from
     * @param searchParameters the identification parameters
     * @param matchKey the key of the match of interest
     *
     * @return true if the PSM can be used for quantification
     * 
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public static boolean isPeptideValid(ReporterPreferences reporterPreferences, Identification identification, SearchParameters searchParameters, String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getPeptideMatchParameter(matchKey, psParameter);
        if (psParameter.getMatchValidationLevel().getIndex() < reporterPreferences.getPeptideValidationLevel().getIndex()) {
            return false;
        }
        // check enzymaticity
        PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);
        Peptide peptide = peptideMatch.getTheoreticPeptide();
        Enzyme enzyme = searchParameters.getEnzyme();
        if (!enzyme.isSemiSpecific()) {
            int nMissedCleavages = peptide.getNMissedCleavages(searchParameters.getEnzyme());
            if (reporterPreferences.isIgnoreMissedCleavages() && nMissedCleavages > 0) {
                return false;
            }
        }
        // check modifications
        for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
            if (reporterPreferences.getexcludingPtms().contains(modificationMatch.getTheoreticPtm())) {
                return false;
            }
        }
        return true;
    }

    /**
     * Filters the protein to be used for quantification according to the user
     * quantification preferences
     *
     * @param reporterPreferences the user quantification preferences
     * @param identification the identification where to get the information
     * from
     * @param matchKey the key of the match of interest
     *
     * @return true if the PSM can be used for quantification
     * 
     * @throws java.sql.SQLException
     * @throws java.io.IOException
     * @throws java.lang.ClassNotFoundException
     * @throws java.lang.InterruptedException
     */
    public static boolean isProteinValid(ReporterPreferences reporterPreferences, Identification identification, String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getProteinMatchParameter(matchKey, psParameter);
        if (psParameter.getMatchValidationLevel().getIndex() < reporterPreferences.getProteinValidationLevel().getIndex()) {
            return false;
        }
        return true;
    }
}
