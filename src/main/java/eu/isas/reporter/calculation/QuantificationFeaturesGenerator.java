package eu.isas.reporter.calculation;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.parameters.identification.advanced.SequenceMatchingParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.settings.ReporterIonSelectionSettings;
import eu.isas.reporter.settings.ReporterSettings;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinPtmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.SpectrumQuantificationDetails;
import java.io.IOException;
import java.sql.SQLException;

/**
 * The quantification features generator provides various quantification
 * features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
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
     * The identification features generator.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
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
     * The sequence matching parameters.
     */
    private SequenceMatchingParameters sequenceMatchingParameters;

    /**
     * Constructor.
     *
     * @param quantificationFeaturesCache the cache to use to store
     * quantification results
     * @param identification the identification object containing all
     * identification results
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param reporterSettings the reporter settings
     * @param reporterIonQuantification the reporter ion quantification settings
     * @param searchParameters the identification parameters used for the
     * identification of spectra
     * @param sequenceMatchingParameters the sequence matching preferences
     */
    public QuantificationFeaturesGenerator(
            QuantificationFeaturesCache quantificationFeaturesCache,
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ReporterSettings reporterSettings,
            ReporterIonQuantification reporterIonQuantification,
            SearchParameters searchParameters,
            SequenceMatchingParameters sequenceMatchingParameters
    ) {
        this.quantificationFeaturesCache = quantificationFeaturesCache;
        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.reporterSettings = reporterSettings;
        this.reporterIonQuantification = reporterIonQuantification;
        this.searchParameters = searchParameters;
        this.sequenceMatchingParameters = sequenceMatchingParameters;
    }

    /**
     * Returns the quantification details of a protein match.
     *
     * @param spectrumProvider the spectrum provider
     * @param matchKey the key of the match of interest
     * @param waitingHandler the waiting handler
     *
     * @return the quantification details of the match
     */
    public ProteinQuantificationDetails getProteinMatchQuantificationDetails(
            SpectrumProvider spectrumProvider,
            long matchKey,
            WaitingHandler waitingHandler
    ) {

        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        int nPeptides = proteinMatch.getPeptideCount();
        ProteinQuantificationDetails result = quantificationFeaturesCache.getProteinMatchQuantificationDetails(nPeptides, matchKey);

        if (result == null) {

            result = Reporter.estimateProteinMatchQuantificationDetails(
                    identification,
                    spectrumProvider,
                    identificationFeaturesGenerator,
                    this,
                    reporterSettings.getRatioEstimationSettings(),
                    reporterIonQuantification,
                    searchParameters,
                    proteinMatch,
                    waitingHandler
            );

            quantificationFeaturesCache.addProteinMatchQuantificationDetails(nPeptides, matchKey, result);

        }

        return result;

    }

    /**
     * Returns the quantification details of a PTM.
     *
     * @param spectrumProvider the spectrum provider
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
     */
    public ProteinPtmQuantificationDetails getPTMQuantificationDetails(
            SpectrumProvider spectrumProvider,
            String ptmName,
            long matchKey,
            int site,
            WaitingHandler waitingHandler
    ) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        ProteinPtmQuantificationDetails result = quantificationFeaturesCache.getPtmQuantificationDetails(ptmName, matchKey, site);

        if (result == null) {

            result = Reporter.estimatePTMQuantificationDetails(
                    identification,
                    spectrumProvider,
                    this,
                    reporterSettings.getRatioEstimationSettings(),
                    reporterIonQuantification,
                    searchParameters,
                    sequenceMatchingParameters,
                    ptmName,
                    matchKey,
                    site,
                    waitingHandler
            );

            quantificationFeaturesCache.addPtmQuantificationDetails(ptmName, matchKey, site, result);
        }

        return result;
    }

    /**
     * Returns the quantification details of a peptide match.
     *
     * @param spectrumProvider the spectrum provider
     * @param peptideMatch the peptide match
     * @param waitingHandler the waiting handler
     *
     * @return the quantification details of the match
     */
    public PeptideQuantificationDetails getPeptideMatchQuantificationDetails(
            SpectrumProvider spectrumProvider,
            PeptideMatch peptideMatch,
            WaitingHandler waitingHandler
    ) {

        int nPsms = peptideMatch.getSpectrumCount();
        long matchKey = peptideMatch.getKey();
        PeptideQuantificationDetails result = quantificationFeaturesCache.getPeptideMatchQuantificationDetails(nPsms, matchKey);

        if (result == null) {

            result = Reporter.estimatePeptideMatchQuantificationDetails(
                    identification,
                    spectrumProvider,
                    this,
                    reporterSettings.getRatioEstimationSettings(),
                    reporterIonQuantification,
                    peptideMatch,
                    waitingHandler
            );

            quantificationFeaturesCache.addPeptideMatchQuantificationDetails(nPsms, matchKey, result);
        }

        return result;

    }

    /**
     * Returns the quantification details of a PSM.
     *
     * @param spectrumProvider the spectrum provider
     * @param matchKey the key of the match of interest
     *
     * @return the quantification details of the match
     */
    public PsmQuantificationDetails getPSMQuantificationDetails(
            SpectrumProvider spectrumProvider,
            Long matchKey
    ) {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
        String spectrumFile = spectrumMatch.getSpectrumFile();
        String spectrumTitle = spectrumMatch.getSpectrumTitle();

        PsmQuantificationDetails result = quantificationFeaturesCache.getPSMQuantificationDetails(spectrumFile, spectrumTitle);

        if (result == null) {

            result = Reporter.estimatePSMQuantificationDetails(
                    identification,
                    spectrumProvider,
                    this,
                    reporterSettings.getReporterIonSelectionSettings(),
                    reporterSettings.getRatioEstimationSettings(),
                    reporterIonQuantification,
                    matchKey
            );

            quantificationFeaturesCache.addPSMQuantificationDetails(spectrumFile, spectrumTitle, result);
        }

        return result;
    }

    /**
     * Returns the quantification details of a spectrum.
     *
     * @param spectrumProvider the spectrum provider
     * @param reporterIonQuantification the quantification object
     * @param reporterIonSelectionSettings the reporter ion selection settings
     * @param matchKey the key of the match of interest
     *
     * @return the quantification details of the match
     */
    public SpectrumQuantificationDetails getSpectrumQuantificationDetails(
            SpectrumProvider spectrumProvider,
            ReporterIonQuantification reporterIonQuantification,
            ReporterIonSelectionSettings reporterIonSelectionSettings,
            Long matchKey
    ) {

        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
        String spectrumFile = spectrumMatch.getSpectrumFile();
        String spectrumTitle = spectrumMatch.getSpectrumTitle();

        SpectrumQuantificationDetails result = quantificationFeaturesCache.getSpectrumQuantificationDetails(spectrumFile, spectrumTitle);

        if (result == null) {

            result = Reporter.estimateSpectrumQuantificationDetails(
                    identification,
                    spectrumProvider,
                    this,
                    reporterIonQuantification,
                    reporterIonSelectionSettings,
                    matchKey
            );

            quantificationFeaturesCache.addSpectrumQuantificationDetails(spectrumFile, spectrumTitle, result);
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
    public Deisotoper getDeisotoper(
            ReporterMethod reporterMethod,
            double reporterIonMassAccuracy
    ) {

        if (deisotoper == null) {
            deisotoper = new Deisotoper(reporterMethod, reporterIonMassAccuracy);
        }

        return deisotoper;

    }

    /**
     * Returns the quantification features cache.
     *
     * @return the quantification features cache
     */
    public QuantificationFeaturesCache getQuantificationFeaturesCache() {
        return quantificationFeaturesCache;
    }

    /**
     * Sets the quantification features cache.
     *
     * @param quantificationFeaturesCache the quantification features cache
     */
    public void setQuantificationFeaturesCache(
            QuantificationFeaturesCache quantificationFeaturesCache
    ) {
        this.quantificationFeaturesCache = quantificationFeaturesCache;
    }
}
