package eu.isas.reporter.export.report.sections;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.sections.PsmSection;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.export.report.export_features.PeptideFeatures;
import eu.isas.reporter.export.report.export_features.PsmFeatures;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the peptide related quantification export features.
 *
 * @author Marc Vaudel
 */
public class PeptideSection {
    
    /**
     * The peptide identification features to export.
     */
    private ArrayList<ExportFeature> identificationFeatures = new ArrayList<ExportFeature>();
    /**
     * The peptide quantification features to export.
     */
    private ArrayList<ExportFeature> quantificationFeatures = new ArrayList<ExportFeature>();
    /**
     * The psm subsection if needed.
     */
    private PsmSection psmSection = null;
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
     * @param exportFeatures the features to export in this section
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public PeptideSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> psmFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures) {
                identificationFeatures.add(exportFeature);
            } else if (exportFeature instanceof PeptideFeatures) {
                quantificationFeatures.add(exportFeature);
            } else if (exportFeature instanceof PsmFeatures
                    || exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PsmFeatures
                    || exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures) {
                psmFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        if (!psmFeatures.isEmpty()) {
            psmSection = new PsmSection(psmFeatures, separator, indexes, header, writer);
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
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the protein matches to output
     * @param nSurroundingAA the number of surrounding amino acids to export
     * @param linePrefix the line prefix to use.
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
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, int nSurroundingAA, String linePrefix, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader();
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

            peptideMatch = identification.getPeptideMatch(peptideKey);

            if (indexes) {
                if (linePrefix != null) {
                    writer.write(linePrefix);
                }
                writer.write(line + separator);
            }

            for (ExportFeature exportFeature : identificationFeatures) {
                eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures peptideFeature = (eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures) exportFeature;
                writer.write(eu.isas.peptideshaker.export.sections.PeptideSection.getfeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, nSurroundingAA, linePrefix, separator, peptideMatch, psParameter, peptideFeature, waitingHandler) + separator);
            }
            
            for (ExportFeature exportFeature : quantificationFeatures) {
                PeptideFeatures peptideFeature = (PeptideFeatures) exportFeature;
                writer.write(getFeature(peptideFeature) + separator);
            }
            
            writer.newLine();
            if (psmSection != null) {
                String psmSectionPrefix = "";
                if (linePrefix != null) {
                    psmSectionPrefix += linePrefix;
                }
                psmSectionPrefix += line + ".";
                psmSection.writeSection(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, peptideMatch.getSpectrumMatches(), psmSectionPrefix, null);
            }
            line++;
        }
    }
    
    public static String getFeature(PeptideFeatures peptideFeatures) {
        switch (peptideFeatures) {
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the title of the section.
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
