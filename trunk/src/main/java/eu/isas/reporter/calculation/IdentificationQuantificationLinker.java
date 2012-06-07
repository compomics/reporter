package eu.isas.reporter.calculation;

/**
 * This class links identifications with quantification results.
 *
 * @author Marc Vaudel
 */
public class IdentificationQuantificationLinker {

    /**
     * Index of the linking by spectrum title (fastest method).
     */
    public static final int SPECTRUM_TITLE = 0;
    /**
     * Index of the linking by precursor matching (used for combination of
     * fragmentation techniques).
     */
    public static final int PRECURSOR = 1;
    /**
     * Method used by the linker (see static fields).
     */
    private int index;
    /**
     * mz tolerance used for precursor matching.
     */
    private double mzTolerance;
    /**
     * Retention time tolerance used for precursor matching.
     */
    private double rtTolerance;

    /**
     * Constructor.
     *
     * @param index the method used for spectrum to identification matching (see
     * static fields)
     */
    public IdentificationQuantificationLinker(int index) {
        this.index = index;
    }

    /**
     * Sets the m/z tolerance for precursor matching.
     *
     * @param mzTolerance the m/z tolerance to set
     */
    public void setMzTolerance(double mzTolerance) {
        this.mzTolerance = mzTolerance;
    }

    /**
     * Set the retention time tolerance for precursor matching.
     *
     * @param rtTolerance the retention time tolerance to set
     */
    public void setRtTolerance(double rtTolerance) {
        this.rtTolerance = rtTolerance;
    }

    /**
     * Returns the m/z tolerance for precursor matching.
     *
     * @return the m/z tolerance
     */
    public double getMzTolerance() {
        return mzTolerance;
    }

    /**
     * Returns the retention time tolerance for precursor matching.
     *
     * @return the retention time tolerance
     */
    public double getRtTolerance() {
        return rtTolerance;
    }

    /**
     * Returns the index of the matching method used (see static fields).
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }
}
