package eu.isas.reporter.myparameters;

import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.util.ArrayList;

/**
 * Settings for the estimation of a peptide or protein ratio.
 *
 * @author Marc Vaudel
 */
public class RatioEstimationSettings {

    /**
     * Boolean indicating whether spectra presenting null intensities should be
     * ignored.
     */
    private boolean ignoreNullIntensities = false;
    /**
     * Boolean indicating whether peptides presenting missed cleavages should be
     * ignored.
     */
    private boolean ignoreMissedCleavages = false;
    /**
     * Percentage of ratios to consider for the likelihood estimator window
     * setting.
     */
    private double percentile = 68;
    /**
     * Ratio resolution.
     */
    private double ratioResolution = 0.01;
    /**
     * List of PTMs to exclude. Peptides presenting these PTMs will not be
     * accounted for during quantification.
     */
    private ArrayList<String> excludingPTM = new ArrayList<String>();
    /**
     * The validation threshold to use for protein quantification.
     */
    private MatchValidationLevel proteinValidation = MatchValidationLevel.confident;
    /**
     * The validation threshold to use for peptide quantification.
     */
    private MatchValidationLevel peptideValidation = MatchValidationLevel.confident;
    /**
     * The validation threshold to use for PSM quantification.
     */
    private MatchValidationLevel psmValidation = MatchValidationLevel.confident;
    
    /**
     * Constructor
     */
    public RatioEstimationSettings() {
        
    }
    
    /**
     * Returns a boolean indicating whether miscleaved peptides should be
     * ignored.
     *
     * @return a boolean indicating whether miscleaved peptides should be
     * ignored
     */
    public boolean isIgnoreMissedCleavages() {
        return ignoreMissedCleavages;
    }

    /**
     * Sets whether miscleaved peptides should be ignored.
     *
     * @param ignoreMissedCleavages a boolean indicating whether miscleaved
     * peptides should be ignored
     */
    public void setIgnoreMissedCleavages(boolean ignoreMissedCleavages) {
        this.ignoreMissedCleavages = ignoreMissedCleavages;
    }

    /**
     * Returns a boolean indicating whether null intensities should be ignored.
     *
     * @return a boolean indicating whether null intensities should be ignored
     */
    public boolean isIgnoreNullIntensities() {
        return ignoreNullIntensities;
    }

    /**
     * Sets whether null intensities should be ignored.
     *
     * @param ignoreNullIntensities a boolean indicating whether null
     * intensities should be ignored
     */
    public void setIgnoreNullIntensities(boolean ignoreNullIntensities) {
        this.ignoreNullIntensities = ignoreNullIntensities;
    }

    /**
     * Returns the share in percent of ratios considered to set the width of the
     * maximum likelihood estimators.
     *
     * @return the share in percent of ratios considered to set the width of the
     * maximum likelihood estimators
     */
    public double getPercentile() {
        return percentile;
    }

    /**
     * Sets the share in percent of ratios considered to set the width of the
     * maximum likelihood estimators.
     *
     * @param percentile the share in percent of ratios considered to set the
     * width of the maximum likelihood estimators
     */
    public void setPercentile(double percentile) {
        this.percentile = percentile;
    }

    /**
     * Returns the ratio resolution.
     *
     * @return the ratio resolution
     */
    public double getRatioResolution() {
        return ratioResolution;
    }

    /**
     * Sets the ratio resolution.
     *
     * @param ratioResolution the ratio resolution
     */
    public void setRatioResolution(double ratioResolution) {
        this.ratioResolution = ratioResolution;
    }

    /**
     * Returns the list of PTMs to ignore.
     *
     * @return the list of PTMs to ignore
     */
    public ArrayList<String> getexcludingPtms() {
        return excludingPTM;
    }

    /**
     * Adds an excluding PTM.
     *
     * @param ptmName the name of the excluding ptm
     */
    public void addExcludingPtm(String ptmName) {
        this.excludingPTM.add(ptmName);
    }

    /**
     * Empty the list of ignored PTMs.
     */
    public void emptyPTMList() {
        excludingPTM.clear();
    }

    /**
     * Indicates the validation level to require for protein quantification.
     *
     * @return the minimal validation level for a protein to be accounted for
     */
    public MatchValidationLevel getProteinValidationLevel() {
        return proteinValidation;
    }

    /**
     * Sets the validation level to require for quantification.
     *
     * @param matchValidationLevel the minimal validation level for a protein to
     * be accounted for
     */
    public void setProteinValidationLevel(MatchValidationLevel matchValidationLevel) {
        this.proteinValidation = matchValidationLevel;
    }

    /**
     * Indicates the validation level to require for peptide quantification.
     *
     * @return the minimal validation level for a protein to be accounted for
     */
    public MatchValidationLevel getPeptideValidationLevel() {
        return peptideValidation;
    }

    /**
     * Sets the validation level to require for quantification.
     *
     * @param matchValidationLevel the minimal validation level for a peptide to
     * be accounted for
     */
    public void setPeptideValidationLevel(MatchValidationLevel matchValidationLevel) {
        this.peptideValidation = matchValidationLevel;
    }

    /**
     * Indicates the validation level to require for PSM quantification.
     *
     * @return the minimal validation level for a protein to be accounted for
     */
    public MatchValidationLevel getPsmValidationLevel() {
        return psmValidation;
    }

    /**
     * Sets the validation level to require for quantification.
     *
     * @param matchValidationLevel the minimal validation level for a PSM to be
     * accounted for
     */
    public void setPsmValidationLevel(MatchValidationLevel matchValidationLevel) {
        this.psmValidation = matchValidationLevel;
    }
    
    
}
