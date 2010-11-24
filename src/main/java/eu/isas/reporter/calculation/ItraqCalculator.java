package eu.isas.reporter.calculation;

import com.compomics.util.Util;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.IdfileReader;
import com.compomics.util.experiment.identification.IdfileReaderFactory;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.filereaders.MgfReader;
import com.compomics.util.experiment.quantification.Ratio;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.SpectrumQuantification;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.compomicsutilitiessettings.IgnoredRatios;
import eu.isas.reporter.gui.WaitingDialog;
import eu.isas.reporter.identifications.FdrCalculator;
import eu.isas.reporter.identifications.IdFilter;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.SwingWorker;

/**
 * This class will compute the iTRAQ ratios.
 * 
 * @author Marc Vaudel
 */
public class ItraqCalculator {

    private Reporter parent;
    private WaitingDialog waitingDialog;
    private MsExperiment experiment;
    private ArrayList<File> idFiles;
    private ArrayList<File> mgfFiles;
    private IdFilter idFilter;
    private double fdrLimit;
    private double ionTolerance;
    private HashMap<String, MSnSpectrum> spectra = new HashMap<String, MSnSpectrum>();
    private HashMap<String, ProteinMatch> proteins = new HashMap<String, ProteinMatch>();
    private RatiosCompilator ratiosCompilator = new RatiosCompilator();
    private IdentificationProcessor identificationProcessor = new IdentificationProcessor();
    private MgfProcessor mgfProcessor = new MgfProcessor();
    private ReporterIonQuantification quantification;
    private IdentificationQuantificationLinker linker;
    /**
     * The number of decimals to display for the e-values in the report.
     */
    private int eValueDecimals = 6;

    /**
     * @TODO: JavaDoc missing
     *
     * @param parent
     * @param experiment
     * @param idFiles
     * @param mgfFiles
     * @param idFilter
     * @param fdrLimit
     * @param quantification
     * @param ionTolerance
     * @param linker
     */
    public ItraqCalculator(Reporter parent, MsExperiment experiment, ArrayList<File> idFiles, ArrayList<File> mgfFiles,
            IdFilter idFilter, double fdrLimit, ReporterIonQuantification quantification, double ionTolerance, IdentificationQuantificationLinker linker) {
        this.idFiles = idFiles;
        this.mgfFiles = mgfFiles;
        this.experiment = experiment;
        this.parent = parent;
        this.idFilter = idFilter;
        this.fdrLimit = fdrLimit;
        this.quantification = quantification;
        this.ionTolerance = ionTolerance;
        this.linker = linker;
    }

    /**
     * Returns the experimet object.
     *
     * @return the experimet object
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * @TODO: JavaDoc missing
     */
    public void computeRatios() {
        waitingDialog = new WaitingDialog(parent.getMainFrame(), true, this);

        identificationProcessor.execute();
        waitingDialog.appendReport("Importing identifications.");
        mgfProcessor.execute();
        waitingDialog.appendReport("Importing spectra.");
        ratiosCompilator.execute();
        waitingDialog.setLocationRelativeTo(parent.getMainFrame());
        waitingDialog.setVisible(true);
    }

    /**
     * @TODO: JavaDoc missing
     */
    public void displayResults() {
        parent.displayResults(quantification, experiment);
    }

    /**
     * @TODO: JavaDoc missing
     */
    public void restart() {
        parent.restart();
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param spectrumQuantification
     */
    private void processSpectrum(SpectrumQuantification spectrumQuantification) {
        for (ReporterIon reporterIon : quantification.getReporterMethod().getReporterIons()) {
            spectrumQuantification.addIonMatch(reporterIon.getIndex(), matchIon(reporterIon, spectrumQuantification.getSpectrum()));
        }
        estimateRatios(spectrumQuantification);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param spectrumQuantification
     */
    private void estimateRatios(SpectrumQuantification spectrumQuantification) {
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        Deisotoper deisotoper = new Deisotoper(quantification.getReporterMethod());
        int referenceLabel = quantification.getReferenceLabel();
        HashMap<Integer, IonMatch> reporterMatches = spectrumQuantification.getReporterMatches();
        for (int ion : reporterMatches.keySet()) {
            if (reporterMatches.get(ion).peak == null) {
                ignoredRatios.ignore(ion);
            }
        }
        HashMap<Integer, Double> deisotopedInt = deisotoper.deisotope(reporterMatches);
        Double referenceInt = deisotopedInt.get(referenceLabel);
        if (referenceInt > 0) {
            for (int label : deisotopedInt.keySet()) {
                spectrumQuantification.addRatio(label, new Ratio(referenceLabel, label, deisotopedInt.get(label) / referenceInt));
                if (deisotopedInt.get(label) <= 0) {
                    for (int labeltemp : deisotopedInt.keySet()) {
                        ignoredRatios.ignore(labeltemp);
                    }
                }
            }
        } else {
            for (int label : deisotopedInt.keySet()) {
                if (label != quantification.getReferenceLabel()) {
                    spectrumQuantification.addRatio(label, new Ratio(referenceLabel, label, 9 * Math.pow(10, 99)));
                } else {
                    spectrumQuantification.addRatio(label, new Ratio(referenceLabel, label, 0));
                }
                ignoredRatios.ignore(label);
            }
        }
        spectrumQuantification.addUrParam(ignoredRatios);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param ion
     * @param spectrum
     * @return
     */
    private IonMatch matchIon(ReporterIon ion, MSnSpectrum spectrum) {
        HashMap<Double, Peak> peakMap = spectrum.getPeakMap();
        ArrayList<Double> mzArray = new ArrayList(peakMap.keySet());
        Collections.sort(mzArray);
        Peak bestPeak = null;
        double bestMz = 0;
        for (Double mz : mzArray) {
            if (mz > ion.theoreticMass - ionTolerance && mz < ion.theoreticMass + ionTolerance) {
                if (bestPeak == null) {
                    bestPeak = peakMap.get(mz);
                    bestMz = mz;
                } else if (Math.abs(mz - ion.theoreticMass) < Math.abs(bestMz - ion.theoreticMass)) {
                    bestPeak = peakMap.get(mz);
                    bestMz = mz;
                }
            } else if (mz > ion.theoreticMass + ionTolerance) {
                break;
            }
        }
        return new IonMatch(bestPeak, ion);
    }

    /**
     * @TODO: JavaDoc missing
     */
    private class RatiosCompilator extends SwingWorker {

        @Override
        protected Object doInBackground() throws Exception {
            queue();
            try {
                waitingDialog.appendReport("Spectrum ratio computation.");
                if (proteins.isEmpty()) {
                    waitingDialog.appendReport("No proteins imported.");
                    waitingDialog.setRunCancelled();
                    return 1;
                }
                for (ProteinMatch proteinMatch : proteins.values()) {
                    ArrayList<PeptideQuantification> peptideQuantifications = new ArrayList<PeptideQuantification>();
                    for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
                        ArrayList<SpectrumQuantification> spectrumQuantifications = new ArrayList<SpectrumQuantification>();
                        for (SpectrumMatch spectrumMatch : peptideMatch.getSpectrumMatches().values()) {
                            ArrayList<String> spectrumIds = getSpectrumIds(spectrumMatch);
                            for (String spectrumId : spectrumIds) {
                                MSnSpectrum currentSpectrum = spectra.get(spectrumId);
                                if (currentSpectrum == null) {
                                    String fileName = spectrumMatch.getSpectrum().getFileName();
                                    boolean fileFound = false;
                                    for (File mgfFile : mgfFiles) {
                                        if (mgfFile.getName().equals(fileName)) {
                                            fileFound = true;
                                        }
                                    }
                                    if (!fileFound) {
                                        waitingDialog.appendReport(fileName + " not found.");
                                    }
                                    if (fileFound) {
                                        waitingDialog.appendReport("Spectrum " + spectrumMatch.getSpectrum().getSpectrumTitle() + " not found in " + fileName + ".");
                                    }
                                }
                                SpectrumQuantification spectrumQuantification = new SpectrumQuantification(currentSpectrum);
                                processSpectrum(spectrumQuantification);
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
                                    spectrumQuantifications.add(spectrumQuantification);
                                }
                                if (waitingDialog.isRunCancelled()) {
                                    waitingDialog.setRunCancelled();
                                    return 1;
                                }
                            }
                        }
                        PeptideQuantification peptideQuantification = new PeptideQuantification(peptideMatch, spectrumQuantifications);
                        peptideQuantification.addUrParam(new IgnoredRatios());
                        peptideQuantifications.add(peptideQuantification);
                    }
                    ProteinQuantification proteinQuantification = new ProteinQuantification(proteinMatch, peptideQuantifications);
                    proteinQuantification.addUrParam(new IgnoredRatios());
                    quantification.addProteinQuantification(proteinQuantification);
                }
                waitingDialog.appendReport("Ratio computation completed.");
                waitingDialog.setRunFinished();
            } catch (Exception e) {
                waitingDialog.appendReport("An error occured while calculating ratios:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
            return 0;
        }

        /**
         * @TODO: JavaDoc missing
         *
         * @param spectrumMatch
         * @return
         */
        private ArrayList<String> getSpectrumIds(SpectrumMatch spectrumMatch) {
            String spectrumKey = spectrumMatch.getSpectrum().getFileName() + "_" + spectrumMatch.getSpectrum().getSpectrumTitle();
            ArrayList<String> result = new ArrayList<String>();
            if (linker.getIndex() == IdentificationQuantificationLinker.SPECTRUM_TITLE) {
                result.add(spectrumKey);
            } else {
                MSnSpectrum spectrum;
                String fileName = spectrumMatch.getSpectrum().getFileName();
                double rtTol = linker.getRtTolerance();
                double mzTol = linker.getMzTolerance();
                double mzRef = spectra.get(spectrumKey).getPrecursor().getMz();
                double rtRef = spectra.get(spectrumKey).getPrecursor().getRt();
                double errPpm;
                for (String spectrumId : spectra.keySet()) {
                    spectrum = spectra.get(spectrumId);
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

        /**
         * @TODO: JavaDoc missing
         */
        private synchronized void queue() {
            try {
                if (!identificationProcessor.isFinished() || !mgfProcessor.isfinished()) {
                    wait();
                }
            } catch (Exception e) {
                waitingDialog.appendReport("An error occured when waiting for file processing.");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
        }

        /**
         * @TODO: JavaDoc missing
         */
        public synchronized void restart() {
            try {
                if (identificationProcessor.isFinished() && mgfProcessor.isfinished()) {
                    notify();
                }
            } catch (Exception e) {
                waitingDialog.appendReport("An error occured when waiting for file processing.");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
        }
    }

    /**
     * @TODO: JavaDoc missing
     */
    private class IdentificationProcessor extends SwingWorker {

        private HashMap<Integer, HashSet<SpectrumMatch>> allSpectrumMatches = new HashMap<Integer, HashSet<SpectrumMatch>>();
        private boolean finished = false;
        private IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();
        private PTMFactory ptmFactory = PTMFactory.getInstance();
        private FdrCalculator fdrCalculator = new FdrCalculator();

        @Override
        protected Object doInBackground() throws Exception {
            int nTotal = 0;
            int nRetained = 0;
            try {
                HashSet<SpectrumMatch> tempSet;
                for (File idFile : idFiles) {
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
                            return 1;
                        }
                    }
                    if (allSpectrumMatches.get(searchEngine) == null) {
                        allSpectrumMatches.put(searchEngine, tempSet);
                    } else {
                        allSpectrumMatches.get(searchEngine).addAll(tempSet);
                    }
                    if (waitingDialog.isRunCancelled()) {
                        waitingDialog.setRunCancelled();
                        return 1;
                    }
                }
                if (nRetained == 0) {
                    waitingDialog.appendReport("No identification retained.");
                    return 1;
                }
                waitingDialog.appendReport("Identification file(s) import completed. " + nTotal + " identifications imported, " + nRetained + " identifications retained.");
                waitingDialog.appendReport("FDR estimation.");
                HashMap<Integer, Double> thresholds = fdrCalculator.getEvalueLimits(fdrLimit);
                double eValueMax;
                for (int searchEngine : allSpectrumMatches.keySet()) {
                    eValueMax = thresholds.get(searchEngine);
                    Iterator<SpectrumMatch> matchIt = allSpectrumMatches.get(searchEngine).iterator();
                    SpectrumMatch match;
                    while (matchIt.hasNext()) {
                        match = matchIt.next();
                        if (match.getFirstHit(searchEngine).getEValue() > eValueMax
                                || match.getFirstHit(searchEngine).isDecoy()) {
                            matchIt.remove();
                        }
                    }
                }
                String report = "FDR processing completed:\n\n\tEngine\te-value\t#hits at " + fdrLimit + "% FDR\n";
                if (allSpectrumMatches.get(SearchEngine.MASCOT) == null) {
                    report += "\tMascot\t\t\tno hits\n";
                } else {
                    report += "\tMascot\t" + Util.roundDouble(thresholds.get(SearchEngine.MASCOT), eValueDecimals)
                            + "\t" + allSpectrumMatches.get(SearchEngine.MASCOT).size() + "\n";
                }
                if (allSpectrumMatches.get(SearchEngine.OMSSA) == null) {
                    report += "\tOMSSA\t\t\tno hits\n";
                } else {
                    report += "\tOMSSA\t" + Util.roundDouble(thresholds.get(SearchEngine.OMSSA), eValueDecimals)
                            + "\t" + allSpectrumMatches.get(SearchEngine.OMSSA).size() + "\n";
                }
                if (allSpectrumMatches.get(SearchEngine.XTANDEM) == null) {
                    report += "\tX!Tandem\t\t\tno hits\n";
                } else {
                    report += "\tX!Tandem\t" + Util.roundDouble(thresholds.get(SearchEngine.XTANDEM), eValueDecimals)
                            + "\t" + allSpectrumMatches.get(SearchEngine.XTANDEM).size() + "\n";
                }
                waitingDialog.appendReport(report);
                waitingDialog.appendReport("Building protein objects.");

                for (int searchEngine : allSpectrumMatches.keySet()) {
                    for (SpectrumMatch match : allSpectrumMatches.get(searchEngine)) {
                        Peptide peptide = match.getFirstHit(searchEngine).getPeptide();
                        Protein protein = peptide.getParentProteins().get(0);
                        PeptideMatch peptideMatch = new PeptideMatch(peptide, match);
                        if (proteins.get(protein.getAccession()) == null) {
                            proteins.put(protein.getAccession(), new ProteinMatch(protein, peptideMatch));
                        } else {
                            proteins.get(protein.getAccession()).addPeptideMatch(peptideMatch);
                        }
                    }
                }
                waitingDialog.appendReport(proteins.size() + " proteins imported.");

                finished = true;
                ratiosCompilator.restart();
            } catch (Exception e) {
                waitingDialog.appendReport("An error occured while loading the identification Files:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
            return 0;
        }

        public boolean isFinished() {
            return finished;
        }
    }

    /**
     * @TODO: JavaDoc missing
     */
    private class MgfProcessor extends SwingWorker {

        private boolean finished = false;

        @Override
        protected Object doInBackground() throws Exception {
            MgfReader reader = new MgfReader();
            try {
                ArrayList<MSnSpectrum> spectraTemp;
                for (File mgfFile : mgfFiles) {
                    spectraTemp = reader.getSpectra(mgfFile);
                    for (MSnSpectrum spectrum : spectraTemp) {
                        spectra.put(spectrum.getFileName() + "_" + spectrum.getSpectrumTitle(), spectrum);
                    }
                    if (waitingDialog.isRunCancelled()) {
                        waitingDialog.setRunCancelled();
                        return 1;
                    }
                }
                waitingDialog.appendReport("Spectra file(s) import completed. " + spectra.size() + " spectra imported.");
                finished = true;
                ratiosCompilator.restart();
            } catch (Exception e) {
                waitingDialog.appendReport("An error occured while loading the mgf Files:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
            return 0;
        }

        public boolean isfinished() {
            return finished;
        }
    }
}
