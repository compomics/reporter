package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.preferences.DigestionPreferences;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.reporter.settings.RatioEstimationSettings;
import java.io.IOException;
import java.sql.SQLException;

/**
 * This class indicates whether identification features or ratios are valid
 * according to the user settings.
 *
 * @author Marc Vaudel
 */
public class QuantificationFilter {

    /**
     * Filters out NaN and 0 ratios.
     *
     * @param ratioEstimationSettings the ratio estimation settings
     * @param ratio the ratio of interest
     *
     * @return true if the ratio is not NaN and should be accounted for
     * according to the user settings
     */
    public static boolean isRatioValid(RatioEstimationSettings ratioEstimationSettings, Double ratio) {
        return !ratio.isNaN() && (!ratioEstimationSettings.isIgnoreNullIntensities() || ratio > 0);
    }

    /**
     * Filters the PSMs to be used for quantification according to the user
     * quantification preferences.
     *
     * @param ratioEstimationSettings the ratio estimation settings
     * @param identification the identification where to get the information
     * from
     * @param matchKey the key of the match of interest
     *
     * @return true if the PSM can be used for quantification
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     */
    public static boolean isPsmValid(RatioEstimationSettings ratioEstimationSettings, Identification identification, String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getSpectrumMatchParameter(matchKey, psParameter);
        if (psParameter.getMatchValidationLevel().getIndex() < ratioEstimationSettings.getPsmValidationLevel().getIndex()) {
            return false;
        }
        return true;
    }

    /**
     * Filters the peptide to be used for quantification according to the user
     * quantification preferences.
     *
     * @param ratioEstimationSettings the ratio estimation settings
     * @param identification the identification where to get the information
     * from
     * @param searchParameters the identification parameters
     * @param peptideMatch the match of interest
     *
     * @return true if the PSM can be used for quantification
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     */
    public static boolean isPeptideValid(RatioEstimationSettings ratioEstimationSettings, Identification identification,
            SearchParameters searchParameters, PeptideMatch peptideMatch) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideMatch.getKey(), psParameter);
        if (psParameter.getMatchValidationLevel().getIndex() < ratioEstimationSettings.getPeptideValidationLevel().getIndex()) {
            return false;
        }

        // check enzymaticity
        Peptide peptide = peptideMatch.getTheoreticPeptide();
        DigestionPreferences digestionPreferences = searchParameters.getDigestionPreferences();
        if (digestionPreferences.getCleavagePreference() == DigestionPreferences.CleavagePreference.enzyme) {
            Integer minMissedCleavages = null;
            for (Enzyme enzyme : digestionPreferences.getEnzymes()) {
                int nMissedCleavages = peptide.getNMissedCleavages(enzyme);
                if (minMissedCleavages == null || nMissedCleavages < minMissedCleavages) {
                    minMissedCleavages = nMissedCleavages;
                }
            }
            if (minMissedCleavages != null && minMissedCleavages > 0 && ratioEstimationSettings.isIgnoreMissedCleavages()) {
                return false;
            }
        }

        // check modifications
        if (peptide.isModified()) {
            for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                if (ratioEstimationSettings.getExcludingPtms().contains(modificationMatch.getTheoreticPtm())) {
                    return false;
                }
            }
        }

        return true;
    }

    /**
     * Filters the protein to be used for quantification according to the user
     * quantification preferences.
     *
     * @param ratioEstimationSettings the ratio estimation settings
     * @param identification the identification where to get the information
     * from
     * @param matchKey the key of the match of interest
     *
     * @return true if the PSM can be used for quantification
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     */
    public static boolean isProteinValid(RatioEstimationSettings ratioEstimationSettings, Identification identification,
            String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getProteinMatchParameter(matchKey, psParameter);
        if (psParameter.getMatchValidationLevel().getIndex() < ratioEstimationSettings.getProteinValidationLevel().getIndex()) {
            return false;
        }
        return true;
    }
}
