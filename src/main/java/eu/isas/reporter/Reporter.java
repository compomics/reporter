package eu.isas.reporter;

import com.compomics.software.CompomicsWrapper;
import com.compomics.software.settings.UtilitiesPathPreferences;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.identification.spectrum_annotation.SpectrumAnnotator;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
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
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import javax.swing.JOptionPane;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Reporter performs reporter ion based quantification on MS2 spectra.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class Reporter {

    /**
     * The location of the folder used for the database. //@TODO: make this
     * editable by the user
     */
    private static String MATCHES_FOLDER = "resources/matches";
    /**
     * Enzymes file.
     */
    private static String enzymeFile = "resources/conf/searchGUI_enzymes.xml";
    /**
     * Default methods file.
     */
    private static String methodsFile = "resources/conf/defaultMethods.xml";

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
    public static ProteinQuantificationDetails estimateProteinMatchQuantificationDetails(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification, SearchParameters searchParameters, ProteinMatch proteinMatch, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        ProteinQuantificationDetails result = new ProteinQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();
        HashMap<String, ArrayList<Double>> uniqueRatios = new HashMap<String, ArrayList<Double>>();
        HashMap<String, ArrayList<Double>> sharedRatios = new HashMap<String, ArrayList<Double>>();
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, null, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            PeptideMatch peptideMatch = peptideMatchesIterator.next();

            if (QuantificationFilter.isPeptideValid(ratioEstimationSettings, identification, searchParameters, peptideMatch)) {
                for (String index : indexes) {
                    PeptideQuantificationDetails peptideQuantification = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch, waitingHandler);
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
                        if (identificationFeaturesGenerator.getNValidatedProteinGroups(peptideMatch.getTheoreticPeptide()) == 1) {
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
     * Returns the quantification details of a PTM on a protein. //@TODO:
     * discriminate peptides according to the neighboring sites?
     *
     * @param identification the identification containing identification
     * details
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param ratioEstimationSettings the ratio estimation settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param searchParameters the identification settings used
     * @param sequenceMatchingPreferences the sequence matching preferences
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
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     */
    public static ProteinPtmQuantificationDetails estimatePTMQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification, SearchParameters searchParameters,
            SequenceMatchingPreferences sequenceMatchingPreferences, String ptmName, String matchKey, int site, WaitingHandler waitingHandler)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        ProteinPtmQuantificationDetails result = new ProteinPtmQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, parameters, waitingHandler);

        while (peptideMatchesIterator.hasNext()) {

            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            Peptide peptide = peptideMatch.getTheoreticPeptide();
            if (peptide.isModified()) {
                boolean modified = false;
                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                    if (modificationMatch.getTheoreticPtm().equals(ptmName) && modificationMatch.isConfident()) {
                        String leadingAccession = proteinMatch.getMainMatch();
                        Protein leadingProtein = SequenceFactory.getInstance().getProtein(leadingAccession);
                        ArrayList<Integer> peptideIndexes = leadingProtein.getPeptideStart(peptide.getSequence(),
                                sequenceMatchingPreferences);
                        for (int index : peptideIndexes) {
                            if (index + modificationMatch.getModificationSite() == site) {
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
                    PeptideQuantificationDetails peptideQuantification = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch, waitingHandler);
                    double ratio = peptideQuantification.getRatio(index, reporterIonQuantification.getNormalizationFactors());
                    ArrayList<Double> channelRatios = ratios.get(index);
                    if (channelRatios == null) {
                        channelRatios = new ArrayList<Double>(proteinMatch.getPeptideCount());
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
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param ratioEstimationSettings the ratio estimation settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param peptideMatch the peptide match
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
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     */
    public static PeptideQuantificationDetails estimatePeptideMatchQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification, PeptideMatch peptideMatch, WaitingHandler waitingHandler)
            throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        PeptideQuantificationDetails result = new PeptideQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        PsmIterator psmIterator = identification.getPsmIterator(peptideMatch.getSpectrumMatchesKeys(), parameters, false, waitingHandler);

        while (psmIterator.hasNext()) {

            SpectrumMatch spectrumMatch = psmIterator.next();
            String spectrumKey = spectrumMatch.getKey();

            if (QuantificationFilter.isPsmValid(ratioEstimationSettings, identification, spectrumKey)) {
                for (String index : indexes) {
                    PsmQuantificationDetails spectrumQuantification = quantificationFeaturesGenerator.getPSMQuantificationDetails(spectrumKey);
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
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param reporterIonSelectionSettings the reporter ion selection settings
     * @param ratioEstimationSettings the ratio estimation settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param matchKey the key of the match of interest
     *
     * @return the quantification details of the match
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
    public static PsmQuantificationDetails estimatePSMQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonSelectionSettings reporterIonSelectionSettings, RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification, String matchKey)
            throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        PsmQuantificationDetails result = new PsmQuantificationDetails();
        // Find the spectra corresponding to this PSM according to the matching type selected by the user
        ArrayList<String> spectra = new ArrayList<String>(1);

        if (reporterIonSelectionSettings.isSameSpectra()) {
            spectra.add(matchKey);
        } else {
            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            String refFile = Spectrum.getSpectrumFile(matchKey);
            Precursor refPrecursor = spectrumFactory.getPrecursor(matchKey, true);
            // match spectra by mass and retention time
            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(refFile)) {
                Precursor precursor = spectrumFactory.getPrecursor(refFile, spectrumTitle);
                if (Math.abs(precursor.getRt() - refPrecursor.getRt()) <= reporterIonSelectionSettings.getPrecursorRTTolerance()) {
                    if (reporterIonSelectionSettings.isPrecursorMzPpm()) {
                        double error = (precursor.getMz() - refPrecursor.getMz()) / refPrecursor.getMz() * 1000000;
                        if (Math.abs(error) <= reporterIonSelectionSettings.getPrecursorMzTolerance()) {
                            String key = Spectrum.getSpectrumKey(refFile, spectrumTitle);
                            spectra.add(key);
                        }
                    } else if (Math.abs(precursor.getMz() - refPrecursor.getMz()) <= reporterIonSelectionSettings.getPrecursorMzTolerance()) {
                        String key = Spectrum.getSpectrumKey(refFile, spectrumTitle);
                        spectra.add(key);
                    }
                }
            }
        }

        // compute spectrum level ratios
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();

        for (String spectrumKey : spectra) {
            SpectrumQuantificationDetails spectrumQuantification = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(reporterIonQuantification, reporterIonSelectionSettings, spectrumKey);
            ArrayList<String> controlIndexes = reporterIonQuantification.getControlSamples();
            if (controlIndexes == null || controlIndexes.isEmpty()) {
                controlIndexes = new ArrayList<String>(indexes);
            }
            ArrayList<Double> controlIntensities = new ArrayList<Double>(controlIndexes.size());
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
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param reporterIonQuantification the reporter ion quantification details
     * @param reporterIonSelectionSettings the reporter ion selection settings
     * @param matchKey the key of the spectrum of interest
     *
     * @return the quantification details of the spectrum
     *
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while interacting with a file
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     */
    public static SpectrumQuantificationDetails estimateSpectrumQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification,
            ReporterIonSelectionSettings reporterIonSelectionSettings, String matchKey) throws IOException, MzMLUnmarshallerException {

        ReporterMethod reporterMethod = reporterIonQuantification.getReporterMethod();
        MSnSpectrum spectrum = (MSnSpectrum) SpectrumFactory.getInstance().getSpectrum(matchKey);

        SpectrumQuantificationDetails result = new SpectrumQuantificationDetails();

        // get reporter intensities
        Set<String> labels = reporterIonQuantification.getSampleIndexes();
        HashMap<String, IonMatch> matchesMap = new HashMap<String, IonMatch>(labels.size());
        for (String ionName : labels) {
            ReporterIon reporterIon = reporterMethod.getReporterIon(ionName);
            IonMatch bestMatch = getBestReporterIonMatch(reporterIon, 1, spectrum, reporterIonSelectionSettings.getReporterIonsMzTolerance(), reporterIonSelectionSettings.isMostAccurate());
            if (bestMatch != null) {
                result.setReporterMatch(ionName, bestMatch);
                matchesMap.put(ionName, bestMatch);
            }
        }

        // get deisotoped intensities
        Deisotoper deisotoper = quantificationFeaturesGenerator.getDeisotoper(reporterMethod, reporterIonSelectionSettings.getReporterIonsMzTolerance());
        HashMap<String, Double> deisotoped = deisotoper.deisotope(matchesMap, spectrum, reporterIonSelectionSettings.getReporterIonsMzTolerance(), reporterIonSelectionSettings.isMostAccurate());
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
    public static IonMatch getBestReporterIonMatch(ReporterIon reporterIon, int charge, Spectrum spectrum, double mzTolerance, boolean mostAccurate) {
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
                    double intensity = ionMatch.peak.intensity;
                    if (intensity > bestIntensity) {
                        bestIon = true;
                        bestIntensity = intensity;
                    }
                }
            } else {
                double intensity = ionMatch.peak.intensity;
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
        return CompomicsWrapper.getJarFilePath((new Reporter()).getClass().getResource("Reporter.class").getPath(), "Reporter");
    }

    /**
     * Returns the file containing the database.
     *
     * @return the file containing the database
     */
    public static File getMatchesFolder() {
        return new File(getJarFilePath(), MATCHES_FOLDER);
    }

    /**
     * Sets the path configuration.
     *
     * @throws java.io.IOException exception thrown whenever an error occurs
     * while reading or writing the paths configuration file
     */
    public static void setPathConfiguration() throws IOException {
        File pathConfigurationFile = new File(getJarFilePath(), UtilitiesPathPreferences.configurationFileName);
        if (pathConfigurationFile.exists()) {
            ReporterPathPreferences.loadPathPreferencesFromFile(pathConfigurationFile);
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
            JOptionPane.showMessageDialog(null, enzymeFile + " not found.", "Enzymes File Error", JOptionPane.ERROR_MESSAGE);
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
            JOptionPane.showMessageDialog(null, methodsFile + " not found.", "Methods File Error", JOptionPane.ERROR_MESSAGE);
        }
        return result;
    }
}
