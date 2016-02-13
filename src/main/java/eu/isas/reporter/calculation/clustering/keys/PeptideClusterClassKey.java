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
     * Indicates whether the psms must be starred.
     */
    private Boolean starred = false;

    /**
     * The PTM carried by the peptides in this class.
     */
    private String ptm = null;

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
     * Indicates whether the psms must be starred.
     *
     * @return a boolean indicating whether the psms must be starred
     */
    public Boolean isStarred() {
        return starred;
    }

    /**
     * Sets whether the psms must be starre.
     *
     * @param starred a boolean indicating whether the psms must be starred
     */
    public void setStarred(Boolean starred) {
        this.starred = starred;
    }

    /**
     * Returns the PTM carried by the peptide.
     *
     * @return the PTM carried by the peptide
     */
    public String getPtm() {
        return ptm;
    }

    /**
     * Sets the PTM carried by the peptide.
     *
     * @param ptm the PTM carried by the peptide
     */
    public void setPtm(String ptm) {
        this.ptm = ptm;
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
        if (nTerm) {
            return "N-term";
        } else if (cTerm) {
            return "C-term";
        } else if (ptm != null) {
            return ptm;
        } else if (starred) {
            return "Starred";
        } else {
            return "All";
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
        } else if (ptm == null) {
            description.append("All ");
        }
        description.append("peptides");
        if (ptm != null) {
            description.append(" carrying ").append(ptm);
        }
        return description.toString();
    }

}
