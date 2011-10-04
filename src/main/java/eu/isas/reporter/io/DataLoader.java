package eu.isas.reporter.io;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.io.massspectrometry.MgfReader;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PsmQuantification;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.gui.WaitingDialog;
import eu.isas.reporter.identifications.FdrCalculator;
import eu.isas.reporter.identifications.IdFilter;
import eu.isas.reporter.myparameters.IdentificationDetails;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JOptionPane;

/**
 * This class will load information from spectra and identifications files.
 *
 * @author Marc
 */
public class DataLoader {

    /**
     * The compomics utilities identification file reader factory
     */
    private IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();
    /**
     * The FDR estimator
     */
    private FdrCalculator fdrCalculator = new FdrCalculator();
    /**
     * The waiting dialog will display feedback to the user
     */
    private WaitingDialog waitingDialog;
    /**
     * The quantification preferences
     */
    private QuantificationPreferences quantificationPreferences;
    /**
     * modification file
     */
    private final String MODIFICATIONS_FILE = "conf/reporter_mods.xml";
    /**
     * user modification file
     */
    private final String USER_MODIFICATIONS_FILE = "conf/reporter_usermods.xml";
    /**
     * The compomics PTM factory
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The identification
     */
    private Identification identification;
    /**
     * The number of decimals to display for the e-values in the report.
     */
    private final int eValueDecimals = 6;
    /**
     * List of all identified spectra
     */
    private ArrayList<String> identifiedSpectra = new ArrayList<String>();
    /**
     * List of needed mgf files
     */
    private ArrayList<String> mgfFilesNeeded = new ArrayList<String>();
    /**
     * The spectrum factory
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance(100);

    /**
     * Constructor
     * @param reporter                  The main reporter class
     * @param quantificationPreferences The quantification preferences
     * @param waitingDialog             A waiting dialog to display feedback to the user
     * @param identification            the identification
     */
    public DataLoader(QuantificationPreferences quantificationPreferences, WaitingDialog waitingDialog, Identification identification) {
        this.identification = identification;
        this.quantificationPreferences = quantificationPreferences;
        this.waitingDialog = waitingDialog;
    }

    /**
     * Loads identifications from a file
     * @param idFiles   the identification files
     */
    public void loadIdentifications(ArrayList<File> idFiles) {
        loadModifications();
        HashSet<SpectrumMatch> allSpectrumMatches = new HashSet<SpectrumMatch>();
        IdFilter idFilter = new IdFilter(quantificationPreferences);
        int nTotal = 0;
        int nRetained = 0;
        try {
            HashSet<SpectrumMatch> tempSet;
            for (File idFile : idFiles) {
                waitingDialog.appendReport("Importing identifications from " + idFile.getName() + ".");
                int searchEngine = readerFactory.getSearchEngine(idFile);
                IdfileReader fileReader = readerFactory.getFileReader(idFile);
                tempSet = fileReader.getAllSpectrumMatches();
                Iterator<SpectrumMatch> matchIt = tempSet.iterator();
                SpectrumMatch match;
                while (matchIt.hasNext()) {
                    match = matchIt.next();
                    nTotal++;
                    if (!idFilter.validate(match.getFirstHit(searchEngine))) {
                        matchIt.remove();
                    } else {
                        match.setBestAssumption(match.getFirstHit(searchEngine));
                        fdrCalculator.addHit(match.getBestAssumption().getEValue(), match.getFirstHit(searchEngine).isDecoy());
                        nRetained++;
                    }
                    if (waitingDialog.isRunCancelled()) {
                        waitingDialog.setRunCancelled();
                        return;
                    }
                }
                allSpectrumMatches.addAll(tempSet);
                if (waitingDialog.isRunCancelled()) {
                    waitingDialog.setRunCancelled();
                    return;
                }
            }
            if (nRetained == 0) {
                waitingDialog.appendReport("No identification retained.");
                return;
            }
            waitingDialog.appendReport("Identification file(s) import completed. " + nTotal + " identifications imported, " + nRetained + " identifications retained.");
            waitingDialog.appendReport("FDR estimation.");
            double eValueMax = fdrCalculator.getEvalueLimit(quantificationPreferences.getFdrThreshold());
            String fileName;
            Iterator<SpectrumMatch> matchIt = allSpectrumMatches.iterator();
            SpectrumMatch match;
            IdentificationDetails identificationDetails;
            String matchKey;
            while (matchIt.hasNext()) {
                match = matchIt.next();
                if (match.getBestAssumption().getEValue() > eValueMax) {
                    matchIt.remove();
                } else {
                    matchKey = match.getKey();
                    fileName = MSnSpectrum.getSpectrumFile(matchKey);
                    identifiedSpectra.add(matchKey);
                    if (!mgfFilesNeeded.contains(fileName)) {
                        mgfFilesNeeded.add(fileName);
                    }
                    identificationDetails = new IdentificationDetails();
                    identificationDetails.setValidated(true);
                    identification.addMatchParameter(matchKey, identificationDetails);
                }
            }
            int nTarget = fdrCalculator.getNTargetTotal();
            String report = "FDR processing completed at " + quantificationPreferences.getFdrThreshold() * 100 + "% FDR, " + nTarget + " PSMs retained.\n";
            waitingDialog.appendReport(report);
            waitingDialog.appendReport("Building protein objects.");
            identification.addSpectrumMatch(allSpectrumMatches);
            identification.buildPeptidesAndProteins();

            int nDecoy = 0, nPeptides = 0;
            nTarget = 0;
            IdentificationDetails peptideDetails;
            ProteinMatch proteinMatch;
            for (String proteinKey : identification.getProteinIdentification()) {
                identificationDetails = new IdentificationDetails();
                if (ProteinMatch.isDecoy(proteinKey)) {
                    nDecoy++;
                    identificationDetails.setValidated(false);
                } else {
                    nTarget++;
                    identificationDetails.setValidated(true);
                    proteinMatch = identification.getProteinMatch(proteinKey);
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        peptideDetails = new IdentificationDetails();
                        peptideDetails.setValidated(true);
                        identification.addMatchParameter(peptideKey, peptideDetails);
                        nPeptides++;
                    }
                }
                identification.addMatchParameter(proteinKey, identificationDetails);
            }
            double fdr = (100.0 * nDecoy) / nTarget;
            waitingDialog.appendReport(identification.getProteinIdentification().size() + " proteins imported (" + nPeptides + " peptides). Estimated protein FDR: " + fdr + "%.");


        } catch (Exception e) {
            e.printStackTrace();
            waitingDialog.appendReport("An error occured while loading the identification Files:");
            waitingDialog.appendReport(e.getLocalizedMessage());
            waitingDialog.setRunCancelled();
        }
    }

    /**
     * Loads identifications from a Peptide-Shaker (cps) file
     * @param idFiles   the identification file
     */
    public void loadIdentifications(File cpsFile) {
        loadModifications();
        PSParameter psParameter = new PSParameter();
        IdentificationDetails identificationDetails;
        String fileName;
        for (String proteinKey : identification.getProteinIdentification()) {
            identificationDetails = new IdentificationDetails();
            psParameter = (PSParameter) identification.getMatchParameter(proteinKey, psParameter);
            identificationDetails.setValidated(psParameter.isValidated());
            identification.addMatchParameter(proteinKey, identificationDetails);
        }
        for (String peptideKey : identification.getPeptideIdentification()) {
            identificationDetails = new IdentificationDetails();
            psParameter = (PSParameter) identification.getMatchParameter(peptideKey, psParameter);
            identificationDetails.setValidated(psParameter.isValidated());
            identification.addMatchParameter(peptideKey, identificationDetails);
        }
        for (String psmKey : identification.getSpectrumIdentification()) {
            identificationDetails = new IdentificationDetails();
            psParameter = (PSParameter) identification.getMatchParameter(psmKey, psParameter);
            identificationDetails.setValidated(psParameter.isValidated());
            identification.addMatchParameter(psmKey, identificationDetails);
            identifiedSpectra.add(psmKey);
            fileName = Spectrum.getSpectrumFile(psmKey);
            if (!mgfFilesNeeded.contains(fileName)) {
                mgfFilesNeeded.add(fileName);
            }
        }
    }

    /**
     * Verifies that identifications were loaded and that all needed spectrum files are provided
     * @param mgfFiles  The provided mgf files
     * @return          true if we have all needed mgf files
     */
    private boolean validateFiles(ArrayList<File> mgfFiles) {
        if (identifiedSpectra.isEmpty()) {
            waitingDialog.appendReport("No identification was retained, import will be cancelled.");
            return false;
        }
        for (String fileNeeded : mgfFilesNeeded) {
            boolean found = false;
            for (File file : mgfFiles) {
                if (file.getName().equals(fileNeeded)) {
                    found = true;
                }
            }
            if (!found) {
                waitingDialog.appendReport(fileNeeded + " could not be found, import will be cancelled.");
                return false;
            }
        }
        return true;
    }

    /**
     * Loads the modifications from the modification file
     */
    private void loadModifications() {
        try {
            ptmFactory.importModifications(new File(MODIFICATIONS_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            ptmFactory.importModifications(new File(USER_MODIFICATIONS_FILE));
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + USER_MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Method which returns the keys of the spectra potentially containing the quantitative information of a desired psm.
     *
     * @param spectrumKey the inspected psm key
     * @return the spectrum references of the corresponding spectra
     */
    private ArrayList<String> getSpectrumIds(String spectrumKey) throws Exception {
        ArrayList<String> result = new ArrayList<String>();
        if (quantificationPreferences.isSameSpectra()) {
            result.add(spectrumKey);
        } else {
            Precursor precursor, referencePrecursor = spectrumFactory.getPrecursor(spectrumKey);
            double rtTol = quantificationPreferences.getPrecursorRTTolerance();
            double mzTol = quantificationPreferences.getPrecursorMzTolerance();
            double mzRef = referencePrecursor.getMz();
            double rtRef = referencePrecursor.getRt();
            double errPpm;
            String newKey;
            for (String spectrumFile : spectrumFactory.getMgfFileNames()) {
                for (String spectrumTitle : spectrumFactory.getSpectrumTitles(spectrumFile)) {
                    newKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                    precursor = spectrumFactory.getPrecursor(newKey);
                    errPpm = Math.abs((mzRef - precursor.getMz()) / mzRef * 1000000);
                    if (errPpm < mzTol && Math.abs(precursor.getRt() - rtRef) < rtTol) {
                        result.add(newKey);
                    }
                }
            }
        }
        return result;
    }

    /**
     * Method which matches reporter ions in a spectrum
     *
     * @param reporterIons       The ions to look for
     * @param spectrum  The spectrum analyzed
     * @return the corresponding ion matches. If no peak was found the peak will be null.
     */
    private HashMap<ReporterIon, IonMatch> matchIons(String spectrumKey, ArrayList<ReporterIon> reporterIons) throws Exception {
        HashMap<Double, Peak> peakMap = spectrumFactory.getSpectrum(spectrumKey).getPeakMap();
        ArrayList<Double> mzArray = new ArrayList(peakMap.keySet());
        Collections.sort(mzArray);
        HashMap<ReporterIon, Peak> peaks = new HashMap<ReporterIon, Peak>();
        boolean over;
        double ionTolerance = quantificationPreferences.getReporterIonsMzTolerance();
        for (Double mz : mzArray) {
            over = true;
            for (ReporterIon ion : reporterIons) {
                if (mz > ion.theoreticMass - ionTolerance && mz < ion.theoreticMass + ionTolerance) {
                    if (peaks.get(ion) == null) {
                        peaks.put(ion, peakMap.get(mz));
                    } else if (Math.abs(mz - ion.theoreticMass) < peaks.get(ion).mz - ion.theoreticMass) {
                        peaks.put(ion, peakMap.get(mz));
                    }
                    over = false;
                } else if (mz <= ion.theoreticMass + ionTolerance) {
                    over = false;
                }
            }
            if (over) {
                break;
            }
        }
        HashMap<ReporterIon, IonMatch> result = new HashMap<ReporterIon, IonMatch>();
        for (ReporterIon ion : peaks.keySet()) {
            result.put(ion, new IonMatch(peaks.get(ion), ion, new Charge(Charge.PLUS, 1)));
        }
        return result;
    }

    /**
     * Imports spectra from various spectrum files.
     * 
     * @param waitingDialog Dialog displaying feedback to the user
     */
    public void importSpectra(ArrayList<File> spectrumFiles) {

        String fileName = "";

        waitingDialog.appendReport("Importing spectra.");

        for (File spectrumFile : spectrumFiles) {
            if (mgfFilesNeeded.contains(spectrumFile.getName())) {
                try {
                    fileName = spectrumFile.getName();
                    waitingDialog.appendReport("Importing " + fileName);
                    spectrumFactory.addSpectra(spectrumFile, null);
                } catch (Exception e) {
                    waitingDialog.appendReport("Spectrum files import failed when trying to import " + fileName + ".");
                    e.printStackTrace();
                }
            }
        }
        waitingDialog.appendReport("Spectra import completed.");
    }

    public void loadQuantification(ReporterIonQuantification quantification, ArrayList<File> mgfFiles) {
        try {
            waitingDialog.appendReport("Spectrum import.");
            if (!validateFiles(mgfFiles)) {
                waitingDialog.setRunCancelled();
                return;
            }
            importSpectra(mgfFiles);
            for (String matchKey : identifiedSpectra) {
                if (waitingDialog.isRunCancelled()) {
                    waitingDialog.setRunCancelled();
                    return;
                }
                for (String spectrumKey : getSpectrumIds(matchKey)) {
                    PsmQuantification spectrumQuantification = new PsmQuantification(spectrumKey, matchKey);
                    HashMap<ReporterIon, IonMatch> ionMatches = matchIons(spectrumKey, quantification.getReporterMethod().getReporterIons());
                    for (ReporterIon ion : ionMatches.keySet()) {
                        spectrumQuantification.addIonMatch(ion.getIndex(), ionMatches.get(ion));
                    }
                    quantification.addPsmQuantification(spectrumQuantification);
                    if (waitingDialog.isRunCancelled()) {
                        waitingDialog.setRunCancelled();
                        return;
                    }
                }
            }
            waitingDialog.appendReport("PSM quantification completed.");
        } catch (Exception e) {
            e.printStackTrace();
            waitingDialog.appendReport("An error occured while quantifying PSMs:");
            waitingDialog.appendReport(e.getLocalizedMessage());
            waitingDialog.setRunCancelled();
        }
    }
}
