package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.quantification.Quantification;
import com.compomics.util.experiment.quantification.Ratio;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.matches.PeptideQuantification;
import com.compomics.util.experiment.quantification.matches.ProteinQuantification;
import com.compomics.util.experiment.quantification.matches.PsmQuantification;
import com.compomics.util.math.BasicMathFunctions;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.ItraqScore;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.myparameters.RatioLimits;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;

/**
 * This class estimates ratios at the peptide and protein level.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class RatioEstimator {

    /**
     * The resolution of the method.
     */
    private double resolution;
    /**
     * The "k" of the M-estimator (see PMID ).
     */
    private double k;
    /**
     * The ignorer to be used.
     */
    private Ignorer ignorer;
    /**
     * The reporter ions.
     */
    private ArrayList<ReporterIon> reporterIons;
    /**
     * Shall the calculation ignore null intensities?
     */
    private boolean ignoreNullIntensities;
    /**
     * The deisotoper for PSM quantification.
     */
    private Deisotoper deisotoper;
    /**
     * The quantification.
     */
    private Quantification quantification;

    /**
     * Constructor.
     *
     * @param quantification
     * @param method
     * @param referenceLabel
     * @param quantificationPreferences
     * @param identificationPreferences
     */
    public RatioEstimator(Quantification quantification, ReporterMethod method, QuantificationPreferences quantificationPreferences, Enzyme enzyme) {
        this.quantification = quantification;
        this.deisotoper = new Deisotoper(method);
        this.reporterIons = method.getReporterIons();
        resolution = quantificationPreferences.getRatioResolution();
        k = quantificationPreferences.getK();
        ignorer = new Ignorer(quantificationPreferences, enzyme);
        ignoreNullIntensities = quantificationPreferences.isIgnoreNullIntensities();
    }

    /**
     * Method to estimate ratios at the protein level.
     *
     * @param proteinKey the protein keys
     * @throws Exception
     */
    public void estimateProteinRatios(String proteinKey) throws Exception {

        ProteinQuantification proteinQuantification = quantification.getProteinMatch(proteinKey);
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();

        for (String peptideKey : proteinQuantification.getPeptideQuantification()) {

            PeptideQuantification peptideQuantification = quantification.getPeptideMatch(peptideKey);
            estimatePeptideRatios(peptideKey);
            ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
            HashMap<Integer, Ratio> ratiosMap = peptideQuantification.getRatios();

            for (int ion : peptideQuantification.getRatios().keySet()) {
                if (!ignoredRatios.isIgnored(ion)) {
                    if (!allRatios.containsKey(ion)) {
                        allRatios.put(ion, new ArrayList<Double>());
                    }
                    allRatios.get(ion).add(ratiosMap.get(ion).getRatio());
                }
            }
        }

        HashMap<Integer, Ratio> proteinRatios = new HashMap<Integer, Ratio>();
        RatioLimits limits = new RatioLimits();
        ItraqScore scores = new ItraqScore();

        for (int ion : allRatios.keySet()) {

            ArrayList<Double> ratiosList = allRatios.get(ion);
            double[] ratios = new double[ratiosList.size()];

            for (int i = 0; i < ratiosList.size(); i++) {
                ratios[i] = ratiosList.get(i);
            }

            double ratio = estimateRatios(ratios);
            double window;

            if (ratios.length > 2) {
                window = k * BasicStats.mad(ratios);
            } else if (ratios.length == 2) {
                window = Math.abs(ratios[1] - ratios[0]);
            } else {
                window = resolution;
            }

            int nPeptides = getNPeptides(ion, proteinKey, ratio, window);

            if (nPeptides > 0) {
                double relativeSpread = -(window) / (3 * ratio);
                double quality = Math.pow(10, relativeSpread);
                double score = (1 + quality / 2) * (nPeptides - 1);
                scores.addScore(ion, score);
            }

            proteinRatios.put(ion, new Ratio(ion, ratio));
            limits.addLimits(ion, ratio - window, ratio + window);
        }

        ignoredRatios = new IgnoredRatios();

        for (ReporterIon ion : reporterIons) {
            if (!proteinRatios.containsKey(ion.getIndex())) {
                ignoredRatios.ignore(ion.getIndex());
                proteinRatios.put(ion.getIndex(), new Ratio(ion.getIndex(), Double.NaN));
            }
        }

        proteinQuantification.setRatios(proteinRatios);
        proteinQuantification.addUrParam(ignoredRatios);
        proteinQuantification.addUrParam(scores);
        proteinQuantification.addUrParam(limits);
    }

    /**
     * Returns the number of peptides supporting this protein ratio.
     *
     * @param ion The reporter ion considered
     * @param proteinQuantification The protein quantification considered
     * @param ratio The protein ratio
     * @param window The window indicating the width of the peptide distribution
     * @return the number of peptides in the window centered around the protein
     * ratio
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
     * Returns the peptide ratios of a given peptide quantification.
     *
     * @param peptideQuantification the peptide quantification considered
     */
    private void estimatePeptideRatios(String peptideKey) throws Exception {

        PeptideQuantification peptideQuantification = quantification.getPeptideMatch(peptideKey);
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        HashMap<Integer, ArrayList<Double>> allRatios = new HashMap<Integer, ArrayList<Double>>();

        for (String psmKey : peptideQuantification.getPsmQuantification()) {
            for (String spectrumKey : quantification.getPsmIDentificationToQuantification().get(psmKey)) {

                PsmQuantification psmQuantification = quantification.getSpectrumMatch(spectrumKey);
                estimateRatios(spectrumKey, deisotoper);
                ignoredRatios = (IgnoredRatios) psmQuantification.getUrParam(ignoredRatios);
                HashMap<Integer, Ratio> ratiosMap = psmQuantification.getRatios();

                for (int ion : ratiosMap.keySet()) {
                    if (!ignoredRatios.isIgnored(ion)) {
                        double ratio = ratiosMap.get(ion).getRatio();
                        if (!allRatios.containsKey(ion)) {
                            allRatios.put(ion, new ArrayList<Double>());
                        }
                        allRatios.get(ion).add(ratio);
                    }
                }
            }
        }

        RatioLimits ratioLimits = new RatioLimits();
        HashMap<Integer, Ratio> peptideRatios = new HashMap<Integer, Ratio>();
        ItraqScore scores = new ItraqScore();

        for (int ion : allRatios.keySet()) {

            ArrayList<Double> ratiosList = allRatios.get(ion);
            double[] ratios = new double[ratiosList.size()];

            for (int i = 0; i < ratiosList.size(); i++) {
                ratios[i] = ratiosList.get(i);
            }

            double window;
            double ratio = estimateRatios(ratios);

            if (ratios.length > 2) {
                window = k * BasicStats.mad(ratios);
            } else if (ratios.length == 2) {
                window = Math.abs(ratios[1] - ratios[0]);
            } else {
                window = resolution;
            }

            int nSpectra = getNSpectra(ion, peptideKey, ratio, window);

            if (nSpectra > 0) {
                double quality = Math.pow(10, -(window) / (3 * ratio));
                double score = (1 + quality / 2) * (nSpectra - 1);
                scores.addScore(ion, score);
            }

            peptideRatios.put(ion, new Ratio(ion, ratio));
            ratioLimits.addLimits(ion, ratio - window, ratio + window);
        }

        ignoredRatios = new IgnoredRatios();

        for (ReporterIon possibleIon : reporterIons) {
            if (!allRatios.keySet().contains(possibleIon.getIndex())) {
                ignoredRatios.ignore(possibleIon.getIndex());
                peptideRatios.put(possibleIon.getIndex(), new Ratio(possibleIon.getIndex(), Double.NaN));
            }
        }

        peptideQuantification.setRatios(peptideRatios);
        peptideQuantification.addUrParam(scores);
        peptideQuantification.addUrParam(ratioLimits);
        peptideQuantification.addUrParam(ignoredRatios);
    }

    /**
     * Returns the number of psm supporting a peptide ratio.
     *
     * @param ion The reporter ion considered
     * @param peptideQuantification The processed peptide quantification
     * @param ratio The peptide ratio considered
     * @param window The window representing the width of psm ratios
     * distribution
     * @return the number of psm ratios comprised in the window centered around
     * the peptide ratio
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
     * @param ratios The input ratios
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
     * Returns the compilation of various ratios using a redescending
     * M-estimator.
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
     * Method which estimates ratios for a spectrum.
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
        // avoid negative intensities and deisotoping artifacts
        for (int label : deisotopedInt.keySet()) {
            if (nullIntensities.contains(label)) {
                deisotopedInt.put(label, 0.0);
            }
        }
        psmQuantification.setDeisotopedIntensities(deisotopedInt);

        // User dependant code!
        //String spectrumName = Spectrum.getSpectrumFile(psmQuantification.getKey());
        //deisotopedInt = deisotoper.deisotopeSwitch(reporterMatches, spectrumName);

        Double referenceInt = BasicMathFunctions.median(new ArrayList<Double>(deisotopedInt.values()));
        if (referenceInt == 0.0) {
            referenceInt = Collections.max(deisotopedInt.values());
        }
        psmQuantification.setReferenceIntensity(referenceInt);

        for (int label : deisotopedInt.keySet()) {

            double ratio;

            if (referenceInt > 0) {
                ratio = deisotopedInt.get(label) / referenceInt;
            } else {
                ratio = 0.0;
            }

            psmQuantification.addRatio(label, new Ratio(label, ratio));

            if (ignorer.ignoreRatio(ratio)) {
                ignoredRatios.ignore(label);
            }
        }

        if (ignoreNullIntensities) {
            for (int label : deisotopedInt.keySet()) {
                if (nullIntensities.contains(label)) {
                    ignoredRatios.ignore(label);
                }
            }
        }

        psmQuantification.addUrParam(ignoredRatios);
    }
}
