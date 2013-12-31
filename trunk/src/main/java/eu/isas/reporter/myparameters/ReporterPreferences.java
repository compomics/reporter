package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.io.SerializationUtils;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.io.File;
import java.util.ArrayList;

/**
 * This class contains the quantification options set by the user.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterPreferences implements UrParameter {

    /**
     * fixed serial number for serialization backward compatibility
     */
    static final long serialVersionUID = 3054710627326315328L;
    /**
     * Location of the user preferences file.
     */
    public static final String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.reporter/userpreferences.cup";
    /////////////////////
    // Import parameters
    /////////////////////
    /*
     * Tolerance for reporter ion matching.
     */
    private double ReporterIonsMzTolerance = 0.01;
    /**
     * Quantification and identification are conducted on the same spectra
     * (identification files import only).
     */
    private boolean sameSpectra = true;
    /**
     * Precursor mz tolerance used to link quantification to identifications in
     * case these are not recorded on the same spectra. (identification files
     * import only)
     */
    private double precursorMzTolerance = 1;
    /**
     * Indicates if the precursor mz tolerance in ppm
     */
    private boolean precursorMzPpm;
    /**
     * Precursor RT tolerance used to link quantification to identifications in
     * case these are not recorded on the same spectra. (identification files
     * import only)
     */
    private double precursorRTTolerance = 10;
    //////////////////////////
    //  Processing parameters
    //////////////////////////
    /**
     * Boolean indicating whether spectra presenting null intensities should be
     * ignored.
     */
    private boolean ignoreNullIntensities = true;
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
     * List of PTMs to exclude. Peptides presenting these ptms will not be
     * accounted for during quantification.
     */
    private ArrayList<String> excludingPTM = new ArrayList<String>();
    /**
     * The validation threshold to use for protein quantification
     */
    private MatchValidationLevel proteinValidation = MatchValidationLevel.confident;
    /**
     * The validation threshold to use for peptide quantification
     */
    private MatchValidationLevel peptideValidation = MatchValidationLevel.confident;
    /**
     * The validation threshold to use for psm quantification
     */
    private MatchValidationLevel psmValidation = MatchValidationLevel.confident;

    /**
     * Constructor.
     */
    private ReporterPreferences() {
    }

    /**
     * Returns the tolerance used to match reporter ions.
     *
     * @return the tolerance used to match reporter ions
     */
    public double getReporterIonsMzTolerance() {
        return ReporterIonsMzTolerance;
    }

    /**
     * Sets the tolerance used to match reporter ions.
     *
     * @param ReporterIonsMzTolerance the tolerance used to match reporter ions
     */
    public void setReporterIonsMzTolerance(double ReporterIonsMzTolerance) {
        this.ReporterIonsMzTolerance = ReporterIonsMzTolerance;
    }

    /**
     * Returns the precursor m/Z tolerance used to match quantification spectra
     * to identification spectra.
     *
     * @return the precursor m/Z tolerance used to match quantification spectra
     * to identification spectra
     */
    public double getPrecursorMzTolerance() {
        return precursorMzTolerance;
    }

    /**
     * Sets the precursor m/z tolerance used to match quantification spectra to
     * identification spectra.
     *
     * @param precursorMzTolerance the precursor m/z tolerance used to match
     * quantification spectra to identification spectra
     */
    public void setPrecursorMzTolerance(double precursorMzTolerance) {
        this.precursorMzTolerance = precursorMzTolerance;
    }

    /**
     * Returns the precursor RT tolerance used to match quantification spectra
     * to identification spectra.
     *
     * @return the precursor RT tolerance used to match quantification spectra
     * to identification spectra
     */
    public double getPrecursorRTTolerance() {
        return precursorRTTolerance;
    }

    /**
     * Sets the precursor RT tolerance used to match quantification spectra to
     * identification spectra.
     *
     * @param precursorRTTolerance the precursor RT tolerance used to match
     * quantification spectra to identification spectra
     */
    public void setPrecursorRTTolerance(double precursorRTTolerance) {
        this.precursorRTTolerance = precursorRTTolerance;
    }

    /**
     * Returns a boolean indicating whether identification and quantification
     * are performed on the same spectra.
     *
     * @return a boolean indicating whether identification and quantification
     * are performed on the same spectra
     */
    public boolean isSameSpectra() {
        return sameSpectra;
    }

    /**
     * Sets whether identification and quantification are performed on the same
     * spectra.
     *
     * @param sameSpectra whether identification and quantification are
     * performed on the same spectra
     */
    public void setSameSpectra(boolean sameSpectra) {
        this.sameSpectra = sameSpectra;
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
     * Indicates whether the precursor matching tolerance is in ppm or in m/z
     *
     * @return true if ppm
     */
    public boolean isPrecursorMzPpm() {
        return precursorMzPpm;
    }

    /**
     * Sets whether the precursor matching tolerance is in ppm or in m/z
     *
     * @param precursorMzPpm true for ppm
     */
    public void setPrecursorMzPpm(boolean precursorMzPpm) {
        this.precursorMzPpm = precursorMzPpm;
    }

    /**
     * Convenience method saving the user preferences.
     *
     * @param userPreferences the new user preferences
     */
    public static void saveUserPreferences(ReporterPreferences userPreferences) {

        try {
            File file = new File(USER_PREFERENCES_FILE);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            SerializationUtils.writeObject(userPreferences, file);
        } catch (Exception e) {
            System.err.println("An error occurred while saving " + USER_PREFERENCES_FILE + " (see below).");
            e.printStackTrace();
        }
    }

    /**
     * Loads the quantification user preferences. If an error is encountered,
     * preferences are set back to default.
     *
     * @return returns the utilities user preferences
     */
    public static ReporterPreferences getUserPreferences() {
        ReporterPreferences userPreferences;
        File file = new File(USER_PREFERENCES_FILE);

        if (!file.exists()) {
            userPreferences = new ReporterPreferences();
            ReporterPreferences.saveUserPreferences(userPreferences);
        } else {
            try {
                userPreferences = (ReporterPreferences) SerializationUtils.readObject(file);
            } catch (Exception e) {
                System.err.println("An error occurred while loading " + USER_PREFERENCES_FILE + " (see below). Preferences set back to default.");
                e.printStackTrace();
                userPreferences = new ReporterPreferences();
                ReporterPreferences.saveUserPreferences(userPreferences);
            }
        }

        return userPreferences;
    }

    /**
     * Indicates the validation level to require for protein quantification
     *
     * @return the minimal validation level for a protein to be accounted for
     */
    public MatchValidationLevel getProteinValidationLevel() {
        return proteinValidation;
    }

    /**
     * Sets the validation level to require for quantification
     *
     * @param matchValidationLevel the minimal validation level for a protein to
     * be accounted for
     */
    public void setProteinValidationLevel(MatchValidationLevel matchValidationLevel) {
        this.proteinValidation = matchValidationLevel;
    }

    /**
     * Indicates the validation level to require for peptide quantification
     *
     * @return the minimal validation level for a protein to be accounted for
     */
    public MatchValidationLevel getPeptideValidationLevel() {
        return peptideValidation;
    }

    /**
     * Sets the validation level to require for quantification
     *
     * @param matchValidationLevel the minimal validation level for a peptide to
     * be accounted for
     */
    public void setPeptideValidationLevel(MatchValidationLevel matchValidationLevel) {
        this.peptideValidation = matchValidationLevel;
    }

    /**
     * Indicates the validation level to require for psm quantification
     *
     * @return the minimal validation level for a protein to be accounted for
     */
    public MatchValidationLevel getPsmValidationLevel() {
        return psmValidation;
    }

    /**
     * Sets the validation level to require for quantification
     *
     * @param matchValidationLevel the minimal validation level for a psm to be
     * accounted for
     */
    public void setPsmValidationLevel(MatchValidationLevel matchValidationLevel) {
        this.psmValidation = matchValidationLevel;
    }

    @Override
    public String getFamilyName() {
        return "Reporter";
    }

    @Override
    public int getIndex() {
        return 0;
    }
}
