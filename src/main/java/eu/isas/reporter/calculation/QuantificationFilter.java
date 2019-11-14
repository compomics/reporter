package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.enzymes.Enzyme;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.parameters.identification.search.DigestionParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import eu.isas.reporter.settings.RatioEstimationSettings;
import java.io.IOException;
import java.sql.SQLException;

/**
 * This class indicates whether identification features or ratios are valid
 * according to the user settings.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
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
     */
    public static boolean isPsmValid(RatioEstimationSettings ratioEstimationSettings, Identification identification, long matchKey) {
        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getSpectrumMatch(matchKey).getUrParam(psParameter);
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
     */
    public static boolean isPeptideValid(RatioEstimationSettings ratioEstimationSettings, Identification identification,
            SearchParameters searchParameters, PeptideMatch peptideMatch) {

        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getPeptideMatch(peptideMatch.getKey()).getUrParam(psParameter);
        if (psParameter.getMatchValidationLevel().getIndex() < ratioEstimationSettings.getPeptideValidationLevel().getIndex()) {
            return false;
        }

        // check enzymaticity
        Peptide peptide = peptideMatch.getPeptide();
        DigestionParameters digestionParameters = searchParameters.getDigestionParameters();
        if (digestionParameters.getCleavageParameter() == DigestionParameters.CleavageParameter.enzyme) {
            Integer minMissedCleavages = null;
            for (Enzyme enzyme : digestionParameters.getEnzymes()) {
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
        if (peptide.getNVariableModifications() > 0) {
            for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {
                if (ratioEstimationSettings.getExcludingPtms().contains(modificationMatch.getModification())) {
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
            long matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException {
        // check match validation
        PSParameter psParameter = new PSParameter();
        psParameter = (PSParameter) identification.getProteinMatch(matchKey).getUrParam(psParameter);
        if (psParameter.getMatchValidationLevel().getIndex() < ratioEstimationSettings.getProteinValidationLevel().getIndex()) {
            return false;
        }
        return true;
    }
}
