package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.util.ArrayList;

/**
 * This class is used to filter out doubtful ratios.
 *
 * @author Marc Vaudel
 */
public class Ignorer {

    /**
     * Boolean indicating whether miscleaved peptides should be ignored.
     */
    private boolean ignoreMissedCleavages;
    /**
     * All peptides presenting a modification contained in this list will be
     * ignored.
     */
    private ArrayList<String> ignoredModifications = new ArrayList<String>();
    /**
     * minimal ratio to be considered.
     */
    private double ratioMin;
    /**
     * maximal ratio to be considered.
     */
    private double ratioMax;
    /**
     * The enzyme used.
     */
    private Enzyme enzymeUsed;

    /**
     * Constructor.
     *
     * @param quantificationPreferences the quantification preferences
     * @param enzyme the enzyme used
     */
    public Ignorer(QuantificationPreferences quantificationPreferences, Enzyme enzyme) {
        this.ratioMin = quantificationPreferences.getRatioMin();
        this.ratioMax = quantificationPreferences.getRatioMax();
        this.ignoreMissedCleavages = quantificationPreferences.isIgnoreMissedCleavages();
        this.ignoredModifications.addAll(quantificationPreferences.getIgnoredPTM());
        this.enzymeUsed = enzyme;
    }

    /**
     * Method indicating whether a ratio should be ignored.
     *
     * @param ratio the examined ratio
     * @return a boolean indicating whether the ratio should be ignored
     */
    public boolean ignoreRatio(double ratio) {
        return ratio < ratioMin || ratio > ratioMax;
    }

    /**
     * Method indicating whether a peptide should be ignored.
     *
     * @param peptide the examined peptide
     * @return a boolean indicating whether the peptide should be ignored.
     */
    public boolean ignorePeptide(Peptide peptide) {
        if (ignoreMissedCleavages && peptide.getNMissedCleavages(enzymeUsed) > 0) {
            return true;
        }
        ArrayList<String> foundMods = new ArrayList<String>();
        for (ModificationMatch mod : peptide.getModificationMatches()) {
            if (mod.isVariable()) {
                foundMods.add(mod.getTheoreticPtm().toLowerCase());
            }
        }
        for (String forbiddenPtm : ignoredModifications) {
            if (foundMods.contains(forbiddenPtm)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Sets the minimal ratio to be considered.
     *
     * @param ratioMin minimal ratio to be considered
     */
    public void setRatioMin(double ratioMin) {
        this.ratioMin = ratioMin;
    }

    /**
     * Sets the maximal ratio to be considered.
     *
     * @param ratioMax maximal ratio to be considered
     */
    public void setRatioMax(double ratioMax) {
        this.ratioMax = ratioMax;
    }
}
