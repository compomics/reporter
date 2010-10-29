package eu.isas.reporter.identifications;

import com.compomics.util.experiment.identification.PeptideAssumption;
import java.util.HashMap;

/**
 *
 * @author Marc
 */
public class IdFilter {

    private double nAAmin;
    private double nAAmax;
    private double deltaMass;
    private HashMap<Integer, Double> eValues;

    public IdFilter(double nAAmin, double nAAmax, double deltaMass, HashMap<Integer, Double> eValues) {
        this.nAAmin = nAAmin;
        this.nAAmax = nAAmax;
        this.deltaMass = deltaMass;
        this.eValues = eValues;
    }

    public boolean validate(PeptideAssumption identification) {
        int sequenceLength = identification.getPeptide().getSequence().length();
        return identification.getDeltaMass() < deltaMass
                && identification.getEValue() < eValues.get(identification.getAdvocate())
                && sequenceLength > nAAmin
                && sequenceLength < nAAmax;
    }
}
