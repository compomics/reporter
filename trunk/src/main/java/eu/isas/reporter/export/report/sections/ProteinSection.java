/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.reporter.export.report.sections;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.sections.PeptideSection;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.export_features.PeptideFeatures;
import eu.isas.reporter.export.report.export_features.ProteinFeatures;
import eu.isas.reporter.export.report.export_features.PsmFeatures;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
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
    private ArrayList<ExportFeature> quantificationFeatures = new ArrayList<ExportFeature>();
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
                quantificationFeatures.add(exportFeature);
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
     * @param quantificationFeaturesGenerator the quantification features generator containing the quantification information
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
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, int nSurroundingAas, WaitingHandler waitingHandler) 
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
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
            for (ExportFeature exportFeature : quantificationFeatures) {
                ProteinFeatures tempProteinFeatures = (ProteinFeatures) exportFeature;
                writer.write(getFeature(quantificationFeaturesGenerator, proteinKey, tempProteinFeatures) + separator);
            }
            writer.newLine();
            if (peptideSection != null) {
                peptideSection.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, proteinMatch.getPeptideMatches(), nSurroundingAas, line + ".", null);
            }
            line++;
        }
    }
    
    public static String getFeature(QuantificationFeaturesGenerator quantificationFeaturesGenerator, String proteinKey, ProteinFeatures proteinFeatures) {
        switch (proteinFeatures) {
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the header of the protein section.
     *
     * @throws IOException
     */
    public void writeHeader() throws IOException {
        if (indexes) {
            writer.write(separator);
        }
        boolean firstColumn = true;
        for (ExportFeature exportFeature : identificationFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.write(separator);
            }
            writer.write(exportFeature.getTitle(separator));
        }
        for (ExportFeature exportFeature : quantificationFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.write(separator);
            }
            writer.write(exportFeature.getTitle(separator));
        }
        writer.newLine();
    }
}
