package eu.isas.reporter.calculation;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.myparameters.ReporterIonSelectionSettings;
import eu.isas.reporter.myparameters.ReporterSettings;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PtmSiteQuantificationDetails;
import eu.isas.reporter.quantificationdetails.SpectrumQuantificationDetails;
import java.io.IOException;
import java.sql.SQLException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The quantification features generator provides various quantification
 * features.
 *
 * @author Marc Vaudel
 */
public class QuantificationFeaturesGenerator {

    /**
     * The cache to use.
     */
    private QuantificationFeaturesCache quantificationFeaturesCache;
    /**
     * The identification object provides identification matches.
     */
    private Identification identification;
    /**
     * A deisotoper to deisotope reporter ion intensities.
     */
    private Deisotoper deisotoper;
    /**
     * The reporter settings.
     */
    private ReporterSettings reporterSettings;
    /**
     * The reporter ion quantification.
     */
    private ReporterIonQuantification reporterIonQuantification;
    /**
     * The search parameters used for the search.
     */
    private SearchParameters searchParameters;
    /**
     * The sequence matching preferences.
     */
    private SequenceMatchingPreferences sequenceMatchingPreferences;

    /**
     * Constructor.
     *
     * @param quantificationFeaturesCache the cache to use to store
     * quantification results
     * @param identification the identification object containing all
     * identification results
     * @param reporterSettings the reporter settings
     * @param reporterIonQuantification the reporter ion quantification settings
     * @param searchParameters the identification parameters used for the
     * identification of spectra
     * @param sequenceMatchingPreferences the sequence matching preferences
     */
    public QuantificationFeaturesGenerator(QuantificationFeaturesCache quantificationFeaturesCache, Identification identification, ReporterSettings reporterSettings,
            ReporterIonQuantification reporterIonQuantification, SearchParameters searchParameters, SequenceMatchingPreferences sequenceMatchingPreferences) {
        this.quantificationFeaturesCache = quantificationFeaturesCache;
        this.identification = identification;
        this.reporterSettings = reporterSettings;
        this.reporterIonQuantification = reporterIonQuantification;
        this.searchParameters = searchParameters;
        this.sequenceMatchingPreferences = sequenceMatchingPreferences;
    }

    /**
     * Returns the quantification details of a protein match.
     *
     * @param matchKey the key of the match of interest
     * @param waitingHandler the waiting handler
     *
     * @return the quantification details of the match
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown if an
     * MzMLUnmarshallerException occurs
     */
    public ProteinQuantificationDetails getProteinMatchQuantificationDetails(String matchKey, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        int nPeptides = proteinMatch.getPeptideCount();
        ProteinQuantificationDetails result = quantificationFeaturesCache.getProteinMatchQuantificationDetails(nPeptides, matchKey);
        if (result == null) {
            result = Reporter.estimateProteinMatchQuantificationDetails(identification, this, reporterSettings.getRatioEstimationSettings(), reporterIonQuantification, searchParameters, proteinMatch, waitingHandler);
            quantificationFeaturesCache.addProteinMatchQuantificationDetails(nPeptides, matchKey, result);
        }
        return result;
    }

    /**
     * Returns the quantification details of a PTM.
     *
     * @param ptmName the name of the PTM
     * @param matchKey the key of the match of interest
     * @param site the site on the protein sequence
     * @param waitingHandler the waiting handler
     *
     * @return the quantification details of the match
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown if an
     * MzMLUnmarshallerException occurs
     */
    public PtmSiteQuantificationDetails getPTMQuantificationDetails(String ptmName, String matchKey, int site, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        PtmSiteQuantificationDetails result = quantificationFeaturesCache.getPtmQuantificationDetails(ptmName, matchKey, site);
        if (result == null) {
            result = Reporter.estimatePTMQuantificationDetails(identification, this, reporterSettings.getRatioEstimationSettings(), reporterIonQuantification, searchParameters, sequenceMatchingPreferences, ptmName, matchKey, site, waitingHandler);
            quantificationFeaturesCache.addPtmQuantificationDetails(ptmName, matchKey, site, result);
        }
        return result;
    }

    /**
     * Returns the quantification details of a peptide match.
     *
     * @param peptideMatch the peptide match
     * @param waitingHandler the waiting handler
     *
     * @return the quantification details of the match
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown if an
     * MzMLUnmarshallerException occurs
     */
    public PeptideQuantificationDetails getPeptideMatchQuantificationDetails(PeptideMatch peptideMatch, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        int nPsms = peptideMatch.getSpectrumCount();
        String matchKey = peptideMatch.getKey();
        PeptideQuantificationDetails result = quantificationFeaturesCache.getPeptideMatchQuantificationDetails(nPsms, matchKey);
        if (result == null) {
            result = Reporter.estimatePeptideMatchQuantificationDetails(identification, this, reporterSettings.getRatioEstimationSettings(), reporterIonQuantification, peptideMatch, waitingHandler);
            quantificationFeaturesCache.addPeptideMatchQuantificationDetails(nPsms, matchKey, result);
        }
        return result;
    }

    /**
     * Returns the quantification details of a PSM.
     *
     * @param matchKey the key of the match of interest
     *
     * @return the quantification details of the match
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown if an
     * MzMLUnmarshallerException occurs
     */
    public PsmQuantificationDetails getPSMQuantificationDetails(String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        PsmQuantificationDetails result = quantificationFeaturesCache.getPSMQuantificationDetails(matchKey);
        if (result == null) {
            result = Reporter.estimatePSMQuantificationDetails(identification, this, reporterSettings.getReporterIonSelectionSettings(), reporterSettings.getRatioEstimationSettings(), reporterIonQuantification, matchKey);
            quantificationFeaturesCache.addPSMQuantificationDetails(matchKey, result);
        }
        return result;
    }

    /**
     * Returns the quantification details of a spectrum.
     *
     * @param reporterIonQuantification the quantification object
     * @param reporterIonSelectionSettings the reporter ion selection settings
     * @param matchKey the key of the match of interest
     *
     * @return the quantification details of the match
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown if an
     * MzMLUnmarshallerException occurs
     */
    public SpectrumQuantificationDetails getSpectrumQuantificationDetails(ReporterIonQuantification reporterIonQuantification,
            ReporterIonSelectionSettings reporterIonSelectionSettings, String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        if (matchKey.contains("10019.10019")) {
            int debug = 1;
        }
        SpectrumQuantificationDetails result = quantificationFeaturesCache.getSpectrumQuantificationDetails(matchKey);
        if (result == null) {
            result = Reporter.estimateSpectrumQuantificationDetails(identification, this, reporterIonQuantification, reporterIonSelectionSettings, matchKey);
            quantificationFeaturesCache.addSpectrumQuantificationDetails(matchKey, result);
        }
        return result;
    }

    /**
     * Returns the deisotoper corresponding to the given method.
     *
     * @param reporterMethod the reporter method
     * @param reporterIonMassAccuracy the mass accuracy in the reporter ion
     * region
     *
     * @return the deisotoper corresponding to the given method
     */
    public Deisotoper getDeisotoper(ReporterMethod reporterMethod, double reporterIonMassAccuracy) {
        if (deisotoper == null) {
            deisotoper = new Deisotoper(reporterMethod, reporterIonMassAccuracy);
        }
        return deisotoper;
    }
}
