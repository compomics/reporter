package eu.isas.reporter.calculation.normalization;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.io.biology.protein.FastaParameters;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.normalization.NormalizationFactors;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.parameters.identification.advanced.PeptideVariantsParameters;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import com.compomics.util.waiting.WaitingHandler;
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
import java.util.Collection;
import java.util.HashMap;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Normalizes the ratios according to the NormalizationSettings.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class Normalizer {

    /**
     * Sets the PSM normalization factors in the ReporterIonQuantification
     * object.
     *
     * @param reporterIonQuantification the reporter ion quantification
     * @param ratioEstimationSettings the ratio estimation settings
     * @param normalizationSettings the normalization settings
     * @param sequenceMatchingParameters the peptide to protein sequence
     * matching preferences
     * @param identification the identification
     * @param spectrumProvider the spectrum provider
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param processingParameters the processing preferences
     * @param searchParameters the search parameters
     * @param peptideVariantsPreferences the peptide variants parameters
     * @param fastaParameters the FASTA parameters

     * @param exceptionHandler handler in case exception occur
     * @param waitingHandler waiting handler displaying progress to the user
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     */
    public void setPsmNormalizationFactors(
            ReporterIonQuantification reporterIonQuantification, 
            RatioEstimationSettings ratioEstimationSettings,
            NormalizationSettings normalizationSettings, 
            SequenceMatchingParameters sequenceMatchingParameters, 
            Identification identification,
            SpectrumProvider spectrumProvider,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, 
            ProcessingParameters processingParameters,
            SearchParameters searchParameters, 
            FastaParameters fastaParameters, 
            PeptideVariantsParameters peptideVariantsPreferences, 
            ExceptionHandler exceptionHandler, 
            WaitingHandler waitingHandler
    ) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        HashMap<String, ArrayList<Double>> allRawRatios = new HashMap<>();
        HashMap<String, ArrayList<Double>> seedRawRatios = new HashMap<>();
        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            allRawRatios.put(sampleIndex, new ArrayList<>());
            seedRawRatios.put(sampleIndex, new ArrayList<>());
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<>(1);
        parameters.add(psParameter);

        if (normalizationSettings.getPsmNormalization() != NormalizationType.none) {

            if (waitingHandler != null) {
                waitingHandler.setWaitingText("PSM Ratio Normalization. Please Wait...");
                waitingHandler.resetPrimaryProgressCounter();
                waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxPrimaryProgressCounter(identification.getSpectrumIdentificationSize() + 1);
                waitingHandler.increasePrimaryProgressCounter();
            }

            Collection<String> seeds = normalizationSettings.getStableProteins(searchParameters, fastaParameters, peptideVariantsPreferences, waitingHandler);
            Collection<String> exclusion = normalizationSettings.getContaminants(searchParameters, fastaParameters, peptideVariantsPreferences, waitingHandler);

            int nThreads = processingParameters.getnThreads();

            SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler);
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            ArrayList<PsmNormalizerRunnable> runnables = new ArrayList<>(nThreads);

            for (int i = 1; i <= nThreads && waitingHandler != null && !waitingHandler.isRunCanceled(); i++) {
                PsmNormalizerRunnable runnable = new PsmNormalizerRunnable(
                        reporterIonQuantification, quantificationFeaturesGenerator, identification, spectrumProvider, spectrumMatchesIterator, seeds,
                        exclusion, ratioEstimationSettings, sequenceMatchingParameters, waitingHandler, exceptionHandler);
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
                        ratios = new ArrayList<>();
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
     * @param sequenceMatchingParameters the peptide to protein sequence
     * matching preferences
     * @param identification the identification
     * @param spectrumProvider the spectrum provider
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param processingParameters the processing preferences
     * @param searchParameters the search parameters
     * @param fastaParameters the FASTA parameters
     * @param exceptionHandler handler in case exception occur
     * @param peptideVariantsPreferences the peptide variants parameters
     * @param waitingHandler waiting handler displaying progress to the user
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     */
    public void setPeptideNormalizationFactors(
            ReporterIonQuantification reporterIonQuantification, 
            RatioEstimationSettings ratioEstimationSettings, 
            NormalizationSettings normalizationSettings, 
            SequenceMatchingParameters sequenceMatchingParameters,
            Identification identification, 
            SpectrumProvider spectrumProvider,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, 
            ProcessingParameters processingParameters,
            SearchParameters searchParameters, 
            FastaParameters fastaParameters, 
            PeptideVariantsParameters peptideVariantsPreferences, 
            ExceptionHandler exceptionHandler, 
            WaitingHandler waitingHandler
    ) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        HashMap<String, ArrayList<Double>> allRawRatios = new HashMap<>();
        HashMap<String, ArrayList<Double>> seedRawRatios = new HashMap<>();
        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            allRawRatios.put(sampleIndex, new ArrayList<Double>());
            seedRawRatios.put(sampleIndex, new ArrayList<Double>());
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<>(1);
        parameters.add(psParameter);

        if (normalizationSettings.getPeptideNormalization() != NormalizationType.none) {

            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Peptide Ratio Normalization. Please Wait...");
                waitingHandler.resetPrimaryProgressCounter();
                waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size() + 1);
                waitingHandler.increasePrimaryProgressCounter();
            }

            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);

            Collection<String> seeds = normalizationSettings.getStableProteins(searchParameters, fastaParameters, peptideVariantsPreferences, waitingHandler);
            Collection<String> exclusion = normalizationSettings.getContaminants(searchParameters, fastaParameters, peptideVariantsPreferences, waitingHandler);

            int nThreads = processingParameters.getnThreads();
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            ArrayList<PeptideNormalizerRunnable> runnables = new ArrayList<PeptideNormalizerRunnable>(nThreads);

            for (int i = 1; i <= nThreads && waitingHandler != null && !waitingHandler.isRunCanceled(); i++) {
                PeptideNormalizerRunnable runnable = new PeptideNormalizerRunnable(
                        reporterIonQuantification, quantificationFeaturesGenerator, identification, spectrumProvider, peptideMatchesIterator,
                        seeds, exclusion, ratioEstimationSettings, sequenceMatchingParameters, waitingHandler, exceptionHandler);
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
     * @param spectrumProvider the spectrum provider
     * @param metrics the identification metrics
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param processingParameters the processing parameters
     * @param searchParameters the search parameters
     * @param fastaParameters the FASTA parameters
     * @param peptideVariantsPreferences the peptide variants parameters

     * @param exceptionHandler handler in case exception occur
     * @param waitingHandler waiting handler displaying progress to the user
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     */
    public void setProteinNormalizationFactors(
            ReporterIonQuantification reporterIonQuantification, 
            RatioEstimationSettings ratioEstimationSettings, 
            NormalizationSettings normalizationSettings,
            Identification identification, 
            SpectrumProvider spectrumProvider,
            Metrics metrics, 
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, 
            ProcessingParameters processingParameters, 
            SearchParameters searchParameters, 
            FastaParameters fastaParameters, 
            PeptideVariantsParameters peptideVariantsPreferences, 
            ExceptionHandler exceptionHandler, 
            WaitingHandler waitingHandler
    ) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        Set<String> sampleIndexes = reporterIonQuantification.getSampleIndexes();
        HashMap<String, ArrayList<Double>> allRawRatios = new HashMap<>(sampleIndexes.size());
        HashMap<String, ArrayList<Double>> seedRawRatios = new HashMap<>(sampleIndexes.size());
        for (String sampleIndex : sampleIndexes) {
            allRawRatios.put(sampleIndex, new ArrayList<Double>(metrics.getnValidatedProteins()));
            seedRawRatios.put(sampleIndex, new ArrayList<Double>(metrics.getnValidatedProteins()));
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<>(1);
        parameters.add(psParameter);

        if (normalizationSettings.getProteinNormalization() != NormalizationType.none) {

            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Protein Ratio Normalization. Please Wait...");
                waitingHandler.resetPrimaryProgressCounter();
                waitingHandler.setPrimaryProgressCounterIndeterminate(false);
                waitingHandler.setMaxPrimaryProgressCounter(identification.getProteinIdentification().size() + 1);
                waitingHandler.increasePrimaryProgressCounter();
            }

            ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(waitingHandler);

            Collection<String> seeds = normalizationSettings.getStableProteins(searchParameters, fastaParameters, peptideVariantsPreferences, waitingHandler);
            Collection<String> exclusion = normalizationSettings.getContaminants(searchParameters, fastaParameters, peptideVariantsPreferences, waitingHandler);

            int nThreads = processingParameters.getnThreads();
            ExecutorService pool = Executors.newFixedThreadPool(nThreads);
            ArrayList<ProteinNormalizerRunnable> runnables = new ArrayList<ProteinNormalizerRunnable>(nThreads);

            for (int i = 1; i <= nThreads && waitingHandler != null && !waitingHandler.isRunCanceled(); i++) {
                ProteinNormalizerRunnable runnable = new ProteinNormalizerRunnable(
                        reporterIonQuantification, quantificationFeaturesGenerator, identification, spectrumProvider, 
                        proteinMatchesIterator, seeds, exclusion, ratioEstimationSettings, waitingHandler, exceptionHandler);
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
            if (rawRatios != null && !rawRatios.isEmpty()) {
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
    private static boolean isSeed(Collection<String> seeds, String[] accessions) {
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
    private static boolean isContaminant(Collection<String> contaminants, String[] accessions) {
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
         * The spectrum provider.
         */
        private SpectrumProvider spectrumProvider;
        /**
         * The quantification features generator.
         */
        private QuantificationFeaturesGenerator quantificationFeaturesGenerator;
        /**
         * The seed proteins.
         */
        private Collection<String> seeds;
        /**
         * The excluded proteins.
         */
        private Collection<String> exclusion;
        /**
         * The ratio estimation settings.
         */
        private RatioEstimationSettings ratioEstimationSettings;
        /**
         * The raw ratios gathered in a map.
         */
        private HashMap<String, ArrayList<Double>> allRawRatios = new HashMap<String, ArrayList<Double>>();
        /**
         * The raw seed ratios gathered in a map.
         */
        private HashMap<String, ArrayList<Double>> seedRawRatios = new HashMap<String, ArrayList<Double>>();
        /**
         * The raw unique ratios gathered in a map.
         */
        private HashMap<String, ArrayList<Double>> allUniqueRawRatios = new HashMap<String, ArrayList<Double>>();
        /**
         * The raw seed unique ratios gathered in a map.
         */
        private HashMap<String, ArrayList<Double>> seedUniqueRawRatios = new HashMap<String, ArrayList<Double>>();
        /**
         * The raw shared ratios gathered in a map.
         */
        private HashMap<String, ArrayList<Double>> allSharedRawRatios = new HashMap<String, ArrayList<Double>>();
        /**
         * The raw seed shared ratios gathered in a map.
         */
        private HashMap<String, ArrayList<Double>> seedSharedRawRatios = new HashMap<String, ArrayList<Double>>();
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
         * @param spectrumProvider the spectrum provider
         * @param proteinMatchesIterator the iterator of the matches
         * @param seeds the seed proteins
         * @param exclusion the exclusion proteins
         * @param ratioEstimationSettings the ratio estimation settings
         * @param waitingHandler a waiting handler
         * @param exceptionHandler an exception handler
         */
        public ProteinNormalizerRunnable(
                ReporterIonQuantification reporterIonQuantification, 
                QuantificationFeaturesGenerator quantificationFeaturesGenerator,
                Identification identification, 
                SpectrumProvider spectrumProvider,
                ProteinMatchesIterator proteinMatchesIterator, 
                Collection<String> seeds, 
                Collection<String> exclusion, 
                RatioEstimationSettings ratioEstimationSettings, 
                WaitingHandler waitingHandler, 
                ExceptionHandler exceptionHandler
        ) {
            
            this.reporterIonQuantification = reporterIonQuantification;
            this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
            this.proteinMatchesIterator = proteinMatchesIterator;
            this.identification = identification;
            this.spectrumProvider = spectrumProvider;
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
                ProteinMatch proteinMatch;

                while ((proteinMatch = proteinMatchesIterator.next()) != null) {

                    if (proteinMatch != null) {

                        if (exclusion == null || !isContaminant(exclusion, proteinMatch.getAccessions())) {

                            long proteinMatchKey = proteinMatch.getKey();
                            psParameter = (PSParameter) identification.getProteinMatch(proteinMatchKey).getUrParam(psParameter);

                            if (psParameter.getMatchValidationLevel().getIndex() >= ratioEstimationSettings.getProteinValidationLevel().getIndex()) {

                                ProteinQuantificationDetails matchQuantificationDetails = quantificationFeaturesGenerator.getProteinMatchQuantificationDetails(spectrumProvider, proteinMatchKey, waitingHandler);

                                for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
                                    Double ratio = matchQuantificationDetails.getRawRatio(sampleIndex);
                                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio) && ratio > 0) {
                                        ArrayList<Double> ratios = allRawRatios.get(sampleIndex);
                                        if (ratios == null) {
                                            ratios = new ArrayList<Double>();
                                            allRawRatios.put(sampleIndex, ratios);
                                        }
                                        ratios.add(ratio);
                                        if (seeds != null && isSeed(seeds, proteinMatch.getAccessions())) {
                                            ratios = seedRawRatios.get(sampleIndex);
                                            if (ratios == null) {
                                                ratios = new ArrayList<Double>();
                                                seedRawRatios.put(sampleIndex, ratios);
                                            }
                                            ratios.add(ratio);
                                        }
                                    }
                                    ratio = matchQuantificationDetails.getUniqueRawRatio(sampleIndex);
                                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio) && ratio > 0) {
                                        ArrayList<Double> ratios = allUniqueRawRatios.get(sampleIndex);
                                        if (ratios == null) {
                                            ratios = new ArrayList<Double>();
                                            allUniqueRawRatios.put(sampleIndex, ratios);
                                        }
                                        ratios.add(ratio);
                                        if (seeds != null && isSeed(seeds, proteinMatch.getAccessions())) {
                                            ratios = seedUniqueRawRatios.get(sampleIndex);
                                            if (ratios == null) {
                                                ratios = new ArrayList<Double>();
                                                seedUniqueRawRatios.put(sampleIndex, ratios);
                                            }
                                            ratios.add(ratio);
                                        }
                                    }
                                    ratio = matchQuantificationDetails.getSharedRawRatio(sampleIndex);
                                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio) && ratio > 0) {
                                        ArrayList<Double> ratios = allSharedRawRatios.get(sampleIndex);
                                        if (ratios == null) {
                                            ratios = new ArrayList<Double>();
                                            allSharedRawRatios.put(sampleIndex, ratios);
                                        }
                                        ratios.add(ratio);
                                        if (seeds != null && isSeed(seeds, proteinMatch.getAccessions())) {
                                            ratios = seedSharedRawRatios.get(sampleIndex);
                                            if (ratios == null) {
                                                ratios = new ArrayList<Double>();
                                                seedSharedRawRatios.put(sampleIndex, ratios);
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

        /**
         * Returns the raw unique ratios found while iterating.
         *
         * @return the raw unique ratios found while iterating
         */
        public HashMap<String, ArrayList<Double>> getAllUniqueRawRatios() {
            return allUniqueRawRatios;
        }

        /**
         * Returns the seed raw unique ratios found while iterating.
         *
         * @return the seed raw unique ratios found while iterating
         */
        public HashMap<String, ArrayList<Double>> getSeedUniqueRawRatios() {
            return seedUniqueRawRatios;
        }

        /**
         * Returns the raw shared ratios found while iterating.
         *
         * @return the raw shared ratios found while iterating
         */
        public HashMap<String, ArrayList<Double>> getAllSharedRawRatios() {
            return allSharedRawRatios;
        }

        /**
         * Returns the seed raw shared ratios found while iterating.
         *
         * @return the seed raw shared ratios found while iterating
         */
        public HashMap<String, ArrayList<Double>> getSeedSharedRawRatios() {
            return seedSharedRawRatios;
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
         * The spectrum provider.
         */
        private SpectrumProvider spectrumProvider;
        /**
         * The quantification features generator.
         */
        private QuantificationFeaturesGenerator quantificationFeaturesGenerator;
        /**
         * The seed proteins.
         */
        private Collection<String> seeds;
        /**
         * The excluded proteins.
         */
        private Collection<String> exclusion;
        /**
         * The peptide to protein sequence matching parameters.
         */
        private SequenceMatchingParameters sequenceMatchingParameters;
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
         * @param spectrumProvider the spectrum provider
         * @param peptideMatchesIterator the iterator of the peptide matches
         * @param seeds the seed proteins
         * @param exclusion the exclusion proteins
         * @param ratioEstimationSettings the ratio estimation settings
         * @param sequenceMatchingParameters the sequence matching parameters
         * @param waitingHandler a waiting handler
         * @param exceptionHandler an exception handler
         */
        public PeptideNormalizerRunnable(
                ReporterIonQuantification reporterIonQuantification, 
                QuantificationFeaturesGenerator quantificationFeaturesGenerator,
                Identification identification, 
                SpectrumProvider spectrumProvider,
                PeptideMatchesIterator peptideMatchesIterator, 
                Collection<String> seeds, 
                Collection<String> exclusion,
                RatioEstimationSettings ratioEstimationSettings, 
                SequenceMatchingParameters sequenceMatchingParameters, 
                WaitingHandler waitingHandler, 
                ExceptionHandler exceptionHandler
        ) {
            
            this.reporterIonQuantification = reporterIonQuantification;
            this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
            this.peptideMatchesIterator = peptideMatchesIterator;
            this.identification = identification;
            this.spectrumProvider = spectrumProvider;
            this.seeds = seeds;
            this.exclusion = exclusion;
            this.sequenceMatchingParameters = sequenceMatchingParameters;
            this.ratioEstimationSettings = ratioEstimationSettings;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void run() {

            try {
                PSParameter psParameter = new PSParameter();
                PeptideMatch peptideMatch;

                while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                    if (peptideMatch != null) {
                        Peptide peptide = peptideMatch.getPeptide();

                        String[] parentProteins = peptide.getProteinMapping().keySet().stream().toArray(String[]::new);
                        
                        if (exclusion == null || !isContaminant(exclusion, parentProteins)) {

                            long peptideKey = peptideMatch.getKey();
                            psParameter = (PSParameter) identification.getPeptideMatch(peptideKey).getUrParam(psParameter);

                            if (psParameter.getMatchValidationLevel().getIndex() >= ratioEstimationSettings.getPeptideValidationLevel().getIndex()) {

                                PeptideQuantificationDetails matchQuantificationDetails = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(spectrumProvider, peptideMatch, waitingHandler);

                                for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
                                    Double ratio = matchQuantificationDetails.getRawRatio(sampleIndex);
                                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio) && ratio > 0) {
                                        ArrayList<Double> ratios = allRawRatios.get(sampleIndex);
                                        if (ratios == null) {
                                            ratios = new ArrayList<Double>();
                                            allRawRatios.put(sampleIndex, ratios);
                                        }
                                        ratios.add(ratio);
                                        if (seeds != null && isSeed(seeds, parentProteins)) {
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
        private SpectrumMatchesIterator spectrumMatchesIterator;
        /**
         * The identification.
         */
        private Identification identification;
        /**
         * The spectrum provider.
         */
        private SpectrumProvider spectrumProvider;
        /**
         * The quantification features generator.
         */
        private QuantificationFeaturesGenerator quantificationFeaturesGenerator;
        /**
         * The seed proteins.
         */
        private Collection<String> seeds;
        /**
         * The excluded proteins.
         */
        private Collection<String> exclusion;
        /**
         * The peptide to protein sequence matching parameters.
         */
        private SequenceMatchingParameters sequenceMatchingParameters;
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
         * @param spectrumProvider the spectrum provider
         * @param spectrumMatchesIterator the iterator of the matches
         * @param seeds the seed proteins
         * @param exclusion the exclusion proteins
         * @param ratioEstimationSettings the ratio estimation settings
         * @param sequenceMatchingParameters the sequence matching parameters
         * @param waitingHandler a waiting handler
         * @param exceptionHandler an exception handler
         */
        public PsmNormalizerRunnable(
                ReporterIonQuantification reporterIonQuantification, 
                QuantificationFeaturesGenerator quantificationFeaturesGenerator,
                Identification identification,
                SpectrumProvider spectrumProvider,
                SpectrumMatchesIterator spectrumMatchesIterator, 
                Collection<String> seeds, 
                Collection<String> exclusion, 
                RatioEstimationSettings ratioEstimationSettings,
                SequenceMatchingParameters sequenceMatchingParameters, 
                WaitingHandler waitingHandler, 
                ExceptionHandler exceptionHandler
        ) {
            
            this.reporterIonQuantification = reporterIonQuantification;
            this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
            this.spectrumMatchesIterator = spectrumMatchesIterator;
            this.identification = identification;
            this.spectrumProvider = spectrumProvider;
            this.seeds = seeds;
            this.exclusion = exclusion;
            this.sequenceMatchingParameters = sequenceMatchingParameters;
            this.ratioEstimationSettings = ratioEstimationSettings;
            this.waitingHandler = waitingHandler;
            this.exceptionHandler = exceptionHandler;
        }

        @Override
        public void run() {

            try {
                PSParameter psParameter = new PSParameter();
                SpectrumMatch spectrumMatch;

                while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

                    if (spectrumMatch != null) {

                        PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                        if (peptideAssumption != null) {

                            Peptide peptide = peptideAssumption.getPeptide();
                            
                            String[] parentProteins = peptide.getProteinMapping().keySet().stream().toArray(String[]::new);

                            if (exclusion == null || !isContaminant(exclusion, parentProteins)) {

                                psParameter = (PSParameter) identification.getSpectrumMatch(spectrumMatch.getKey()).getUrParam(psParameter);

                                if (psParameter.getMatchValidationLevel().getIndex() >= ratioEstimationSettings.getPsmValidationLevel().getIndex()) {

                                    PsmQuantificationDetails matchQuantificationDetails = quantificationFeaturesGenerator.getPSMQuantificationDetails(spectrumProvider, spectrumMatch.getKey());

                                    for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
                                        Double ratio = matchQuantificationDetails.getRawRatio(sampleIndex);
                                        if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio) && ratio > 0) {
                                            ArrayList<Double> ratios = allRawRatios.get(sampleIndex);
                                            if (ratios == null) {
                                                ratios = new ArrayList<Double>();
                                                allRawRatios.put(sampleIndex, ratios);
                                            }
                                            ratios.add(ratio);
                                            if (seeds != null && isSeed(seeds, parentProteins)) {
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
