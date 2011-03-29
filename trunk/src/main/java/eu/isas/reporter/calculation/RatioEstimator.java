package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.quantification.Ratio;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PsmQuantification;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.ItraqScore;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.myparameters.RatioLimits;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

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
     * Ignore non validated hits
     */
    private boolean onlyValidated;
    /**
     * the deisotoper for PSM quantification
     */
    private Deisotoper deisotoper;

    /**
     * constructor
     *
     * @param resolution        The resolution to use
     * @param k                 The k to be used for distribution width estimation
     * @param ignorer           The ignorer to be used
     */
    public RatioEstimator(ReporterMethod method, int referenceLabel, QuantificationPreferences quantificationPreferences) {
        this.deisotoper = new Deisotoper(method);
        this.reporterIons = method.getReporterIons();
        this.referenceLabel = referenceLabel;
        resolution = quantificationPreferences.getRatioResolution();
        k = quantificationPreferences.getK();
        ignorer = new Ignorer(quantificationPreferences);
        ignoreNullIntensities = quantificationPreferences.isIgnoreNullIntensities();
        onlyValidated = quantificationPreferences.isOnlyValidated();
    }

    /**
     * method to estimate ratios at the protein level
     *
     * @param proteinQuantification the processed protein quantification
     */
    public void estimateRatios(ProteinQuantification proteinQuantification) {
        boolean validated = true;
        try {
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) proteinQuantification.getProteinMatch().getUrParam(psParameter);
            validated = psParameter.isValidated();
        } catch (Exception e) {
            // Not Peptide-shaker identification: ignore
        }
        if (validated || !onlyValidated) {
            IgnoredRatios ignoredRatios = new IgnoredRatios();
            HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();
            HashMap<Integer, Ratio> ratiosMap;
            for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification().values()) {
                estimateRatios(peptideQuantification);
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
                nPeptides = getNPeptides(ion, proteinQuantification, ratio, window);
                if (nPeptides > 0) {
                    relativeSpread = -(window) / (3 * ratio);
                    quality = Math.pow(10, relativeSpread);
                    score = (1 + quality / 2) * (nPeptides - 1);
                    scores.addScore(ion, score);
                }
                proteinRatios.put(ion, new Ratio(referenceLabel, ion, ratio));
                limits.addLimits(ion, ratio - window, ratio + window);
            }
            proteinQuantification.setProteinRatios(proteinRatios);
            ignoredRatios = new IgnoredRatios();
            for (ReporterIon ion : reporterIons) {
                if (!proteinRatios.containsKey(ion.getIndex())) {
                    ignoredRatios.ignore(ion.getIndex());
                }
            }
            proteinQuantification.addUrParam(ignoredRatios);
            proteinQuantification.addUrParam(scores);
            proteinQuantification.addUrParam(limits);
        }
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
    private int getNPeptides(int ion, ProteinQuantification proteinQuantification, double ratio, double window) {
        int nPeptides = 0;
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification().values()) {
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
    private void estimateRatios(PeptideQuantification peptideQuantification) {
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();
        HashMap<Integer, Ratio> ratiosMap;
        double ratio;
        for (PsmQuantification psmQuantification : peptideQuantification.getPsmQuantification().values()) {
            estimateRatios(psmQuantification, deisotoper);
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
            nSpectra = getNSpectra(ion, peptideQuantification, ratio, window);
            if (nSpectra > 0) {
                quality = Math.pow(10, -(window) / (3 * ratio));
                score = (1 + quality / 2) * (nSpectra - 1);
                scores.addScore(ion, score);
            }
            peptideRatios.put(ion, new Ratio(referenceLabel, ion, ratio));
            ratioLimits.addLimits(ion, ratio - window, ratio + window);
        }
        peptideQuantification.setPeptideRatios(peptideRatios);
        ignoredRatios = new IgnoredRatios();
        boolean validated = true;
        try {
            PSParameter psParameter = new PSParameter();
            psParameter = (PSParameter) peptideQuantification.getPeptideMatch().getUrParam(psParameter);
            validated = psParameter.isValidated();
        } catch (Exception e) {
            // Not Peptide-shaker identification: ignore
        }
        for (ReporterIon reporterIon : reporterIons) {
            if (onlyValidated && !validated
                    || ignorer.ignorePeptide(peptideQuantification.getPeptideMatch().getTheoreticPeptide())
                    || !peptideRatios.containsKey(reporterIon.getIndex())
                    || ignorer.ignoreRatio(peptideRatios.get(reporterIon.getIndex()).getRatio())) {
                ignoredRatios.ignore(reporterIon.getIndex());
            }
        }
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
    private int getNSpectra(int ion, PeptideQuantification peptideQuantification, double ratio, double window) {
        int nSpectra = 0;
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        for (PsmQuantification spectrumQuantification : peptideQuantification.getPsmQuantification().values()) {
            ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
            if (!ignoredRatios.isIgnored(ion)) {
                if (spectrumQuantification.getRatios().get(ion).getRatio() < ratio + window
                        && spectrumQuantification.getRatios().get(ion).getRatio() > ratio - window) {
                    nSpectra++;
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
    private void estimateRatios(PsmQuantification psmQuantification, Deisotoper deisotoper) {
        IgnoredRatios ignoredRatios = new IgnoredRatios();
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
