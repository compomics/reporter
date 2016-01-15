package eu.isas.reporter.calculation.normalization;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.normalization.NormalizationFactors;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFilter;
import eu.isas.reporter.settings.NormalizationSettings;
import eu.isas.reporter.settings.RatioEstimationSettings;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
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
     * Sets the PSM normalization factors in the ReporterIonQuantification
     * object.
     *
     * @param reporterIonQuantification the reporter ion quantification
     * @param ratioEstimationSettings the ratio estimation settings
     * @param normalizationSettings the normalization settings
     * @param sequenceMatchingPreferences the peptide to protein sequence
     * matching preferences
     * @param identification the identification
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param processingPreferences the processing preferences
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
    public void setPsmNormalizationFactors(ReporterIonQuantification reporterIonQuantification, RatioEstimationSettings ratioEstimationSettings, NormalizationSettings normalizationSettings, SequenceMatchingPreferences sequenceMatchingPreferences,
            Identification identification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ProcessingPreferences processingPreferences, ExceptionHandler exceptionHandler, WaitingHandler waitingHandler)
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

        if (normalizationSettings.getPsmNormalization() != NormalizationType.none) {

            if (waitingHandler != null) {
                waitingHandler.setWaitingText("PSM Ratio Normalization. Please Wait...");
                waitingHandler.resetPrimaryProgressCounter();
                waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxPrimaryProgressCounter(identification.getSpectrumIdentificationSize() + 1);
                waitingHandler.increasePrimaryProgressCounter();
            }

            HashSet<String> seeds = normalizationSettings.getStableProteins();
            HashSet<String> exclusion = normalizationSettings.getContaminants();

            int nThreads = processingPreferences.getnThreads();

            for (String spectrumFile : identification.getOrderedSpectrumFileNames()) {

                PsmIterator psmIterator = identification.getPsmIterator(spectrumFile, parameters, false, waitingHandler);
                int nSpectra = identification.getSpectrumIdentification(spectrumFile).size();
                int batchSize = Math.min(Math.max(nSpectra / 100, 100), 10000);
                psmIterator.setBatchSize(batchSize);
                ExecutorService pool = Executors.newFixedThreadPool(nThreads);
                ArrayList<PsmNormalizerRunnable> runnables = new ArrayList<PsmNormalizerRunnable>(nThreads);

                for (int i = 1; i <= nThreads && waitingHandler != null && !waitingHandler.isRunCanceled(); i++) {
                    PsmNormalizerRunnable runnable = new PsmNormalizerRunnable(
                            reporterIonQuantification, quantificationFeaturesGenerator, identification, psmIterator, seeds,
                            exclusion, ratioEstimationSettings, sequenceMatchingPreferences, waitingHandler, exceptionHandler);
                    pool.submit(runnable);
                    runnables.add(runnable);
                }
                if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                    pool.shutdownNow();
                    return;
                }
                pool.shutdown();
                if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
                    throw new InterruptedException("PSM validation timed out. Please contact the developers.");
                }
                for (PsmNormalizerRunnable runnable : runnables) {
                    for (String reagent : runnable.getAllRawRatios().keySet()) {
                        ArrayList<Double> ratios = allRawRatios.get(reagent);
                        if (ratios == null) {
                            ratios = new ArrayList<Double>();
                            allRawRatios.put(reagent, ratios);
                        }
                        ratios.addAll(runnable.getAllRawRatios().get(reagent));
                    }
                    for (String reagent : runnable.getSeedRawRatios().keySet()) {
                        ArrayList<Double> ratios = seedRawRatios.get(reagent);
                        if (ratios == null) {
                            ratios = new ArrayList<Double>();
                            seedRawRatios.put(reagent, ratios);
                        }
                        ratios.addAll(runnable.getSeedRawRatios().get(reagent));
                    }
                }
            }
        }

        NormalizationFactors normalizationFactors = reporterIonQuantification.getNormalizationFactors();
        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            double normalisationFactor;
            ArrayList<Double> rawRatios = allRawRatios.get(sampleIndex);
            ArrayList<Double> seedRatios = seedRawRatios.get(sampleIndex);
            if (allRawRatios.get(sampleIndex) != null && !rawRatios.isEmpty()) {
                NormalizationType normalizationType = normalizationSettings.getPsmNormalization();
                if (normalizationType == NormalizationType.none) {
                    normalisationFactor = 1;
                } else if (normalizationType == NormalizationType.mean) {
                    if (seedRatios != null && !seedRatios.isEmpty()) {
                        normalisationFactor = BasicMathFunctions.mean(seedRatios);
                    } else {
                        normalisationFactor = BasicMathFunctions.mean(rawRatios);
                    }
                } else if (normalizationType == NormalizationType.median) {
                    if (seedRatios != null && !seedRatios.isEmpty()) {
                        normalisationFactor = BasicMathFunctions.median(seedRatios);
                    } else {
                        normalisationFactor = BasicMathFunctions.median(rawRatios);
                    }
                } else if (normalizationType == NormalizationType.mode) {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                } else if (normalizationType == NormalizationType.sum) {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                } else {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                }
            } else {
                normalisationFactor = 1;
            }
            normalizationFactors.addPsmNormalisationFactor(sampleIndex, normalisationFactor);
        }
    }

    /**
     * Sets the peptide normalization factors in the ReporterIonQuantification
     * object.
     *
     * @param reporterIonQuantification the reporter ion quantification
     * @param ratioEstimationSettings the ratio estimation settings
     * @param normalizationSettings the normalization settings
     * @param sequenceMatchingPreferences the peptide to protein sequence
     * matching preferences
     * @param identification the identification
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param processingPreferences the processing preferences
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
            Identification identification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ProcessingPreferences processingPreferences, ExceptionHandler exceptionHandler, WaitingHandler waitingHandler)
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
                waitingHandler.setWaitingText("Peptide Ratio Normalization. Please Wait...");
                waitingHandler.resetPrimaryProgressCounter();
                waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size() + 1);
                waitingHandler.increasePrimaryProgressCounter();
            }

            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, true, parameters, waitingHandler);
            int nPeptides = identification.getPeptideIdentification().size();
            int batchSize = Math.min(Math.max(nPeptides / 100, 100), 10000);
            peptideMatchesIterator.setBatchSize(batchSize);

            HashSet<String> seeds = normalizationSettings.getStableProteins();
            HashSet<String> exclusion = normalizationSettings.getContaminants();

            int nThreads = processingPreferences.getnThreads();
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            ArrayList<PeptideNormalizerRunnable> runnables = new ArrayList<PeptideNormalizerRunnable>(nThreads);

            for (int i = 1; i <= nThreads && waitingHandler != null && !waitingHandler.isRunCanceled(); i++) {
                PeptideNormalizerRunnable runnable = new PeptideNormalizerRunnable(
                        reporterIonQuantification, quantificationFeaturesGenerator, identification, peptideMatchesIterator,
                        seeds, exclusion, ratioEstimationSettings, sequenceMatchingPreferences, waitingHandler, exceptionHandler);
                pool.submit(runnable);
                runnables.add(runnable);
            }
            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                pool.shutdownNow();
                return;
            }
            pool.shutdown();
            if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
                throw new InterruptedException("Peptide validation timed out. Please contact the developers.");
            }
            for (PeptideNormalizerRunnable runnable : runnables) {
                for (String reagent : runnable.getAllRawRatios().keySet()) {
                    ArrayList<Double> ratios = allRawRatios.get(reagent);
                    if (ratios == null) {
                        ratios = new ArrayList<Double>();
                        allRawRatios.put(reagent, ratios);
                    }
                    ratios.addAll(runnable.getAllRawRatios().get(reagent));
                }
                for (String reagent : runnable.getSeedRawRatios().keySet()) {
                    ArrayList<Double> ratios = seedRawRatios.get(reagent);
                    if (ratios == null) {
                        ratios = new ArrayList<Double>();
                        seedRawRatios.put(reagent, ratios);
                    }
                    ratios.addAll(runnable.getSeedRawRatios().get(reagent));
                }
            }
        }

        NormalizationFactors normalizationFactors = reporterIonQuantification.getNormalizationFactors();
        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            double normalisationFactor;
            ArrayList<Double> rawRatios = allRawRatios.get(sampleIndex);
            ArrayList<Double> seedRatios = seedRawRatios.get(sampleIndex);
            if (allRawRatios.get(sampleIndex) != null && !rawRatios.isEmpty()) {
                NormalizationType normalizationType = normalizationSettings.getPeptideNormalization();
                if (normalizationType == NormalizationType.none) {
                    normalisationFactor = 1;
                } else if (normalizationType == NormalizationType.mean) {
                    if (seedRatios != null && !seedRatios.isEmpty()) {
                        normalisationFactor = BasicMathFunctions.mean(seedRatios);
                    } else {
                        normalisationFactor = BasicMathFunctions.mean(rawRatios);
                    }
                } else if (normalizationType == NormalizationType.median) {
                    if (seedRatios != null && !seedRatios.isEmpty()) {
                        normalisationFactor = BasicMathFunctions.median(seedRatios);
                    } else {
                        normalisationFactor = BasicMathFunctions.median(rawRatios);
                    }
                } else if (normalizationType == NormalizationType.mode) {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                } else if (normalizationType == NormalizationType.sum) {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                } else {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                }
            } else {
                normalisationFactor = 1;
            }
            normalizationFactors.addPeptideNormalisationFactor(sampleIndex, normalisationFactor);
        }
    }

    /**
     * Sets the protein normalization factors in the ReporterIonQuantification
     * object.
     *
     * @param reporterIonQuantification the reporter ion quantification
     * @param ratioEstimationSettings the ratio estimation settings
     * @param normalizationSettings the normalization settings
     * @param identification the identification
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param processingPreferences the processing preferences
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
    public void setProteinNormalizationFactors(ReporterIonQuantification reporterIonQuantification, RatioEstimationSettings ratioEstimationSettings, NormalizationSettings normalizationSettings,
            Identification identification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ProcessingPreferences processingPreferences, ExceptionHandler exceptionHandler, WaitingHandler waitingHandler)
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
                waitingHandler.setWaitingText("Protein Ratio Normalization. Please Wait...");
                waitingHandler.resetPrimaryProgressCounter();
                waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size() + 1);
                waitingHandler.increasePrimaryProgressCounter();
            }

            ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(parameters, true, parameters, true, parameters, waitingHandler);
            int nProteins = identification.getProteinIdentification().size();
            int batchSize = Math.min(Math.max(nProteins / 100, 100), 10000);
            proteinMatchesIterator.setBatchSize(batchSize);

            HashSet<String> seeds = normalizationSettings.getStableProteins();
            HashSet<String> exclusion = normalizationSettings.getContaminants();

            int nThreads = processingPreferences.getnThreads();
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            ArrayList<ProteinNormalizerRunnable> runnables = new ArrayList<ProteinNormalizerRunnable>(nThreads);

            for (int i = 1; i <= nThreads && waitingHandler != null && !waitingHandler.isRunCanceled(); i++) {
                ProteinNormalizerRunnable runnable = new ProteinNormalizerRunnable(
                        reporterIonQuantification, quantificationFeaturesGenerator, identification, proteinMatchesIterator,
                        seeds, exclusion, ratioEstimationSettings, waitingHandler, exceptionHandler);
                pool.submit(runnable);
                runnables.add(runnable);
            }
            if (waitingHandler != null && waitingHandler.isRunCanceled()) {
                pool.shutdownNow();
                return;
            }
            pool.shutdown();
            if (!pool.awaitTermination(7, TimeUnit.DAYS)) {
                throw new InterruptedException("Protein validation timed out. Please contact the developers.");
            }
            for (ProteinNormalizerRunnable runnable : runnables) {
                for (String reagent : runnable.getAllRawRatios().keySet()) {
                    ArrayList<Double> ratios = allRawRatios.get(reagent);
                    if (ratios == null) {
                        ratios = new ArrayList<Double>();
                        allRawRatios.put(reagent, ratios);
                    }
                    ratios.addAll(runnable.getAllRawRatios().get(reagent));
                }
                for (String reagent : runnable.getSeedRawRatios().keySet()) {
                    ArrayList<Double> ratios = seedRawRatios.get(reagent);
                    if (ratios == null) {
                        ratios = new ArrayList<Double>();
                        seedRawRatios.put(reagent, ratios);
                    }
                    ratios.addAll(runnable.getSeedRawRatios().get(reagent));
                }
            }
        }

        NormalizationFactors normalizationFactors = reporterIonQuantification.getNormalizationFactors();
        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            double normalisationFactor;
            ArrayList<Double> rawRatios = allRawRatios.get(sampleIndex);
            ArrayList<Double> seedRatios = seedRawRatios.get(sampleIndex);
            if (allRawRatios.get(sampleIndex) != null && !rawRatios.isEmpty()) {
                NormalizationType normalizationType = normalizationSettings.getProteinNormalization();
                if (normalizationType == NormalizationType.none) {
                    normalisationFactor = 1;
                } else if (normalizationType == NormalizationType.mean) {
                    if (seedRatios != null && !seedRatios.isEmpty()) {
                        normalisationFactor = BasicMathFunctions.mean(seedRatios);
                    } else {
                        normalisationFactor = BasicMathFunctions.mean(rawRatios);
                    }
                } else if (normalizationType == NormalizationType.median) {
                    if (seedRatios != null && !seedRatios.isEmpty()) {
                        normalisationFactor = BasicMathFunctions.median(seedRatios);
                    } else {
                        normalisationFactor = BasicMathFunctions.median(rawRatios);
                    }
                } else if (normalizationType == NormalizationType.mode) {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                } else if (normalizationType == NormalizationType.sum) {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                } else {
                    throw new UnsupportedOperationException("Normalization method not implemented.");
                }
            } else {
                normalisationFactor = 1;
            }
            normalizationFactors.addProteinNormalisationFactor(sampleIndex, normalisationFactor);
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
            if (contaminants.contains(accession)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Runnable gathering values for the normalization of proteins.
     *
     * @author Marc Vaudel
     */
    private class ProteinNormalizerRunnable implements Runnable {

        /**
         * The reporter ion quantification.
         */
        private ReporterIonQuantification reporterIonQuantification;
        /**
         * An iterator for the matches.
         */
        private ProteinMatchesIterator proteinMatchesIterator;
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
         * @param proteinMatchesIterator the iterator of the matches
         * @param seeds the seed proteins
         * @param exclusion the exclusion proteins
         * @param ratioEstimationSettings the ratio estimation settings
         * @param waitingHandler a waiting handler
         * @param exceptionHandler an exception handler
         */
        public ProteinNormalizerRunnable(ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, Identification identification, ProteinMatchesIterator proteinMatchesIterator, HashSet<String> seeds, HashSet<String> exclusion, RatioEstimationSettings ratioEstimationSettings, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
            this.reporterIonQuantification = reporterIonQuantification;
            this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
            this.proteinMatchesIterator = proteinMatchesIterator;
            this.identification = identification;
            this.seeds = seeds;
            this.exclusion = exclusion;
            this.ratioEstimationSettings = ratioEstimationSettings;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void run() {

            try {
                PSParameter psParameter = new PSParameter();

                while (proteinMatchesIterator.hasNext()) {

                    ProteinMatch proteinMatch = proteinMatchesIterator.next();

                    if (proteinMatch != null) {

                        if (exclusion == null || !isContaminant(exclusion, proteinMatch.getTheoreticProteinsAccessions())) {

                            String proteinMatchKey = proteinMatch.getKey();
                            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinMatchKey, psParameter);

                            if (psParameter.getMatchValidationLevel().getIndex() >= ratioEstimationSettings.getProteinValidationLevel().getIndex()) {

                                ProteinQuantificationDetails matchQuantificationDetails = quantificationFeaturesGenerator.getProteinMatchQuantificationDetails(proteinMatchKey, waitingHandler);

                                for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
                                    Double ratio = matchQuantificationDetails.getRawRatio(sampleIndex);
                                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio) && ratio > 0) {
                                        ArrayList<Double> ratios = allRawRatios.get(sampleIndex);
                                        if (ratios == null) {
                                            ratios = new ArrayList<Double>();
                                            allRawRatios.put(sampleIndex, ratios);
                                        }
                                        ratios.add(ratio);
                                        if (seeds != null && isSeed(seeds, proteinMatch.getTheoreticProteinsAccessions())) {
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
                        }
                    }

                    if (waitingHandler != null) {
                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                        waitingHandler.increaseSecondaryProgressCounter();
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

                            if (psParameter.getMatchValidationLevel().getIndex() >= ratioEstimationSettings.getPeptideValidationLevel().getIndex()) {

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
                        }
                    }

                    if (waitingHandler != null) {
                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                        waitingHandler.increaseSecondaryProgressCounter();
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

    /**
     * Runnable gathering values for the normalization of PSMs.
     *
     * @author Marc Vaudel
     */
    private class PsmNormalizerRunnable implements Runnable {

        /**
         * The reporter ion quantification.
         */
        private ReporterIonQuantification reporterIonQuantification;
        /**
         * An iterator for the matches.
         */
        private PsmIterator psmIterator;
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
         * @param psmIterator the iterator of the matches
         * @param seeds the seed proteins
         * @param exclusion the exclusion proteins
         * @param ratioEstimationSettings the ratio estimation settings
         * @param sequenceMatchingPreferences the sequence matching preferences
         * @param waitingHandler a waiting handler
         * @param exceptionHandler an exception handler
         */
        public PsmNormalizerRunnable(ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, Identification identification, PsmIterator psmIterator, HashSet<String> seeds, HashSet<String> exclusion, RatioEstimationSettings ratioEstimationSettings, SequenceMatchingPreferences sequenceMatchingPreferences, WaitingHandler waitingHandler, ExceptionHandler exceptionHandler) {
            this.reporterIonQuantification = reporterIonQuantification;
            this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
            this.psmIterator = psmIterator;
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

                while (psmIterator.hasNext()) {

                    SpectrumMatch spectrumMatch = psmIterator.next();

                    if (spectrumMatch != null) {

                        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                        if (peptideAssumption != null) {

                            Peptide peptide = peptideAssumption.getPeptide();

                            if (exclusion == null || !isContaminant(exclusion, peptide.getParentProteins(sequenceMatchingPreferences))) {

                                String spectrumKey = spectrumMatch.getKey();
                                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                                if (psParameter.getMatchValidationLevel().getIndex() >= ratioEstimationSettings.getPsmValidationLevel().getIndex()) {

                                    PsmQuantificationDetails matchQuantificationDetails = quantificationFeaturesGenerator.getPSMQuantificationDetails(spectrumKey);

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
