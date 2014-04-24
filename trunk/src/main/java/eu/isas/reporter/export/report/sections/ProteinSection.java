package eu.isas.reporter.export.report.sections;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.sections.PeptideSection;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.ReporterExportFeature;
import eu.isas.reporter.export.report.export_features.PeptideFeatures;
import eu.isas.reporter.export.report.export_features.ProteinFeatures;
import eu.isas.reporter.export.report.export_features.PsmFeatures;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import java.io.BufferedWriter;
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
public class ProteinSection {

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
    private PeptideSection peptideSection = null;
    /**
     * The separator used to separate columns.
     */
    private String separator;
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
    private BufferedWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section.
     * ProteinFeatures as main features. If Peptide or protein features are
     * selected, they will be added as sub-sections.
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public ProteinSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> peptideFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof ProteinFeatures) {
                quantificationFeatures.add((ReporterExportFeature) exportFeature);
            } else if (exportFeature instanceof PeptideFeatures || exportFeature instanceof PsmFeatures
                    || exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures
                    || exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PsmFeatures
                    || exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures) {
                peptideFeatures.add(exportFeature);
            } else if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures) {
                identificationFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        if (!peptideFeatures.isEmpty()) {
            peptideSection = new PeptideSection(peptideFeatures, separator, indexes, header, writer);
        }
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
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
     * @param keys the keys of the protein matches to output. if null all
     * proteins will be exported.
     * @param nSurroundingAas in case a peptide export is included with
     * surrounding amino-acids, the number of surrounding amino acids to use
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     * @throws org.apache.commons.math.MathException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, int nSurroundingAas, WaitingHandler waitingHandler)
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
        ProteinMatch proteinMatch = null;

        if (peptideSection != null) {
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Loading Peptides. Please Wait...");
                waitingHandler.resetSecondaryProgressCounter();
            }
            identification.loadPeptideMatches(waitingHandler);
            if (waitingHandler != null) {
                waitingHandler.setWaitingText("Loading Peptide Details. Please Wait...");
                waitingHandler.resetSecondaryProgressCounter();
            }
            identification.loadPeptideMatchParameters(psParameter, waitingHandler);
        }

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Proteins. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadProteinMatches(keys, waitingHandler);
        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Protein Details. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadProteinMatchParameters(keys, psParameter, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(keys.size());
        }

        for (String proteinKey : keys) {

            if (waitingHandler != null) {
                if (waitingHandler.isRunCanceled()) {
                    return;
                }
                waitingHandler.increaseSecondaryProgressCounter();
            }

            if (indexes) {
                writer.write(line + separator);
            }

            proteinMatch = identification.getProteinMatch(proteinKey);
            psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);

            for (ExportFeature exportFeature : identificationFeatures) {
                eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures tempProteinFeatures = (eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures) exportFeature;
                writer.write(eu.isas.peptideshaker.export.sections.ProteinSection.getFeature(identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, separator, nSurroundingAas, proteinKey, proteinMatch, psParameter, tempProteinFeatures, waitingHandler) + separator);
            }
            ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
            Collections.sort(sampleIndexes);
            for (ExportFeature exportFeature : quantificationFeatures) {
                ProteinFeatures tempProteinFeatures = (ProteinFeatures) exportFeature;
                if (tempProteinFeatures.hasChannels()) {
                    for (String sampleIndex : sampleIndexes) {
                        writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, proteinKey, tempProteinFeatures, sampleIndex) + separator);
                    }
                } else {
                    writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, proteinKey, tempProteinFeatures, "") + separator);
                }
            }
            writer.newLine();
            if (peptideSection != null) {
                peptideSection.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, proteinMatch.getPeptideMatches(), nSurroundingAas, line + ".", null);
            }
            line++;
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
    public static String getFeature(QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, String proteinKey, ProteinFeatures proteinFeatures, String sampleIndex) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (proteinFeatures) {
            case ratio:
                ProteinQuantificationDetails quantificationDetails = quantificationFeaturesGenerator.getProteinMatchQuantificationDetails(proteinKey);
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
     * @throws IOException
     */
    public void writeHeader(ReporterIonQuantification reporterIonQuantification) throws IOException {

        boolean needSecondLine = false;
        ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

        boolean firstColumn = true;
        if (indexes) {
            writer.write(separator);
        }
        for (ExportFeature exportFeature : identificationFeatures) {
            for (String title : exportFeature.getTitles()) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(title);
            }
        }
        for (ReporterExportFeature exportFeature : quantificationFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.write(separator);
            }
            for (String title : exportFeature.getTitles()) {
                writer.write(title);
                if (exportFeature.hasChannels()) {
                    for (int i = 1; i < sampleIndexes.size(); i++) {
                        writer.write(separator);
                    }
                    needSecondLine = true;
                }
            }
        }
        if (needSecondLine) {
            writer.newLine();
            firstColumn = true;
            if (indexes) {
                writer.write(separator);
            }
            for (ExportFeature exportFeature : identificationFeatures) {
                for (String title : exportFeature.getTitles()) {
                    if (firstColumn) {
                        firstColumn = false;
                    } else {
                        writer.write(separator);
                    }
                }
            }
            for (ReporterExportFeature exportFeature : quantificationFeatures) {
                for (String title : exportFeature.getTitles()) {
                    if (exportFeature.hasChannels()) {
                        for (String sampleIndex : sampleIndexes) {
                            if (firstColumn) {
                                firstColumn = false;
                            } else {
                                writer.write(separator);
                            }
                            writer.write(reporterIonQuantification.getSample(sampleIndex).getReference());
                        }
                    } else {
                        if (firstColumn) {
                            firstColumn = false;
                        } else {
                            writer.write(separator);
                        }
                    }
                }
            }
        }
        writer.newLine();
    }
}
