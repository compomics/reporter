package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.personalization.UrParameter;
import java.util.ArrayList;
import org.ujmp.core.collections.ArrayIndexList;

/**
 * This class contains the quantification options set by the user.
 *
 * @TODO: user preferences should be loaded and saved automatically
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class QuantificationPreferences implements UrParameter {

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
     * Double indicating the value of k for protein ratio inference.
     */
    private double k = 1.4;
    /**
     * Rheoretic minimal ratio.
     */
    private double ratioMin = 0.01;
    /**
     * Theoretic maximal ratio.
     */
    private double ratioMax = 100;
    /**
     * Ratio resolution.
     */
    private double ratioResolution = 0.01;
    /**
     * List of PTM. Peptides presenting these ptms will be ignored.
     */
    private ArrayList<PTM> ignoredPTM = new ArrayList<PTM>();

    /**
     * Constructor.
     */
    public QuantificationPreferences() {
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
     * Returns the k used for peptide and protein ratio inference.
     *
     * @return the k used for peptide and protein ratio inference
     */
    public double getK() {
        return k;
    }

    /**
     * Sets the k used for peptide and protein ratio inference.
     *
     * @param k the k used for peptide and protein ratio inference
     */
    public void setK(double k) {
        this.k = k;
    }

    /**
     * Returns the maximal ratio expected.
     *
     * @return the maximal ratio expected
     */
    public double getRatioMax() {
        return ratioMax;
    }

    /**
     * Sets the maximal ratio expected.
     *
     * @param ratioMax the maximal ratio expected
     */
    public void setRatioMax(double ratioMax) {
        this.ratioMax = ratioMax;
    }

    /**
     * Returns the minimal ratio expected.
     *
     * @return the minimal ratio expected
     */
    public double getRatioMin() {
        return ratioMin;
    }

    /**
     * Sets the minimal ratio expected.
     *
     * @param ratioMin the minimal ratio expected
     */
    public void setRatioMin(double ratioMin) {
        this.ratioMin = ratioMin;
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
    public ArrayList<String> getIgnoredPTM() {
        ArrayList<String> results = new ArrayIndexList<String>();
        for (PTM ptm : ignoredPTM) {
            results.add(ptm.getName().toLowerCase());
        }
        return results;
    }

    /**
     * Adds a PTM to ignore.
     *
     * @param ignoredPTM a PTM to ignore
     */
    public void addIgnoredPTM(PTM ignoredPTM) {
        this.ignoredPTM.add(ignoredPTM);
    }

    /**
     * Empty the list of ignored PTMs.
     */
    public void emptyPTMList() {
        ignoredPTM = new ArrayList<PTM>();
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
