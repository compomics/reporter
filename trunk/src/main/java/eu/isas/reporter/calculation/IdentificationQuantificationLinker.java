package eu.isas.reporter.calculation;

/**
 *
 * @author Marc
 */
public class IdentificationQuantificationLinker {

    public static final int SPECTRUM_TITLE = 0;
    public static final int PRECURSOR = 1;
    
    private int index;
    
    private double mzTolerance;
    private double rtTolerance;

    public IdentificationQuantificationLinker(int index) {
        this.index = index;
    }

    public void setMzTolerance(double mzTolerance) {
        this.mzTolerance = mzTolerance;
    }

    public void setRtTolerance(double rtTolerance) {
        this.rtTolerance = rtTolerance;
    }

    public double getMzTolerance() {
        return mzTolerance;
    }

    public double getRtTolerance() {
        return rtTolerance;
    }

    public int getIndex() {
        return index;
    }
}
