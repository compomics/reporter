package eu.isas.reporter.calculation;

import com.compomics.util.Util;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.io.massspectrometry.MgfReader;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.quantification.Ratio;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.SpectrumQuantification;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.myparameters.IgnoredRatios;
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

    /**
     * The reporter class
     */
    private Reporter parent;

    /**
     * The dialog which will display feedback to the user
     */
    private WaitingDialog waitingDialog;

    /**
     * The experiment conducted
     */
    private MsExperiment experiment;
    /**
     * The sample analyzed
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;

    /**
     * The identification files to process
     */
    private ArrayList<File> idFiles;
    /**
     * The corresponding mgf files
     */
    private ArrayList<File> mgfFiles;
    /**
     * The identification filter to use
     */
    private IdFilter idFilter;
    /**
     * The FDR threshold
     */
    private double fdrLimit;
    /**
     * The tolerance for reporter ion matching
     */
    private double ionTolerance;
    /**
     * Map of all spectra
     */
    private HashMap<String, MSnSpectrum> spectra = new HashMap<String, MSnSpectrum>();

    /**
     * The worker which will compile all ratios
     */
    private RatiosCompilator ratiosCompilator = new RatiosCompilator();
    /**
     * The worker which will process identifications
     */
    private IdentificationProcessor identificationProcessor = new IdentificationProcessor();
    /**
     * The worker which will process the mgf files
     */
    private MgfProcessor mgfProcessor = new MgfProcessor();

    /**
     * The resulting quantification
     */
    private ReporterIonQuantification quantification;
    /**
     * The identification-quantification linker
     */
    private IdentificationQuantificationLinker linker;
    /**
     * The number of decimals to display for the e-values in the report.
     */
    private final int eValueDecimals = 6;

    /**
     * Constructor
     * @param parent            The reporter class
     * @param experiment        The experiment conducted
     * @param sample            The sample analyzed
     * @param replicateNum      The replicate number
     * @param idFiles           The identification files to process
     * @param mgfFiles          The mgf files to process
     * @param idFilter          The identification filter
     * @param fdrLimit          The FDR threshold to be used
     * @param quantification    The quantification to work on
     * @param ionTolerance      The tolerance for reporter ion matching
     * @param linker            The identification-quantification linker
     */
    public ItraqCalculator(Reporter parent, MsExperiment experiment, Sample sample, int replicateNum, ArrayList<File> idFiles, ArrayList<File> mgfFiles,
            IdFilter idFilter, double fdrLimit, ReporterIonQuantification quantification, double ionTolerance, IdentificationQuantificationLinker linker) {
        this.idFiles = idFiles;
        this.mgfFiles = mgfFiles;
        this.experiment = experiment;
        this.sample = sample;
        this.replicateNumber = replicateNum;
        this.parent = parent;
        this.idFilter = idFilter;
        this.fdrLimit = fdrLimit;
        this.quantification = quantification;
        this.ionTolerance = ionTolerance;
        this.linker = linker;
    }

    /**
     * Returns the experiment conducted.
     *
     * @return the experiment conducted
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * Method starting the various workers
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
     * Method called for result display
     */
    public void displayResults() {
        parent.displayResults(quantification, experiment);
    }

    /**
     * Method called to restart reporter
     */
    public void restart() {
        parent.restart();
    }

    /**
     * Method used to process a spectrum
     *
     * @param spectrumQuantification the quantification at the spectrum level
     */
    private void processSpectrum(SpectrumQuantification spectrumQuantification) {
        for (ReporterIon reporterIon : quantification.getReporterMethod().getReporterIons()) {
            spectrumQuantification.addIonMatch(reporterIon.getIndex(), matchIon(reporterIon, spectrumQuantification.getSpectrum()));
        }
        estimateRatios(spectrumQuantification);
    }

    /**
     * Method which estimates ratios for a spectrum
     *
     * @param spectrumQuantification the current spectrum quantification
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
     * Method which matches an ion to a peak
     *
     * @param ion       The ion to look for
     * @param spectrum  The spectrum analyzed
     * @return the corresponding ion match. If no peak was found the peak will be null.
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
     * Worker which compiles psm ratios into peptide ratios and protein ratios
     */
    private class RatiosCompilator extends SwingWorker {

        @Override
        protected Object doInBackground() throws Exception {
            queue();
            try {
                waitingDialog.appendReport("Spectrum ratio computation.");
                ArrayList<ProteinMatch> proteins = new ArrayList(experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION).getProteinIdentification().values());
                if (proteins.isEmpty()) {
                    waitingDialog.appendReport("No proteins imported.");
                    waitingDialog.setRunCancelled();
                    return 1;
                }
                PSParameter psParameter = new PSParameter();
                for (ProteinMatch proteinMatch : proteins) {
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
                                try {
                                    psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);
                                    if (!psParameter.isValidated()) {
                                        IgnoredRatios ignoredRatios = new IgnoredRatios();
                                        ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
                                        for (ReporterIon ion : quantification.getReporterMethod().getReporterIons()) {
                                            ignoredRatios.ignore(ion.getIndex());
                                        }
                                    }
                                } catch (Exception e) {
                                    // the identifications were not processed by peptide shaker
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
                e.printStackTrace();
                waitingDialog.appendReport("An error occured while calculating ratios:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
            return 0;
        }

        /**
         * Method which returns the keys of the spectra potentially containing the quantitative information of a desired psm.
         *
         * @param spectrumMatch the inspectred psm
         * @return the spectrum references of the corresponding spectra
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
         * Method used to synchronize the workers. The ratio compilation will only start once the identification worker and the spectrum worker are done.
         */
        private synchronized void queue() {
            try {
                if (!identificationProcessor.isFinished() || !mgfProcessor.isfinished()) {
                    wait();
                }
            } catch (Exception e) {
                e.printStackTrace();
                waitingDialog.appendReport("An error occured when waiting for file processing.");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
        }

        /**
         * Method used to restart the worker once the others are ready
         */
        public synchronized void restart() {
            try {
                if (identificationProcessor.isFinished() && mgfProcessor.isfinished()) {
                    notify();
                }
            } catch (Exception e) {
                e.printStackTrace();
                waitingDialog.appendReport("An error occured when waiting for file processing.");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
        }
    }

    /**
     * Worker which processed the identifications
     */
    private class IdentificationProcessor extends SwingWorker {

        /**
         * Map containing all psms from the various search engines indexed by their compomics utilities index.
         */
        private HashMap<Integer, HashSet<SpectrumMatch>> allSpectrumMatches = new HashMap<Integer, HashSet<SpectrumMatch>>();
        /**
         * Boolean indicating whether the worker is done or not
         */
        private boolean finished = false;
        /**
         * The compomics utilities identification file reader factory
         */
        private IdfileReaderFactory readerFactory = IdfileReaderFactory.getInstance();
        /**
         * The compomics utilities post-translational modifications factory
         */
        private PTMFactory ptmFactory = PTMFactory.getInstance();
        /**
         * The FDR estimator
         */
        private FdrCalculator fdrCalculator = new FdrCalculator();

        @Override
        protected Object doInBackground() throws Exception {
            int nTotal = 0;
            int nRetained = 0;
            try {
                if (idFiles.size() > 1 || !idFiles.get(0).getName().endsWith(".cps")) {
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

                    Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
                    for (int searchEngine : allSpectrumMatches.keySet()) {
                        identification.addSpectrumMatch(allSpectrumMatches.get(searchEngine));
                    }
                    waitingDialog.appendReport(identification.getProteinIdentification().size() + " proteins imported.");
                } else {
                    waitingDialog.appendReport("Identifications imported from Compomics experiment " + experiment.getReference() + " semple " + sample.getReference() + " replicate " + replicateNumber + ".");
                }
                finished = true;
                ratiosCompilator.restart();
            } catch (Exception e) {
                e.printStackTrace();
                waitingDialog.appendReport("An error occured while loading the identification Files:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
            return 0;
        }

        /**
         * Method indicating whether the worker is done or not
         * @return a boolean indicating whether the worker is done or not
         */
        public boolean isFinished() {
            return finished;
        }
    }

    /**
     * worker used to process an mgf file
     */
    private class MgfProcessor extends SwingWorker {

        /**
         * Boolean indicating whether the worker is done
         */
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
                e.printStackTrace();
                waitingDialog.appendReport("An error occured while loading the mgf files:");
                waitingDialog.appendReport(e.getLocalizedMessage());
                waitingDialog.setRunCancelled();
            }
            return 0;
        }
         /**
         * Method indicating whether the worker is done or not
         * @return a boolean indicating whether the worker is done or not
         */
        public boolean isfinished() {
            return finished;
        }
    }
}
