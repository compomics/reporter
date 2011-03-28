package eu.isas.reporter.calculation;

import com.compomics.util.Util;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.io.massspectrometry.MgfReader;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PsmQuantification;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.reporter.gui.WaitingDialog;
import eu.isas.reporter.identifications.FdrCalculator;
import eu.isas.reporter.identifications.IdFilter;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

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
     * The number of decimals to display for the e-values in the report.
     */
    private Identification identification;
    private final int eValueDecimals = 6;
    private HashMap<String, ArrayList<String>> identifiedSpectra = new HashMap<String, ArrayList<String>>();

    public DataLoader(QuantificationPreferences quantificationPreferences, WaitingDialog waitingDialog, Identification identification) {
        this.identification = identification;
        this.quantificationPreferences = quantificationPreferences;
        this.waitingDialog = waitingDialog;
    }

    public void loadIdentifications(ArrayList<File> idFiles) {
        HashMap<Integer, HashSet<SpectrumMatch>> allSpectrumMatches = new HashMap<Integer, HashSet<SpectrumMatch>>();
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
                        fdrCalculator.addHit(searchEngine, match.getFirstHit(searchEngine).getEValue(), match.getFirstHit(searchEngine).isDecoy());
                        nRetained++;
                    }
                    if (waitingDialog.isRunCancelled()) {
                        waitingDialog.setRunCancelled();
                        return;
                    }
                }
                if (allSpectrumMatches.get(searchEngine) == null) {
                    allSpectrumMatches.put(searchEngine, tempSet);
                } else {
                    allSpectrumMatches.get(searchEngine).addAll(tempSet);
                }
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
            HashMap<Integer, Double> thresholds = fdrCalculator.getEvalueLimits(quantificationPreferences.getFdrThreshold());
            double eValueMax;
            String fileName;
            for (int searchEngine : allSpectrumMatches.keySet()) {
                eValueMax = thresholds.get(searchEngine);
                Iterator<SpectrumMatch> matchIt = allSpectrumMatches.get(searchEngine).iterator();
                SpectrumMatch match;
                while (matchIt.hasNext()) {
                    match = matchIt.next();
                    if (match.getFirstHit(searchEngine).getEValue() > eValueMax
                            || match.getFirstHit(searchEngine).isDecoy()) {
                        matchIt.remove();
                    } else {
                        fileName = MSnSpectrum.getSpectrumFile(match.getKey());
                        if (!identifiedSpectra.containsKey(fileName)) {
                            identifiedSpectra.put(fileName, new ArrayList<String>());
                        }
                        if (!identifiedSpectra.get(fileName).contains(match.getKey())) {
                            identifiedSpectra.get(fileName).add(match.getKey());
                        }
                    }
                }
            }
            String report = "FDR processing completed:\n\n\tEngine\te-value\t#hits at " + quantificationPreferences.getFdrThreshold() * 100 + "% FDR\n";
            if (allSpectrumMatches.get(SearchEngine.MASCOT) == null) {
                report += "\tMascot\t\tno hits\n";
            } else {
                report += "\tMascot\t" + Util.roundDouble(thresholds.get(SearchEngine.MASCOT), eValueDecimals)
                        + "\t" + allSpectrumMatches.get(SearchEngine.MASCOT).size() + "\n";
            }
            if (allSpectrumMatches.get(SearchEngine.OMSSA) == null) {
                report += "\tOMSSA\t\tno hits\n";
            } else {
                report += "\tOMSSA\t" + Util.roundDouble(thresholds.get(SearchEngine.OMSSA), eValueDecimals)
                        + "\t" + allSpectrumMatches.get(SearchEngine.OMSSA).size() + "\n";
            }
            if (allSpectrumMatches.get(SearchEngine.XTANDEM) == null) {
                report += "\tX!Tandem\t\tno hits\n";
            } else {
                report += "\tX!Tandem\t" + Util.roundDouble(thresholds.get(SearchEngine.XTANDEM), eValueDecimals)
                        + "\t" + allSpectrumMatches.get(SearchEngine.XTANDEM).size() + "\n";
            }
            waitingDialog.appendReport(report);
            waitingDialog.appendReport("Building protein objects.");

            for (int searchEngine : allSpectrumMatches.keySet()) {
                identification.addSpectrumMatch(allSpectrumMatches.get(searchEngine));
            }
            waitingDialog.appendReport(identification.getProteinIdentification().size() + " proteins imported.");
        } catch (Exception e) {
            e.printStackTrace();
            waitingDialog.appendReport("An error occured while loading the identification Files:");
            waitingDialog.appendReport(e.getLocalizedMessage());
            waitingDialog.setRunCancelled();
        }
    }

    private boolean validateFiles(ArrayList<File> mgfFiles) {
        if (identifiedSpectra.isEmpty()) {
            waitingDialog.appendReport("No identification was retained, import will be cancelled.");
            return false;
        }
        for (String fileNeeded : identifiedSpectra.keySet()) {
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
     * Method which returns the keys of the spectra potentially containing the quantitative information of a desired psm.
     *
     * @param spectrumMatch the inspected psm
     * @return the spectrum references of the corresponding spectra
     */
    private ArrayList<String> getSpectrumIds(SpectrumMatch spectrumMatch, HashMap<String, MSnSpectrum> spectrumMap) {
        String spectrumKey = spectrumMatch.getKey();
        ArrayList<String> result = new ArrayList<String>();
        if (quantificationPreferences.isSameSpectra()) {
            result.add(spectrumKey);
        } else {
            MSnSpectrum spectrum;
            String fileName = MSnSpectrum.getSpectrumFile(spectrumKey);
            double rtTol = quantificationPreferences.getPrecursorRTTolerance();
            double mzTol = quantificationPreferences.getPrecursorMzTolerance();
            double mzRef = spectrumMap.get(spectrumKey).getPrecursor().getMz();
            double rtRef = spectrumMap.get(spectrumKey).getPrecursor().getRt();
            double errPpm;
            for (String spectrumId : spectrumMap.keySet()) {
                spectrum = spectrumMap.get(spectrumId);
                if (fileName.equals(spectrum.getFileName())) {
                    errPpm = Math.abs((mzRef - spectrum.getPrecursor().getMz()) / mzRef * 1000000);
                    if (errPpm < mzTol && Math.abs(spectrum.getPrecursor().getRt() - rtRef) < rtTol) {
                        result.add(spectrumId);
                    }
                }
            }
        }
        return result;
    }

    private HashMap<String, MSnSpectrum> importSpectra(File mgfFile) {
        MgfReader reader = new MgfReader();
        HashMap<String, MSnSpectrum> result = new HashMap<String, MSnSpectrum>();
        waitingDialog.appendReport("Importing spectra from " + mgfFile.getName());
        try {
            ArrayList<MSnSpectrum> spectraTemp;
            spectraTemp = reader.getSpectra(mgfFile);
            for (MSnSpectrum spectrum : spectraTemp) {
                result.put(spectrum.getSpectrumKey(), spectrum);
            }
        } catch (Exception e) {
            e.printStackTrace();
            waitingDialog.appendReport("An error occured while loading the mgf files:");
            e.printStackTrace();
            waitingDialog.setRunCancelled();
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
    private HashMap<ReporterIon, IonMatch> matchIons(MSnSpectrum spectrum, ArrayList<ReporterIon> reporterIons) {
        HashMap<Double, Peak> peakMap = spectrum.getPeakMap();
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

    public void loadQuantification(ReporterIonQuantification quantification, ArrayList<File> mgfFiles) {
        HashMap<String, File> files = new HashMap<String, File>();
        for (File file : mgfFiles) {
            files.put(file.getName(), file);
        }
        try {
            waitingDialog.appendReport("Spectrum import.");
            if (!validateFiles(mgfFiles)) {
                waitingDialog.setRunCancelled();
                return;
            }
            PSParameter psParameter = new PSParameter();
            for (String fileName : identifiedSpectra.keySet()) {
                HashMap<String, MSnSpectrum> spectrumMap = importSpectra(files.get(fileName));
                if (waitingDialog.isRunCancelled()) {
                    waitingDialog.setRunCancelled();
                    return;
                }
                for (String matchKey : identifiedSpectra.get(fileName)) {
                    SpectrumMatch spectrumMatch = identification.getSpectrumIdentification().get(matchKey);
                    ArrayList<String> spectrumIds = getSpectrumIds(spectrumMatch, spectrumMap);
                    for (String spectrumId : spectrumIds) {
                        MSnSpectrum currentSpectrum = spectrumMap.get(spectrumId);
                        PsmQuantification spectrumQuantification = new PsmQuantification(currentSpectrum);
                        HashMap<ReporterIon, IonMatch> ionMatches = matchIons(currentSpectrum, quantification.getReporterMethod().getReporterIons());
                        for (ReporterIon ion : ionMatches.keySet()) {
                            spectrumQuantification.addIonMatch(ion.getIndex(), ionMatches.get(ion));
                        }
                        boolean reporterFound = false;
                        if (spectrumQuantification.getReporterMatches() != null) {
                            HashMap<Integer, IonMatch> reporterMatches = spectrumQuantification.getReporterMatches();
                            for (int ion : reporterMatches.keySet()) {
                                if (reporterMatches.get(ion).peak != null) {
                                    reporterFound = true;
                                    break;
                                }
                            }
                        }
                        if (reporterFound) {
                            for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
                                String proteinKey = ProteinMatch.getProteinMatchKey(assumption.getPeptide());
                                if (!quantification.getProteinQuantification().containsKey(proteinKey)) {
                                    ProteinQuantification proteinQuantification;
                                    proteinQuantification = new ProteinQuantification(identification.getProteinIdentification().get(proteinKey));
                                    proteinQuantification.addUrParam(new IgnoredRatios());
                                    quantification.addProteinQuantification(proteinQuantification);
                                }
                                ProteinQuantification proteinQuantification = quantification.getProteinQuantification(proteinKey);
                                String peptideKey = assumption.getPeptide().getKey();
                                if (!proteinQuantification.getPeptideQuantification().containsKey(peptideKey)) {
                                    PeptideQuantification peptideQuantification = new PeptideQuantification(identification.getPeptideIdentification().get(peptideKey));
                                    peptideQuantification.addUrParam(new IgnoredRatios());
                                    proteinQuantification.addPeptideQuantification(peptideQuantification);
                                }
                                quantification.getProteinQuantification(proteinKey).getPeptideQuantification(peptideKey).addPsmQuantification(spectrumQuantification);
                            }
                        }
                        if (waitingDialog.isRunCancelled()) {
                            waitingDialog.setRunCancelled();
                            return;
                        }
                    }

                }
            }
            waitingDialog.appendReport("Spectrum import completed.");
        } catch (Exception e) {
            e.printStackTrace();
            waitingDialog.appendReport("An error occured while calculating ratios:");
            waitingDialog.appendReport(e.getLocalizedMessage());
            waitingDialog.setRunCancelled();
        }
    }
}
