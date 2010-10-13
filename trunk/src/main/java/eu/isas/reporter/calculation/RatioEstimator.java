package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.quantification.Ratio;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.SpectrumQuantification;
import eu.isas.reporter.compomicsutilitiessettings.CompomicsKeysFactory;
import eu.isas.reporter.compomicsutilitiessettings.IgnoredRatios;
import eu.isas.reporter.compomicsutilitiessettings.ItraqScore;
import eu.isas.reporter.compomicsutilitiessettings.RatioLimits;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

/**
 *
 * @author Marc
 */
public class RatioEstimator {

    private double resolution;
    private double k;
    private double ratioMin;
    private double ratioMax;
    private CompomicsKeysFactory compomicsKeyFactory = CompomicsKeysFactory.getInstance();
    private ReporterIonQuantification quantification;

    public RatioEstimator(ReporterIonQuantification quantification, double resolution, double k, double ratioMin, double ratioMax) {
        this.resolution = resolution;
        this.k = k;
        this.ratioMin = ratioMin;
        this.ratioMax = ratioMax;
        this.quantification = quantification;
    }

    public void estimateRatios(ProteinQuantification proteinQuantification) {
        for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
            estimateRatios(peptideQuantification);
        }
        IgnoredRatios ignoredRatios;
        HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();
        HashMap<Integer, Ratio> ratiosMap;
        for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
            ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
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
        ignoredRatios = (IgnoredRatios) proteinQuantification.getUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
        for (ReporterIon ion : quantification.getMethod().getReporterIons()) {
            if (!proteinRatios.containsKey(ion.getIndex())) {
                ignoredRatios.ignore(ion.getIndex());
            }
        }
        proteinQuantification.addUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.SCORE), scores);
        proteinQuantification.addUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.RATIO_LIMITS), limits);
    }

    private int getNPeptides(int ion, ProteinQuantification proteinQuantification, double ratio, double window) {
        int nPeptides = 0;
        IgnoredRatios ignoredRatios;
        for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
            ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
            if (!ignoredRatios.isIgnored(ion)) {
                if (peptideQuantification.getRatios().get(ion).getRatio() < ratio + window
                        && peptideQuantification.getRatios().get(ion).getRatio() > ratio - window) {
                    nPeptides++;
                }
            }
        }
        return nPeptides;
    }

    private void estimateRatios(PeptideQuantification peptideQuantification) {
        IgnoredRatios ignoredRatios;
        HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();
        HashMap<Integer, Ratio> ratiosMap;
        double ratio, intensity;
        ItraqScore scores;
        for (SpectrumQuantification spectrumQuantification : peptideQuantification.getSpectrumQuantification()) {
            scores = new ItraqScore();
            ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
            ratiosMap = spectrumQuantification.getRatios();
            for (int ion : spectrumQuantification.getRatios().keySet()) {
                if (!ignoredRatios.isIgnored(ion)) {
                    ratio = ratiosMap.get(ion).getRatio();
                    if (ratio < ratioMin || ratio > ratioMax) {
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
            spectrumQuantification.addUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.SCORE), scores);
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
        ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
        for (ReporterIon ion : quantification.getMethod().getReporterIons()) {
            if (!peptideRatios.containsKey(ion.getIndex())) {
                ignoredRatios.ignore(ion.getIndex());
            }
        }
        peptideQuantification.addUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.SCORE), scores);
        peptideQuantification.addUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.RATIO_LIMITS), ratioLimits);
    }

    private int getNSpectra(int ion, PeptideQuantification peptideQuantification, double ratio, double window) {
        int nSpectra = 0;
        IgnoredRatios ignoredRatios;
        for (SpectrumQuantification spectrumQuantification : peptideQuantification.getSpectrumQuantification()) {
            ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
            if (!ignoredRatios.isIgnored(ion)) {
                if (spectrumQuantification.getRatios().get(ion).getRatio() < ratio + window
                        && spectrumQuantification.getRatios().get(ion).getRatio() > ratio - window) {
                    nSpectra++;
                }
            }
        }
        return nSpectra;
    }

    private double estimateRatios(double[] ratios) {
        if (ratios.length < 6) {
            return BasicStats.median(ratios);
        } else {
            return mEstimate(ratios);
        }
    }

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
        double integral, ratio = -1, bestIntegral = -1;
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
            if (nPeptides > 0.8 * nPeptidesMax && integral < bestIntegral || bestIntegral == -1) {
                bestIntegral = integral;
                ratio = r0;
            }
        }
        return ratio;
    }
}
