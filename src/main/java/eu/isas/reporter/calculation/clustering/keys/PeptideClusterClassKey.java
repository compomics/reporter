package eu.isas.reporter.calculation.clustering.keys;

import eu.isas.reporter.calculation.clustering.ClusterClassKey;
import java.util.ArrayList;

/**
 * Key for the class of a peptide cluster.
 *
 * @author Marc Vaudel
 */
public class PeptideClusterClassKey implements ClusterClassKey {

    /**
     * Indicates whether the peptides must be starred.
     */
    private Boolean starred = false;

    /**
     * The PTMs eventually carried by the peptides in this class.
     */
    private ArrayList<String> possiblePtms = null;

    /**
     * The PTMs not carried by the peptides in this class.
     */
    private ArrayList<String> forbiddenPtms = null;

    /**
     * Indicates whether the peptides are not modified.
     */
    private boolean notModified = false;

    /**
     * Boolean indicating whether the peptides are N-term
     */
    private Boolean nTerm = false;

    /**
     * Boolean indicating whether the peptides are C-term
     */
    private Boolean cTerm = false;

    /**
     * Constructor.
     */
    public PeptideClusterClassKey() {

    }

    /**
     * Indicates whether the peptides must be starred.
     *
     * @return a boolean indicating whether the peptides must be starred
     */
    public Boolean isStarred() {
        return starred;
    }

    /**
     * Sets whether the peptides must be starred.
     *
     * @param starred a boolean indicating whether the peptides must be starred
     */
    public void setStarred(Boolean starred) {
        this.starred = starred;
    }

    /**
     * Returns the PTMs possibly carried by the peptides.
     *
     * @return the PTMs possibly carried by the peptides
     */
    public ArrayList<String> getPossiblePtms() {
        return possiblePtms;
    }

    /**
     * Sets the PTMs possibly carried by the peptides.
     *
     * @param possiblePtms the PTMs possibly carried by the peptides
     */
    public void setPossiblePtms(ArrayList<String> possiblePtms) {
        this.possiblePtms = possiblePtms;
    }

    /**
     * Returns the PTMs not carried by the peptide.
     *
     * @return the PTMs not carried by the peptide
     */
    public ArrayList<String> getForbiddenPtms() {
        return forbiddenPtms;
    }

    /**
     * Sets the PTMs not carried by the peptide.
     *
     * @param forbiddenPtms the PTMs not carried by the peptide
     */
    public void setForbiddenPtms(ArrayList<String> forbiddenPtms) {
        this.forbiddenPtms = forbiddenPtms;
    }

    /**
     * Indicates whether the peptides must be not modified.
     *
     * @return a boolean indicating whether the peptides must be not modified
     */
    public boolean isNotModified() {
        return notModified;
    }

    /**
     * Sets whether the peptides must be not modified.
     *
     * @param notModified a boolean indicating whether the peptides must be not
     * modified
     */
    public void setNotModified(boolean notModified) {
        this.notModified = notModified;
    }

    /**
     * Indicates whether the peptide is N-term.
     *
     * @return a boolean indicating whether the peptide is N-term
     */
    public Boolean isNTerm() {
        return nTerm;
    }

    /**
     * Sets whether the peptide is N-term.
     *
     * @param nTerm a boolean indicating whether the peptide is N-term
     */
    public void setnTerm(Boolean nTerm) {
        this.nTerm = nTerm;
    }

    /**
     * Indicates whether the peptide is C-term.
     *
     * @return a boolean indicating whether the peptide is C-term
     */
    public Boolean getcTerm() {
        return cTerm;
    }

    /**
     * Sets whether the peptide is C-term.
     *
     * @param cTerm a boolean indicating whether the peptide is C-term
     */
    public void setcTerm(Boolean cTerm) {
        this.cTerm = cTerm;
    }

    @Override
    public String getName() {
        StringBuilder name = new StringBuilder();
        if (nTerm) {
            name.append("N-term");
        } else if (cTerm) {
            name.append("C-term");
        } else if (notModified) {
            name.append("Not modified");
        } else if (starred) {
            name.append("Starred");
        }
        if (possiblePtms != null || forbiddenPtms != null) {
            StringBuilder possible = new StringBuilder();
            for (String possiblePtm : possiblePtms) {
                if (possible.length() > 0) {
                    possible.append(", ");
                }
                possible.append(possiblePtm);
            }
            StringBuilder forbidden = new StringBuilder();
            for (String possiblePtm : forbiddenPtms) {
                if (forbidden.length() > 0) {
                    forbidden.append(", ");
                }
                forbidden.append(possiblePtm);
            }
            if (possible.length() > 0 && forbidden.length() > 0) {
                if (name.length() > 0) {
                    name.append(" ");
                }
                name.append(possible).append("; but not ").append(forbidden);
            } else if (possible.length() > 0) {
                if (name.length() > 0) {
                    name.append(" ");
                }
                name.append(possible);
            } else if (forbidden.length() > 0) {
                if (name.length() > 0) {
                    name.append(" ");
                }
                name.append(forbidden);
            }
        }
        if (name.length() == 0) {
            return "All";
        } else {
            return name.toString();
        }
    }

    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder();
        if (starred) {
            description.append("Starred ");
        } else if (nTerm) {
            description.append("N-terminal ");
        } else if (cTerm) {
            description.append("C-terminal ");
        } else if (notModified) {
            description.append("Not Modified ");
        } else {
            description.append("All ");
        }
        description.append("peptides");
        StringBuilder possible = new StringBuilder();
        for (String possiblePtm : possiblePtms) {
            if (possible.length() > 0) {
                possible.append(", ");
            }
            possible.append(possiblePtm);
        }
        StringBuilder forbidden = new StringBuilder();
        for (String possiblePtm : forbiddenPtms) {
            if (forbidden.length() > 0) {
                forbidden.append(", ");
            }
            forbidden.append(possiblePtm);
        }
        if (possible.length() > 0 && forbidden.length() > 0) {
            description.append(" carrying ").append(possible).append("; but not ").append(forbidden);
        } else if (possible.length() > 0) {
            description.append(" carrying ").append(possible);
        } else {
            description.append(" not carrying ").append(possible);
        }
        return description.toString();
    }

    @Override
    public String toString() {
        return "Peptide_" + getName();
    }

}
