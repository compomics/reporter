package eu.isas.reporter.myparameters;

import eu.isas.reporter.calculation.normalization.NormalizationType;
import java.io.Serializable;
import java.util.HashSet;

/**
 * Settings used for the normalization.
 *
 * @author Marc Vaudel
 */
public class NormalizationSettings implements Serializable {

    /**
     * The normalization to conduct at the PSM level
     */
    private NormalizationType psmNormalization = NormalizationType.none;
    /**
     * The normalization to conduct at the peptide level
     */
    private NormalizationType peptideNormalization = NormalizationType.none;
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
    
    @Override
    public NormalizationSettings clone() throws CloneNotSupportedException {
        NormalizationSettings clone = new NormalizationSettings();
        clone.setPsmNormalization(psmNormalization);
        clone.setPeptideNormalization(peptideNormalization);
        clone.setProteinNormalization(proteinNormalization);
        for (String accession : proteinSeeds) {
            clone.addSeedProtein(accession);
        }
        for (String accession : proteinExcluded) {
            clone.addExcludedProtein(accession);
        }
        return clone;
    }
    
    /**
     * Indicates whether another setting is the same as this one.
     * 
     * @param anotherSetting another setting
     * 
     * @return a boolean indicating whether another setting is the same as this one
     */
    public boolean isSameAs(NormalizationSettings anotherSetting) {
        if (psmNormalization != anotherSetting.getPsmNormalization()
                || peptideNormalization != anotherSetting.getPeptideNormalization()
                || proteinNormalization != anotherSetting.getProteinNormalization()
                || proteinSeeds.size() != anotherSetting.getProteinSeeds().size()
                || proteinExcluded.size() != anotherSetting.getProteinExcluded().size()) {
            return false;
        }
        for (String accession : anotherSetting.getProteinSeeds()) {
            if (!proteinSeeds.contains(accession)) {
                return false;
            }
        }
        for (String accession : anotherSetting.getProteinExcluded()) {
            if (!proteinExcluded.contains(accession)) {
                return false;
            }
        }
        return true;
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
     * Returns the accessions of proteins to use as seeds for normalization.
     * 
     * @return the accessions of proteins to use as seeds for normalization
     */
    public HashSet<String> getProteinSeeds() {
        return proteinSeeds;
    }
    
    /**
     * Adds a protein to the seed list.
     * 
     * @param proteinAccession the accession of the protein
     */
    public void addSeedProtein(String proteinAccession) {
        proteinSeeds.add(proteinAccession);
    }
    
    /**
     * Removes a protein from the seed list.
     * 
     * @param proteinAccession the accession of the protein
     */
    public void removeSeedProtein(String proteinAccession) {
        proteinSeeds.remove(proteinAccession);
    }

    /**
     * Sets the accessions of proteins to use as seeds for normalization.
     * 
     * @param proteinSeeds the accessions of proteins to use as seeds for normalization
     */
    public void setProteinSeeds(HashSet<String> proteinSeeds) {
        this.proteinSeeds = proteinSeeds;
    }

    /**
     * Returns the list of accessions of proteins to exclude during the normalization.
     * 
     * @return the list of accessions of proteins to exclude during the normalization
     */
    public HashSet<String> getProteinExcluded() {
        return proteinExcluded;
    }
    
    /**
     * Adds a protein to be excluded from normalization.
     * 
     * @param proteinAccession the accession of the protein
     */
    public void addExcludedProtein(String proteinAccession) {
        proteinExcluded.add(proteinAccession);
    }
    
    /**
     * Removes a protein to be excluded from normalization.
     * 
     * @param proteinAccession the accession of the protein
     */
    public void removeExcludedProtein(String proteinAccession) {
        proteinExcluded.remove(proteinAccession);
    }

    /**
     * Sets the list of accessions of proteins to exclude during the normalization.
     * 
     * @param proteinExcluded the list of accessions of proteins to exclude during the normalization
     */
    public void setProteinExcluded(HashSet<String> proteinExcluded) {
        this.proteinExcluded = proteinExcluded;
    }
    
    
    
}
