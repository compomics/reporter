package eu.isas.reporter.settings;

import java.io.Serializable;

/**
 * Preferences for the reporter ions selection in spectra.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterIonSelectionSettings implements Serializable {

    /**
     * The location of the reporter ions.
     */
    private ReporterIonsLocationType reporterIonsLocation = ReporterIonsLocationType.ms2Spectra;
    /*
     * Tolerance for reporter ion matching.
     */
    private double reporterIonsMzTolerance = 0.0016;
    /**
     * Boolean indicating whether the most accurate ion should be selected.
     */
    private boolean mostAccurate = true;
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
     * Constructor. Creates new reporter ion selection settings set to default.
     */
    public ReporterIonSelectionSettings() {

    }

    @Override
    public ReporterIonSelectionSettings clone() {

        ReporterIonSelectionSettings clone = new ReporterIonSelectionSettings();

        clone.setReporterIonsMzTolerance(reporterIonsMzTolerance);
        clone.setMostAccurate(mostAccurate);
        clone.setReporterIonsLocation(reporterIonsLocation);
        clone.setPrecursorMzTolerance(precursorMzTolerance);
        clone.setPrecursorMzPpm(precursorMzPpm);
        clone.setPrecursorRTTolerance(precursorRTTolerance);

        return clone;

    }

    /**
     * Indicates whether another setting is the same as this one.
     *
     * @param anotherSetting another setting
     *
     * @return a boolean indicating whether another setting is the same as this
     * one
     */
    public boolean isSameAs(ReporterIonSelectionSettings anotherSetting) {

        return reporterIonsMzTolerance == anotherSetting.getPrecursorMzTolerance()
                && mostAccurate == anotherSetting.isMostAccurate()
                && reporterIonsLocation == anotherSetting.getReporterIonsLocation()
                && precursorMzTolerance == anotherSetting.getPrecursorMzTolerance()
                && precursorMzPpm == anotherSetting.isPrecursorMzPpm()
                && precursorRTTolerance == anotherSetting.getPrecursorRTTolerance();

    }

    /**
     * Returns the tolerance used to match reporter ions.
     *
     * @return the tolerance used to match reporter ions
     */
    public double getReporterIonsMzTolerance() {
        return reporterIonsMzTolerance;
    }

    /**
     * Sets the tolerance used to match reporter ions.
     *
     * @param ReporterIonsMzTolerance the tolerance used to match reporter ions
     */
    public void setReporterIonsMzTolerance(double ReporterIonsMzTolerance) {
        this.reporterIonsMzTolerance = ReporterIonsMzTolerance;
    }

    /**
     * Returns a boolean indicating whether the most accurate ion should be
     * retained for quantification. The most intense will be used otherwise.
     *
     * @return a boolean indicating whether the most accurate ion should be
     * retained for quantification
     */
    public boolean isMostAccurate() {
        return mostAccurate;
    }

    /**
     * Sets whether the most accurate ion should be retained for quantification.
     * The most intense will be used otherwise.
     *
     * @param mostAccurate a boolean indicating whether the most accurate ion
     * should be retained for quantification
     */
    public void setMostAccurate(boolean mostAccurate) {
        this.mostAccurate = mostAccurate;
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
     * Returns the reporter ions location.
     *
     * @return the reporter ions location
     */
    public ReporterIonsLocationType getReporterIonsLocation() {
        return reporterIonsLocation;
    }

    /**
     * Set the reporter ions location.
     *
     * @param reporterIonsLocation the reporter ions location
     */
    public void setReporterIonsLocation(ReporterIonsLocationType reporterIonsLocation) {
        this.reporterIonsLocation = reporterIonsLocation;
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
