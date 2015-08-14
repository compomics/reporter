package eu.isas.reporter.calculation.normalization;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFilter;
import eu.isas.reporter.settings.NormalizationSettings;
import eu.isas.reporter.settings.RatioEstimationSettings;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Normalizes the ratios according to the NormalizationSettings.
 *
 * @author Marc Vaudel
 */
public class Normalizer {

    /**
     * Sets the normalization factors in the ReporterIonQuantification object.
     *
     * @param reporterIonQuantification the reporter ion quantification
     * @param ratioEstimationSettings the ratio estimation settings
     * @param normalizationSettings the normalization settings
     * @param sequenceMatchingPreferences the peptide to protein sequence
     * matching preferences
     * @param identification the identification
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param exceptionHandler handler in case exception occur
     * @param waitingHandler waiting handler displaying progress to the user
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     */
    public void setPeptideNormalizationFactors(ReporterIonQuantification reporterIonQuantification, RatioEstimationSettings ratioEstimationSettings, NormalizationSettings normalizationSettings, SequenceMatchingPreferences sequenceMatchingPreferences,
            Identification identification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ExceptionHandler exceptionHandler, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        HashMap<String, ArrayList<Double>> allRawRatios = new HashMap<String, ArrayList<Double>>();
        HashMap<String, ArrayList<Double>> seedRawRatios = new HashMap<String, ArrayList<Double>>();
        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            allRawRatios.put(sampleIndex, new ArrayList<Double>());
            seedRawRatios.put(sampleIndex, new ArrayList<Double>());
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        if (normalizationSettings.getPeptideNormalization() != NormalizationType.none) {

            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Ratio Normalization. Please Wait...");
                waitingHandler.resetPrimaryProgressCounter();
                waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size());
            }

            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, true, parameters, waitingHandler);

            HashSet<String> seeds = normalizationSettings.getStableProteins();
            HashSet<String> exclusion = normalizationSettings.getContaminants();

            int nThreads = Runtime.getRuntime().availableProcessors(); //@TODO: make this a user parameter
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            ArrayList<PeptideNormalizerRunnable> peptideNormalizerRunnables = new ArrayList<PeptideNormalizerRunnable>(nThreads);

            for (int i = 1; i <= nThreads && waitingHandler != null && !waitingHandler.isRunCanceled(); i++) {
                PeptideNormalizerRunnable peptideNormalizerRunnable = new PeptideNormalizerRunnable(reporterIonQuantification, quantificationFeaturesGenerator, identification, peptideMatchesIterator, seeds, exclusion, ratioEstimationSettings, sequenceMatchingPreferences, waitingHandler, exceptionHandler);
                pool.submit(peptideNormalizerRunnable);
                peptideNormalizerRunnables.add(peptideNormalizerRunnable);
            }
            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                pool.shutdownNow();
                return;
            }
            pool.shutdown();
            if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
                throw new InterruptedException("PSM validation timed out. Please contact the developers.");
            }
            for (PeptideNormalizerRunnable peptideNormalizerRunnable : peptideNormalizerRunnables) {
                for (String reagent : peptideNormalizerRunnable.getAllRawRatios().keySet()) {
                    ArrayList<Double> ratios = allRawRatios.get(reagent);
                    if (ratios == null) {
                        ratios = new ArrayList<Double>();
                        allRawRatios.put(reagent, ratios);
                    }
                    ratios.addAll(peptideNormalizerRunnable.getAllRawRatios().get(reagent));
                }
                for (String reagent : peptideNormalizerRunnable.getSeedRawRatios().keySet()) {
                    ArrayList<Double> ratios = seedRawRatios.get(reagent);
                    if (ratios == null) {
                        ratios = new ArrayList<Double>();
                        seedRawRatios.put(reagent, ratios);
                    }
                    ratios.addAll(peptideNormalizerRunnable.getSeedRawRatios().get(reagent));
                }
            }
        }

        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            double normalisationFactor;
            ArrayList<Double> rawRatios = allRawRatios.get(sampleIndex);
            ArrayList<Double> seedRatios = seedRawRatios.get(sampleIndex);
            if (allRawRatios.get(sampleIndex) != null && !rawRatios.isEmpty()) {
                if (normalizationSettings.getPeptideNormalization() == NormalizationType.none) {
                    normalisationFactor = 1;
                } else if (normalizationSettings.getPeptideNormalization() == NormalizationType.mean) {
                    if (seedRatios != null && !seedRatios.isEmpty()) {
                        normalisationFactor = BasicMathFunctions.mean(seedRatios);
                    } else {
                        normalisationFactor = BasicMathFunctions.mean(rawRatios);
                    }
                } else if (normalizationSettings.getPeptideNormalization() == NormalizationType.median) {
                    if (seedRatios != null && !seedRatios.isEmpty()) {
                        normalisationFactor = BasicMathFunctions.mean(seedRatios);
                    } else {
                        normalisationFactor = BasicMathFunctions.median(rawRatios);
                    }
                } else if (normalizationSettings.getPeptideNormalization() == NormalizationType.mode) {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                } else if (normalizationSettings.getPeptideNormalization() == NormalizationType.sum) {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                } else {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                }
            } else {
                normalisationFactor = 1;
            }
            reporterIonQuantification.addNormalisationFactor(sampleIndex, normalisationFactor);
        }
    }

    /**
     * Indicates whether all the given accessions are seed proteins.
     *
     * @param seeds the list of seed proteins
     * @param accessions the accessions to inspect
     *
     * @return a boolean indicating whether all the given accessions are seed
     * proteins
     */
    private static boolean isSeed(HashSet<String> seeds, ArrayList<String> accessions) {
        for (String accession : accessions) {
            if (!seeds.contains(accession)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Indicates whether one of the given accessions is contaminant.
     *
     * @param contaminants the list of contaminant proteins
     * @param accessions the accessions to inspect
     *
     * @return a boolean indicating whether all the given accessions are seed
     * proteins
     */
    private static boolean isContaminant(HashSet<String> contaminants, ArrayList<String> accessions) {
        for (String accession : accessions) {
            if (!contaminants.contains(accession)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runnable gathering values for the normalization of peptides.
     *
     * @author Marc Vaudel
     */
    private class PeptideNormalizerRunnable implements Runnable {

        /**
         * The reporter ion quantification.
         */
        private ReporterIonQuantification reporterIonQuantification;
        /**
         * An iterator for the peptide matches.
         */
        private PeptideMatchesIterator peptideMatchesIterator;
        /**
         * The identification.
         */
        private Identification identification;
        /**
         * The quantification features generator.
         */
        private QuantificationFeaturesGenerator quantificationFeaturesGenerator;
        /**
         * The seed proteins.
         */
        private HashSet<String> seeds;
        /**
         * The excluded proteins.
         */
        private HashSet<String> exclusion;
        /**
         * The peptide to protein sequence matching preferences.
         */
        private SequenceMatchingPreferences sequenceMatchingPreferences;
        /**
         * The ratio estimation settings.
         */
        private RatioEstimationSettings ratioEstimationSettings;
        /**
         * The raw peptide ratios gathered in a map.
         */
        private HashMap<String, ArrayList<Double>> allRawRatios = new HashMap<String, ArrayList<Double>>();
        /**
         * The raw seed peptide ratios gathered in a map.
         */
        private HashMap<String, ArrayList<Double>> seedRawRatios = new HashMap<String, ArrayList<Double>>();
        /**
         * The waiting handler.
         */
        private WaitingHandler waitingHandler;
        /**
         * Handler for the exceptions.
         */
        private ExceptionHandler exceptionHandler;

        /**
         * Constructor.
         *
         * @param reporterIonQuantification the reporter ion quantification
         * object
         * @param quantificationFeaturesGenerator the quantification features
         * generator
         * @param identification the identification object
         * @param peptideMatchesIterator the iterator of the peptide matches
         * @param seeds the seed proteins
         * @param exclusion the exclusion proteins
         * @param ratioEstimationSettings the ratio estimation settings
         * @param sequenceMatchingPreferences the sequence matching preferences
         * @param waitingHandler a waiting handler
         * @param exceptionHandler an exception handler
         */
        public PeptideNormalizerRunnable(ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, Identification identification, PeptideMatchesIterator peptideMatchesIterator, HashSet<String> seeds, HashSet<String> exclusion, RatioEstimationSettings ratioEstimationSettings, SequenceMatchingPreferences sequenceMatchingPreferences, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
            this.reporterIonQuantification = reporterIonQuantification;
            this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
            this.peptideMatchesIterator = peptideMatchesIterator;
            this.identification = identification;
            this.seeds = seeds;
            this.exclusion = exclusion;
            this.sequenceMatchingPreferences = sequenceMatchingPreferences;
            this.ratioEstimationSettings = ratioEstimationSettings;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void run() {
            try {

                PSParameter psParameter = new PSParameter();

                while (peptideMatchesIterator.hasNext()) {

                    PeptideMatch peptideMatch = peptideMatchesIterator.next();

                    if (peptideMatch != null) {
                        Peptide peptide = peptideMatch.getTheoreticPeptide();

                        if (exclusion == null || !isContaminant(exclusion, peptide.getParentProteins(sequenceMatchingPreferences))) {
                            String peptideKey = peptideMatch.getKey();
                            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

                            if (psParameter.getMatchValidationLevel().isValidated()) {

                                PeptideQuantificationDetails matchQuantificationDetails = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch, waitingHandler);

                                for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
                                    Double ratio = matchQuantificationDetails.getRawRatio(sampleIndex);
                                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio) && ratio > 0) {
                                        ArrayList<Double> ratios = allRawRatios.get(sampleIndex);
                                        if (ratios == null) {
                                            ratios = new ArrayList<Double>();
                                            allRawRatios.put(sampleIndex, ratios);
                                        }
                                        ratios.add(ratio);
                                        if (seeds != null && isSeed(seeds, peptide.getParentProteins(sequenceMatchingPreferences))) {
                                            ratios = seedRawRatios.get(sampleIndex);
                                            if (ratios == null) {
                                                ratios = new ArrayList<Double>();
                                                seedRawRatios.put(sampleIndex, ratios);
                                            }
                                            ratios.add(ratio);
                                        }
                                    }
                                }
                            }

                            if (waitingHandler != null) {
                                if (waitingHandler.isRunCanceled()) {
                                    return;
                                }
                                waitingHandler.increaseSecondaryProgressCounter();
                            }
                        }
                    }
                }

            } catch (Exception e) {
                waitingHandler.setRunCanceled();
                exceptionHandler.catchException(e);
            }
        }

        /**
         * Returns the raw ratios found while iterating.
         *
         * @return the raw ratios found while iterating
         */
        public HashMap<String, ArrayList<Double>> getAllRawRatios() {
            return allRawRatios;
        }

        /**
         * Returns the seed raw ratios found while iterating.
         *
         * @return the seed raw ratios found while iterating
         */
        public HashMap<String, ArrayList<Double>> getSeedRawRatios() {
            return seedRawRatios;
        }
    }
}
