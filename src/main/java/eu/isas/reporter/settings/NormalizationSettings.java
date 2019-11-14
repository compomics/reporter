package eu.isas.reporter.settings;

import eu.isas.reporter.Reporter;
import eu.isas.reporter.calculation.normalization.NormalizationType;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.util.HashSet;

/**
 * Settings used for the normalization.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class NormalizationSettings implements Serializable {

    /**
     * The normalization to conduct at the PSM level.
     */
    private NormalizationType psmNormalization = NormalizationType.none;
    /**
     * The normalization to conduct at the peptide level.
     */
    private NormalizationType peptideNormalization = NormalizationType.median;
    /**
     * The normalization to conduct at the protein level.
     */
    private NormalizationType proteinNormalization = NormalizationType.median;
    /**
     * FASTA file containing the proteins to consider stable.
     */
    private File stableProteinsFastaFile = null;
    /**
     * FASTA file containing the contaminants.
     */
    private File contaminantsFastaFile = getDefaultContaminantFile();

    /**
     * Constructor.
     */
    public NormalizationSettings() {

    }

    @Override
    public NormalizationSettings clone() throws CloneNotSupportedException {
        NormalizationSettings clone = new NormalizationSettings();
        clone.setPsmNormalization(psmNormalization);
        clone.setPeptideNormalization(peptideNormalization);
        clone.setProteinNormalization(proteinNormalization);
        clone.setStableProteinsFastaFile(stableProteinsFastaFile);
        clone.setContaminantsFastaFile(contaminantsFastaFile);
        return clone;
    }

    /**
     * Indicates whether another setting is the same as this one.
     *
     * @param anotherSetting another setting
     *
     * @return a boolean indicating whether another setting is the same as this
     * one
     */
    public boolean isSameAs(NormalizationSettings anotherSetting) {
        if (stableProteinsFastaFile == null && anotherSetting.getStableProteinsFastaFile() != null
                || stableProteinsFastaFile != null && anotherSetting.getStableProteinsFastaFile() == null) {
            return false;
        }
        if (contaminantsFastaFile == null && anotherSetting.getContaminantsFastaFile() != null
                || contaminantsFastaFile != null && anotherSetting.getContaminantsFastaFile() == null) {
            return false;
        }
        if (stableProteinsFastaFile != null && anotherSetting.getStableProteinsFastaFile() != null && !stableProteinsFastaFile.getAbsolutePath().equals(anotherSetting.getStableProteinsFastaFile().getAbsolutePath())) {
            return false;
        }
        if (contaminantsFastaFile != null && anotherSetting.getContaminantsFastaFile() != null && !contaminantsFastaFile.getAbsolutePath().equals(anotherSetting.getContaminantsFastaFile().getAbsolutePath())) {
            return false;
        }
        return psmNormalization == anotherSetting.getPsmNormalization()
                && peptideNormalization == anotherSetting.getPeptideNormalization()
                && proteinNormalization == anotherSetting.getProteinNormalization();
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
     * @param psmNormalization the type of normalization used to normalize the
     * PSMs
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
     * @param peptideNormalization the type of normalization used to normalize
     * the peptides
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
     * @param proteinNormalization the type of normalization used to normalize
     * the proteins
     */
    public void setProteinNormalization(NormalizationType proteinNormalization) {
        this.proteinNormalization = proteinNormalization;
    }

    /**
     * Returns the FASTA file containing the stable proteins.
     *
     * @return the FASTA file containing the stable proteins
     */
    public File getStableProteinsFastaFile() {
        return stableProteinsFastaFile;
    }

    /**
     * Sets the FASTA file containing the stable proteins.
     *
     * @param stableProteinsFastaFile the FASTA file containing the stable
     * proteins
     */
    public void setStableProteinsFastaFile(File stableProteinsFastaFile) {
        this.stableProteinsFastaFile = stableProteinsFastaFile;
    }

    /**
     * Returns the FASTA file containing the contaminant proteins.
     *
     * @return the FASTA file containing the contaminant proteins
     */
    public File getContaminantsFastaFile() {
        return contaminantsFastaFile;
    }

    /**
     * Sets the FASTA file containing the contaminant proteins.
     *
     * @param contaminantsFastaFile the FASTA file containing the contaminant
     * proteins
     */
    public void setContaminantsFastaFile(File contaminantsFastaFile) {
        this.contaminantsFastaFile = contaminantsFastaFile;
    }

    /**
     * Returns the accessions of the stable proteins as a set taken from the
     * stableProteinsFastaFile. Null if no file is set.
     *
     * @return the accessions of the stable proteins as a set
     *
     * @throws IOException exception thrown whenever an error occurred while
     * accessing the file.
     */
    public HashSet<String> getStableProteins() throws IOException {
        if (stableProteinsFastaFile != null) {
            FastaIndex fastaIndex = SequenceFactory.getFastaIndex(stableProteinsFastaFile, false, null);
            return new HashSet<String>(fastaIndex.getIndexes().keySet());
        }
        return null;
    }

    /**
     * Returns the accessions of the contaminants as a set taken from the
     * contaminantsFastaFile. Null if no file is set.
     *
     * @return the accessions of the contaminants as a set
     *
     * @throws IOException exception thrown whenever an error occurred while
     * accessing the file.
     */
    public HashSet<String> getContaminants() throws IOException {
        if (contaminantsFastaFile != null) {
            FastaIndex fastaIndex = SequenceFactory.getFastaIndex(contaminantsFastaFile, false, null);
            return new HashSet<String>(fastaIndex.getIndexes().keySet());
        }
        return null;
    }

    /**
     * Returns the default contaminants file.
     *
     * @return the default contaminants file
     */
    public static File getDefaultContaminantFile() {
        return new File(Reporter.getJarFilePath(), "resources/crap.fasta"); //@TODO: implement as path setting
    }
}
