package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import java.util.ArrayList;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class Ignorer {

    private boolean ignoreMissedCleavages;
    private ArrayList<String> ignoredModifications = new ArrayList<String>();
    private double ratioMin;
    private double ratioMax;

    public Ignorer(boolean ignoreMissedCleavages, ArrayList<String> ignoredPtms) {
        this.ignoreMissedCleavages = ignoreMissedCleavages;
        this.ignoredModifications.addAll(ignoredPtms);
    }

    public boolean ignoreRatio(double ratio) {
        return ratio < ratioMin || ratio > ratioMax;
    }

    public boolean ignorePeptide(Peptide peptide) {
        String sequence = peptide.getSequence();
        if (ignoreMissedCleavages && peptide.getNMissedCleavages() > 0) {
            return true;
        }
        ArrayList<String> foundMods = new ArrayList<String>();
        for (ModificationMatch mod : peptide.getModificationMatches()) {
            if (mod.isVariable()) {
                foundMods.add(mod.getTheoreticPtm().getName().toLowerCase());
            }
        }
        for (String forbiddenPtm : ignoredModifications) {
            if (foundMods.contains(forbiddenPtm)) {
                return true;
            }
        }
        return false;
    }

    public void setRatioMin(double ratioMin) {
        this.ratioMin = ratioMin;
    }

    public void setRatioMax(double ratioMax) {
        this.ratioMax = ratioMax;
    }
}
