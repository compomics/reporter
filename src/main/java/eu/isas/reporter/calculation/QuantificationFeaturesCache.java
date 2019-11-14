package eu.isas.reporter.calculation;

import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinPtmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.SpectrumQuantificationDetails;
import java.util.Collections;
import java.util.HashMap;

/**
 * The quantification features cache stores quantification features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class QuantificationFeaturesCache {

    /**
     * Share of the memory to be used.
     */
    private double memoryShare = 0.99;
    /**
     * The protein quantification details in a map: number of peptides > protein
     * match key > match quantification details.
     */
    private HashMap<Integer, HashMap<Long, ProteinQuantificationDetails>> proteinRatios = new HashMap<Integer, HashMap<Long, ProteinQuantificationDetails>>();
    /**
     * The protein level PTM quantification details in a map: PTM name > protein
     * match key > site > match quantification details.
     */
    private HashMap<String, HashMap<Long, HashMap<String, ProteinPtmQuantificationDetails>>> proteinPtmRatios = new HashMap<String, HashMap<Long, HashMap<String, ProteinPtmQuantificationDetails>>>();
    /**
     * The peptide quantification details in a map: number of PSMs > peptide
     * match key > match quantification details.
     */
    private HashMap<Integer, HashMap<Long, PeptideQuantificationDetails>> peptideRatios = new HashMap<Integer, HashMap<Long, PeptideQuantificationDetails>>();
    /**
     * The PSM quantification details in a map: Spectrum file name > spectrum
     * key > match quantification details.
     */
    private HashMap<String, HashMap<String, PsmQuantificationDetails>> psmRatios = new HashMap<String, HashMap<String, PsmQuantificationDetails>>();
    /**
     * The spectrum quantification details in a map: Spectrum file name >
     * spectrum key > match quantification details Note: this is used in
     * precursor matching mode only, otherwise the spectrum ratios are the same
     * as the PSM ratios.
     */
    private HashMap<String, HashMap<String, SpectrumQuantificationDetails>> spectrumRatios = new HashMap<String, HashMap<String, SpectrumQuantificationDetails>>();
    /**
     * Boolean indicating whether a thread is editing the cache.
     */
    private boolean editing = false;

    /**
     * Constructor.
     */
    public QuantificationFeaturesCache() {
    }

    /**
     * Checks whether there is still memory left and empties the cache if not.
     */
    private void adaptCacheSize() {
        if (!editing
                && !memoryCheck()
                && !isEmpty()) {
            adaptCacheSizeSynchronized();
        }
    }

    /**
     * Indicates whether the cache is empty
     *
     * @return true if the cache is empty
     */
    public boolean isEmpty() {
        return peptideRatios.isEmpty()
                && proteinRatios.isEmpty()
                && spectrumRatios.isEmpty()
                && proteinPtmRatios.isEmpty();
    }

    /**
     * Checks whether there is still memory left and empties the cache if not.
     */
    private synchronized void adaptCacheSizeSynchronized() {

        if (editing || memoryCheck() || isEmpty()) {
            return;
        }
        editing = true;
        for (int i = 1; i < 10; i++) {
            peptideRatios.remove(i);
            if (memoryCheck()) {
                editing = false;
                return;
            }
        }
        for (int i = 1; i < 10; i++) {
            proteinRatios.remove(i);
            if (memoryCheck()) {
                editing = false;
                return;
            }
        }
        spectrumRatios.clear();
        if (memoryCheck()) {
            editing = false;
            return;
        }
        psmRatios.clear();
        if (memoryCheck()) {
            editing = false;
            return;
        }
        int keyMax = Collections.max(peptideRatios.keySet());
        for (int i = 10; i <= keyMax; i++) {
            peptideRatios.remove(i);
            if (memoryCheck()) {
                editing = false;
                return;
            }
        }
        keyMax = Collections.max(proteinRatios.keySet());
        for (int i = 10; i <= keyMax; i++) {
            proteinRatios.remove(i);
            if (memoryCheck()) {
                editing = false;
                return;
            }
        }
        for (String ptm : proteinPtmRatios.keySet()) {
            proteinPtmRatios.remove(ptm);
            if (memoryCheck()) {
                editing = false;
                return;
            }
        }
    }

    /**
     * Indicates whether the memory used by the application is lower than a
     * given share of the heap size. The share is set by memoryShare.
     *
     * @return a boolean indicating whether the memory used by the application
     * is lower than 99% of the heap
     */
    public boolean memoryCheck() {
        return Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory() < (long) (memoryShare * Runtime.getRuntime().maxMemory());
    }

    /**
     * Adds protein quantification details to the cache.
     *
     * @param nPeptides the number of peptides of this protein match
     * @param matchKey the key of the protein match
     * @param matchQuantificationDetails the protein quantification details
     */
    public synchronized void addProteinMatchQuantificationDetails(int nPeptides, long matchKey, ProteinQuantificationDetails matchQuantificationDetails) {
        editing = true;
        HashMap<Long, ProteinQuantificationDetails> submap = proteinRatios.get(nPeptides);
        if (submap == null) {
            submap = new HashMap<Long, ProteinQuantificationDetails>();
            proteinRatios.put(nPeptides, submap);
        }
        submap.put(matchKey, matchQuantificationDetails);
        editing = false;
        adaptCacheSize();
    }

    /**
     * Returns protein quantification details, null if not in cache.
     *
     * @param nPeptides the number of peptides of this protein match
     * @param matchKey the key of the protein match
     *
     * @return the protein quantification details
     */
    public ProteinQuantificationDetails getProteinMatchQuantificationDetails(int nPeptides, long matchKey) {
        ProteinQuantificationDetails result = null;
        HashMap<Long, ProteinQuantificationDetails> submap = proteinRatios.get(nPeptides);
        if (submap != null) {
            result = submap.get(matchKey);
        }
        adaptCacheSize();
        return result;
    }

    /**
     * Adds protein level PTM quantification details to the cache.
     *
     * @param ptmName the name of the PTM
     * @param matchKey the key of the protein match
     * @param site the site of the PTM on the protein sequence
     * @param matchQuantificationDetails the protein quantification details
     */
    public synchronized void addPtmQuantificationDetails(String ptmName, long matchKey, int site, ProteinPtmQuantificationDetails matchQuantificationDetails) {
        editing = true;
        HashMap<Long, HashMap<String, ProteinPtmQuantificationDetails>> submap = proteinPtmRatios.get(ptmName);
        if (submap == null) {
            submap = new HashMap<Long, HashMap<String, ProteinPtmQuantificationDetails>>();
            proteinPtmRatios.put(ptmName, submap);
        }
        HashMap<String, ProteinPtmQuantificationDetails> subsubmap = submap.get(matchKey);
        if (subsubmap == null) {
            subsubmap = new HashMap<String, ProteinPtmQuantificationDetails>();
            submap.put(matchKey, subsubmap);
        }
        subsubmap.put(site + "", matchQuantificationDetails); //@TODO: implement site key
        editing = false;
        adaptCacheSize();
    }

    /**
     * Returns protein level PTM quantification details, null if not in cache.
     *
     * @param ptmName the name of the PTM
     * @param matchKey the key of the protein match
     * @param site the site of the PTM on the protein sequence
     *
     * @return The protein quantification details
     */
    public ProteinPtmQuantificationDetails getPtmQuantificationDetails(String ptmName, long matchKey, int site) {
        ProteinPtmQuantificationDetails result = null;
        HashMap<Long, HashMap<String, ProteinPtmQuantificationDetails>> submap = proteinPtmRatios.get(ptmName);
        if (submap != null) {
            HashMap<String, ProteinPtmQuantificationDetails> subsubmap = submap.get(matchKey);
            if (subsubmap != null) {
                result = subsubmap.get(site + ""); //@TODO: implement site key
            }
        }
        adaptCacheSize();
        return result;
    }

    /**
     * Adds peptide quantification details ratio to the cache.
     *
     * @param nPeptides the number of peptides of this protein match
     * @param matchKey the key of the protein match
     * @param matchQuantificationDetails The protein quantification details
     */
    public synchronized void addPeptideMatchQuantificationDetails(int nPeptides, Long matchKey, PeptideQuantificationDetails matchQuantificationDetails) {
        editing = true;
        HashMap<Long, PeptideQuantificationDetails> submap = peptideRatios.get(nPeptides);
        if (submap == null) {
            submap = new HashMap<Long, PeptideQuantificationDetails>();
            peptideRatios.put(nPeptides, submap);
        }
        submap.put(matchKey, matchQuantificationDetails);
        editing = false;
        adaptCacheSize();
    }

    /**
     * Returns peptide quantification details, null if not in cache.
     *
     * @param nPsms the number of PSMs of this peptide match
     * @param matchKey the key of the peptide match
     *
     * @return the peptide quantification details
     */
    public PeptideQuantificationDetails getPeptideMatchQuantificationDetails(int nPsms, Long matchKey) {
        PeptideQuantificationDetails result = null;
        HashMap<Long, PeptideQuantificationDetails> submap = peptideRatios.get(nPsms);
        if (submap != null) {
            result = submap.get(matchKey);
        }
        adaptCacheSize();
        return result;
    }

    /**
     * Adds PSM quantification details ratio to the cache.
     *
     * @param matchKey the key of spectrum
     * @param matchQuantificationDetails the protein quantification details
     */
    public synchronized void addPSMQuantificationDetails(String matchKey, PsmQuantificationDetails matchQuantificationDetails) {
        editing = true;
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        HashMap<String, PsmQuantificationDetails> submap = psmRatios.get(spectrumFile);
        if (submap == null) {
            submap = new HashMap<String, PsmQuantificationDetails>();
            psmRatios.put(spectrumFile, submap);
        }
        submap.put(matchKey, matchQuantificationDetails);
        editing = false;
        adaptCacheSize();
    }

    /**
     * Returns PSM quantification details, null if not in cache.
     *
     * @param matchKey the key of the spectrum
     *
     * @return the PSM quantification details
     */
    public PsmQuantificationDetails getPSMQuantificationDetails(String matchKey) {
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        PsmQuantificationDetails result = null;
        HashMap<String, PsmQuantificationDetails> submap = psmRatios.get(spectrumFile);
        if (submap != null) {
            result = submap.get(matchKey);
        }
        adaptCacheSize();
        return result;
    }

    /**
     * Adds spectrum quantification details ratio to the cache.
     *
     * @param matchKey the key of spectrum
     * @param matchQuantificationDetails The protein quantification details
     */
    public synchronized void addSpectrumQuantificationDetails(String matchKey, SpectrumQuantificationDetails matchQuantificationDetails) {
        editing = true;
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        HashMap<String, SpectrumQuantificationDetails> submap = spectrumRatios.get(spectrumFile);
        if (submap == null) {
            submap = new HashMap<String, SpectrumQuantificationDetails>();
            spectrumRatios.put(spectrumFile, submap);
        }
        submap.put(matchKey, matchQuantificationDetails);
        editing = false;
        adaptCacheSize();
    }

    /**
     * Returns spectrum quantification details, null if not in cache.
     *
     * @param matchKey the key of the spectrum
     *
     * @return The spectrum quantification details
     */
    public SpectrumQuantificationDetails getSpectrumQuantificationDetails(String matchKey) {
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        SpectrumQuantificationDetails result = null;
        HashMap<String, SpectrumQuantificationDetails> submap = spectrumRatios.get(spectrumFile);
        if (submap != null) {
            result = submap.get(matchKey);
        }
        adaptCacheSize();
        return result;
    }
}
