/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.reporter.calculation;

import com.compomics.util.experiment.massspectrometry.Spectrum;
import java.util.Collections;
import java.util.HashMap;

/**
 * The quantification features cache stores quantification features
 *
 * @author Marc
 */
public class QuantificationFeaturesCache {

    /**
     * Share of the memory to be used.
     */
    private double memoryShare = 0.99;
    /**
     * The protein quantification details in a map: number of peptides > protein
     * match key > match quantification details
     */
    private HashMap<Integer, HashMap<String, MatchQuantificationDetails>> proteinRatios = new HashMap<Integer, HashMap<String, MatchQuantificationDetails>>();
    /**
     * The protein level ptm quantification details in a map: ptm name > protein
     * match key > site > match quantification details
     */
    private HashMap<String, HashMap<String, HashMap<Integer, MatchQuantificationDetails>>> proteinPtmRatios = new HashMap<String, HashMap<String, HashMap<Integer, MatchQuantificationDetails>>>();
    /**
     * The peptide quantification details in a map: number of psms > peptide
     * match key > match quantification details
     */
    private HashMap<Integer, HashMap<String, MatchQuantificationDetails>> peptideRatios = new HashMap<Integer, HashMap<String, MatchQuantificationDetails>>();
    /**
     * The PSM quantification details in a map: Spectrum file name > spectrum
     * key > match quantification details
     */
    private HashMap<String, HashMap<String, MatchQuantificationDetails>> psmRatios = new HashMap<String, HashMap<String, MatchQuantificationDetails>>();
    /**
     * The spectrum quantification details in a map: Spectrum file name >
     * spectrum key > match quantification details Note: this is used in
     * precursor matching mode only, otherwise the spectrum ratios are the same
     * as the psm ratios
     */
    private HashMap<String, HashMap<String, MatchQuantificationDetails>> spectrumRatios = new HashMap<String, HashMap<String, MatchQuantificationDetails>>();

    /**
     * Constructor
     */
    public QuantificationFeaturesCache() {

    }

    /**
     * Checks whether there is still memory left and empties the cache if not.
     */
    private void adaptCacheSize() {
        if (memoryCheck()) {
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
            proteinPtmRatios.get(ptm).clear();
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
        return Runtime.getRuntime().totalMemory() < (long) (memoryShare * Runtime.getRuntime().maxMemory());
    }

    /**
     * Adds protein quantification details to the cache.
     *
     * @param nPeptides the number of peptides of this protein match
     * @param matchKey the key of the protein match
     * @param matchQuantificationDetails The protein quantification details
     */
    public void addProteinMatchQuantificationDetails(int nPeptides, String matchKey, MatchQuantificationDetails matchQuantificationDetails) {
        HashMap<String, MatchQuantificationDetails> submap = proteinRatios.get(nPeptides);
        if (submap == null) {
            submap = new HashMap<String, MatchQuantificationDetails>();
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
    public MatchQuantificationDetails getProteinMatchQuantificationDetails(int nPeptides, String matchKey) {
        MatchQuantificationDetails result = null;
        HashMap<String, MatchQuantificationDetails> submap = proteinRatios.get(nPeptides);
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
    public void addPtmQuantificationDetails(String ptmName, String matchKey, int site, MatchQuantificationDetails matchQuantificationDetails) {
        HashMap<String, HashMap<Integer, MatchQuantificationDetails>> submap = proteinPtmRatios.get(ptmName);
        if (submap == null) {
            submap = new HashMap<String, HashMap<Integer, MatchQuantificationDetails>>();
            proteinPtmRatios.put(ptmName, submap);
        }
        HashMap<Integer, MatchQuantificationDetails> subsubmap = submap.get(matchKey);
        if (subsubmap == null) {
            subsubmap = new HashMap<Integer, MatchQuantificationDetails>();
            submap.put(matchKey, subsubmap);
        }
        subsubmap.put(site, matchQuantificationDetails);
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
    public MatchQuantificationDetails getPtmQuantificationDetails(String ptmName, String matchKey, int site) {
        MatchQuantificationDetails result = null;
        HashMap<String, HashMap<Integer, MatchQuantificationDetails>> submap = proteinPtmRatios.get(ptmName);
        if (submap != null) {
            HashMap<Integer, MatchQuantificationDetails> subsubmap = submap.get(matchKey);
            if (subsubmap != null) {
                result = subsubmap.get(site);
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
    public void addPeptideMatchQuantificationDetails(int nPeptides, String matchKey, MatchQuantificationDetails matchQuantificationDetails) {
        HashMap<String, MatchQuantificationDetails> submap = peptideRatios.get(nPeptides);
        if (submap == null) {
            submap = new HashMap<String, MatchQuantificationDetails>();
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
    public MatchQuantificationDetails getPeptideMatchQuantificationDetails(int nPsms, String matchKey) {
        MatchQuantificationDetails result = null;
        HashMap<String, MatchQuantificationDetails> submap = peptideRatios.get(nPsms);
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
    public void addPSMQuantificationDetails(String matchKey, MatchQuantificationDetails matchQuantificationDetails) {
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        HashMap<String, MatchQuantificationDetails> submap = psmRatios.get(spectrumFile);
        if (submap == null) {
            submap = new HashMap<String, MatchQuantificationDetails>();
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
    public MatchQuantificationDetails getPSMQuantificationDetails(String matchKey) {
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        MatchQuantificationDetails result = null;
        HashMap<String, MatchQuantificationDetails> submap = psmRatios.get(spectrumFile);
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
    public void addSpectrumQuantificationDetails(String matchKey, MatchQuantificationDetails matchQuantificationDetails) {
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        HashMap<String, MatchQuantificationDetails> submap = spectrumRatios.get(spectrumFile);
        if (submap == null) {
            submap = new HashMap<String, MatchQuantificationDetails>();
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
    public MatchQuantificationDetails getSpectrumQuantificationDetails(String matchKey) {
        String spectrumFile = Spectrum.getSpectrumFile(matchKey);
        MatchQuantificationDetails result = null;
        HashMap<String, MatchQuantificationDetails> submap = spectrumRatios.get(spectrumFile);
        if (submap != null) {
            result = submap.get(matchKey);
        }
        adaptCacheSize();
        return result;
    }
}
