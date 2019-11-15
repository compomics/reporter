package eu.isas.reporter.export.report.sections;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.spectrum_assumptions.PeptideAssumption;
import com.compomics.util.experiment.identification.spectrum_assumptions.TagAssumption;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.io.export.writers.ExcelWriter;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.export.sections.PsFragmentSection;
import eu.isas.peptideshaker.export.sections.PsIdentificationAlgorithmMatchesSection;
import eu.isas.peptideshaker.export.sections.PsPsmSection;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.ReporterExportFeature;
import eu.isas.reporter.export.report.ReporterReportStyle;
import eu.isas.reporter.export.report.export_features.ReporterPsmFeatures;
import eu.isas.reporter.settings.ReporterSettings;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.SpectrumQuantificationDetails;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the PSM level quantification export features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterPsmSection {

    /**
     * The features to export.
     */
    private ArrayList<PsPsmFeature> psmFeatures = new ArrayList<PsPsmFeature>();
    /**
     * The features to export.
     */
    private ArrayList<PsIdentificationAlgorithmMatchesFeature> identificationAlgorithmMatchesFeatures = new ArrayList<PsIdentificationAlgorithmMatchesFeature>();
    /**
     * The quantification features to export.
     */
    private ArrayList<ReporterExportFeature> quantificationFeatures = new ArrayList<ReporterExportFeature>();
    /**
     * The fragment subsection if needed.
     */
    private PsFragmentSection fragmentSection = null;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private boolean header;
    /**
     * The writer used to send the output to file.
     */
    private ExportWriter writer;
    /**
     * Style for the reporter output.
     */
    private ReporterReportStyle reporterStyle;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public ReporterPsmSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof ReporterPsmFeatures) {
                quantificationFeatures.add((ReporterExportFeature) exportFeature);
            } else if (exportFeature instanceof PsPsmFeature) {
                psmFeatures.add((PsPsmFeature) exportFeature);
            } else if (exportFeature instanceof PsIdentificationAlgorithmMatchesFeature) {
                identificationAlgorithmMatchesFeatures.add((PsIdentificationAlgorithmMatchesFeature) exportFeature);
            } else if (exportFeature instanceof PsFragmentFeature) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        if (!fragmentFeatures.isEmpty()) {
            fragmentSection = new PsFragmentSection(fragmentFeatures, indexes, header, writer);
        }
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
        if (writer instanceof ExcelWriter) {
            reporterStyle = ReporterReportStyle.getReportStyle((ExcelWriter) writer);
        }
    }

    /**
     * Writes the desired section.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param quantificationFeaturesGenerator the quantification features
     * generator containing the quantification information
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param reporterSettings the reporter settings
     * @param identificationParameters the identification parameters
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an error occurred while transforming the ratios
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, SequenceProvider sequenceProvider, 
            ProteinDetailsProvider proteinDetailsProvider, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, 
            ReporterSettings reporterSettings, IdentificationParameters identificationParameters, long[] keys, String linePrefix, int nSurroundingAA, boolean validatedOnly, 
            boolean decoys, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader(reporterIonQuantification);
        }

        int line = 1;
        
        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.length);
        }

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(keys, waitingHandler);
        SpectrumMatch spectrumMatch;

        while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            String spectrumKey = spectrumMatch.getSpectrumKey();

            psParameter = (PSParameter) spectrumMatch.getUrParam(psParameter);

            if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                if (decoys || peptideAssumption == null || !PeptideUtils.isDecoy(peptideAssumption.getPeptide(), sequenceProvider)) {

                    boolean first = true;

                    if (indexes) {
                        if (linePrefix != null) {
                            writer.write(linePrefix);
                        }
                        writer.write(line + "");
                        first = false;
                    }
                    for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : identificationAlgorithmMatchesFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        String feature;
                        if (peptideAssumption != null) {
                            peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                            
                            feature = PsIdentificationAlgorithmMatchesSection.getPeptideAssumptionFeature(identification, identificationFeaturesGenerator, sequenceProvider, 
                                    proteinDetailsProvider, identificationParameters, linePrefix, nSurroundingAA, peptideAssumption, spectrumKey, psParameter, 
                                    identificationAlgorithmMatchesFeature, waitingHandler);
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                            feature = PsIdentificationAlgorithmMatchesSection.getTagAssumptionFeature(identification, identificationFeaturesGenerator, identificationParameters, linePrefix, 
                                    tagAssumption, spectrumKey, psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                        } else {
                            throw new IllegalArgumentException("No best match found for spectrum " + spectrumMatch.getKey() + ".");
                        }
                        writer.write(feature);
                    }
                    for (PsPsmFeature psmFeature : psmFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        writer.write(PsPsmSection.getFeature(identification, identificationFeaturesGenerator,
                                identificationParameters, linePrefix, spectrumMatch, psParameter, psmFeature, validatedOnly, decoys, waitingHandler));
                    }
                    ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
                    Collections.sort(sampleIndexes);
                    for (ExportFeature exportFeature : quantificationFeatures) {
                        ReporterPsmFeatures psmFeature = (ReporterPsmFeatures) exportFeature;
                        if (psmFeature.hasChannels()) {
                            for (String sampleIndex : sampleIndexes) {
                                if (!first) {
                                    writer.addSeparator();
                                } else {
                                    first = false;
                                }
                                writer.write(getFeature(identification, quantificationFeaturesGenerator, reporterIonQuantification, reporterSettings, spectrumMatch.getKey(), psmFeature, sampleIndex), reporterStyle);
                            }
                        } else {
                            if (!first) {
                                writer.addSeparator();
                            } else {
                                first = false;
                            }
                            writer.write(getFeature(identification, quantificationFeaturesGenerator, reporterIonQuantification, reporterSettings, spectrumMatch.getKey(), psmFeature, ""), reporterStyle);
                        }
                    }
                    writer.newLine();
                    if (fragmentSection != null) {
                        String fractionPrefix = "";
                        if (linePrefix != null) {
                            fractionPrefix += linePrefix;
                        }
                        fractionPrefix += line + ".";
                        if (spectrumMatch.getBestPeptideAssumption() != null) {
                            fragmentSection.writeSection(spectrumKey, spectrumMatch.getBestPeptideAssumption(), sequenceProvider, identificationParameters, fractionPrefix, null);
                        } else if (spectrumMatch.getBestTagAssumption() != null) {
                            fragmentSection.writeSection(spectrumKey, spectrumMatch.getBestTagAssumption(), sequenceProvider, identificationParameters, fractionPrefix, null);
                        }
                    }
                    line++;
                }
            }
        }
    }

    /**
     * Returns the report component corresponding to a feature at a given
     * channel.
     *
     * @param identification the identification of the project
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param reporterSettings the reporter settings
     * @param matchKey the match key
     * @param psmFeatures the PSM feature to export
     * @param sampleIndex the index of the sample in case the feature is channel
     * dependent, ignored otherwise
     *
     * @return the report component corresponding to a feature at a given
     * channel
     *
     * @throws java.sql.SQLException exception thrown whenever an error occurred
     * while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown
     * whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred
     */
    public static String getFeature(Identification identification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, ReporterSettings reporterSettings, long matchKey, ReporterPsmFeatures psmFeatures, String sampleIndex) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        
        SpectrumMatch spectrumMatch = identification.getSpectrumMatch(matchKey);
        String spectrumKey = spectrumMatch.getSpectrumKey();
        
        switch (psmFeatures) {
            case raw_ratio:
                PsmQuantificationDetails psmDetails = quantificationFeaturesGenerator.getPSMQuantificationDetails(matchKey);
                return psmDetails.getRawRatio(sampleIndex).toString();
            case ratio:
                psmDetails = quantificationFeaturesGenerator.getPSMQuantificationDetails(matchKey);
                return psmDetails.getRatio(sampleIndex, reporterIonQuantification.getNormalizationFactors()).toString();
            case reporter_intensity:
                SpectrumQuantificationDetails spectrumDetails = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(reporterIonQuantification, reporterSettings.getReporterIonSelectionSettings(), spectrumKey);
                IonMatch ionMatch = spectrumDetails.getRepoterMatch(sampleIndex);
                if (ionMatch == null) {
                    return "";
                }
                return ionMatch.peak.intensity + "";
            case reporter_mz:
                spectrumDetails = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(reporterIonQuantification, reporterSettings.getReporterIonSelectionSettings(), spectrumKey);
                ionMatch = spectrumDetails.getRepoterMatch(sampleIndex);
                if (ionMatch == null) {
                    return "";
                }
                return ionMatch.peak.mz + "";
            case deisotoped_intensity:
                spectrumDetails = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(reporterIonQuantification, reporterSettings.getReporterIonSelectionSettings(), spectrumKey);
                return spectrumDetails.getDeisotopedIntensity(sampleIndex).toString();
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the header of this section.
     *
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     *
     * @throws java.io.IOException exception thrown whenever an error occurred
     * while interacting with a file
     */
    public void writeHeader(ReporterIonQuantification reporterIonQuantification) throws IOException {

        boolean needSecondLine = false;
        ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

        boolean firstColumn = true;
        if (indexes) {
            writer.writeHeaderText("");
            writer.addSeparator();
        }
        for (ExportFeature exportFeature : identificationAlgorithmMatchesFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.write(exportFeature.getTitle());
        }
        for (ExportFeature exportFeature : psmFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.write(exportFeature.getTitle());
        }
        for (ReporterExportFeature exportFeature : quantificationFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle(), reporterStyle);
            if (exportFeature.hasChannels()) {
                for (int i = 1; i < sampleIndexes.size(); i++) {
                    if (firstColumn) {
                        firstColumn = false;
                    } else {
                        writer.addSeparator();
                    }
                    writer.writeHeaderText(" ", reporterStyle); // Space used for the excel style
                }
                needSecondLine = true;
            }
        }
        if (needSecondLine) {
            writer.newLine();
            firstColumn = true;
            if (indexes) {
                writer.writeHeaderText("");
                writer.addSeparator();
            }
            for (ExportFeature exportFeature : identificationAlgorithmMatchesFeatures) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.writeHeaderText("");
                    writer.addSeparator();
                }
            }
            for (ExportFeature exportFeature : psmFeatures) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.writeHeaderText("");
                    writer.addSeparator();
                }
            }
            for (ReporterExportFeature exportFeature : quantificationFeatures) {
                if (exportFeature.hasChannels()) {
                    for (String sampleIndex : sampleIndexes) {
                        if (firstColumn) {
                            firstColumn = false;
                        } else {
                            writer.addSeparator();
                        }
                        writer.writeHeaderText(reporterIonQuantification.getSample(sampleIndex), reporterStyle);
                    }
                } else {
                    if (firstColumn) {
                        firstColumn = false;
                    } else {
                        writer.writeHeaderText("", reporterStyle);
                        writer.addSeparator();
                    }
                }
            }
        }
        writer.newLine();
    }
}
