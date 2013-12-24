/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.reporter.calculation;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.myparameters.ReporterPreferences;
import java.io.IOException;
import java.sql.SQLException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The quantification features generator provides various quantification features
 *
 * @author Marc
 */
public class QuantificationFeaturesGenerator {
    
    /**
     * The cache to use
     */
    private QuantificationFeaturesCache quantificationFeaturesCache;
    /**
     * The indentification object provides identification matches
     */
    private Identification identification;
    /**
     * A deisotoper to deisotope reporter ion intensities
     */
    private Deisotoper deisotoper;
    /**
     * The user quantification preferences
     */
    private ReporterPreferences reporterPreferences;
    /**
     * The reporter ion quantification
     */
    private ReporterIonQuantification reporterIonQuantification;
    /**
     * The search parameters used for the search
     */
    private SearchParameters searchParameters;
    /**
     * Constructor
     * 
     * @param quantificationFeaturesCache the cache to use to store quantification results
     * @param identification the identification object containing all identification results
     * @param reporterPreferences the user quantification preferences
     * @param reporterIonQuantification the reporter ion quantification settings
     * @param searchParameters the identification parameters used for the identification of spectra
     */
    public QuantificationFeaturesGenerator(QuantificationFeaturesCache quantificationFeaturesCache, Identification identification, ReporterPreferences reporterPreferences, ReporterIonQuantification reporterIonQuantification, SearchParameters searchParameters) {
        this.quantificationFeaturesCache = quantificationFeaturesCache;
        this.identification = identification;
        this.reporterPreferences = reporterPreferences;
        this.reporterIonQuantification = reporterIonQuantification;
        this.searchParameters = searchParameters;
    }
    
    /**
     * Returns the quantification details of a protein match.
     * 
     * @param matchKey the key of the match of interest
     * 
     * @return the quantification details of the match
     * 
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException 
     */
    public MatchQuantificationDetails getProteinMatchQuantificationDetails(String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        ProteinMatch proteinMatch = identification.getProteinMatch(matchKey);
        int nPeptides = proteinMatch.getPeptideCount();
        MatchQuantificationDetails result = quantificationFeaturesCache.getProteinMatchQuantificationDetails(nPeptides, matchKey);
        if (result == null) {
            result = Reporter.estimateProteinMatchQuantificationDetails(identification, this, reporterPreferences, reporterIonQuantification, searchParameters, matchKey);
            quantificationFeaturesCache.addProteinMatchQuantificationDetails(nPeptides, matchKey, result);
        }
        return result;
    }
    
    /**
     * Returns the quantification details of a PTM.
     * 
     * @param ptmName the name of the ptm
     * @param matchKey the key of the match of interest
     * @param site the site on the protein sequence
     * 
     * @return the quantification details of the match
     * 
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException 
     */
    public MatchQuantificationDetails getPTMQuantificationDetails(String ptmName, String matchKey, int site) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        MatchQuantificationDetails result = quantificationFeaturesCache.getPtmQuantificationDetails(ptmName, matchKey, site);
        if (result == null) {
            result = Reporter.estimatePTMQuantificationDetails(identification, this, reporterPreferences, reporterIonQuantification, searchParameters, ptmName, matchKey, site);
            quantificationFeaturesCache.addPtmQuantificationDetails(ptmName, matchKey, site, result);
        }
        return result;
    }
    
    /**
     * Returns the quantification details of a peptide match.
     * 
     * @param matchKey the key of the match of interest
     * 
     * @return the quantification details of the match
     * 
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException 
     */
    public MatchQuantificationDetails getPeptideMatchQuantificationDetails(String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        PeptideMatch peptideMatch = identification.getPeptideMatch(matchKey);
        int nPsms = peptideMatch.getSpectrumCount();
        MatchQuantificationDetails result = quantificationFeaturesCache.getPeptideMatchQuantificationDetails(nPsms, matchKey);
        if (result == null) {
            result = Reporter.estimatePeptideMatchQuantificationDetails(identification, this, reporterPreferences, reporterIonQuantification, matchKey);
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
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException 
     */
    public MatchQuantificationDetails getPSMQuantificationDetails(String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        MatchQuantificationDetails result = quantificationFeaturesCache.getPSMQuantificationDetails(matchKey);
        if (result == null) {
            result = Reporter.estimatePSMQuantificationDetails(identification, this, reporterPreferences, reporterIonQuantification, matchKey);
            quantificationFeaturesCache.addPSMQuantificationDetails(matchKey, result);
        }
        return result;
    }
    
    /**
     * Returns the quantification details of a spectrum.
     * 
     * @param reporterIonQuantification the quantification object
     * @param reporterPreferences the quantification preferences
     * @param matchKey the key of the match of interest
     * 
     * @return the quantification details of the match
     * 
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException 
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException 
     */
    public MatchQuantificationDetails getSpectrumQuantificationDetails(ReporterIonQuantification reporterIonQuantification, ReporterPreferences reporterPreferences, String matchKey) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        MatchQuantificationDetails result = quantificationFeaturesCache.getSpectrumQuantificationDetails(matchKey);
        if (result == null) {
            result = Reporter.estimateSpectrumQuantificationDetails(identification, this, reporterIonQuantification, reporterPreferences, matchKey);
            quantificationFeaturesCache.addSpectrumQuantificationDetails(matchKey, result);
        }
        return result;
    }
    
    /**
     * Returns the deisotoper corresponding to the given method.
     * 
     * @param reporterMethod the reporter method
     * 
     * @return the deisotoper corresponding to the given method
     */
    public Deisotoper getDeisotoper(ReporterMethod reporterMethod) {
        if (deisotoper == null) {
            deisotoper = new Deisotoper(reporterMethod);
        }
        return deisotoper;
    }
    
}
