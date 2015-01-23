package eu.isas.reporter;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.SpectrumAnnotator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
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
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.reporter.calculation.Deisotoper;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFilter;
import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.myparameters.ReporterPreferences;
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
     * Sets the normalization factors in the ReporterIonQuantification object.
     *
     * @param reporterIonQuantification the reporter ion quantification
     * @param reporterPreferences the quantification preferences
     * @param identification the identification
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param waitingHandler waiting handler displaying progress to the user
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred
     */
    public static void setNormalizationFactors(ReporterIonQuantification reporterIonQuantification, ReporterPreferences reporterPreferences,
            Identification identification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();
        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            ratios.put(sampleIndex, new ArrayList<Double>());
        }
        int progress = 0;
        int totalProgress = 2 * identification.getSpectrumFiles().size() + 4;
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Ratio Normalization (Step " + ++progress + " of " + totalProgress + "). Please Wait...");
            waitingHandler.resetPrimaryProgressCounter();
            waitingHandler.setPrimaryProgressCounterIndeterminate(false);
            waitingHandler.setMaxPrimaryProgressCounter(identification.getPeptideIdentification().size());
        }

        progress++;
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, true, parameters);
        while (peptideMatchesIterator.hasNext()) {
            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            String peptideKey = peptideMatch.getKey();
            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);
            if (psParameter.getMatchValidationLevel().isValidated()) {
                PeptideQuantificationDetails matchQuantificationDetails = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch);
                for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
                    Double ratio = matchQuantificationDetails.getRawRatio(sampleIndex);
                    if (QuantificationFilter.isRatioValid(reporterPreferences, ratio) && ratio > 0) {
                        ratios.get(sampleIndex).add(ratio);
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
        for (String sampleIndex : reporterIonQuantification.getSampleIndexes()) {
            double normalisationFactor;
            if (ratios.get(sampleIndex) != null && !ratios.get(sampleIndex).isEmpty()) {
                normalisationFactor = BasicMathFunctions.median(ratios.get(sampleIndex));
            } else {
                normalisationFactor = 1;
            }
            reporterIonQuantification.addNormalisationFactor(sampleIndex, normalisationFactor);
        }
    }

    /**
     * Returns the quantification details of a protein match.
     *
     * @param identification the identification containing identification
     * details
     * @param quantificationFeaturesGenerator the quantification features
     * generator used to store and retrieve quantification details
     * @param reporterPreferences the quantification user settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param searchParameters the identification parameters
     * @param proteinMatch the protein match
     *
     * @return the quantification details of the match
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred
     */
    public static ProteinQuantificationDetails estimateProteinMatchQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterPreferences reporterPreferences,
            ReporterIonQuantification reporterIonQuantification, SearchParameters searchParameters, ProteinMatch proteinMatch)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        ProteinQuantificationDetails result = new ProteinQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();
        
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, parameters);

        while (peptideMatchesIterator.hasNext()) {
            
            PeptideMatch peptideMatch = peptideMatchesIterator.next();
            
            if (QuantificationFilter.isPeptideValid(reporterPreferences, identification, searchParameters, peptideMatch)) {
                for (String index : indexes) {
                    PeptideQuantificationDetails peptideQuantification = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch);
                    double ratio = peptideQuantification.getRatio(index, reporterIonQuantification);
                    ArrayList<Double> channelRatios = ratios.get(index);
                    if (channelRatios == null) {
                        channelRatios = new ArrayList<Double>(proteinMatch.getPeptideCount());
                        ratios.put(index, channelRatios);
                    }
                    if (QuantificationFilter.isRatioValid(reporterPreferences, ratio)) {
                        channelRatios.add(ratio);
                    }
                }
            }
        }

        for (String index : indexes) {
            ArrayList<Double> channelRatios = ratios.get(index);
            result.setRatio(index, RatioEstimator.estimateRatios(reporterPreferences, channelRatios));
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
     * @param reporterPreferences the quantification user settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param searchParameters the identification settings used
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param ptmName the name of the PTM
     * @param matchKey the key of the match of interest
     * @param site the site of the PTM on the protein sequence
     *
     * @return the quantification details of the match
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred
     */
    public static PtmSiteQuantificationDetails estimatePTMQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterPreferences reporterPreferences,
            ReporterIonQuantification reporterIonQuantification, SearchParameters searchParameters,
            SequenceMatchingPreferences sequenceMatchingPreferences, String ptmName, String matchKey, int site)
            throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        PtmSiteQuantificationDetails result = new PtmSiteQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();
        
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(proteinMatch.getPeptideMatchesKeys(), parameters, false, parameters);
        
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
            if (QuantificationFilter.isPeptideValid(reporterPreferences, identification, searchParameters, peptideMatch)) {
                for (String index : indexes) {
                    PeptideQuantificationDetails peptideQuantification = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch);
                    double ratio = peptideQuantification.getRatio(index, reporterIonQuantification);
                    ArrayList<Double> channelRatios = ratios.get(index);
                    if (channelRatios == null) {
                        channelRatios = new ArrayList<Double>(proteinMatch.getPeptideCount());
                        ratios.put(index, channelRatios);
                    }
                    if (QuantificationFilter.isRatioValid(reporterPreferences, ratio)) {
                        channelRatios.add(ratio);
                    }
                }
            }
        }

        for (String index : indexes) {
            ArrayList<Double> channelRatios = ratios.get(index);
            result.setRatio(index, RatioEstimator.estimateRatios(reporterPreferences, channelRatios));
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
     * @param reporterPreferences the quantification user settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param peptideMatch the peptide match
     *
     * @return the quantification details of the match
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred
     */
    public static PeptideQuantificationDetails estimatePeptideMatchQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterPreferences reporterPreferences,
            ReporterIonQuantification reporterIonQuantification, PeptideMatch peptideMatch)
            throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        PeptideQuantificationDetails result = new PeptideQuantificationDetails();
        HashMap<String, ArrayList<Double>> ratios = new HashMap<String, ArrayList<Double>>();
        Set<String> indexes = reporterIonQuantification.getSampleIndexes();
        
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        
        PsmIterator psmIterator = identification.getPsmIterator(peptideMatch.getSpectrumMatches(), parameters, false);

        while (psmIterator.hasNext()) {
            
            SpectrumMatch spectrumMatch = psmIterator.next();
            String spectrumKey = spectrumMatch.getKey();
            
            if (QuantificationFilter.isPsmValid(reporterPreferences, identification, spectrumKey)) {
                for (String index : indexes) {
                    PsmQuantificationDetails spectrumQuantification = quantificationFeaturesGenerator.getPSMQuantificationDetails(spectrumKey);
                    double ratio = spectrumQuantification.getRatio(index);
                    ArrayList<Double> channelRatios = ratios.get(index);
                    if (channelRatios == null) {
                        channelRatios = new ArrayList<Double>(peptideMatch.getSpectrumCount());
                        ratios.put(index, channelRatios);
                    }
                    if (QuantificationFilter.isRatioValid(reporterPreferences, ratio)) {
                        channelRatios.add(ratio);
                    }
                }
            }
        }

        for (String index : indexes) {
            ArrayList<Double> channelRatios = ratios.get(index);
            result.setRawRatio(index, RatioEstimator.estimateRatios(reporterPreferences, channelRatios));
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
     * @param reporterPreferences the quantification user settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param matchKey the key of the match of interest
     *
     * @return the quantification details of the match
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred
     */
    public static PsmQuantificationDetails estimatePSMQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterPreferences reporterPreferences,
            ReporterIonQuantification reporterIonQuantification, String matchKey)
            throws IOException, MzMLUnmarshallerException, SQLException, ClassNotFoundException, InterruptedException {

        PsmQuantificationDetails result = new PsmQuantificationDetails();
        // Find the spectra corresponding to this PSM according to the matching type selected by the user
        ArrayList<String> spectra = new ArrayList<String>();

        if (reporterPreferences.isSameSpectra()) {
            spectra.add(matchKey);
        } else {
            SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
            String refFile = Spectrum.getSpectrumFile(matchKey);
            Precursor refPrecursor = spectrumFactory.getPrecursor(matchKey, true);
            // match spectra by mass and retention time
            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(refFile)) {
                Precursor precursor = spectrumFactory.getPrecursor(refFile, spectrumTitle);
                if (Math.abs(precursor.getRt() - refPrecursor.getRt()) <= reporterPreferences.getPrecursorRTTolerance()) {
                    if (reporterPreferences.isPrecursorMzPpm()) {
                        double error = (precursor.getMz() - refPrecursor.getMz()) / refPrecursor.getMz() * 1000000;
                        if (Math.abs(error) <= reporterPreferences.getPrecursorMzTolerance()) {
                            String key = Spectrum.getSpectrumKey(refFile, spectrumTitle);
                            spectra.add(key);
                        }
                    } else {
                        if (Math.abs(precursor.getMz() - refPrecursor.getMz()) <= reporterPreferences.getPrecursorMzTolerance()) {
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
            SpectrumQuantificationDetails spectrumQuantification = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(reporterIonQuantification, reporterPreferences, spectrumKey);
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
                if (QuantificationFilter.isRatioValid(reporterPreferences, ratio)) {
                    channelRatios.add(ratio);
                }
            }
        }

        for (String index : indexes) {
            ArrayList<Double> channelRatios = ratios.get(index);
            result.setRatio(index, RatioEstimator.estimateRatios(reporterPreferences, channelRatios));
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
     * @param reporterPreferences the quantification preferences
     * @param matchKey the key of the spectrum of interest
     *
     * @return the quantification details of the spectrum
     * 
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     */
    public static SpectrumQuantificationDetails estimateSpectrumQuantificationDetails(Identification identification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification,
            ReporterPreferences reporterPreferences, String matchKey) throws IOException, MzMLUnmarshallerException {

        ReporterMethod reporterMethod = reporterIonQuantification.getReporterMethod();
        MSnSpectrum spectrum = (MSnSpectrum) SpectrumFactory.getInstance().getSpectrum(matchKey);

        SpectrumQuantificationDetails result = new SpectrumQuantificationDetails();

        // get reporter intensities
        HashMap<String, IonMatch> matchesMap = new HashMap<String, IonMatch>();
        for (String ionName : reporterIonQuantification.getSampleIndexes()) {
            ReporterIon reporterIon = reporterMethod.getReporterIon(ionName);
            IonMatch bestMatch = getBestReporterIonMatch(reporterIon, 1, spectrum, reporterPreferences.getReporterIonsMzTolerance());
            if (bestMatch != null) {
                result.setReporterMatch(ionName, bestMatch);
                matchesMap.put(ionName, bestMatch);
            }
        }

        // get deisotoped intensities
        Deisotoper deisotoper = quantificationFeaturesGenerator.getDeisotoper(reporterMethod, reporterPreferences.getReporterIonsMzTolerance());
        HashMap<String, Double> deisotoped = deisotoper.deisotope(matchesMap, spectrum, reporterPreferences.getReporterIonsMzTolerance());
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
}
