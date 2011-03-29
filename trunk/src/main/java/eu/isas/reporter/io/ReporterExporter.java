package eu.isas.reporter.io;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PsmQuantification;
import eu.isas.reporter.myparameters.IgnoredRatios;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import javax.swing.JOptionPane;

/**
 * This class will export the quantification results as csv files
 *
 * @author Marc Vaudel
 */
public class ReporterExporter {

    /**
     * suffix for psm file
     */
    private final static String spectraSuffix = "_Reporter_spectra.txt";
    /**
     * suffix for peptide file
     */
    private final static String peptidesSuffix = "_Reporter_peptides.txt";
    /**
     * suffix for protein file
     */
    private final static String proteinsSuffix = "_Reporter_proteins.txt";
    /**
     * separator used to create the csv files
     */
    private String separator;

    /**
     * The experiment conducted
     */
    private MsExperiment experiment;
    /**
     * the reporter ions used in the method method
     */
    private ArrayList<ReporterIon> ions;
    /**
     * The reference reporter ion
     */
    private int reference;

    /**
     * Constructor
     *
     * @param experiment    The experiment conducted
     * @param separator     The separator to use
     */
    public ReporterExporter(MsExperiment experiment, String separator) {
        this.separator = separator;
        this.experiment = experiment;
    }

    /**
     * Exports the quantification results into csv files
     *
     * @param quantification    The quantification achieved
     * @param location          The folder where to save the files
     */
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

            String content = "Protein" + separator + "Sequence" + separator + "Variable Modifications" + separator + "Spectrum" + separator 
                    + "Spectrum File" + separator + "Identification File" + separator + "Mass Error" + separator + "Mascot E-Value" + separator
                    + "OMSSA E-Value" + separator + "X!Tandem E-Value" + separator + getRatiosLabels(quantification) + getIntensitiesLabels() + "Comment" + "\n";
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
            for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification().values()) {
                accession = proteinQuantification.getProteinMatch().getKey();
                pr++;
                pe = 0;
                s = 0;
                if (pr == 20) {
                    int test = 0;
                }
                for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification().values()) {
                    pe++;
                    s = 0;
                    sequence = peptideQuantification.getPeptideMatch().getTheoreticPeptide().getSequence();
                    variableModifications = "";
                    for (ModificationMatch modification : peptideQuantification.getPeptideMatch().getTheoreticPeptide().getModificationMatches()) {
                        if (modification.isVariable()) {
                            variableModifications += modification.getTheoreticPtm().getName() + " ";
                        }
                    }
                    for (PsmQuantification psmQuantification : peptideQuantification.getPsmQuantification().values()) {
                        s++;
                        spectrumFile = MSnSpectrum.getSpectrumFile(psmQuantification.getKey());
                        spectrumTitle = MSnSpectrum.getSpectrumTitle(psmQuantification.getKey());
                        match = peptideQuantification.getPeptideMatch().getSpectrumMatches().get(psmQuantification.getKey());
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
                        ignoredRatios = (IgnoredRatios) psmQuantification.getUrParam(ignoredRatios);
                        for (ReporterIon ion : ions) {
                            if (ignoredRatios.isIgnored(ion.getIndex())) {
                                comment += ion.getIndex() + " ignored. ";
                            }
                        }
                        content = accession + separator + sequence + separator + variableModifications + separator + spectrumTitle
                                + separator + spectrumFile + separator + idFile + separator + deltaMass + separator + mascotEValue
                                + separator + omssaEValue + separator + xTandemEValue + separator + getRatios(psmQuantification)
                                + getIntensities(psmQuantification) + comment + "\n";
                        spectraOutput.write(content);
                    }
                    comment = "";
                    ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
                    for (ReporterIon ion : ions) {
                        if (ignoredRatios.isIgnored(ion.getIndex())) {
                            comment += ion.getIndex() + " ignored. ";
                        }
                    }
                    content = accession + separator + sequence + separator + peptideQuantification.getPsmQuantification().size()
                            + separator + getRatios(peptideQuantification) + comment + "\n";
                    peptidesOutput.write(content);
                }
                comment = "";
                ignoredRatios = (IgnoredRatios) proteinQuantification.getUrParam(ignoredRatios);
                for (ReporterIon ion : ions) {
                    if (ignoredRatios.isIgnored(ion.getIndex())) {
                        comment += ion.getIndex() + " ignored. ";
                    }
                }
                content = accession + separator + proteinQuantification.getPeptideQuantification().size() + separator
                        + proteinQuantification.getProteinMatch().getSpectrumCount() + separator + getRatios(proteinQuantification) + comment + "\n";
                proteinsOutput.write(content);
            }

            spectraOutput.close();
            peptidesOutput.close();
            proteinsOutput.close();
            JOptionPane.showMessageDialog(null, "Result export finished.", "Export Finished", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception e) {
            String report = pr + " " + pe + " " + s;
            JOptionPane.showMessageDialog(null, "Output Failed" + report, "Output Failed", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Returns the labels of the computed ratios
     *
     * @param quantification the quantification achieved
     * @return String of the different labels
     */
    private String getRatiosLabels(ReporterIonQuantification quantification) {
        String result = "";
        for (ReporterIon ion : ions) {
            result += quantification.getSample(ion.getIndex()).getReference() + "/" + quantification.getSample(reference).getReference();
            result += separator;
        }
        return result;
    }

    /**
     * returns the ratios of the selected protein quantification
     *
     * @param proteinQuantification the selected protein quantification
     * @return  the corresponding ratios
     */
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

    /**
     * returns the ratios of the selected peptide quantification
     *
     * @param peptideQuantification the selected peptide quantification
     * @return  the corresponding ratios
     */
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

    /**
     * returns the ratios of the selected spectrum quantification
     *
     * @param spectrumQuantification the selected spectrum quantification
     * @return  the corresponding ratios
     */
    private String getRatios(PsmQuantification spectrumQuantification) {
        String result = "";
        for (ReporterIon ion : ions) {
            result += spectrumQuantification.getRatios().get(ion.getIndex()).getRatio() + separator;
        }
        return result;
    }

    /**
     * returns the intensities of the selected spectrum quantification
     *
     * @param spectrumQuantification the selected spectrum quantification
     * @return  the corresponding intensities
     */
    private String getIntensities(PsmQuantification spectrumQuantification) {
        String result = "";
        for (ReporterIon ion : ions) {
            IonMatch match = spectrumQuantification.getReporterMatches().get(ion.getIndex());
            if (match != null) {
                result += match.peak.intensity + separator;
            } else {
                result += 0 + separator;
            }
        }
        return result;
    }

    /**
     * Returns the labels of the different ions
     *
     * @return the labels of the different ions
     */
    private String getIntensitiesLabels() {
        String result = "";
        for (ReporterIon ion : ions) {
            result += ion.getIndex();
            result += separator;
        }
        return result;
    }
}
