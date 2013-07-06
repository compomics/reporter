package eu.isas.reporter.io;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.matches.PeptideQuantification;
import com.compomics.util.experiment.quantification.matches.ProteinQuantification;
import com.compomics.util.experiment.quantification.matches.PsmQuantification;
import com.compomics.util.experiment.refinementparameters.MascotScore;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JOptionPane;

/**
 * This class will export the quantification results as text files.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterExporter {

    /**
     * Suffix for PSM file.
     */
    private final static String spectraSuffix = "_Reporter_spectra.txt";
    /**
     * Suffix for peptide file.
     */
    private final static String peptidesSuffix = "_Reporter_peptides.txt";
    /**
     * Suffix for protein file.
     */
    private final static String proteinsSuffix = "_Reporter_proteins.txt";
    /**
     * Separator used to create the text files.
     */
    private String SEPARATOR;
    /**
     * The experiment conducted.
     */
    private MsExperiment experiment;
    /**
     * The reporter ions used in the method method.
     */
    private ArrayList<ReporterIon> ions;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();

    /**
     * The export levels.
     */
    public enum ExportLevel {

        PROTEIN, PEPTIDE, PSM
    };

    /**
     * Constructor.
     *
     * @param experiment The experiment conducted
     * @param separator The separator to use
     */
    public ReporterExporter(MsExperiment experiment, String separator) {
        this.SEPARATOR = separator;
        this.experiment = experiment;
    }

    /**
     * Exports the quantification results into text files.
     *
     * @param quantification The quantification achieved
     * @param identification The corresponding identification
     * @param saveFile The file to save the results to
     * @param exportLevel The export level: protein, peptide or PSM
     * @param progressDialog
     * @throws Exception
     */
    public void exportResults(ReporterIonQuantification quantification, Identification identification, File saveFile, ExportLevel exportLevel, ProgressDialogX progressDialog) throws Exception {

        // @TODO: add number of spectra/peptides used for quantification of peptides/proteins
        // @TODO: add indicators of the spread for the values

        ions = quantification.getReporterMethod().getReporterIons();

        long progress = 0, increment;

        if (exportLevel == ExportLevel.PROTEIN) {
            increment = quantification.getProteinQuantification().size();
        } else if (exportLevel == ExportLevel.PEPTIDE) {
            increment = quantification.getPeptideQuantification().size();
        } else { // psm level
            increment = quantification.getPsmIDentificationToQuantification().keySet().size();
        }

        increment /= 100;

        if (progressDialog != null) {
            progressDialog.setPrimaryProgressCounterIndeterminate(false);
            progressDialog.setMaxPrimaryProgressCounter(100);
            progressDialog.setValue(0);
        }

        if (exportLevel == ExportLevel.PSM) {

            BufferedWriter spectraOutput = new BufferedWriter(new FileWriter(saveFile));
            String titles = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "PTM Location Confidence" + SEPARATOR
                    + "Spectrum Charge" + SEPARATOR + "Identification Charge" + SEPARATOR + "Spectrum" + SEPARATOR + "Spectrum File" + SEPARATOR + "Identification File(s)"
                    + SEPARATOR + "Precursor RT" + SEPARATOR + "Precursor mz" + SEPARATOR + "Theoretic Mass" + SEPARATOR + "Mass Error (ppm)" + SEPARATOR
                    + "Mascot Score" + SEPARATOR + "Mascot E-Value" + SEPARATOR + "OMSSA E-Value"
                    + SEPARATOR + "X!Tandem E-Value" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + SEPARATOR
                    + getIntensitiesLabels() + SEPARATOR + getDeisotopedIntensitiesLabels() + SEPARATOR + "Reference" + SEPARATOR + getRatiosLabels(quantification) + "\n";
            spectraOutput.write(titles);
            SpectrumMatch spectrumMatch;

            ArrayList<String> loadedfiles = new ArrayList<String>(); // dirty speeding trick. To be removed when utilities has been upgraded

            // @TODO: has to use batch select/insert!!!

            for (String psmKey : quantification.getPsmIDentificationToQuantification().keySet()) {

                String fileName = Spectrum.getSpectrumFile(psmKey);
                if (!loadedfiles.contains(fileName)) {
                    loadedfiles.add(fileName);
                    identification.loadSpectrumMatchParameters(fileName, new PSParameter(), new ProgressDialogX(false)); // @TODO: should use the proper progress dialog?
                    identification.loadSpectrumMatches(fileName, new ProgressDialogX(false));
                }

                spectrumMatch = identification.getSpectrumMatch(psmKey);

                for (String spectrumKey : quantification.getPsmIDentificationToQuantification().get(psmKey)) {

                    Peptide bestAssumption = spectrumMatch.getBestAssumption().getPeptide();

                    // sort so that the protein accessions always come in the same order
                    ArrayList<String> allProteins = bestAssumption.getParentProteins();
                    Collections.sort(allProteins);
                    boolean first = true;

                    for (String otherProtein : allProteins) {
                        if (first) {
                            first = false;
                        } else {
                            spectraOutput.write(", ");
                        }
                        spectraOutput.write(otherProtein);
                    }

                    spectraOutput.write(SEPARATOR);
                    spectraOutput.write(bestAssumption.getSequence() + SEPARATOR);

                    HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();

                    for (ModificationMatch modificationMatch : bestAssumption.getModificationMatches()) {
                        if (modificationMatch.isVariable()) {
                            if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                                modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                            }
                            modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                        }
                    }

                    first = true;
                    ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
                    Collections.sort(modifications);

                    for (String mod : modifications) {
                        if (first) {
                            first = false;
                        } else {
                            spectraOutput.write(", ");
                        }

                        boolean first2 = true;
                        spectraOutput.write(mod + "(");

                        for (int aa : modMap.get(mod)) {
                            if (first2) {
                                first2 = false;
                            } else {
                                spectraOutput.write(", ");
                            }
                            spectraOutput.write(aa + "");
                        }

                        spectraOutput.write(")");
                    }

                    spectraOutput.write(SEPARATOR);
                    PSPtmScores ptmScores = new PSPtmScores();
                    first = true;

                    for (String mod : modifications) {

                        if (first) {
                            first = false;
                        } else {
                            spectraOutput.write(", ");
                        }

                        if (spectrumMatch.getUrParam(ptmScores) != null) {

                            ptmScores = (PSPtmScores) spectrumMatch.getUrParam(new PSPtmScores());
                            spectraOutput.write(mod + " (");

                            if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {

                                int ptmConfidence = ptmScores.getPtmScoring(mod).getPtmSiteConfidence();

                                if (ptmConfidence == PtmScoring.NOT_FOUND) {
                                    spectraOutput.write("Not Scored"); // Well this should not happen
                                } else if (ptmConfidence == PtmScoring.RANDOM) {
                                    spectraOutput.write("Random");
                                } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                                    spectraOutput.write("Doubtfull");
                                } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                                    spectraOutput.write("Confident");
                                } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                                    spectraOutput.write("Very Confident");
                                }
                            } else {
                                spectraOutput.write("Not Scored");
                            }

                            spectraOutput.write(")");
                        }
                    }

                    spectraOutput.write(SEPARATOR);
                    String spectrumTitle = Spectrum.getSpectrumTitle(spectrumMatch.getKey());
                    Precursor precursor = spectrumFactory.getPrecursor(fileName, spectrumTitle);
                    spectraOutput.write(precursor.getPossibleChargesAsString() + SEPARATOR);
                    spectraOutput.write(spectrumMatch.getBestAssumption().getIdentificationCharge().value + SEPARATOR);
                    spectraOutput.write(fileName + SEPARATOR);
                    spectraOutput.write(spectrumTitle + SEPARATOR);

                    ArrayList<String> fileNames = new ArrayList<String>();

                    for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions()) {
                        if (assumption.getPeptide().isSameAs(bestAssumption)) {
                            if (!fileNames.contains(assumption.getFile())) {
                                fileNames.add(assumption.getFile());
                            }
                        }
                    }

                    Collections.sort(fileNames);

                    for (String name : fileNames) {
                        spectraOutput.write(name + " ");
                    }

                    spectraOutput.write(SEPARATOR);
                    spectraOutput.write(precursor.getRt() + SEPARATOR);
                    spectraOutput.write(precursor.getMz() + SEPARATOR);
                    spectraOutput.write(spectrumMatch.getBestAssumption().getPeptide().getMass() + SEPARATOR);
                    spectraOutput.write(Math.abs(spectrumMatch.getBestAssumption().getDeltaMass(precursor.getMz(), true)) + SEPARATOR);
                    Double mascotEValue = null;
                    Double omssaEValue = null;
                    Double xtandemEValue = null;
                    double mascotScore = 0;

                    for (int se : spectrumMatch.getAdvocates()) {
                        for (double eValue : spectrumMatch.getAllAssumptions(se).keySet()) {
                            for (PeptideAssumption assumption : spectrumMatch.getAllAssumptions(se).get(eValue)) {
                                if (assumption.getPeptide().isSameAs(bestAssumption)) {
                                    if (se == Advocate.MASCOT) {
                                        if (mascotEValue == null || mascotEValue > eValue) {
                                            mascotEValue = eValue;
                                            mascotScore = ((MascotScore) assumption.getUrParam(new MascotScore(0))).getScore();
                                        }
                                    } else if (se == Advocate.OMSSA) {
                                        if (omssaEValue == null || omssaEValue > eValue) {
                                            omssaEValue = eValue;
                                        }
                                    } else if (se == Advocate.XTANDEM) {
                                        if (xtandemEValue == null || xtandemEValue > eValue) {
                                            xtandemEValue = eValue;
                                        }
                                    }
                                }
                            }
                        }
                    }

                    if (mascotEValue != null) {
                        spectraOutput.write(mascotScore + "");
                    }

                    spectraOutput.write(SEPARATOR);

                    if (mascotEValue != null) {
                        spectraOutput.write(mascotEValue + "");
                    }

                    spectraOutput.write(SEPARATOR);

                    if (omssaEValue != null) {
                        spectraOutput.write(omssaEValue + "");
                    }

                    spectraOutput.write(SEPARATOR);

                    if (xtandemEValue != null) {
                        spectraOutput.write(xtandemEValue + "");
                    }

                    spectraOutput.write(SEPARATOR);
                    PSParameter probabilities = new PSParameter();
                    probabilities = (PSParameter) identification.getSpectrumMatchParameter(psmKey, probabilities);

                    spectraOutput.write(probabilities.getPsmProbabilityScore() + SEPARATOR
                            + probabilities.getPsmProbability() + SEPARATOR);

                    if (spectrumMatch.getBestAssumption().getPeptide().isDecoy()) {
                        spectraOutput.write("1" + SEPARATOR);
                    } else {
                        spectraOutput.write("0" + SEPARATOR);
                    }

                    if (probabilities.isValidated()) {
                        spectraOutput.write("1" + SEPARATOR);
                    } else {
                        spectraOutput.write("0" + SEPARATOR);
                    }

                    // @TODO: has to use batch select/insert!!!

                    PsmQuantification psmQuantification = quantification.getSpectrumMatch(spectrumKey);
                    spectraOutput.write(getIntensities(psmQuantification) + SEPARATOR);
                    spectraOutput.write(getDeisotopedIntensities(psmQuantification) + SEPARATOR);
                    spectraOutput.write(psmQuantification.getReferenceIntensity() + SEPARATOR);
                    spectraOutput.write(getRatios(psmQuantification));
                    spectraOutput.write("\n");
                }

                if (progressDialog != null) {
                    progress++;
                    progressDialog.setValue((int) (progress / increment));
                    if (progressDialog.isRunCanceled()) {
                        spectraOutput.close();
                        saveFile.delete();
                        return;
                    }
                }
            }

            spectraOutput.close();

        } else if (exportLevel == ExportLevel.PEPTIDE) {

            BufferedWriter peptidesOutput = new BufferedWriter(new FileWriter(saveFile));
            String titles = "Protein(s)" + SEPARATOR + "Sequence" + SEPARATOR + "Variable Modification(s)" + SEPARATOR + "PTM Location Confidence" + SEPARATOR
                    + "#Spectra" + SEPARATOR + "#Spectra Validated" + SEPARATOR + "p score" + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR
                    + "Validated" + SEPARATOR + getRatiosLabels(quantification) + "\n";
            peptidesOutput.write(titles);

            identification.loadPeptideMatches(progressDialog);
            identification.loadPeptideMatchParameters(new PSParameter(), progressDialog);

            for (String peptideKey : quantification.getPeptideQuantification()) {

                PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

                // sort so that the protein accessions always come in the same order
                ArrayList<String> allProteins = peptideMatch.getTheoreticPeptide().getParentProteins();
                Collections.sort(allProteins);
                boolean first = true;

                for (String otherProtein : allProteins) {
                    if (first) {
                        first = false;
                    } else {
                        peptidesOutput.write(", ");
                    }
                    peptidesOutput.write(otherProtein);
                }

                peptidesOutput.write(SEPARATOR);
                peptidesOutput.write(peptideMatch.getTheoreticPeptide().getSequence() + SEPARATOR);

                HashMap<String, ArrayList<Integer>> modMap = new HashMap<String, ArrayList<Integer>>();

                for (ModificationMatch modificationMatch : peptideMatch.getTheoreticPeptide().getModificationMatches()) {
                    if (modificationMatch.isVariable()) {
                        if (!modMap.containsKey(modificationMatch.getTheoreticPtm())) {
                            modMap.put(modificationMatch.getTheoreticPtm(), new ArrayList<Integer>());
                        }
                        modMap.get(modificationMatch.getTheoreticPtm()).add(modificationMatch.getModificationSite());
                    }
                }

                first = true;
                ArrayList<String> modifications = new ArrayList<String>(modMap.keySet());
                Collections.sort(modifications);

                for (String mod : modifications) {
                    if (first) {
                        first = false;
                    } else {
                        peptidesOutput.write(", ");
                    }

                    boolean first2 = true;
                    peptidesOutput.write(mod + "(");

                    for (int aa : modMap.get(mod)) {
                        if (first2) {
                            first2 = false;
                        } else {
                            peptidesOutput.write(", ");
                        }
                        peptidesOutput.write(aa + "");
                    }

                    peptidesOutput.write(")");
                }

                peptidesOutput.write(SEPARATOR);
                first = true;

                for (String mod : modifications) {

                    if (first) {
                        first = false;
                    } else {
                        peptidesOutput.write(", ");
                    }

                    PSPtmScores ptmScores = (PSPtmScores) peptideMatch.getUrParam(new PSPtmScores());
                    peptidesOutput.write(mod + " (");

                    if (ptmScores != null && ptmScores.getPtmScoring(mod) != null) {

                        int ptmConfidence = ptmScores.getPtmScoring(mod).getPtmSiteConfidence();

                        if (ptmConfidence == PtmScoring.NOT_FOUND) {
                            peptidesOutput.write("Not Scored"); // Well this should not happen
                        } else if (ptmConfidence == PtmScoring.RANDOM) {
                            peptidesOutput.write("Random");
                        } else if (ptmConfidence == PtmScoring.DOUBTFUL) {
                            peptidesOutput.write("Doubtfull");
                        } else if (ptmConfidence == PtmScoring.CONFIDENT) {
                            peptidesOutput.write("Confident");
                        } else if (ptmConfidence == PtmScoring.VERY_CONFIDENT) {
                            peptidesOutput.write("Very Confident");
                        }
                    } else {
                        peptidesOutput.write("Not Scored");
                    }

                    peptidesOutput.write(")");
                }

                peptidesOutput.write(SEPARATOR);

                peptidesOutput.write(peptideMatch.getSpectrumCount() + SEPARATOR);
                PSParameter probabilities = new PSParameter();
                int nSpectraValidated = 0;

                for (String spectrumKey : peptideMatch.getSpectrumMatches()) {
                    probabilities = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, probabilities);
                    if (probabilities.isValidated()) {
                        nSpectraValidated++;
                    }
                }

                peptidesOutput.write(nSpectraValidated + SEPARATOR);
                probabilities = (PSParameter) identification.getPeptideMatchParameter(peptideKey, probabilities);

                peptidesOutput.write(probabilities.getPeptideProbabilityScore() + SEPARATOR
                        + probabilities.getPeptideProbability() + SEPARATOR);

                if (peptideMatch.getTheoreticPeptide().isDecoy()) {
                    peptidesOutput.write("1" + SEPARATOR);
                } else {
                    peptidesOutput.write("0" + SEPARATOR);
                }

                if (probabilities.isValidated()) {
                    peptidesOutput.write("1" + SEPARATOR);
                } else {
                    peptidesOutput.write("0" + SEPARATOR);
                }

                PeptideQuantification peptideQuantification = quantification.getPeptideMatch(peptideKey);
                peptidesOutput.write(getRatios(peptideQuantification) + SEPARATOR);
                peptidesOutput.write("\n");

                if (progressDialog != null) {
                    progress++;
                    progressDialog.setValue((int) (progress / increment));
                    if (progressDialog.isRunCanceled()) {
                        peptidesOutput.close();
                        saveFile.delete();
                        return;
                    }
                }
            }

            peptidesOutput.close();

        } else { // protein level

            BufferedWriter proteinsOutput = new BufferedWriter(new FileWriter(saveFile));
            String titles = "Protein" + SEPARATOR + "Other Protein(s) (alphabetical order)" + SEPARATOR + "Complete Protein Group (alphabetical order)" + SEPARATOR + "Group class"
                    + SEPARATOR + "#Peptides" + SEPARATOR + "#Spectra"
                    + SEPARATOR + "#Peptides Validated" + SEPARATOR + "#Spectra Validated" + SEPARATOR + "MW" + SEPARATOR + "NSAF" + SEPARATOR + "p score"
                    + SEPARATOR + "p" + SEPARATOR + "Decoy" + SEPARATOR + "Validated" + SEPARATOR + "Description" + SEPARATOR + getRatiosLabels(quantification) + "\n";
            proteinsOutput.write(titles);

            identification.loadProteinMatchParameters(new PSParameter(), new ProgressDialogX(false)); // @TODO: should use the proper progress dialog?
            identification.loadProteinMatches(new ProgressDialogX(false)); // @TODO: should use the proper progress dialog?

            for (String proteinKey : quantification.getProteinQuantification()) {

                PSParameter probabilities = new PSParameter();
                probabilities = (PSParameter) identification.getProteinMatchParameter(proteinKey, probabilities);
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

                proteinsOutput.write(proteinMatch.getMainMatch() + SEPARATOR);

                // sort so that the protein accessions always come in the same order
                ArrayList<String> allProteins = proteinMatch.getTheoreticProteinsAccessions();
                Collections.sort(allProteins);
                StringBuilder completeProteinGroup = new StringBuilder();
                boolean first = true;

                for (String otherProtein : allProteins) {
                    if (!otherProtein.equalsIgnoreCase(proteinMatch.getMainMatch())) {
                        if (first) {
                            first = false;
                        } else {
                            proteinsOutput.write(", ");
                        }
                        proteinsOutput.write(otherProtein);
                    }

                    if (completeProteinGroup.length() > 0) {
                        completeProteinGroup.append(", ");
                    }

                    completeProteinGroup.append(otherProtein);
                }

                proteinsOutput.write(SEPARATOR + completeProteinGroup.toString());


                proteinsOutput.write(SEPARATOR + probabilities.getProteinInferenceClassAsString() + SEPARATOR);
                proteinsOutput.write(proteinMatch.getPeptideCount() + SEPARATOR);
                int nSpectra = 0;
                int nValidatedPeptides = 0;
                int nValidatedPsms = 0;
                PSParameter psParameter = new PSParameter();

                for (String peptideKey : proteinMatch.getPeptideMatches()) {

                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);
                    nSpectra += peptideMatch.getSpectrumCount();
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

                    if (psParameter.isValidated()) {

                        nValidatedPeptides++;

                        for (String spectrumKey : peptideMatch.getSpectrumMatches()) {

                            psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                            if (psParameter.isValidated()) {
                                nValidatedPsms++;
                            }
                        }
                    }
                }

                proteinsOutput.write(nSpectra + SEPARATOR);
                proteinsOutput.write(nValidatedPeptides + SEPARATOR + nValidatedPsms + SEPARATOR);

                try {
                    proteinsOutput.write(sequenceFactory.getProtein(proteinMatch.getMainMatch()).computeMolecularWeight() + SEPARATOR);
                    proteinsOutput.write("Not implemented" + SEPARATOR);
                } catch (Exception e) {
                    proteinsOutput.write("protein not found " + SEPARATOR + SEPARATOR);
                }

                try {
                    proteinsOutput.write(probabilities.getProteinProbabilityScore() + SEPARATOR
                            + probabilities.getProteinProbability() + SEPARATOR);
                } catch (Exception e) {
                    proteinsOutput.write(SEPARATOR + SEPARATOR);
                }

                if (proteinMatch.isDecoy()) {
                    proteinsOutput.write("1" + SEPARATOR);
                } else {
                    proteinsOutput.write("0" + SEPARATOR);
                }

                if (probabilities.isValidated()) {
                    proteinsOutput.write("1" + SEPARATOR);
                } else {
                    proteinsOutput.write("0" + SEPARATOR);
                }

                try {
                    proteinsOutput.write(sequenceFactory.getHeader(proteinMatch.getMainMatch()).getDescription());
                } catch (Exception e) {
                    proteinsOutput.write("Protein not found");
                }
                proteinsOutput.write(SEPARATOR);
                ProteinQuantification proteinQuantification = quantification.getProteinMatch(proteinKey);
                proteinsOutput.write(getRatios(proteinQuantification));
                proteinsOutput.write("\n");

                if (progressDialog != null) {
                    progress++;
                    progressDialog.setValue((int) (progress / increment));
                    if (progressDialog.isRunCanceled()) {
                        proteinsOutput.close();
                        saveFile.delete();
                        return;
                    }
                }
            }

            proteinsOutput.close();
        }
    }

    /**
     * Returns the labels of the computed ratios.
     *
     * @param quantification the quantification achieved
     * @return String of the different labels
     */
    private String getRatiosLabels(ReporterIonQuantification quantification) {

        String result = "";
        boolean first = true;

        for (ReporterIon ion : ions) {
            if (!first) {
                result += SEPARATOR;
            } else {
                first = false;
            }
            result += quantification.getSample(ion.getIndex()).getReference();
        }

        return result;
    }

    /**
     * Returns the ratios of the selected protein quantification.
     *
     * @param proteinQuantification the selected protein quantification
     * @return the corresponding ratios
     */
    private String getRatios(ProteinQuantification proteinQuantification) {

        String result = "";
        boolean first = true;

        for (ReporterIon ion : ions) {
            if (!first) {
                result += SEPARATOR;
            } else {
                first = false;
            }
            try {
                result += proteinQuantification.getRatios().get(ion.getIndex()).getRatio();
            } catch (Exception e) {
                result += "NA";
            }
        }

        return result;
    }

    /**
     * Returns the ratios of the selected peptide quantification.
     *
     * @param peptideQuantification the selected peptide quantification
     * @return the corresponding ratios
     */
    private String getRatios(PeptideQuantification peptideQuantification) {

        String result = "";
        boolean first = true;

        for (ReporterIon ion : ions) {
            if (!first) {
                result += SEPARATOR;
            } else {
                first = false;
            }
            try {
                result += peptideQuantification.getRatios().get(ion.getIndex()).getRatio();
            } catch (Exception e) {
                result += "NA";
            }
        }

        return result;
    }

    /**
     * Returns the ratios of the selected spectrum quantification.
     *
     * @param spectrumQuantification the selected spectrum quantification
     * @return the corresponding ratios
     */
    private String getRatios(PsmQuantification spectrumQuantification) {

        String result = "";
        boolean first = true;

        for (ReporterIon ion : ions) {
            if (!first) {
                result += SEPARATOR;
            } else {
                first = false;
            }
            result += spectrumQuantification.getRatios().get(ion.getIndex()).getRatio();
        }

        return result;
    }

    /**
     * Returns the intensities of the selected spectrum quantification.
     *
     * @param spectrumQuantification the selected spectrum quantification
     * @return the corresponding intensities
     */
    private String getIntensities(PsmQuantification spectrumQuantification) {

        String result = "";
        boolean first = true;

        for (ReporterIon ion : ions) {
            if (!first) {
                result += SEPARATOR;
            } else {
                first = false;
            }
            IonMatch match = spectrumQuantification.getReporterMatches().get(ion.getIndex());
            if (match != null) {
                result += match.peak.intensity;
            } else {
                result += 0;
            }
        }

        return result;
    }

    /**
     * Returns the intensities of the selected spectrum quantification.
     *
     * @param spectrumQuantification the selected spectrum quantification
     * @return the corresponding intensities
     */
    private String getDeisotopedIntensities(PsmQuantification spectrumQuantification) {

        String result = "";
        boolean first = true;

        for (ReporterIon ion : ions) {
            if (!first) {
                result += SEPARATOR;
            } else {
                first = false;
            }
            result += spectrumQuantification.getDeisotopedIntensities().get(ion.getIndex());
        }

        return result;
    }

    /**
     * Returns the labels of the different ions.
     *
     * @return the labels of the different ions
     */
    private String getIntensitiesLabels() {

        String result = "";
        boolean first = true;

        for (ReporterIon ion : ions) {
            if (!first) {
                result += SEPARATOR;
            } else {
                first = false;
            }
            result += ion.getIndex();
        }

        return result;
    }

    /**
     * Returns the labels of the different ions.
     *
     * @return the labels of the different ions
     */
    private String getDeisotopedIntensitiesLabels() {
        String result = "";
        boolean first = true;
        for (ReporterIon ion : ions) {
            if (!first) {
                result += SEPARATOR;
            } else {
                first = false;
            }
            result += "deisotoped " + ion.getIndex();
        }
        return result;
    }
}
