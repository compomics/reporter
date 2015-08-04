package eu.isas.reporter.export.report.sections;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.io.export.writers.ExcelWriter;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsProteinFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.export.sections.PsProteinSection;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.ReporterExportFeature;
import eu.isas.reporter.export.report.ReporterReportStyle;
import eu.isas.reporter.export.report.export_features.ReporterPeptideFeature;
import eu.isas.reporter.export.report.export_features.ReporterProteinFeatures;
import eu.isas.reporter.export.report.export_features.ReporterPsmFeatures;
import eu.isas.reporter.myparameters.ReporterSettings;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the protein related export features.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterProteinSection {

    /**
     * The protein identification features to export.
     */
    private ArrayList<ExportFeature> identificationFeatures = new ArrayList<ExportFeature>();
    /**
     * The protein quantification features to export.
     */
    private ArrayList<ReporterExportFeature> quantificationFeatures = new ArrayList<ReporterExportFeature>();
    /**
     * The peptide subsection if any.
     */
    private ReporterPeptideSection peptideSection = null;
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
     * @param exportFeatures the features to export in this section.
     * ProteinFeatures as main features. If Peptide or protein features are
     * selected, they will be added as sub-sections.
     * @param indexes indicates whether the line index should be written
     * @param header indicates whether the table header should be written
     * @param writer the writer which will write to the file
     */
    public ReporterProteinSection(ArrayList<ExportFeature> exportFeatures, boolean indexes, boolean header, ExportWriter writer) {
        ArrayList<ExportFeature> peptideFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof ReporterProteinFeatures) {
                quantificationFeatures.add((ReporterExportFeature) exportFeature);
            } else if (exportFeature instanceof ReporterPeptideFeature
                    || exportFeature instanceof ReporterPsmFeatures
                    || exportFeature instanceof PsPeptideFeature
                    || exportFeature instanceof PsPsmFeature
                    || exportFeature instanceof PsIdentificationAlgorithmMatchesFeature
                    || exportFeature instanceof PsFragmentFeature) {
                peptideFeatures.add(exportFeature);
            } else if (exportFeature instanceof PsProteinFeature) {
                identificationFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        if (!peptideFeatures.isEmpty()) {
            peptideSection = new ReporterPeptideSection(peptideFeatures, indexes, header, writer);
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
     * @param reporterSettings the reporter settings
     * @param shotgunProtocol the shotgun protocol
     * @param identificationParameters the identification parameters
     * @param keys the keys of the protein matches to output. if null all
     * proteins will be exported.
     * @param nSurroundingAas in case a peptide export is included with
     * surrounding amino-acids, the number of surrounding amino acids to use
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred
     * @throws org.apache.commons.math.MathException exception thrown whenever an error occurred while transforming the ratios
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, ReporterSettings reporterSettings,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ArrayList<String> keys, int nSurroundingAas, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader(reporterIonQuantification);
        }

        if (keys == null) {
            keys = identification.getProteinIdentification();
        }
        int line = 1;
        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.size());
        }

        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(keys, parameters, peptideSection != null, parameters, peptideSection != null, parameters, waitingHandler);
        
        while (proteinMatchesIterator.hasNext()) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }
            
            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinKey = proteinMatch.getKey();

            if (decoys || !ProteinMatch.isDecoy(proteinKey)) {

                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);

                if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                    boolean first = true;

                    if (indexes) {
                        writer.write(line + "");
                        first = false;
                    }

                    for (ExportFeature exportFeature : identificationFeatures) {
                        if (!first) {
                            writer.addSeparator();
                        } else {
                            first = false;
                        }
                        PsProteinFeature tempProteinFeatures = (PsProteinFeature) exportFeature;
                        writer.write(PsProteinSection.getFeature(identificationFeaturesGenerator, shotgunProtocol, identificationParameters,
                                keys, nSurroundingAas, proteinKey, proteinMatch, psParameter, tempProteinFeatures, waitingHandler));
                    }

                    ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
                    Collections.sort(sampleIndexes);

                    for (ExportFeature exportFeature : quantificationFeatures) {
                        ReporterProteinFeatures tempProteinFeatures = (ReporterProteinFeatures) exportFeature;
                        if (tempProteinFeatures.hasChannels()) {
                            for (String sampleIndex : sampleIndexes) {
                                if (!first) {
                                    writer.addSeparator();
                                } else {
                                    first = false;
                                }
                                writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, proteinKey, tempProteinFeatures, sampleIndex, waitingHandler), reporterStyle);
                            }
                        } else {
                            if (!first) {
                                writer.addSeparator();
                            } else {
                                first = false;
                            }
                            writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, proteinKey, tempProteinFeatures, "", waitingHandler), reporterStyle);
                        }
                    }

                    writer.newLine();
                    if (peptideSection != null) {
                        writer.increaseDepth();
                        peptideSection.writeSection(identification, identificationFeaturesGenerator, quantificationFeaturesGenerator, reporterIonQuantification, reporterSettings, 
                                shotgunProtocol, identificationParameters, proteinMatch.getPeptideMatchesKeys(), nSurroundingAas, 
                                line + ".", validatedOnly, decoys, null);
                        writer.decreseDepth();
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
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param proteinKey the protein key
     * @param proteinFeatures the protein feature to export
     * @param sampleIndex the index of the sample in case the feature is channel
     * dependent, ignored otherwise
     * @param waitingHandler the waiting handler
     *
     * @return the report component corresponding to a feature at a given
     * channel
     * 
     * @throws java.sql.SQLException exception thrown whenever an error occurred while interacting with the database
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
     * @throws java.lang.ClassNotFoundException exception thrown whenever an error occurred while deserializing an object
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException exception thrown whenever an error occurred while reading an mzML file
     * @throws java.lang.InterruptedException exception thrown whenever a threading error occurred
     */
    public static String getFeature(QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, String proteinKey,
            ReporterProteinFeatures proteinFeatures, String sampleIndex, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (proteinFeatures) {
            case ratio:
                ProteinQuantificationDetails quantificationDetails = quantificationFeaturesGenerator.getProteinMatchQuantificationDetails(proteinKey, waitingHandler);
                return quantificationDetails.getRatio(sampleIndex).toString();
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the header of the protein section.
     *
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     *
     * @throws java.io.IOException exception thrown whenever an error occurred while interacting with a file
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
                writer.writeHeaderText("");
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
                        writer.writeHeaderText(reporterIonQuantification.getSample(sampleIndex).getReference(), reporterStyle);
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
