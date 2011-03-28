package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.personalization.UrParameter;
import java.util.ArrayList;
import java.util.HashMap;
import org.ujmp.core.collections.ArrayIndexList;

/**
 * This class contains the quantification options set by the user
 * @TODO user preferences should be loaded and saved automatically
 *
 * @author Marc
 */
public class QuantificationPreferences implements UrParameter {


    /**
     * constructor
     */
    public QuantificationPreferences() {
        maxEValues.put(SearchEngine.MASCOT, 10.0);
        maxEValues.put(SearchEngine.OMSSA, 10.0);
        maxEValues.put(SearchEngine.XTANDEM, 10.0);
    }
    
        /**
        * Import parameters
        */
    /**
     * tolerance for reporter ion matching
     */
    private double ReporterIonsMzTolerance = 0.01;
    /**
     * Minimal number of amino acids allowed for a peptide
     * (identification files import only)
     */
    private int nAAmin = 8;
    /**
     * Maximal number of amino acids allowed for a peptide
     * (identification files import only)
     */
    private int nAAmax = 20;
    /**
     * Maximal precursor mass deviation allowed for a PSM
     * (identification files import only)
     */
    private double precursorMassDeviation = 10;
    /**
     * Maximal e-value allowed for a PSM indexed by the search engine compomics index
     * (identification files import only)
     */
    private HashMap<Integer, Double> maxEValues = new HashMap<Integer, Double>();
    /**
     * Maximal estimated FDR allowed for a search engine PSM set
     * (identification files import only)
     */
    private double fdrThreshold = 0.01;
    /**
     * Quantification and identification are conducted on the same spectra
     * (identification files import only)
     */
    private boolean sameSpectra = true;
    /**
     * Precursor mz tolerance used to link quantification to identifications in case these are not recorded on the same spectra.
     * (identification files import only)
     */
    private double precursorMzTolerance = 1;
    /**
     * Precursor RT tolerance used to link quantification to identifications in case these are not recorded on the same spectra.
     * (identification files import only)
     */
    private double precursorRTTolerance = 10;

    /**
     * returns the tolerance used to match reporter ions
     * @return the tolerance used to match reporter ions
     */
    public double getReporterIonsMzTolerance() {
        return ReporterIonsMzTolerance;
    }

    /**
     * sets the tolerance used to match reporter ions
     * @param ReporterIonsMzTolerance the tolerance used to match reporter ions
     */
    public void setReporterIonsMzTolerance(double ReporterIonsMzTolerance) {
        this.ReporterIonsMzTolerance = ReporterIonsMzTolerance;
    }

    /**
     * returns the FDR threshold to use
     * @return the FDR threshold to use
     */
    public double getFdrThreshold() {
        return fdrThreshold;
    }
    /**
     * Sets the FDR threshold to use
     * @param fdrThreshold the FDR threshold to use
     */
    public void setFdrThreshold(double fdrThreshold) {
        this.fdrThreshold = fdrThreshold;
    }

    /**
     * Returns the maximal e-value allowed for a given search engine
     * @param searchEngineIndex the compomics search engine index
     * @return the maximum e-value allowed
     */
    public double getMaxEValue(int searchEngineIndex) {
        return maxEValues.get(searchEngineIndex);
    }

    /**
     * Sets the maximal e-value allowed for a search engine
     * @param searchEngineIndex the compomics search engine index
     * @param mascotMaxEValue   the corresponding e-value limit
     */
    public void setMaxEValue(int searchEngineIndex, double mascotMaxEValue) {
        maxEValues.put(searchEngineIndex, mascotMaxEValue);
    }

    /**
     * Returns the maximal number of amino-acids allowed for a PSM peptide
     * @return the maximal number of amino-acids allowed for a PSM peptide
     */
    public int getnAAmax() {
        return nAAmax;
    }

    /**
     * Sets the maximal number of amino-acids allowed for a PSM peptide
     * @param nAAmax the maximal number of amino-acids allowed for a PSM peptide
     */
    public void setnAAmax(int nAAmax) {
        this.nAAmax = nAAmax;
    }

    /**
     * Returns the minimal number of amino-acids allowed for a PSM peptide
     * @return the minimal number of amino-acids allowed for a PSM peptide
     */
    public int getnAAmin() {
        return nAAmin;
    }

    /**
     * Sets the minimal number of amino-acids allowed for a PSM peptide
     * @param nAAmin the minimal number of amino-acids allowed for a PSM peptide
     */
    public void setnAAmin(int nAAmin) {
        this.nAAmin = nAAmin;
    }

    /**
     * Returns the maximal precursor mass deviation allowed for a PSM
     * @return the maximal precursor mass deviation allowed for a PSM
     */
    public double getPrecursorMassDeviation() {
        return precursorMassDeviation;
    }

    /**
     * Sets the maximal precursor mass deviation allowed for a PSM
     * @param precursorMassDeviation the maximal precursor mass deviation allowed for a PSM
     */
    public void setPrecursorMassDeviation(double precursorMassDeviation) {
        this.precursorMassDeviation = precursorMassDeviation;
    }

    /**
     * returns the precursor m/Z tolerance used to match quantification spectra to identification spectra
     * @return the precursor m/Z tolerance used to match quantification spectra to identification spectra
     */
    public double getPrecursorMzTolerance() {
        return precursorMzTolerance;
    }

    /**
     * sets the precursor m/z tolerance used to match quantification spectra to identification spectra
     * @param precursorMzTolerance the precursor m/z tolerance used to match quantification spectra to identification spectra
     */
    public void setPrecursorMzTolerance(double precursorMzTolerance) {
        this.precursorMzTolerance = precursorMzTolerance;
    }

    /**
     * Returns the precursor RT tolerance used to match quantification spectra to identification spectra
     * @return the precursor RT tolerance used to match quantification spectra to identification spectra
     */
    public double getPrecursorRTTolerance() {
        return precursorRTTolerance;
    }

    /**
     * Sets the precursor RT tolerance used to match quantification spectra to identification spectra
     * @param precursorRTTolerance the precursor RT tolerance used to match quantification spectra to identification spectra
     */
    public void setPrecursorRTTolerance(double precursorRTTolerance) {
        this.precursorRTTolerance = precursorRTTolerance;
    }

    /**
     * Returns a boolean indicating whether identification and quantification are performed on the same spectra
     * @return a boolean indicating whether identification and quantification are performed on the same spectra
     */
    public boolean isSameSpectra() {
        return sameSpectra;
    }

    /**
     * Returns the max e-values map
     * @return the max e-values map
     */
    public HashMap<Integer, Double> getMaxEValues() {
        return maxEValues;
    }

    /**
     * Sets whether identification and quantification are performed on the same spectra
     * @param sameSpectra whether identification and quantification are performed on the same spectra
     */
    public void setSameSpectra(boolean sameSpectra) {
        this.sameSpectra = sameSpectra;
    }


        /**
         * Processing parameters
         */
    /**
     * boolean indicating whether spectra presenting null intensities should be ignored
     */
    private boolean ignoreNullIntensities = true;

    /**
     * boolean indicating whether peptides presenting missed cleavages should be ignored
     */
    private boolean ignoreMissedCleavages = false;

    /**
     * boolean indicating whether all identifications should be used for quantification or only validated ones (peptide-shaker files only)
     */
    private boolean onlyValidated = true;

    /**
     * double indicating the value of k for protein ratio inference
     */
    private double k = 1.4;

    /**
     * theoretic minimal ratio
     */
    private double ratioMin = 0.01;

    /**
     * Theoretic maximal ratio
     */
    private double ratioMax = 100;

    /**
     * Ratio resolution
     */
    private double ratioResolution = 0.01;

    /**
     * List of PTM. Peptides presenting these ptms will be ignored
     */
    private ArrayList<PTM> ignoredPTM = new ArrayList<PTM>();

    /**
     * returns a boolean indicating whether miscleaved peptides should be ignored
     * @return a boolean indicating whether miscleaved peptides should be ignored
     */
    public boolean isIgnoreMissedCleavages() {
        return ignoreMissedCleavages;
    }

    /**
     * sets whether miscleaved peptides should be ignored
     * @param ignoreMissedCleavages a boolean indicating whether miscleaved peptides should be ignored
     */
    public void setIgnoreMissedCleavages(boolean ignoreMissedCleavages) {
        this.ignoreMissedCleavages = ignoreMissedCleavages;
    }

    /**
     * returns a boolean indicating whether null intensities should be ignored
     * @return a boolean indicating whether null intensities should be ignored
     */
    public boolean isIgnoreNullIntensities() {
        return ignoreNullIntensities;
    }

    /**
     * sets whether null intensities should be ignored
     * @param ignoreNullIntensities  a boolean indicating whether null intensities should be ignored
     */
    public void setIgnoreNullIntensities(boolean ignoreNullIntensities) {
        this.ignoreNullIntensities = ignoreNullIntensities;
    }

    /**
     * returns the k used for peptide and protein ratio inference
     * @return the k used for peptide and protein ratio inference
     */
    public double getK() {
        return k;
    }

    /**
     * sets the k used for peptide and protein ratio inference
     * @param k the k used for peptide and protein ratio inference
     */
    public void setK(double k) {
        this.k = k;
    }

    /**
     * returns a boolean indicating whether only validated identifications should be accounted
     * @return a boolean indicating whether only validated identifications should be accounted
     */
    public boolean isOnlyValidated() {
        return onlyValidated;
    }

    /**
     * indicates whether only validated identifications should be accounted
     * @param onlyValidated a boolean indicating whether only validated identifications should be accounted
     */
    public void setOnlyValidated(boolean onlyValidated) {
        this.onlyValidated = onlyValidated;
    }

    /**
     * Returns the maximal ratio expected
     * @return the maximal ratio expected
     */
    public double getRatioMax() {
        return ratioMax;
    }

    /**
     * sets the maximal ratio expected
     * @param ratioMax the maximal ratio expected
     */
    public void setRatioMax(double ratioMax) {
        this.ratioMax = ratioMax;
    }

    /**
     * returns the minimal ratio expected
     * @return the minimal ratio expected
     */
    public double getRatioMin() {
        return ratioMin;
    }

    /**
     * sets the minimal ratio expected
     * @param ratioMin the minimal ratio expected
     */
    public void setRatioMin(double ratioMin) {
        this.ratioMin = ratioMin;
    }

    /**
     * Returns the ratio resolution
     * @return the ratio resolution
     */
    public double getRatioResolution() {
        return ratioResolution;
    }

    /**
     * Sets  the ratio resolution
     * @param ratioResolution the ratio resolution
     */
    public void setRatioResolution(double ratioResolution) {
        this.ratioResolution = ratioResolution;
    }

    /**
     * Returns the list of PTMs to ignore
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
     * Adds a PTM to ignore
     * @param ignoredPTM a PTM to ignore
     */
    public void addIgnoredPTM(PTM ignoredPTM) {
        this.ignoredPTM.add(ignoredPTM);
    }

    /**
     * Empty the list of ignored PTMs
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
        return 3;
    }
}
