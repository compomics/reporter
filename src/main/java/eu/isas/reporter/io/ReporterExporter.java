package eu.isas.reporter.io;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.SpectrumQuantification;
import eu.isas.reporter.compomicsutilitiessettings.IgnoredRatios;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import javax.swing.JOptionPane;

/**
 *
 * @author Marc
 */
public class ReporterExporter {

    private final static String spectraSuffix = "_Reporter_spectra.txt";
    private final static String peptidesSuffix = "_Reporter_peptides.txt";
    private final static String proteinsSuffix = "_Reporter_proteins.txt";
    private String separator;
    private MsExperiment experiment;
    private ArrayList<ReporterIon> ions;
    private int reference;

    public ReporterExporter(MsExperiment experiment, String separator) {
        this.separator = separator;
        this.experiment = experiment;
    }

    public void exportResults(ReporterIonQuantification quantification, String location) {

        ions = quantification.getReporterMethod().getReporterIons();
        reference = quantification.getReferenceLabel();

        File spectraFile = new File(location, experiment.getReference() + spectraSuffix);
        File peptidesFile = new File(location, experiment.getReference() + peptidesSuffix);
        File proteinsFile = new File(location, experiment.getReference() + proteinsSuffix);

        if (spectraFile.exists()) {
            int outcome = JOptionPane.showConfirmDialog(null, new String[]{"Existing output file found", "Overwrite?"}, "File exists!", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (outcome != JOptionPane.YES_OPTION) {
                return;
            }
        }

        int s = 0;
        int pe = 0;
        int pr = 0;

        try {
            Writer spectraOutput = new BufferedWriter(new FileWriter(spectraFile));
            Writer peptidesOutput = new BufferedWriter(new FileWriter(peptidesFile));
            Writer proteinsOutput = new BufferedWriter(new FileWriter(proteinsFile));

            String content = "Protein" + separator + "Sequence" + separator + "Variable Modifications" + separator + "Spectrum" + separator + "Spectrum File" + separator + "Identification File" + separator + "Mass Error" + separator + "Mascot E-Value" + separator + "OMSSA E-Value" + separator + "X!Tandem E-Value" + separator + getRatiosLabels(quantification) + getIntensitiesLabels() + "Comment" + "\n";
            spectraOutput.write(content);
            content = "Protein" + separator + "Sequence" + separator + "number of Spectra" + separator + getRatiosLabels(quantification) + "Comment" + "\n";
            peptidesOutput.write(content);
            content = "Protein" + separator + "Number of Peptides" + separator + "Number of Spectra" + separator + getRatiosLabels(quantification) + "Comment" + "\n";
            proteinsOutput.write(content);


            IgnoredRatios ignoredRatios = new IgnoredRatios();
            boolean seConflict, found;
            String accession, sequence, variableModifications, spectrumFile, spectrumTitle, idFile, deltaMass, mascotEValue, omssaEValue, xTandemEValue, comment;
            SpectrumMatch match;
            PeptideAssumption assumption;
            for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification()) {
                accession = proteinQuantification.getProteinMatch().getTheoreticProtein().getAccession();
                pr++;
                pe = 0;
                s = 0;
                if (pr == 20) {
                    int test = 0;
                }
                for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
                    pe++;
                    s = 0;
                    sequence = peptideQuantification.getPeptideMatch().getTheoreticPeptide().getSequence();
                    variableModifications = "";
                    for (ModificationMatch modification : peptideQuantification.getPeptideMatch().getTheoreticPeptide().getModificationMatches()) {
                        if (modification.isVariable()) {
                            variableModifications += modification.getTheoreticPtm().getName() + " ";
                        }
                    }
                    for (SpectrumQuantification spectrumQuantification : peptideQuantification.getSpectrumQuantification()) {
                        s++;
                        spectrumFile = spectrumQuantification.getSpectrum().getFileName();
                        spectrumTitle = spectrumQuantification.getSpectrum().getSpectrumTitle();
                        match = peptideQuantification.getPeptideMatch().getSpectrumMatches().get(spectrumFile + "_" + spectrumTitle);
                        found = false;
                        seConflict = false;
                        idFile = "";
                        mascotEValue = "";
                        omssaEValue = "";
                        xTandemEValue = "";
                        deltaMass = "";
                        for (int searchEngine : match.getAdvocates()) {
                            assumption = match.getFirstHit(searchEngine);
                            if (assumption.getPeptide().getSequence().equals(sequence)) {
                                idFile += assumption.getFile() + " ";
                                if (!found) {
                                    deltaMass = assumption.getDeltaMass() + "";
                                    if (searchEngine == SearchEngine.MASCOT) {
                                        mascotEValue += assumption.getEValue();
                                    } else if (searchEngine == SearchEngine.OMSSA) {
                                        omssaEValue += assumption.getEValue();
                                    } else if (searchEngine == SearchEngine.XTANDEM) {
                                        xTandemEValue += assumption.getEValue();
                                    }
                                    found = true;
                                }
                            } else {
                                seConflict = true;
                            }
                        }
                        comment = "";
                        if (seConflict) {
                            comment += "Search engine conflict. ";
                        }
                        ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
                        for (ReporterIon ion : ions) {
                            if (ignoredRatios.isIgnored(ion.getIndex())) {
                                comment += ion.getIndex() + " ignored. ";
                            }
                        }
                        content = accession + separator + sequence + separator + variableModifications + separator + spectrumTitle + separator + spectrumFile + separator + idFile + separator + deltaMass + separator + mascotEValue + separator + omssaEValue + separator + xTandemEValue + separator + getRatios(spectrumQuantification) + getIntensities(spectrumQuantification) + comment + "\n";
                        spectraOutput.write(content);
                    }
                    comment = "";
                    ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
                    for (ReporterIon ion : ions) {
                        if (ignoredRatios.isIgnored(ion.getIndex())) {
                            comment += ion.getIndex() + " ignored. ";
                        }
                    }
                    content = accession + separator + sequence + separator + peptideQuantification.getSpectrumQuantification().size() + separator + getRatios(peptideQuantification) + comment + "\n";
                    peptidesOutput.write(content);
                }
                comment = "";
                ignoredRatios = (IgnoredRatios) proteinQuantification.getUrParam(ignoredRatios);
                for (ReporterIon ion : ions) {
                    if (ignoredRatios.isIgnored(ion.getIndex())) {
                        comment += ion.getIndex() + " ignored. ";
                    }
                }
                content = accession + separator + proteinQuantification.getPeptideQuantification().size() + separator + proteinQuantification.getProteinMatch().getSpectrumCount() + separator + getRatios(proteinQuantification) + comment + "\n";
                proteinsOutput.write(content);
            }

            spectraOutput.close();
            peptidesOutput.close();
            proteinsOutput.close();
            JOptionPane.showMessageDialog(null, "Result export finished.", "Export Finished", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            String report = pr + " " + pe + " " + s;
            JOptionPane.showMessageDialog(null, "Output Failed" + report, "Output Failed", JOptionPane.ERROR_MESSAGE);
            return;
        }

    }

    private String getRatiosLabels(ReporterIonQuantification quantification) {
        String result = "";
        for (ReporterIon ion : ions) {
            result += quantification.getSample(ion.getIndex()).getReference() + "/" + quantification.getSample(reference).getReference();
            result += separator;
        }
        return result;
    }

    private String getRatios(ProteinQuantification proteinQuantification) {
        String result = "";
        for (ReporterIon ion : ions) {
            try {
                result += proteinQuantification.getProteinRatios().get(ion.getIndex()).getRatio() + separator;
            } catch (Exception e) {
                result += "NA" + separator;
            }
        }
        return result;
    }

    private String getRatios(PeptideQuantification peptideQuantification) {
        String result = "";
        for (ReporterIon ion : ions) {
            try {
                result += peptideQuantification.getRatios().get(ion.getIndex()).getRatio() + separator;
            } catch (Exception e) {
                result += "NA" + separator;
            }
        }
        return result;
    }

    private String getRatios(SpectrumQuantification spectrumQuantification) {
        String result = "";
        for (ReporterIon ion : ions) {
            result += spectrumQuantification.getRatios().get(ion.getIndex()).getRatio() + separator;
        }
        return result;
    }

    private String getIntensities(SpectrumQuantification spectrumQuantification) {
        String result = "";
        for (ReporterIon ion : ions) {
            Peak peak = spectrumQuantification.getReporterMatches().get(ion.getIndex()).peak;
            if (peak != null) {
                result += peak.intensity + separator;
            } else {
                result += 0 + separator;
            }
        }
        return result;
    }

    private String getIntensitiesLabels() {
        String result = "";
        for (ReporterIon ion : ions) {
            result += ion.getIndex();
            result += separator;
        }
        return result;
    }
}
