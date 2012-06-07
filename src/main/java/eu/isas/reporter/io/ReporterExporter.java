package eu.isas.reporter.io;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Advocate;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PsmQuantification;
import com.compomics.util.gui.dialogs.ProgressDialogX;
import eu.isas.reporter.myparameters.IdentificationDetails;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JOptionPane;

/**
 * This class will export the quantification results as csv files.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterExporter {

    /**
     * Suffix for psm file.
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
     * Separator used to create the csv files.
     */
    private String separator;
    /**
     * The experiment conducted.
     */
    private MsExperiment experiment;
    /**
     * The reporter ions used in the method method.
     */
    private ArrayList<ReporterIon> ions;
    /**
     * The reference reporter ion.
     */
    private int reference;

    /**
     * Constructor.
     *
     * @param experiment The experiment conducted
     * @param separator The separator to use
     */
    public ReporterExporter(MsExperiment experiment, String separator) {
        this.separator = separator;
        this.experiment = experiment;
    }

    /**
     * Exports the quantification results into csv files.
     *
     * @param quantification The quantification achieved
     * @param identification The corresponding identification
     * @param location The folder where to save the files
     * @param progressDialog
     * @throws Exception
     */
    public void exportResults(ReporterIonQuantification quantification, Identification identification, String location, ProgressDialogX progressDialog) throws Exception {

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
        
        Writer spectraOutput = new BufferedWriter(new FileWriter(spectraFile));
        String content = "Protein" + separator + "Sequence" + separator + "Variable Modifications" + separator + "Spectrum File" + separator 
                + "Identification Spectrum" + separator + "Quantification spectrum" + separator
                + "Identification File" + separator + "Mass Error" + separator + "Mascot E-Value" + separator
                + "OMSSA E-Value" + separator + "X!Tandem E-Value" + separator + "Validated" + separator + "Decoy" + separator 
                + getRatiosLabels(quantification) + separator + getIntensitiesLabels() + "\n";
        spectraOutput.write(content);
        SpectrumMatch spectrumMatch;

        IdentificationDetails identificationDetails = new IdentificationDetails();
        
        for (String psmKey : quantification.getPsmIDentificationToQuantification().keySet()) {
            
            spectrumMatch = identification.getSpectrumMatch(psmKey);
            String spectrumFile = Spectrum.getSpectrumFile(psmKey);
            String identificationSpectrum = Spectrum.getSpectrumTitle(psmKey);
            Peptide peptide = spectrumMatch.getBestAssumption().getPeptide();
            String idFile = "";
            boolean first = true;
            
            for (int se : spectrumMatch.getAdvocates()) {
                if (!first) {
                    idFile += ", ";
                } else {
                    first = false;
                }
                if (spectrumMatch.getFirstHit(se).getPeptide().isSameAs(peptide)) {
                    idFile += spectrumMatch.getFirstHit(se).getFile();
                }
            }
            
            for (String spectrumKey : quantification.getPsmIDentificationToQuantification().get(psmKey)) {
                
                String quantificationSpectrum = Spectrum.getSpectrumTitle(spectrumKey);
                content = "";
                first = true;
                boolean decoy = false;
                
                for (String protein : peptide.getParentProteins()) {
                    if (!first) {
                        content += ", ";
                    } else {
                        first = false;
                    }
                    content += protein;
                    if (ProteinMatch.isDecoy(protein)) {
                        decoy = true;
                    }
                }
                
                content += separator;
                content += peptide.getSequence() + separator;
                HashMap<String, ArrayList<Integer>> modificationMap = new HashMap<String, ArrayList<Integer>>();
                
                for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                    if (modMatch.isVariable()) {
                        if (!modificationMap.containsKey(modMatch.getTheoreticPtm())) {
                            modificationMap.put(modMatch.getTheoreticPtm(), new ArrayList<Integer>());
                        }
                        modificationMap.get(modMatch.getTheoreticPtm()).add(modMatch.getModificationSite());
                    }
                }
                
                first = true;
                
                for (String mod : modificationMap.keySet()) {
                    if (!first) {
                        content += ", ";
                    } else {
                        first = false;
                    }
                    content += mod + " (";
                    boolean first2 = true;
                    for (int position : modificationMap.get(mod)) {
                        if (!first2) {
                            content += ", ";
                        } else {
                            first2 = false;
                        }
                        content += position;
                    }
                    content += ")";
                }
                
                content += separator;
                content += spectrumFile + separator;
                content += identificationSpectrum + separator;
                content += quantificationSpectrum + separator;
                content += idFile + separator;
                content += spectrumMatch.getBestAssumption().getDeltaMass(true) + separator; // @TODO: should delta mass always be in ppm??

                if (spectrumMatch.getFirstHit(Advocate.MASCOT) != null
                        && spectrumMatch.getFirstHit(Advocate.MASCOT).getPeptide().isSameAs(peptide)) {
                    content += spectrumMatch.getFirstHit(Advocate.MASCOT).getEValue();
                }
                
                content += separator;
                
                if (spectrumMatch.getFirstHit(Advocate.OMSSA) != null
                        && spectrumMatch.getFirstHit(Advocate.OMSSA).getPeptide().isSameAs(peptide)) {
                    content += spectrumMatch.getFirstHit(Advocate.OMSSA).getEValue();
                }
                
                content += separator;
                
                if (spectrumMatch.getFirstHit(Advocate.XTANDEM) != null
                        && spectrumMatch.getFirstHit(Advocate.XTANDEM).getPeptide().isSameAs(peptide)) {
                    content += spectrumMatch.getFirstHit(Advocate.XTANDEM).getEValue();
                }
                
                content += separator;

                identificationDetails = (IdentificationDetails) identification.getMatchParameter(psmKey, identificationDetails);
                
                if (identificationDetails.isValidated()) {
                    content += "1" + separator;
                } else {
                    content += "0" + separator;
                }
                
                if (decoy) {
                    content += "1" + separator;
                } else {
                    content += "0" + separator;
                }

                PsmQuantification psmQuantification = quantification.getSpectrumMatch(spectrumKey);
                content += getRatios(psmQuantification);
                content += getIntensities(psmQuantification);
                content += "\n";
                spectraOutput.write(content);
            }
        }
        
        spectraOutput.close();

        Writer peptidesOutput = new BufferedWriter(new FileWriter(peptidesFile));
        content = "Protein(s)" + separator + "Sequence" + separator + "Variable Modification(s)" + separator 
                + "number of Spectra" + separator + "Validated" + separator + "Decoy" + separator + getRatiosLabels(quantification) + "\n";
        peptidesOutput.write(content);
        PeptideMatch peptideMatch;
        
        for (String peptideKey : quantification.getPeptideQuantification()) {
            
            peptideMatch = identification.getPeptideMatch(peptideKey);
            Peptide peptide = peptideMatch.getTheoreticPeptide();
            content = "";
            boolean first = true;
            boolean decoy = false;
            
            for (String protein : peptide.getParentProteins()) {
                if (!first) {
                    content += ", ";
                } else {
                    first = false;
                }
                content += protein;
                if (ProteinMatch.isDecoy(protein)) {
                    decoy = true;
                }
            }
            
            content += separator;
            content += peptide.getSequence() + separator;

            HashMap<String, ArrayList<Integer>> modificationMap = new HashMap<String, ArrayList<Integer>>();
            
            for (ModificationMatch modMatch : peptide.getModificationMatches()) {
                if (modMatch.isVariable()) {
                    if (!modificationMap.containsKey(modMatch.getTheoreticPtm())) {
                        modificationMap.put(modMatch.getTheoreticPtm(), new ArrayList<Integer>());
                    }
                    modificationMap.get(modMatch.getTheoreticPtm()).add(modMatch.getModificationSite());
                }
            }
            
            first = true;
            
            for (String mod : modificationMap.keySet()) {
                if (!first) {
                    content += ", ";
                } else {
                    first = false;
                }
                content += mod + " (";
                boolean first2 = true;
                for (int position : modificationMap.get(mod)) {
                    if (!first2) {
                        content += ", ";
                    } else {
                        first2 = false;
                    }
                    content += position;
                }
                content += ")";
            }
            
            content += separator;
            PeptideQuantification peptideQuantification = quantification.getPeptideMatch(peptideKey);
            content += peptideQuantification.getPsmQuantification().size() + separator;

            identificationDetails = (IdentificationDetails) identification.getMatchParameter(peptideKey, identificationDetails);
            
            if (identificationDetails.isValidated()) {
                content += "1" + separator;
            } else {
                content += "0" + separator;
            }
            
            if (decoy) {
                content += "1" + separator;
            } else {
                content += "0" + separator;
            }

            content += getRatios(peptideQuantification) + separator;
            content += "\n";
            peptidesOutput.write(content);
        }
        
        peptidesOutput.close();

        Writer proteinsOutput = new BufferedWriter(new FileWriter(proteinsFile));
        content = "possible protein(s)" + separator + "Number of identified Peptides" + separator + "Number of quantified peptides" 
                + separator + "Validated" + separator + "Decoy" + separator + getRatiosLabels(quantification) + "\n";
        proteinsOutput.write(content);
        
        for (String proteinKey : quantification.getProteinQuantification()) {
            
            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);
            content = "";
            boolean first = true;
            boolean decoy = false;
            
            for (String protein : ProteinMatch.getAccessions(proteinKey)) {
                if (!first) {
                    content += ", ";
                } else {
                    first = false;
                }
                content += protein;
                if (ProteinMatch.isDecoy(protein)) {
                    decoy = true;
                }
            }
            
            content += separator;
            content += proteinMatch.getPeptideMatches().size() + separator;

            ProteinQuantification proteinQuantification = quantification.getProteinMatch(proteinKey);
            content += proteinQuantification.getPeptideQuantification().size() + separator;

            identificationDetails = (IdentificationDetails) identification.getMatchParameter(proteinKey, identificationDetails);
            
            if (identificationDetails.isValidated()) {
                content += "1" + separator;
            } else {
                content += "0" + separator;
            }
            
            if (decoy) {
                content += "1" + separator;
            } else {
                content += "0" + separator;
            }

            content += getRatios(proteinQuantification);
            content += "\n";

            proteinsOutput.write(content);
        }
        
        proteinsOutput.close();
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
                result += separator;
            } else {
                first = false;
            }
            result += quantification.getSample(ion.getIndex()).getReference() + "/" + quantification.getSample(reference).getReference();
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
                result += separator;
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
                result += separator;
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
        
        for (ReporterIon ion : ions) {
            result += spectrumQuantification.getRatios().get(ion.getIndex()).getRatio() + separator;
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
                result += separator;
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
     * Returns the labels of the different ions.
     *
     * @return the labels of the different ions
     */
    private String getIntensitiesLabels() {
        
        String result = "";
        boolean first = true;
        
        for (ReporterIon ion : ions) {
            if (!first) {
                result += separator;
            } else {
                first = false;
            }
            result += ion.getIndex();
        }
        
        return result;
    }
}
