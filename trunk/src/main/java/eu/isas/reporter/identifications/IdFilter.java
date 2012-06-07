package eu.isas.reporter.identifications;

import com.compomics.util.experiment.identification.PeptideAssumption;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.util.HashMap;

/**
 * This class will filter out identifications which should not be retained for
 * further calculation.
 *
 * @author Marc Vaudel
 */
public class IdFilter {

    /**
     * Minimal size for a sequence.
     */
    private double nAAmin;
    /**
     * Maximal size for a sequence.
     */
    private double nAAmax;
    /**
     * Maximal mass deviation.
     */
    private double deltaMass;
    /**
     * Maximal e-values allowed indexed by their search engine.
     */
    private HashMap<Integer, Double> eValues;

    /**
     * Constructor.
     * 
     * @param quantificationPreferences 
     */
    public IdFilter(QuantificationPreferences quantificationPreferences) {
        this.nAAmin = quantificationPreferences.getnAAmin();
        this.nAAmax = quantificationPreferences.getnAAmax();
        this.deltaMass = quantificationPreferences.getPrecursorMassDeviation();
        this.eValues = quantificationPreferences.getMaxEValues();
    }

    /**
     * Validates a peptide assumption: returns true if the identification did
     * pass the filter; false otherwise.
     *
     * @param identification The considered peptide assumption
     * @return a boolean indicating whether the identification did pass the
     * filter
     */
    public boolean validate(PeptideAssumption identification) {
        int sequenceLength = identification.getPeptide().getSequence().length();

        // @TODO: should delta mass always be in ppm?? 

        return identification.getDeltaMass(true) < deltaMass
                && identification.getEValue() < eValues.get(identification.getAdvocate())
                && sequenceLength > nAAmin
                && sequenceLength < nAAmax;
    }
}
