package eu.isas.reporter.identifications;

import com.compomics.util.experiment.identification.PeptideAssumption;
import java.util.HashMap;

/**
 * This class will filter out identifications which should not be retained for further calculation
 *
 * @author Marc Vaudel
 */
public class IdFilter {

    /**
     * Minimal size for a sequence
     */
    private double nAAmin;
    /**
     * Maximal size for a sequence
     */
    private double nAAmax;
    /**
     * maximal mass deviation
     */
    private double deltaMass;
    /**
     * Maximal e-values allowed indexed by their search engine
     */
    private HashMap<Integer, Double> eValues;

    /**
     * constructor
     *
     * @param nAAmin    Minimal sequence size
     * @param nAAmax    Maximal sequence size
     * @param deltaMass Maximal mass deviation
     * @param eValues   Maximal e-values
     */
    public IdFilter(double nAAmin, double nAAmax, double deltaMass, HashMap<Integer, Double> eValues) {
        this.nAAmin = nAAmin;
        this.nAAmax = nAAmax;
        this.deltaMass = deltaMass;
        this.eValues = eValues;
    }

    /**
     * Validates a peptide assumption: returns true if the identification did pass the filter; false otherwise.
     *
     * @param identification    The considered peptide assumption
     * @return  a boolean indicating whether the identification did pass the filter
     */
    public boolean validate(PeptideAssumption identification) {
        int sequenceLength = identification.getPeptide().getSequence().length();
        return identification.getDeltaMass() < deltaMass
                && identification.getEValue() < eValues.get(identification.getAdvocate())
                && sequenceLength > nAAmin
                && sequenceLength < nAAmax;
    }
}
