package eu.isas.reporter.calculation;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class IdentificationQuantificationLinker {

    public static final int SPECTRUM_TITLE = 0;
    public static final int PRECURSOR = 1;
    
    private int index;
    
    private double mzTolerance;
    private double rtTolerance;

    /**
     * Set the index.
     *
     * @param index the index to set
     */
    public IdentificationQuantificationLinker(int index) {
        this.index = index;
    }

    /**
     * Sets the m/z tolerance.
     *
     * @param mzTolerance the m/z tolerance to set
     */
    public void setMzTolerance(double mzTolerance) {
        this.mzTolerance = mzTolerance;
    }

    /**
     * Set the retention time tolerance.
     *
     * @param rtTolerance the retention time tolerance to set
     */
    public void setRtTolerance(double rtTolerance) {
        this.rtTolerance = rtTolerance;
    }

    /**
     * Returns the m/z tolerance.
     *
     * @return the m/z toleranze
     */
    public double getMzTolerance() {
        return mzTolerance;
    }

    /**
     * Returns the retention time tolerance.
     *
     * @return the retention time tolerance
     */
    public double getRtTolerance() {
        return rtTolerance;
    }

    /**
     * Returns the index.
     *
     * @return the index
     */
    public int getIndex() {
        return index;
    }
}
