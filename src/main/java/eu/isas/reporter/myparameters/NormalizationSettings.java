package eu.isas.reporter.myparameters;

import eu.isas.reporter.calculation.normalization.NormalizationType;
import java.util.ArrayList;
import java.util.HashSet;

/**
 * Settings used for the normalization.
 *
 * @author Marc Vaudel
 */
public class NormalizationSettings {

    /**
     * The normalization to conduct at the PSM level
     */
    private NormalizationType psmNormalization = NormalizationType.none;
    /**
     * The normalization to conduct at the peptide level
     */
    private NormalizationType peptideNormalization = NormalizationType.median;
    /**
     * The normalization to conduct at the protein level
     */
    private NormalizationType proteinNormalization = NormalizationType.median;
    /**
     * List of proteins to use as seed for normalization.
     */
    private HashSet<String> proteinSeeds = new HashSet<String>();
    /**
     * List of proteins to exclude for normalization.
     */
    private HashSet<String> proteinExcluded = new HashSet<String>();
    
    /**
     * Constructor
     */
    public NormalizationSettings() {
        
    }

    /**
     * Returns the type of normalization used to normalize the PSMs.
     * 
     * @return the type of normalization used to normalize the PSMs
     */
    public NormalizationType getPsmNormalization() {
        return psmNormalization;
    }

    /**
     * Sets the type of normalization used to normalize the PSMs.
     * 
     * @param psmNormalization the type of normalization used to normalize the PSMs
     */
    public void setPsmNormalization(NormalizationType psmNormalization) {
        this.psmNormalization = psmNormalization;
    }

    /**
     * Returns the type of normalization used to normalize the peptides.
     * 
     * @return the type of normalization used to normalize the peptides
     */
    public NormalizationType getPeptideNormalization() {
        return peptideNormalization;
    }

    /**
     * Sets the type of normalization used to normalize the peptides.
     * 
     * @param peptideNormalization the type of normalization used to normalize the peptides
     */
    public void setPeptideNormalization(NormalizationType peptideNormalization) {
        this.peptideNormalization = peptideNormalization;
    }

    /**
     * Returns the type of normalization used to normalize the proteins.
     * 
     * @return the type of normalization used to normalize the proteins
     */
    public NormalizationType getProteinNormalization() {
        return proteinNormalization;
    }

    /**
     * Sets the type of normalization used to normalize the proteins.
     * 
     * @param proteinNormalization the type of normalization used to normalize the proteins
     */
    public void setProteinNormalization(NormalizationType proteinNormalization) {
        this.proteinNormalization = proteinNormalization;
    }

    /**
     * Returns the proteins to use as seeds for normalization.
     * 
     * @return the proteins to use as seeds for normalization
     */
    public HashSet<String> getProteinSeeds() {
        return proteinSeeds;
    }

    /**
     * Sets the proteins to use as seeds for normalization.
     * 
     * @param proteinSeeds the proteins to use as seeds for normalization
     */
    public void setProteinSeeds(HashSet<String> proteinSeeds) {
        this.proteinSeeds = proteinSeeds;
    }

    /**
     * Returns the list of proteins to exclude during the normalization.
     * 
     * @return the list of proteins to exclude during the normalization
     */
    public HashSet<String> getProteinExcluded() {
        return proteinExcluded;
    }

    /**
     * Sets the list of proteins to exclude during the normalization.
     * 
     * @param proteinExcluded the list of proteins to exclude during the normalization
     */
    public void setProteinExcluded(HashSet<String> proteinExcluded) {
        this.proteinExcluded = proteinExcluded;
    }
    
    
    
}
