package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.quantification.Ratio;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.SpectrumQuantification;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.ItraqScore;
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
     * The quantification in processing
     */
    private ReporterIonQuantification quantification;
    /**
     * The ignorer to be used
     */
    private Ignorer ignorer;

    /**
     * constructor
     *
     * @param quantification    The quantification in processing
     * @param resolution        The resolution to use
     * @param k                 The k to be used for distribution width estimation
     * @param ignorer           The ignorer to be used
     */
    public RatioEstimator(ReporterIonQuantification quantification, double resolution, double k, Ignorer ignorer) {
        this.resolution = resolution;
        this.k = k;
        this.quantification = quantification;
        this.ignorer = ignorer;
    }

    /**
     * method to estimate ratios at the protein level
     *
     * @param proteinQuantification the processed protein quantification
     */
    public void estimateRatios(ProteinQuantification proteinQuantification) {
        for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
            estimateRatios(peptideQuantification);
        }
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();
        HashMap<Integer, Ratio> ratiosMap;
        for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
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
            proteinRatios.put(ion, new Ratio(quantification.getReferenceLabel(), ion, ratio));
            limits.addLimits(ion, ratio - window, ratio + window);
        }
        proteinQuantification.setProteinRatios(proteinRatios);
        ignoredRatios = (IgnoredRatios) proteinQuantification.getUrParam(ignoredRatios);
        for (ReporterIon ion : quantification.getReporterMethod().getReporterIons()) {
            if (!proteinRatios.containsKey(ion.getIndex())) {
                ignoredRatios.ignore(ion.getIndex());
            }
        }
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
    private int getNPeptides(int ion, ProteinQuantification proteinQuantification, double ratio, double window) {
        int nPeptides = 0;
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
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
        double ratio, intensity;
        ItraqScore scores;
        for (SpectrumQuantification spectrumQuantification : peptideQuantification.getSpectrumQuantification()) {
            scores = new ItraqScore();
            ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
            ratiosMap = spectrumQuantification.getRatios();
            for (int ion : spectrumQuantification.getRatios().keySet()) {
                if (!ignoredRatios.isIgnored(ion)) {
                    ratio = ratiosMap.get(ion).getRatio();
                    if (ignorer.ignoreRatio(ratio) || ignorer.ignorePeptide(peptideQuantification.getPeptideMatch().getTheoreticPeptide())) {
                        ignoredRatios.ignore(ion);
                    } else {
                        if (!allRatios.containsKey(ion)) {
                            allRatios.put(ion, new ArrayList<Double>());
                        }
                        allRatios.get(ion).add(ratio);
                        intensity = spectrumQuantification.getReporterMatches().get(ion).peak.intensity;
                        scores.addScore(ion, Math.log10(intensity));
                    }
                }
            }
            spectrumQuantification.addUrParam(scores);
        }
        double[] ratios;
        double window;
        RatioLimits ratioLimits = new RatioLimits();
        HashMap<Integer, Ratio> peptideRatios = new HashMap<Integer, Ratio>();
        scores = new ItraqScore();
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
            peptideRatios.put(ion, new Ratio(quantification.getReferenceLabel(), ion, ratio));
            ratioLimits.addLimits(ion, ratio - window, ratio + window);
        }
        peptideQuantification.setPeptideRatios(peptideRatios);
        ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
        for (ReporterIon ion : quantification.getReporterMethod().getReporterIons()) {
            if (!peptideRatios.containsKey(ion.getIndex())) {
                ignoredRatios.ignore(ion.getIndex());
            }
        }
        peptideQuantification.addUrParam(scores);
        peptideQuantification.addUrParam(ratioLimits);
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
        for (SpectrumQuantification spectrumQuantification : peptideQuantification.getSpectrumQuantification()) {
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
}
