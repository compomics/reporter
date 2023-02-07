package eu.isas.reporter;

import com.compomics.software.CompomicsWrapper;
import com.compomics.software.settings.UtilitiesPathParameters;
import com.compomics.util.experiment.biology.ions.impl.ReporterIon;
import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.reporter.calculation.Deisotoper;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFilter;
import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.preferences.ReporterPathPreferences;
import eu.isas.reporter.settings.RatioEstimationSettings;
import eu.isas.reporter.settings.ReporterIonSelectionSettings;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinPtmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.SpectrumQuantificationDetails;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

/**
 * Reporter performs reporter ion based quantification on MS2/MS3 spectra.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class Reporter {

    /**
     * The location of the folder used for the database.
     */
    private static String matchesFolder = "matches";
    /**
     * Enzymes file.
     */
    private static String enzymeFile = "resources/conf/searchGUI_enzymes.xml";
    /**
     * Default methods file.
     */
    private static String methodsFile = "resources/conf/defaultMethods.xml";
    /**
     * A folder used to store temporary files.
     */
    private static String tempFolderPath = null;

    /**
     * Empty constructor for instantiation purposes.
     */
    public Reporter() {

    }

    /**
     * Returns the quantification details of a protein match.
     *
     * @param identification the identification containing identification
     * details
     * @param spectrumProvider the spectrum provider
     * @param identificationFeaturesGenerator the identification features
     * generator used to store and retrieve identification details
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param ratioEstimationSettings the ratio estimation settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param searchParameters the identification parameters
     * @param proteinMatch the protein match
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     *
     * @return the quantification details of the match
     */
    public static ProteinQuantificationDetails estimateProteinMatchQuantificationDetails(
            Identification identification,
            SpectrumProvider spectrumProvider,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification,
            SearchParameters searchParameters,
            ProteinMatch proteinMatch,
            WaitingHandler waitingHandler
    ) {

        ProteinQuantificationDetails result = new ProteinQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<>();
        HashMap<String, ArrayList<Double>> uniqueRatios = new HashMap<>();
        HashMap<String, ArrayList<Double>> sharedRatios = new HashMap<>();
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), waitingHandler);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            if (QuantificationFilter.isPeptideValid(ratioEstimationSettings, identification, searchParameters, peptideMatch)) {

                for (String index : indexes) {

                    PeptideQuantificationDetails peptideQuantification = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(spectrumProvider, peptideMatch, waitingHandler);
                    double ratio = peptideQuantification.getRatio(index, reporterIonQuantification.getNormalizationFactors());
                    ArrayList<Double> channelRatios = ratios.get(index);
                    ArrayList<Double> channelUniqueRatios = uniqueRatios.get(index);
                    ArrayList<Double> channelSharedRatios = sharedRatios.get(index);

                    if (channelRatios == null) {
                        channelRatios = new ArrayList<Double>(proteinMatch.getPeptideCount());
                        ratios.put(index, channelRatios);
                        channelUniqueRatios = new ArrayList<Double>(proteinMatch.getPeptideCount());
                        uniqueRatios.put(index, channelUniqueRatios);
                        channelSharedRatios = new ArrayList<Double>(proteinMatch.getPeptideCount());
                        sharedRatios.put(index, channelSharedRatios);
                    }

                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio)) {

                        channelRatios.add(ratio);

                        if (identificationFeaturesGenerator.getNValidatedProteinGroups(peptideMatch.getKey()) == 1) {
                            channelUniqueRatios.add(ratio);
                        } else {
                            channelSharedRatios.add(ratio);
                        }

                    }

                }

            }

        }

        for (String index : indexes) {

            ArrayList<Double> channelUniqueRatios = uniqueRatios.get(index);
            Double uniqueRatio = RatioEstimator.estimateRatios(ratioEstimationSettings, channelUniqueRatios);
            result.setUniqueRawRatio(index, uniqueRatio);
            ArrayList<Double> channelSharedRatios = sharedRatios.get(index);
            result.setSharedRawRatio(index, RatioEstimator.estimateRatios(ratioEstimationSettings, channelSharedRatios));

            if (ratioEstimationSettings.getMinUnique() >= 0 && channelUniqueRatios != null && channelUniqueRatios.size() >= ratioEstimationSettings.getMinUnique()) {
                result.setRawRatio(index, uniqueRatio);
            } else {
                ArrayList<Double> channelRatios = ratios.get(index);
                result.setRawRatio(index, RatioEstimator.estimateRatios(ratioEstimationSettings, channelRatios));
            }

        }

        return result;
    }

    /**
     * Returns the quantification details of a PTM on a protein.//@TODO:
     * discriminate peptides according to the neighboring sites?
     *
     * @param identification the identification containing identification
     * details
     * @param spectrumProvider the spectrum provider
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param ratioEstimationSettings the ratio estimation settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param searchParameters the identification settings used
     * @param sequenceMatchingParameters the sequence matching preferences
     * @param ptmName the name of the PTM
     * @param matchKey the key of the match of interest
     * @param site the site of the PTM on the protein sequence
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     *
     * @return the quantification details of the match
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
    public static ProteinPtmQuantificationDetails estimatePTMQuantificationDetails(
            Identification identification,
            SpectrumProvider spectrumProvider,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters,
            String ptmName,
            long matchKey,
            int site,
            WaitingHandler waitingHandler
    ) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinPtmQuantificationDetails result = new ProteinPtmQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<>();
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), waitingHandler);
        PeptideMatch peptideMatch;

        while ((peptideMatch = peptideMatchesIterator.next()) != null) {

            Peptide peptide = peptideMatch.getPeptide();

            if (peptide.getNVariableModifications() > 0) {

                boolean modified = false;

                for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {

                    if (modificationMatch.getModification().equals(ptmName) && modificationMatch.getConfident()) {

                        String leadingAccession = proteinMatch.getLeadingAccession();
                        int[] startIndexes = peptideMatch.getPeptide().getProteinMapping().get(leadingAccession);

                        for (int index : startIndexes) {
                            if (index + modificationMatch.getSite() == site) {
                                modified = true;
                                break;
                            }
                        }

                    }

                    if (modified) {
                        break;
                    }

                }

            }

            if (QuantificationFilter.isPeptideValid(ratioEstimationSettings, identification, searchParameters, peptideMatch)) {

                for (String index : indexes) {

                    PeptideQuantificationDetails peptideQuantification = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(spectrumProvider, peptideMatch, waitingHandler);
                    double ratio = peptideQuantification.getRatio(index, reporterIonQuantification.getNormalizationFactors());
                    ArrayList<Double> channelRatios = ratios.get(index);

                    if (channelRatios == null) {
                        channelRatios = new ArrayList<>(proteinMatch.getPeptideCount());
                        ratios.put(index, channelRatios);
                    }

                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio)) {
                        channelRatios.add(ratio);
                    }
                }

            }

        }

        for (String index : indexes) {
            ArrayList<Double> channelRatios = ratios.get(index);
            result.setRatio(index, RatioEstimator.estimateRatios(ratioEstimationSettings, channelRatios));
        }

        return result;
    }

    /**
     * Returns the quantification details of a peptide match.
     *
     * @param identification the identification containing identification
     * details
     * @param spectrumProvider the spectrum provider
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param ratioEstimationSettings the ratio estimation settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param peptideMatch the peptide match
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     *
     * @return the quantification details of the match
     */
    public static PeptideQuantificationDetails estimatePeptideMatchQuantificationDetails(
            Identification identification,
            SpectrumProvider spectrumProvider,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification,
            PeptideMatch peptideMatch,
            WaitingHandler waitingHandler
    ) {

        PeptideQuantificationDetails result = new PeptideQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<>();
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();

        SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(peptideMatch.getSpectrumMatchesKeys(), waitingHandler);
        SpectrumMatch spectrumMatch;

        while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

            if (QuantificationFilter.isPsmValid(ratioEstimationSettings, identification, spectrumMatch.getKey())) {

                for (String index : indexes) {

                    PsmQuantificationDetails spectrumQuantification = quantificationFeaturesGenerator.getPSMQuantificationDetails(spectrumProvider, spectrumMatch.getKey());
                    double ratio = spectrumQuantification.getRatio(index, reporterIonQuantification.getNormalizationFactors());
                    ArrayList<Double> channelRatios = ratios.get(index);

                    if (channelRatios == null) {
                        channelRatios = new ArrayList<Double>(peptideMatch.getSpectrumCount());
                        ratios.put(index, channelRatios);
                    }

                    if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio)) {
                        channelRatios.add(ratio);
                    }
                }

            }

        }

        for (String index : indexes) {
            ArrayList<Double> channelRatios = ratios.get(index);
            result.setRawRatio(index, RatioEstimator.estimateRatios(ratioEstimationSettings, channelRatios));
        }

        return result;

    }

    /**
     * Returns the quantification details of a PSM.
     *
     * @param identification the identification containing identification
     * details
     * @param spectrumProvider the spectrum provider
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param reporterIonSelectionSettings the reporter ion selection settings
     * @param ratioEstimationSettings the ratio estimation settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param matchKey the key of the match of interest
     *
     * @return the quantification details of the match
     */
    public static PsmQuantificationDetails estimatePSMQuantificationDetails(
            Identification identification,
            SpectrumProvider spectrumProvider,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            ReporterIonSelectionSettings reporterIonSelectionSettings,
            RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification,
            Long matchKey
    ) {

        PsmQuantificationDetails result = new PsmQuantificationDetails();

        // find the spectra corresponding to this PSM according to the matching type selected by the user
        ArrayList<Long> spectra = new ArrayList<>(1);
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
        Long spectrumKey = spectrumMatch.getKey();

        if (reporterIonSelectionSettings.isSameSpectra()) {

            spectra.add(spectrumKey);

        } else {

            String refFile = spectrumMatch.getSpectrumFile();
            Precursor refPrecursor = spectrumProvider.getPrecursor(
                    spectrumMatch.getSpectrumFile(),
                    spectrumMatch.getSpectrumTitle()
            );

            // match spectra by mass and retention time
            for (String spectrumTitle : spectrumProvider.getSpectrumTitles(refFile)) {

                Precursor precursor = spectrumProvider.getPrecursor(refFile, spectrumTitle);

                if (Math.abs(precursor.rt - refPrecursor.rt)
                        <= reporterIonSelectionSettings.getPrecursorRTTolerance()) {

                    if (reporterIonSelectionSettings.isPrecursorMzPpm()) {

                        double error = (precursor.mz - refPrecursor.mz) / refPrecursor.mz * 1000000;

                        if (Math.abs(error) <= reporterIonSelectionSettings.getPrecursorMzTolerance()) {
                            Long key = SpectrumMatch.getKey(refFile, spectrumTitle);
                            spectra.add(key);
                        }

                    } else if (Math.abs(precursor.mz - refPrecursor.mz)
                            <= reporterIonSelectionSettings.getPrecursorMzTolerance()) {
                        Long key = SpectrumMatch.getKey(refFile, spectrumTitle);
                        spectra.add(key);
                    }
                }

            }

        }

        // compute spectrum level ratios
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<>();

        for (Long tempSpectrumKey : spectra) {

            SpectrumQuantificationDetails spectrumQuantification
                    = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(
                            spectrumProvider,
                            reporterIonQuantification,
                            reporterIonSelectionSettings,
                            tempSpectrumKey
                    );

            ArrayList<String> controlIndexes = reporterIonQuantification.getControlSamples();

            if (controlIndexes == null || controlIndexes.isEmpty()) {
                controlIndexes = new ArrayList<>(indexes);
            }

            ArrayList<Double> controlIntensities = new ArrayList<>(controlIndexes.size());

            for (String index : controlIndexes) {

                double intensity = spectrumQuantification.getDeisotopedIntensity(index);

                if (intensity > 0) {
                    controlIntensities.add(intensity);
                }
            }

            if (controlIntensities.isEmpty()) {

                for (String index : indexes) {

                    double intensity = spectrumQuantification.getDeisotopedIntensity(index);

                    if (intensity > 0) {
                        controlIntensities.add(intensity);
                    }
                }
            }

            double normalization = 0;

            if (!controlIntensities.isEmpty()) {
                normalization = BasicMathFunctions.median(controlIntensities);
            }

            for (String index : indexes) {

                double ratio = 0;

                if (normalization > 0) {
                    double intensity = spectrumQuantification.getDeisotopedIntensity(index);
                    ratio = intensity / normalization;
                }

                ArrayList<Double> channelRatios = ratios.get(index);

                if (channelRatios == null) {
                    channelRatios = new ArrayList<Double>(spectra.size());
                    ratios.put(index, channelRatios);
                }

                if (QuantificationFilter.isRatioValid(ratioEstimationSettings, ratio)) {
                    channelRatios.add(ratio);
                }
            }
        }

        for (String index : indexes) {
            ArrayList<Double> channelRatios = ratios.get(index);
            result.setRawRatio(index, RatioEstimator.estimateRatios(ratioEstimationSettings, channelRatios));
        }

        return result;

    }

    /**
     * Returns the quantification details of a spectrum.
     *
     * @param identification the identification containing identification
     * details
     * @param spectrumProvider the spectrum provider
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param reporterIonQuantification the reporter ion quantification details
     * @param reporterIonSelectionSettings the reporter ion selection settings
     * @param matchKey the key of the spectrum of interest
     *
     * @return the quantification details of the spectrum
     */
    public static SpectrumQuantificationDetails estimateSpectrumQuantificationDetails(
            Identification identification,
            SpectrumProvider spectrumProvider,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            ReporterIonQuantification reporterIonQuantification,
            ReporterIonSelectionSettings reporterIonSelectionSettings,
            Long matchKey
    ) {

        ReporterMethod reporterMethod = reporterIonQuantification.getReporterMethod();
        SpectrumMatch tempSpectrumMatch = identification.getSpectrumMatch(matchKey);

        Spectrum spectrum = spectrumProvider.getSpectrum(
                tempSpectrumMatch.getSpectrumFile(),
                tempSpectrumMatch.getSpectrumTitle()
        );

        SpectrumQuantificationDetails result = new SpectrumQuantificationDetails();

        // get reporter intensities
        Set<String> labels = reporterIonQuantification.getSampleIndexes();
        HashMap<String, IonMatch> matchesMap = new HashMap<>(labels.size());

        for (String ionName : labels) {

            ReporterIon reporterIon = reporterMethod.getReporterIon(ionName);

            IonMatch bestMatch = getBestReporterIonMatch(
                    reporterIon,
                    1,
                    spectrum,
                    reporterIonSelectionSettings.getReporterIonsMzTolerance(),
                    reporterIonSelectionSettings.isMostAccurate()
            );

            if (bestMatch != null) {
                result.setReporterMatch(ionName, bestMatch);
                matchesMap.put(ionName, bestMatch);
            }
        }

        // get deisotoped intensities
        Deisotoper deisotoper = quantificationFeaturesGenerator.getDeisotoper(
                reporterMethod,
                reporterIonSelectionSettings.getReporterIonsMzTolerance()
        );

        HashMap<String, Double> deisotoped = deisotoper.deisotope(
                matchesMap,
                spectrum,
                reporterIonSelectionSettings.getReporterIonsMzTolerance(),
                reporterIonSelectionSettings.isMostAccurate()
        );

        for (String index : reporterIonQuantification.getSampleIndexes()) {

            Double intensity = deisotoped.get(index);

            if (intensity == null || intensity < 0) {
                intensity = 0.0;
            }

            result.setDeisotopedIntensity(index, intensity);
        }

        return result;
    }

    /**
     * Returns the best reporter ion match based on mass accuracy. It is
     * possible to select the most accurate or the most intense ion, as set by
     * the mostAccurate boolean. Null if none found
     *
     * @param reporterIon the reporter ion to match
     * @param charge the expected charge
     * @param spectrum the spectrum inspected
     * @param mzTolerance the m/z tolerance
     * @param mostAccurate boolean indicating whether the most accurate ion
     * should be selected
     *
     * @return the best ion match
     */
    public static IonMatch getBestReporterIonMatch(
            ReporterIon reporterIon,
            int charge,
            Spectrum spectrum,
            double mzTolerance,
            boolean mostAccurate
    ) {

        ArrayList<IonMatch> ionMatches = SpectrumAnnotator.matchReporterIon(reporterIon, 1, spectrum, mzTolerance);
        IonMatch bestMatch = null;
        double bestError = mzTolerance;
        double bestIntensity = 0;

        for (IonMatch ionMatch : ionMatches) {

            boolean bestIon = false;

            if (bestMatch == null) {

                bestIon = true;

            } else if (mostAccurate) {

                double ionError = Math.abs(ionMatch.getAbsoluteError());

                if (ionError < bestError) {

                    bestIon = true;
                    bestError = ionError;

                } else if (ionError == bestError) {

                    double intensity = ionMatch.peakIntensity;

                    if (intensity > bestIntensity) {
                        bestIon = true;
                        bestIntensity = intensity;
                    }

                }

            } else {

                double intensity = ionMatch.peakIntensity;

                if (intensity > bestIntensity) {

                    bestIon = true;
                    bestIntensity = intensity;
                }

            }

            if (bestIon) {
                bestMatch = ionMatch;
            }
        }

        return bestMatch;
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public static String getJarFilePath() {

        return CompomicsWrapper.getJarFilePath(
                (new Reporter()).getClass().getResource("Reporter.class").getPath(),
                "Reporter"
        );

    }

    /**
     * Sets the folder to use for temporary files.
     *
     * @param tempFolderPath the folder to use for temporary files
     */
    public static void setTempFolderPath(
            String tempFolderPath
    ) {
        Reporter.tempFolderPath = tempFolderPath;
    }

    /**
     * Returns the folder to use for temporary files. By default the resources
     * folder is used.
     *
     * @param jarFilePath the path to the jar file
     * @return the folder to use for temporary files
     */
    public static String getTempFolderPath(
            String jarFilePath
    ) {
        if (tempFolderPath == null) {
            if (jarFilePath.equals(".")) {
                tempFolderPath = "resources" + File.separator + "temp";
            } else {
                tempFolderPath = jarFilePath + File.separator + "resources" + File.separator + "temp";
            }
            File tempFolder = new File(tempFolderPath);
            if (!tempFolder.exists()) {
                tempFolder.mkdirs();
            }
        }
        return tempFolderPath;
    }

    /**
     * Returns the file containing the database.
     *
     * @return the file containing the database
     */
    public static File getMatchesFolder() {

        return new File(getTempFolderPath(getJarFilePath()), matchesFolder);

    }

    /**
     * Sets the path configuration.
     *
     * @throws java.io.IOException exception thrown whenever an error occurs
     * while reading or writing the paths configuration file
     */
    public static void setPathConfiguration() throws IOException {

        File pathConfigurationFile = new File(getJarFilePath(), UtilitiesPathParameters.configurationFileName);

        if (pathConfigurationFile.exists()) {
            ReporterPathPreferences.loadPathParametersFromFile(pathConfigurationFile);
        }

    }

    /**
     * Returns the enzymes file.
     *
     * @return the enzymes file
     */
    public static File getEnzymesFile() {

        String jarFilePath = getJarFilePath();
        File result = new File(jarFilePath, enzymeFile);

        if (!result.exists()) {
            System.out.println(result.getAbsolutePath() + " not found!");
            FileNotFoundException ex = new FileNotFoundException(result.getAbsolutePath() + " not found!");
            ex.printStackTrace();
        }

        return result;

    }

    /**
     * Returns the default methods file.
     *
     * @return the default methods file
     */
    public static File getMethodsFile() {

        String jarFilePath = getJarFilePath();
        File result = new File(jarFilePath, methodsFile);

        if (!result.exists()) {
            System.out.println(result.getAbsolutePath() + " not found!");
            FileNotFoundException ex = new FileNotFoundException(result.getAbsolutePath() + " not found!");
            ex.printStackTrace();
        }

        return result;

    }
}
