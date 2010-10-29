package eu.isas.reporter.calculation;

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
import eu.isas.reporter.compomicsutilitiessettings.CompomicsKeysFactory;
import eu.isas.reporter.compomicsutilitiessettings.IgnoredRatios;
import eu.isas.reporter.gui.WaitingPanel;
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
 * @author Marc
 */
public class ItraqCalculator {

    private String MODIFICATION_FILE = "conf/mods.xml";
    private Reporter parent;
    private WaitingPanel waitingPanel;
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
    private CompomicsKeysFactory compomicsKeyFactory = CompomicsKeysFactory.getInstance();
    private ReporterIonQuantification quantification;

    public ItraqCalculator(Reporter parent, MsExperiment experiment, ArrayList<File> idFiles, ArrayList<File> mgfFiles, IdFilter idFilter, double fdrLimit, ReporterIonQuantification quantification, double ionTolerance) {
        this.idFiles = idFiles;
        this.mgfFiles = mgfFiles;
        this.experiment = experiment;
        this.parent = parent;
        this.idFilter = idFilter;
        this.fdrLimit = fdrLimit;
        this.quantification = quantification;
        this.ionTolerance = ionTolerance;
    }

    public void computeRatios() {
        waitingPanel = new WaitingPanel(experiment.getReference(), this);

        identificationProcessor.execute();
        waitingPanel.appendReport("Importing identifications.");
        mgfProcessor.execute();
        waitingPanel.appendReport("Importing spectra.");
        ratiosCompilator.execute();

        waitingPanel.setVisible(true);
    }

    public void displayResults() {
        parent.displayResults(quantification, experiment);
    }

    public void restart() {
        parent.restart();
    }

    private void processSpectrum(SpectrumQuantification spectrumQuantification) {
        for (ReporterIon reporterIon : quantification.getMethod().getReporterIons()) {
            spectrumQuantification.addIonMatch(reporterIon.getIndex(), matchIon(reporterIon, spectrumQuantification.getSpectrum()));
        }
        estimateRatios(spectrumQuantification);
    }

    private void estimateRatios(SpectrumQuantification spectrumQuantification) {
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        Deisotoper deisotoper = new Deisotoper(quantification.getMethod());
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
        spectrumQuantification.addUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS), ignoredRatios);
    }

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

    private class RatiosCompilator extends SwingWorker {

        @Override
        protected Object doInBackground() throws Exception {
            queue();
            try {
                waitingPanel.appendReport("Spectrum ratio computation.");
                if (proteins.isEmpty()) {
                    waitingPanel.appendReport("No proteins imported.");
                    waitingPanel.setRunCancelled();
                    return 1;
                }
                for (ProteinMatch proteinMatch : proteins.values()) {
                    ArrayList<PeptideQuantification> peptideQuantifications = new ArrayList<PeptideQuantification>();
                    for (PeptideMatch peptideMatch : proteinMatch.getPeptideMatches().values()) {
                        ArrayList<SpectrumQuantification> spectrumQuantifications = new ArrayList<SpectrumQuantification>();
                        for (SpectrumMatch spectrumMatch : peptideMatch.getSpectrumMatches().values()) {
                            String spectrumId = spectrumMatch.getSpectrum().getFileName() + "_" + spectrumMatch.getSpectrum().getSpectrumTitle();
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
                                    waitingPanel.appendReport(fileName + " not found. Quantification will not be possible.");
                                }
                                if (fileFound) {
                                    waitingPanel.appendReport("Spectrum " + spectrumMatch.getSpectrum().getSpectrumTitle() + " not found in " + fileName + ". Quantification will not be possible.");
                                }
                            }
                            SpectrumQuantification spectrumQuantification = new SpectrumQuantification(currentSpectrum);
                            processSpectrum(spectrumQuantification);
                            spectrumQuantifications.add(spectrumQuantification);
                            if (waitingPanel.isRunCancelled()) {
                                waitingPanel.setRunCancelled();
                                return 1;
                            }
                        }
                        PeptideQuantification peptideQuantification = new PeptideQuantification(peptideMatch, spectrumQuantifications);
                        peptideQuantification.addUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS), new IgnoredRatios());
                        peptideQuantifications.add(peptideQuantification);
                    }
                    ProteinQuantification proteinQuantification = new ProteinQuantification(proteinMatch, peptideQuantifications);
                    proteinQuantification.addUrParam(compomicsKeyFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS), new IgnoredRatios());
                    quantification.addProteinQuantification(proteinQuantification);
                }
                waitingPanel.appendReport("Ratio computation completed.");
                waitingPanel.setRunFinished();
            } catch (Exception e) {
                waitingPanel.appendReport("An error occured while calculating ratios:");
                waitingPanel.appendReport(e.getLocalizedMessage());
                waitingPanel.setRunCancelled();
            }
            return 0;
        }

        private synchronized void queue() {
            try {
                if (!identificationProcessor.isFinished() || !mgfProcessor.isfinished()) {
                    wait();
                }
            } catch (Exception e) {
                waitingPanel.appendReport("An error occured when waiting for file processing.");
                waitingPanel.appendReport(e.getLocalizedMessage());
                waitingPanel.setRunCancelled();
            }
        }

        public synchronized void restart() {
            try {
                if (identificationProcessor.isFinished() && mgfProcessor.isfinished()) {
                    notify();
                }
            } catch (Exception e) {
                waitingPanel.appendReport("An error occured when waiting for file processing.");
                waitingPanel.appendReport(e.getLocalizedMessage());
                waitingPanel.setRunCancelled();
            }
        }
    }

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
                ptmFactory.importModifications(new File(MODIFICATION_FILE));
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
                        if (waitingPanel.isRunCancelled()) {
                            waitingPanel.setRunCancelled();
                            return 1;
                        }
                    }
                    if (allSpectrumMatches.get(searchEngine) == null) {
                        allSpectrumMatches.put(searchEngine, tempSet);
                    } else {
                        allSpectrumMatches.get(searchEngine).addAll(tempSet);
                    }
                    if (waitingPanel.isRunCancelled()) {
                        waitingPanel.setRunCancelled();
                        return 1;
                    }
                }
                if (nRetained == 0) {
                    waitingPanel.appendReport("No identification retained.");
                    return 1;
                }
                waitingPanel.appendReport("Identification file(s) import completed. " + nTotal + " identifications imported, " + nRetained + " identifications retained.");
                waitingPanel.appendReport("FDR estimation.");
                HashMap<Integer, Double> thresholds = fdrCalculator.getEvalueLimits(fdrLimit);
                double eValueMax;
                for (int searchEngine : allSpectrumMatches.keySet()) {
                    eValueMax = thresholds.get(searchEngine);
                    Iterator<SpectrumMatch> matchIt = allSpectrumMatches.get(searchEngine).iterator();
                    SpectrumMatch match;
                    while (matchIt.hasNext()) {
                        match = matchIt.next();
                        if (match.getFirstHit(searchEngine).getEValue() > eValueMax
                                || match.getFirstHit(searchEngine).isDecoy()
                                || match.getFirstHit(searchEngine).getPeptide().getParentProteins().size() > 1) {
                            matchIt.remove();
                        }
                    }
                }
                String report = "FDR processing completed\nEngine\te-value\t\tnumber of hits at " + fdrLimit + "% FDR.\n";
                if (allSpectrumMatches.get(SearchEngine.MASCOT) == null) {
                    report += "Mascot\t\t\tno hits\n";
                } else {
                    report += "Mascot\t" + thresholds.get(SearchEngine.MASCOT) + "\t" + allSpectrumMatches.get(SearchEngine.MASCOT).size() + "\n";
                }
                if (allSpectrumMatches.get(SearchEngine.OMSSA) == null) {
                    report += "OMSSA\t\t\tno hits\n";
                } else {
                    report += "OMSSA\t" + thresholds.get(SearchEngine.OMSSA) + "\t" + allSpectrumMatches.get(SearchEngine.OMSSA).size() + "\n";
                }
                if (allSpectrumMatches.get(SearchEngine.XTANDEM) == null) {
                    report += "X!Tandem\t\t\tno hits\n";
                } else {
                    report += "X!Tandem\t" + thresholds.get(SearchEngine.XTANDEM) + "\t" + allSpectrumMatches.get(SearchEngine.XTANDEM).size() + "\n";
                }
                waitingPanel.appendReport(report);
                waitingPanel.appendReport("Building protein objects.");

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
                waitingPanel.appendReport(proteins.size() + " proteins imported.");

                finished = true;
                ratiosCompilator.restart();
            } catch (Exception e) {
                waitingPanel.appendReport("An error occured while loading the identification Files:");
                waitingPanel.appendReport(e.getLocalizedMessage());
                waitingPanel.setRunCancelled();
            }
            return 0;
        }

        public boolean isFinished() {
            return finished;
        }
    }

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
                    if (waitingPanel.isRunCancelled()) {
                        waitingPanel.setRunCancelled();
                        return 1;
                    }
                }
                waitingPanel.appendReport("Spectra file(s) import completed. " + spectra.size() + " spectra imported.");
                finished = true;
                ratiosCompilator.restart();
            } catch (Exception e) {
                waitingPanel.appendReport("An error occured while loading the mgf Files:");
                waitingPanel.appendReport(e.getLocalizedMessage());
                waitingPanel.setRunCancelled();
            }
            return 0;
        }

        public boolean isfinished() {
            return finished;
        }
    }
}
