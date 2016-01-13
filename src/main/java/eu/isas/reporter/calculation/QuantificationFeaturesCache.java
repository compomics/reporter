package eu.isas.reporter.calculation;

import com.compomics.util.experiment.massspectrometry.Spectrum;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PtmSiteQuantificationDetails;
import eu.isas.reporter.quantificationdetails.SpectrumQuantificationDetails;
import java.util.Collections;
import java.util.HashMap;

/**
 * The quantification features cache stores quantification features.
 *
 * @author Marc Vaudel
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
    private HashMap<Integer, HashMap<String, ProteinQuantificationDetails>> proteinRatios = new HashMap<Integer, HashMap<String, ProteinQuantificationDetails>>();
    /**
     * The protein level PTM quantification details in a map: PTM name > protein
     * match key > site > match quantification details.
     */
    private HashMap<String, HashMap<String, HashMap<String, PtmSiteQuantificationDetails>>> proteinPtmRatios = new HashMap<String, HashMap<String, HashMap<String, PtmSiteQuantificationDetails>>>();
    /**
     * The peptide quantification details in a map: number of PSMs > peptide
     * match key > match quantification details.
     */
    private HashMap<Integer, HashMap<String, PeptideQuantificationDetails>> peptideRatios = new HashMap<Integer, HashMap<String, PeptideQuantificationDetails>>();
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
     * Constructor.
     */
    public QuantificationFeaturesCache() {
    }

    /**
     * Checks whether there is still memory left and empties the cache if not.
     */
    private synchronized void adaptCacheSize() {
        
        if (memoryCheck() || peptideRatios.isEmpty() && proteinRatios.isEmpty() && spectrumRatios.isEmpty() && proteinPtmRatios.isEmpty()) {
            return;
        }
        for (int i = 1; i < 10; i++) {
            peptideRatios.remove(i);
            if (memoryCheck()) {
                return;
            }
        }
        for (int i = 1; i < 10; i++) {
            proteinRatios.remove(i);
            if (memoryCheck()) {
                return;
            }
        }
        spectrumRatios.clear();
        if (memoryCheck()) {
            return;
        }
        psmRatios.clear();
        if (memoryCheck()) {
            return;
        }
        int keyMax = Collections.max(peptideRatios.keySet());
        for (int i = 10; i <= keyMax; i++) {
            peptideRatios.remove(i);
            if (memoryCheck()) {
                return;
            }
        }
        keyMax = Collections.max(proteinRatios.keySet());
        for (int i = 10; i <= keyMax; i++) {
            proteinRatios.remove(i);
            if (memoryCheck()) {
                return;
            }
        }
        for (String ptm : proteinPtmRatios.keySet()) {
            proteinPtmRatios.remove(ptm);
            if (memoryCheck()) {
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
     * @param matchQuantificationDetails The protein quantification details
     */
    public void addProteinMatchQuantificationDetails(int nPeptides, String matchKey, ProteinQuantificationDetails matchQuantificationDetails) {
        HashMap<String, ProteinQuantificationDetails> submap = proteinRatios.get(nPeptides);
        if (submap == null) {
            submap = new HashMap<String, ProteinQuantificationDetails>();
            proteinRatios.put(nPeptides, submap);
        }
        submap.put(matchKey, matchQuantificationDetails);
        adaptCacheSize();
    }

    /**
     * Returns protein quantification details, null if not in cache.
     *
     * @param nPeptides the number of peptides of this protein match
     * @param matchKey the key of the protein match
     *
     * @return The protein quantification details
     */
    public ProteinQuantificationDetails getProteinMatchQuantificationDetails(int nPeptides, String matchKey) {
        ProteinQuantificationDetails result = null;
        HashMap<String, ProteinQuantificationDetails> submap = proteinRatios.get(nPeptides);
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
     * @param matchQuantificationDetails The protein quantification details
     */
    public void addPtmQuantificationDetails(String ptmName, String matchKey, int site, PtmSiteQuantificationDetails matchQuantificationDetails) {
        HashMap<String, HashMap<String, PtmSiteQuantificationDetails>> submap = proteinPtmRatios.get(ptmName);
        if (submap == null) {
            submap = new HashMap<String, HashMap<String, PtmSiteQuantificationDetails>>();
            proteinPtmRatios.put(ptmName, submap);
        }
        HashMap<String, PtmSiteQuantificationDetails> subsubmap = submap.get(matchKey);
        if (subsubmap == null) {
            subsubmap = new HashMap<String, PtmSiteQuantificationDetails>();
            submap.put(matchKey, subsubmap);
        }
        subsubmap.put(site + "", matchQuantificationDetails); //@TODO: implement site key
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
    public PtmSiteQuantificationDetails getPtmQuantificationDetails(String ptmName, String matchKey, int site) {
        PtmSiteQuantificationDetails result = null;
        HashMap<String, HashMap<String, PtmSiteQuantificationDetails>> submap = proteinPtmRatios.get(ptmName);
        if (submap != null) {
            HashMap<String, PtmSiteQuantificationDetails> subsubmap = submap.get(matchKey);
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
    public void addPeptideMatchQuantificationDetails(int nPeptides, String matchKey, PeptideQuantificationDetails matchQuantificationDetails) {
        HashMap<String, PeptideQuantificationDetails> submap = peptideRatios.get(nPeptides);
        if (submap == null) {
            submap = new HashMap<String, PeptideQuantificationDetails>();
            peptideRatios.put(nPeptides, submap);
        }
        submap.put(matchKey, matchQuantificationDetails);
        adaptCacheSize();
    }

    /**
     * Returns peptide quantification details, null if not in cache.
     *
     * @param nPsms the number of PSMs of this peptide match
     * @param matchKey the key of the peptide match
     *
     * @return The peptide quantification details
     */
    public PeptideQuantificationDetails getPeptideMatchQuantificationDetails(int nPsms, String matchKey) {
        PeptideQuantificationDetails result = null;
        HashMap<String, PeptideQuantificationDetails> submap = peptideRatios.get(nPsms);
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
     * @param matchQuantificationDetails The protein quantification details
     */
    public void addPSMQuantificationDetails(String matchKey, PsmQuantificationDetails matchQuantificationDetails) {
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        HashMap<String, PsmQuantificationDetails> submap = psmRatios.get(spectrumFile);
        if (submap == null) {
            submap = new HashMap<String, PsmQuantificationDetails>();
            psmRatios.put(spectrumFile, submap);
        }
        submap.put(matchKey, matchQuantificationDetails);
        adaptCacheSize();
    }

    /**
     * Returns PSM quantification details, null if not in cache.
     *
     * @param matchKey the key of the spectrum
     *
     * @return The PSM quantification details
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
    public void addSpectrumQuantificationDetails(String matchKey, SpectrumQuantificationDetails matchQuantificationDetails) {
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        HashMap<String, SpectrumQuantificationDetails> submap = spectrumRatios.get(spectrumFile);
        if (submap == null) {
            submap = new HashMap<String, SpectrumQuantificationDetails>();
            spectrumRatios.put(spectrumFile, submap);
        }
        submap.put(matchKey, matchQuantificationDetails);
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
