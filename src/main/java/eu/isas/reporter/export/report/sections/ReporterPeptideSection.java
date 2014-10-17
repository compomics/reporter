package eu.isas.reporter.export.report.sections;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.io.export.writers.ExcelWriter;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.export.sections.PsPeptideSection;
import eu.isas.peptideshaker.export.sections.PsPsmSection;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.ReporterExportFeature;
import eu.isas.reporter.export.report.ReporterReportStyle;
import eu.isas.reporter.export.report.export_features.ReporterPeptideFeature;
import eu.isas.reporter.export.report.export_features.ReporterPsmFeatures;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the peptide related quantification export features.
 *
 * @author Marc Vaudel
 */
public class ReporterPeptideSection {

    /**
     * The peptide identification features to export.
     */
    private ArrayList<ExportFeature> identificationFeatures = new ArrayList<ExportFeature>();
    /**
     * The peptide quantification features to export.
     */
    private ArrayList<ReporterExportFeature> quantificationFeatures = new ArrayList<ReporterExportFeature>();
    /**
     * The PSM subsection if needed.
     */
    private PsPsmSection psmSection = null;
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
    public ReporterPeptideSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> psmFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsPeptideFeature) {
                identificationFeatures.add(exportFeature);
            } else if (exportFeature instanceof ReporterPeptideFeature) {
                quantificationFeatures.add((ReporterExportFeature) exportFeature);
            } else if (exportFeature instanceof ReporterPsmFeatures
                    || exportFeature instanceof PsPsmFeature
                    || exportFeature instanceof PsIdentificationAlgorithmMatchesFeature
                    || exportFeature instanceof PsFragmentFeature) {
                psmFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        if (!psmFeatures.isEmpty()) {
            psmSection = new PsPsmSection(psmFeatures, indexes, header, writer);
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
     * @param quantificationFeaturesGenerator the quantification features
     * generator containing the quantification information
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param keys the keys of the protein matches to output
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, SequenceMatchingPreferences sequenceMatchingPreferences, ArrayList<String> keys, int nSurroundingAA, String linePrefix, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader(reporterIonQuantification);
        }

        if (keys == null) {
            keys = identification.getPeptideIdentification();
        }

        PSParameter psParameter = new PSParameter();
        PeptideMatch peptideMatch;
        int line = 1;

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Peptides. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadPeptideMatches(keys, waitingHandler);
        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Peptide Details. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadPeptideMatchParameters(keys, psParameter, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.size());
        }

        for (String peptideKey : keys) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

            if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                peptideMatch = identification.getPeptideMatch(peptideKey);

                if (decoys || !peptideMatch.getTheoreticPeptide().isDecoy(sequenceMatchingPreferences)) {

                    boolean first = true;

                    if (indexes) {
                        if (linePrefix != null) {
                            writer.write(linePrefix);
                        }
                        writer.write(line + "");
                        first = false;
                    }

                    for (ExportFeature exportFeature : identificationFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        PsPeptideFeature peptideFeature = (PsPeptideFeature) exportFeature;
                        writer.write(PsPeptideSection.getfeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, keys, nSurroundingAA, linePrefix, peptideMatch, psParameter, peptideFeature, validatedOnly, decoys, waitingHandler));
                    }

                    ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
                    Collections.sort(sampleIndexes);
                    for (ExportFeature exportFeature : quantificationFeatures) {
                        ReporterPeptideFeature peptideFeature = (ReporterPeptideFeature) exportFeature;
                        if (peptideFeature.hasChannels()) {
                            for (String sampleIndex : sampleIndexes) {
                                if (!first) {
                                    writer.addSeparator();
                                } else {
                                    first = false;
                                }
                                writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, peptideKey, peptideFeature, sampleIndex), reporterStyle);
                            }
                        } else {
                            if (!first) {
                                writer.addSeparator();
                            } else {
                                first = false;
                            }
                            writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, peptideKey, peptideFeature, ""), reporterStyle);
                        }
                    }
                    writer.newLine();
                    if (psmSection != null) {
                        String psmSectionPrefix = "";
                        if (linePrefix != null) {
                            psmSectionPrefix += linePrefix;
                        }
                        psmSectionPrefix += line + ".";
                        psmSection.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, peptideMatch.getSpectrumMatches(), psmSectionPrefix, validatedOnly, decoys, null);
                    }
                    line++;
                    writer.newLine();
                }
            }  
        }
    }

    /**
     * Returns the report component corresponding to a feature at a given
     * channel.
     *
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param peptideKey the peptide key
     * @param peptideFeatures the peptide feature to export
     * @param sampleIndex the index of the sample in case the feature is channel
     * dependent, ignored otherwise
     *
     * @return the report component corresponding to a feature at a given
     * channel
     *
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static String getFeature(QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, String peptideKey,
            ReporterPeptideFeature peptideFeatures, String sampleIndex) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (peptideFeatures) {
            case raw_ratio:
                PeptideQuantificationDetails quantificationDetails = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideKey);
                return quantificationDetails.getRawRatio(sampleIndex).toString();
            case normalized_ratio:
                quantificationDetails = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideKey);
                return quantificationDetails.getRatio(sampleIndex, reporterIonQuantification).toString();
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the title of the section.
     *
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     *
     * @throws IOException
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
        for (ExportFeature exportFeature : identificationFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.addSeparator();
            }
            writer.writeHeaderText(exportFeature.getTitle());
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
                    writer.writeHeaderText("", reporterStyle);
                    writer.addSeparator();
                }
                needSecondLine = true;
            }
        }
        if (needSecondLine) {
            writer.newLine();
            firstColumn = true;
            if (indexes) {
                writer.addSeparator();
            }
            for (ExportFeature exportFeature : identificationFeatures) {
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
                            writer.writeHeaderText("", reporterStyle);
                            writer.addSeparator();
                        }
                        writer.write(reporterIonQuantification.getSample(sampleIndex).getReference(), reporterStyle);
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
