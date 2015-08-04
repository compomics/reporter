package eu.isas.reporter.myparameters;

/**
 * Preferences for the reporter ions selection in spectra.
 *
 * @author Marc Vaudel
 */
public class ReporterIonSelectionSettings {

    /*
     * Tolerance for reporter ion matching.
     */
    private double ReporterIonsMzTolerance = 0.0016;
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
     * Indicates if the precursor mz tolerance in ppm.
     */
    private boolean precursorMzPpm;
    /**
     * Precursor RT tolerance used to link quantification to identifications in
     * case these are not recorded on the same spectra. (identification files
     * import only)
     */
    private double precursorRTTolerance = 10;
    
    /**
     * Constructor
     */
    public ReporterIonSelectionSettings() {
        
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
}
