package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.quantification.Quantification;
import com.compomics.util.experiment.quantification.Ratio;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PsmQuantification;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.ItraqScore;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.myparameters.RatioLimits;
import eu.isas.reporter.preferences.IdentificationPreferences;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.naming.directory.SearchControls;

/**
 * This class estimates ratios at the peptide and protein level
 *
 * @author Marc Vaudel
 */
public class RatioEstimator {

    /**
     * The resolution of the method
     */
    private double resolution;
    /**
     * the "k" of the M-estimator (see PMID )
     */
    private double k;
    /**
     * The ignorer to be used
     */
    private Ignorer ignorer;
    /**
     * The reference label
     */
    private int referenceLabel;
    /**
     * The reporter ions
     */
    private ArrayList<ReporterIon> reporterIons;
    /**
     * Shall the calculation ignore null intensities?
     */
    private boolean ignoreNullIntensities;
    /**
     * the deisotoper for PSM quantification
     */
    private Deisotoper deisotoper;
    /**
     * the quantification
     */
    private Quantification quantification;

    /**
     * constructor
     *
     * @param resolution        The resolution to use
     * @param k                 The k to be used for distribution width estimation
     * @param ignorer           The ignorer to be used
     */
    public RatioEstimator(Quantification quantification, ReporterMethod method, int referenceLabel, QuantificationPreferences quantificationPreferences, IdentificationPreferences identificationPreferences) {
        this.quantification = quantification;
        this.deisotoper = new Deisotoper(method);
        this.reporterIons = method.getReporterIons();
        this.referenceLabel = referenceLabel;
        resolution = quantificationPreferences.getRatioResolution();
        k = quantificationPreferences.getK();
        ignorer = new Ignorer(quantificationPreferences, identificationPreferences);
        ignoreNullIntensities = quantificationPreferences.isIgnoreNullIntensities();
    }

    /**
     * method to estimate ratios at the protein level
     *
     * @param proteinQuantification the processed protein quantification
     */
    public void estimateProteinRatios(String proteinKey) throws Exception {
        ProteinQuantification proteinQuantification = quantification.getProteinMatch(proteinKey);
            IgnoredRatios ignoredRatios = new IgnoredRatios();
            HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();
            HashMap<Integer, Ratio> ratiosMap;
            PeptideQuantification peptideQuantification;
            for (String peptideKey : proteinQuantification.getPeptideQuantification()) {
                peptideQuantification = quantification.getPeptideMatch(peptideKey);
                estimatePeptideRatios(peptideKey);
                ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
                ratiosMap = peptideQuantification.getRatios();
                for (int ion : peptideQuantification.getRatios().keySet()) {
                    if (!ignoredRatios.isIgnored(ion)) {
                        if (!allRatios.containsKey(ion)) {
                            allRatios.put(ion, new ArrayList<Double>());
                        }
                        allRatios.get(ion).add(ratiosMap.get(ion).getRatio());
                    }
                }
            }
            double[] ratios;
            double ratio, window;
            HashMap<Integer, Ratio> proteinRatios = new HashMap<Integer, Ratio>();
            RatioLimits limits = new RatioLimits();
            ItraqScore scores = new ItraqScore();
            int nPeptides;
            double quality, score, relativeSpread;
            for (int ion : allRatios.keySet()) {
                ArrayList<Double> ratiosList = allRatios.get(ion);
                ratios = new double[ratiosList.size()];
                for (int i = 0; i < ratiosList.size(); i++) {
                    ratios[i] = ratiosList.get(i);
                }
                ratio = estimateRatios(ratios);
                if (ratios.length > 2) {
                    window = k * BasicStats.mad(ratios);
                } else if (ratios.length == 2) {
                    window = Math.abs(ratios[1] - ratios[0]);
                } else {
                    window = resolution;
                }
                nPeptides = getNPeptides(ion, proteinKey, ratio, window);
                if (nPeptides > 0) {
                    relativeSpread = -(window) / (3 * ratio);
                    quality = Math.pow(10, relativeSpread);
                    score = (1 + quality / 2) * (nPeptides - 1);
                    scores.addScore(ion, score);
                }
                proteinRatios.put(ion, new Ratio(referenceLabel, ion, ratio));
                limits.addLimits(ion, ratio - window, ratio + window);
            }
            ignoredRatios = new IgnoredRatios();
            for (ReporterIon ion : reporterIons) {
                if (!proteinRatios.containsKey(ion.getIndex())) {
                    ignoredRatios.ignore(ion.getIndex());
                    proteinRatios.put(ion.getIndex(), new Ratio(referenceLabel, ion.getIndex(), Double.NaN));
                }
            }
            proteinQuantification.setRatios(proteinRatios);
            proteinQuantification.addUrParam(ignoredRatios);
            proteinQuantification.addUrParam(scores);
            proteinQuantification.addUrParam(limits);
        }
    

    /**
     * Returns the number of peptides supporting this protein ratio
     *
     * @param ion                       The reporter ion considered
     * @param proteinQuantification     The protein quantification considered
     * @param ratio                     The protein ratio
     * @param window                    The window indicating the width of the peptide distribution
     * @return  the number of peptides in the window centered around the protein ratio
     */
    private int getNPeptides(int ion, String proteinKey, double ratio, double window) throws Exception {
        int nPeptides = 0;
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        ProteinQuantification proteinQuantification = quantification.getProteinMatch(proteinKey);
        PeptideQuantification peptideQuantification;
        for (String peptideKey : proteinQuantification.getPeptideQuantification()) {
            peptideQuantification = quantification.getPeptideMatch(peptideKey);
            ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
            if (!ignoredRatios.isIgnored(ion)) {
                if (peptideQuantification.getRatios().get(ion).getRatio() < ratio + window
                        && peptideQuantification.getRatios().get(ion).getRatio() > ratio - window) {
                    nPeptides++;
                }
            }
        }
        return nPeptides;
    }

    /**
     * Returns the peptide ratios of a given peptide quantification
     *
     * @param peptideQuantification the peptide quantification considered
     */
    private void estimatePeptideRatios(String peptideKey) throws Exception {
        PeptideQuantification peptideQuantification = quantification.getPeptideMatch(peptideKey);
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();
        HashMap<Integer, Ratio> ratiosMap;
        double ratio;
        PsmQuantification psmQuantification;
        for (String psmKey : peptideQuantification.getPsmQuantification()) {
            for (String spectrumKey : quantification.getPsmIDentificationToQuantification().get(psmKey)) {
                psmQuantification = quantification.getSpectrumMatch(spectrumKey);
            estimateRatios(spectrumKey, deisotoper);
            ignoredRatios = (IgnoredRatios) psmQuantification.getUrParam(ignoredRatios);
            ratiosMap = psmQuantification.getRatios();
            for (int ion : ratiosMap.keySet()) {
                if (!ignoredRatios.isIgnored(ion)) {
                    ratio = ratiosMap.get(ion).getRatio();
                    if (!allRatios.containsKey(ion)) {
                        allRatios.put(ion, new ArrayList<Double>());
                    }
                    allRatios.get(ion).add(ratio);
                }
            }
            }
        }
        double[] ratios;
        double window;
        RatioLimits ratioLimits = new RatioLimits();
        HashMap<Integer, Ratio> peptideRatios = new HashMap<Integer, Ratio>();
        ItraqScore scores = new ItraqScore();
        int nSpectra;
        double quality, score;
        for (int ion : allRatios.keySet()) {
            ArrayList<Double> ratiosList = allRatios.get(ion);
            ratios = new double[ratiosList.size()];
            for (int i = 0; i < ratiosList.size(); i++) {
                ratios[i] = ratiosList.get(i);
            }
            ratio = estimateRatios(ratios);
            if (ratios.length > 2) {
                window = k * BasicStats.mad(ratios);
            } else if (ratios.length == 2) {
                window = Math.abs(ratios[1] - ratios[0]);
            } else {
                window = resolution;
            }
            nSpectra = getNSpectra(ion, peptideKey, ratio, window);
            if (nSpectra > 0) {
                quality = Math.pow(10, -(window) / (3 * ratio));
                score = (1 + quality / 2) * (nSpectra - 1);
                scores.addScore(ion, score);
            }
            peptideRatios.put(ion, new Ratio(referenceLabel, ion, ratio));
            ratioLimits.addLimits(ion, ratio - window, ratio + window);
        }
        ignoredRatios = new IgnoredRatios();
        for (ReporterIon possibleIon : reporterIons) {
            if (!allRatios.keySet().contains(possibleIon.getIndex())) {
                ignoredRatios.ignore(possibleIon.getIndex());
                peptideRatios.put(possibleIon.getIndex(), new Ratio(referenceLabel, possibleIon.getIndex(), Double.NaN));
            }
        }
        peptideQuantification.setRatios(peptideRatios);
        peptideQuantification.addUrParam(scores);
        peptideQuantification.addUrParam(ratioLimits);
        peptideQuantification.addUrParam(ignoredRatios);
    }

    /**
     * Returns the number of psm supporting a peptide ratio
     *
     * @param ion                       The reporter ion considered
     * @param peptideQuantification     The processed peptide quantification
     * @param ratio                     The peptide ratio considered
     * @param window                    The window representing the width of psm ratios distribution
     * @return  the number of psm ratios comprised in the window centered around the peptide ratio
     */
    private int getNSpectra(int ion, String peptideKey, double ratio, double window) throws Exception {
        int nSpectra = 0;
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        PeptideQuantification peptideQuantification = quantification.getPeptideMatch(peptideKey);
        PsmQuantification spectrumQuantification;
        for (String psmKey : peptideQuantification.getPsmQuantification()) {
            for (String spectrumKey : quantification.getPsmIDentificationToQuantification().get(psmKey)) {
                spectrumQuantification = quantification.getSpectrumMatch(spectrumKey);
            ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
            if (!ignoredRatios.isIgnored(ion)) {
                if (spectrumQuantification.getRatios().get(ion).getRatio() < ratio + window
                        && spectrumQuantification.getRatios().get(ion).getRatio() > ratio - window) {
                    nSpectra++;
                }
            }
            }
        }
        return nSpectra;
    }

    /**
     * Estimate the ratio resulting from the compilation of several ratios.
     *
     * @param ratios    The input ratios
     * @return the resulting ratio
     */
    private double estimateRatios(double[] ratios) {
        if (ratios.length < 6) {
            return BasicStats.median(ratios);
        } else {
            return mEstimate(ratios);
        }
    }

    /**
     * Returns the compilation of various ratios using a redescending M-estimator
     *
     * @param ratios various imput ratios
     * @return the resulting ratio
     */
    private double mEstimate(double[] ratios) {
        Arrays.sort(ratios);
        double window = k * BasicStats.mad(ratios);
        if (window == 0) {
            return ratios[0];
        }
        int nPeptides, nPeptidesMax = 0;
        for (double r0 = ratios[0]; r0 < ratios[ratios.length - 1]; r0 += 10 * resolution) {
            nPeptides = 0;
            for (double ratio : ratios) {
                if (Math.abs(ratio - r0) < window) {
                    nPeptides++;
                }
            }
            nPeptidesMax = Math.max(nPeptidesMax, nPeptides);
        }
        double integral, bestIntegral = -1;
        ArrayList<Double> bestRatios = new ArrayList<Double>();
        for (double r0 = ratios[0]; r0 < ratios[ratios.length - 1]; r0 += resolution) {
            integral = 0;
            nPeptides = 0;
            for (double r : ratios) {
                if (Math.abs(r - r0) < window) {
                    nPeptides++;
                    integral += (r - r0) * Math.pow(1 - Math.pow((r - r0) / window, 2), 2);
                }
            }
            integral = Math.abs(integral);
            if (nPeptides > 0.9 * nPeptidesMax) {
                if (integral == bestIntegral || bestIntegral == -1) {
                    bestRatios.add(r0);
                } else if (integral < bestIntegral) {
                    bestIntegral = integral;
                    bestRatios = new ArrayList<Double>();
                    bestRatios.add(r0);
                }
            }
        }
        if (bestRatios.size() == 1) {
            return bestRatios.get(0);
        } else {
            double summ = 0;
            for (double ratio : bestRatios) {
                summ += ratio;
            }
            summ = summ / bestRatios.size();
            return summ;
        }
    }

    /**
     * Method which estimates ratios for a spectrum
     *
     * @param psmQuantification the current spectrum quantification
     */
    private void estimateRatios(String spectrumKey, Deisotoper deisotoper) throws Exception {
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        PsmQuantification psmQuantification = quantification.getSpectrumMatch(spectrumKey);
        HashMap<Integer, IonMatch> reporterMatches = psmQuantification.getReporterMatches();
        ArrayList<Integer> nullIntensities = new ArrayList<Integer>();
        ItraqScore scores = new ItraqScore();
        for (ReporterIon ion : reporterIons) {
            if (reporterMatches.get(ion.getIndex()) == null) {
                nullIntensities.add(ion.getIndex());
            } else {
                scores.addScore(ion.getIndex(), Math.log10(reporterMatches.get(ion.getIndex()).peak.intensity));
            }
        }
        psmQuantification.addUrParam(scores);
        HashMap<Integer, Double> deisotopedInt = deisotoper.deisotope(reporterMatches);
        
        // User dependant code, do not commit!
        //String spectrumName = Spectrum.getSpectrumFile(psmQuantification.getKey());
        //deisotopedInt = deisotoper.deisotopeSwitch(reporterMatches, spectrumName);

        Double referenceInt = deisotopedInt.get(referenceLabel);
        double ratio;
        for (int label : deisotopedInt.keySet()) {
            if (referenceInt > 0 && !nullIntensities.contains(referenceLabel)) {
                ratio = deisotopedInt.get(label) / referenceInt;
            } else if (label != referenceLabel) {
                ratio = 9 * Math.pow(10, 99);
            } else {
                ratio = 0.0;
            }
            psmQuantification.addRatio(label, new Ratio(referenceLabel, label, ratio));
            if (ignorer.ignoreRatio(ratio)) {
                ignoredRatios.ignore(label);
            }
        }
        if (ignoreNullIntensities) {
            if (nullIntensities.contains(referenceLabel)) {
                for (int label : deisotopedInt.keySet()) {
                    ignoredRatios.ignore(label);
                }
            } else {
                for (int label : deisotopedInt.keySet()) {
                    if (nullIntensities.contains(label)) {
                        ignoredRatios.ignore(label);
                    }
                }
            }
        } else {
            if (nullIntensities.contains(referenceLabel)) {
                deisotopedInt.put(referenceLabel, 0.0);
                for (int label : deisotopedInt.keySet()) {
                    if (nullIntensities.contains(label)) {
                        deisotopedInt.put(label, 0.0);
                    } else {
                        deisotopedInt.put(label, 9 * Math.pow(10, 99));
                    }
                }
            } else if (deisotopedInt.get(referenceLabel) > 0) {
                for (int label : deisotopedInt.keySet()) {
                    if (nullIntensities.contains(label)) {
                        deisotopedInt.put(label, 0.0);
                    }
                }
            }
        }
        psmQuantification.addUrParam(ignoredRatios);
    }
}
