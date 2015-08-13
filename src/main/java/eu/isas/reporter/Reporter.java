package eu.isas.reporter;

import com.compomics.software.CompomicsWrapper;
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
import eu.isas.reporter.calculation.Deisotoper;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFilter;
import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.settings.RatioEstimationSettings;
import eu.isas.reporter.settings.ReporterIonSelectionSettings;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PtmSiteQuantificationDetails;
import eu.isas.reporter.quantificationdetails.SpectrumQuantificationDetails;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Reporter performs reporter ion based quantification on MS2 spectra.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class Reporter {

    /**
     * Modification file.
     */
    private static String MODIFICATIONS_FILE = "resources/conf/reporter_mods.xml";
    /**
     * User modification file.
     */
    private static String USER_MODIFICATIONS_FILE = "resources/conf/reporter_usermods.xml";
    
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
    public static ProteinQuantificationDetails estimateProteinMatchQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification, SearchParameters searchParameters, ProteinMatch proteinMatch, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        ProteinQuantificationDetails result = new ProteinQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();
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
                    double ratio = peptideQuantification.getRatio(index, reporterIonQuantification);
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
    public static PtmSiteQuantificationDetails estimatePTMQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, RatioEstimationSettings ratioEstimationSettings,
            ReporterIonQuantification reporterIonQuantification, SearchParameters searchParameters,
            SequenceMatchingPreferences sequenceMatchingPreferences, String ptmName, String matchKey, int site, WaitingHandler waitingHandler)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        PtmSiteQuantificationDetails result = new PtmSiteQuantificationDetails();
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
            if (QuantificationFilter.isPeptideValid(ratioEstimationSettings, identification, searchParameters, peptideMatch)) {
                for (String index : indexes) {
                    PeptideQuantificationDetails peptideQuantification = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch, waitingHandler);
                    double ratio = peptideQuantification.getRatio(index, reporterIonQuantification);
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
                    double ratio = spectrumQuantification.getRatio(index);
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
                    } else {
                        if (Math.abs(precursor.getMz() - refPrecursor.getMz()) <= reporterIonSelectionSettings.getPrecursorMzTolerance()) {
                            String key = Spectrum.getSpectrumKey(refFile, spectrumTitle);
                            spectra.add(key);
                        }
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
            result.setRatio(index, RatioEstimator.estimateRatios(ratioEstimationSettings, channelRatios));
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
            IonMatch bestMatch = getBestReporterIonMatch(reporterIon, 1, spectrum, reporterIonSelectionSettings.getReporterIonsMzTolerance());
            if (bestMatch != null) {
                result.setReporterMatch(ionName, bestMatch);
                matchesMap.put(ionName, bestMatch);
            }
        }

        // get deisotoped intensities
        Deisotoper deisotoper = quantificationFeaturesGenerator.getDeisotoper(reporterMethod, reporterIonSelectionSettings.getReporterIonsMzTolerance());
        HashMap<String, Double> deisotoped = deisotoper.deisotope(matchesMap, spectrum, reporterIonSelectionSettings.getReporterIonsMzTolerance());
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
     * Returns the best reporter ion match based on mass accuracy. Null if none
     * found
     *
     * @param reporterIon the reporter ion to match
     * @param charge the expected charge
     * @param spectrum the spectrum inspected
     * @param mzTolerance the m/z tolerance
     *
     * @return the best ion match
     */
    public static IonMatch getBestReporterIonMatch(ReporterIon reporterIon, int charge, Spectrum spectrum, double mzTolerance) {
        ArrayList<IonMatch> ionMatches = SpectrumAnnotator.matchReporterIon(reporterIon, 1, spectrum, mzTolerance);
        IonMatch bestMatch = null;
        double error = mzTolerance;
        double bestIntensity = 0;
        for (IonMatch ionMatch : ionMatches) {
            if (bestMatch == null
                    || Math.abs(ionMatch.getAbsoluteError()) < error
                    || ionMatch.getAbsoluteError() == 0 && ionMatch.peak.intensity > bestIntensity) {
                bestMatch = ionMatch;
            }
        }
        return bestMatch;
    }

    /**
     * Returns the file used for default modifications pre-loading.
     *
     * @return the file used for default modifications pre-loading
     */
    public static String getDefaultModificationFile() {
        return MODIFICATIONS_FILE;
    }

    /**
     * Sets the file used for default modifications pre-loading.
     *
     * @param modificationFile the file used for default modifications
     * pre-loading
     */
    public static void setDefaultModificationFile(String modificationFile) {
        Reporter.MODIFICATIONS_FILE = modificationFile;
    }

    /**
     * Returns the file used for user modifications pre-loading.
     *
     * @return the file used for user modifications pre-loading
     */
    public static String getUserModificationFile() {
        return USER_MODIFICATIONS_FILE;
    }

    /**
     * Sets the file used for user modifications pre-loading.
     *
     * @param modificationFile the file used for user modifications pre-loading
     */
    public static void setUserModificationFile(String modificationFile) {
        Reporter.USER_MODIFICATIONS_FILE = modificationFile;
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public static String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath((new Reporter()).getClass().getResource("Reporter.class").getPath(), "Reporter");
    }
}
