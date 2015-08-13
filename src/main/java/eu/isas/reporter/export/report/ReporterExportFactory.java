package eu.isas.reporter.export.report;

import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.SerializationUtils;
import com.compomics.util.io.export.ExportFactory;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.io.export.writers.ExcelWriter;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.PSExportFactory;
import eu.isas.peptideshaker.export.PsExportStyle;
import eu.isas.peptideshaker.export.exportfeatures.PsAnnotationFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsInputFilterFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsProjectFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPtmScoringFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsSearchFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsSpectrumCountingFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsValidationFeature;
import eu.isas.peptideshaker.export.sections.PsAnnotationSection;
import eu.isas.peptideshaker.export.sections.PsInputFilterSection;
import eu.isas.peptideshaker.export.sections.PsProjectSection;
import eu.isas.peptideshaker.export.sections.PsPtmScoringSection;
import eu.isas.peptideshaker.export.sections.PsSearchParametersSection;
import eu.isas.peptideshaker.export.sections.PsSpectrumCountingSection;
import eu.isas.peptideshaker.export.sections.PsValidationSection;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.export_features.ReporterPeptideFeature;
import eu.isas.reporter.export.report.export_features.ReporterProteinFeatures;
import eu.isas.reporter.export.report.export_features.ReporterPsmFeatures;
import eu.isas.reporter.export.report.sections.ReporterPeptideSection;
import eu.isas.reporter.export.report.sections.ReporterProteinSection;
import eu.isas.reporter.export.report.sections.ReporterPsmSection;
import eu.isas.reporter.myparameters.ReporterSettings;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The reporter export factory manages the reports available from Reporter.
 *
 * @author Marc Vaudel
 */
public class ReporterExportFactory implements ExportFactory {

    /**
     * The instance of the factory.
     */
    private static ReporterExportFactory instance = null;
    /**
     * User defined factory containing the user schemes.
     */
    private static String SERIALIZATION_FILE = System.getProperty("user.home") + "/.reporter/exportFactory.cus";
    /**
     * The user export schemes.
     */
    private HashMap<String, ExportScheme> userSchemes = new HashMap<String, ExportScheme>();
    /**
     * Sorted list of the implemented reports.
     */
    private ArrayList<String> implementedReports = null;

    /**
     * Constructor.
     */
    private ReporterExportFactory() {
    }

    /**
     * Static method to get the instance of the factory.
     *
     * @return the instance of the factory
     */
    public static ReporterExportFactory getInstance() {
        if (instance == null) {
            try {
                File savedFile = new File(SERIALIZATION_FILE);
                instance = (ReporterExportFactory) SerializationUtils.readObject(savedFile);
            } catch (Exception e) {
                instance = new ReporterExportFactory();
                try {
                    instance.saveFactory();
                } catch (IOException ioe) {
                    // cancel save
                    ioe.printStackTrace();
                }
            }
        }
        return instance;
    }

    /**
     * Saves the factory in the user folder.
     *
     * @throws IOException exception thrown whenever an error occurred while
     * saving the ptmFactory
     */
    public void saveFactory() throws IOException {
        File factoryFile = new File(SERIALIZATION_FILE);
        if (!factoryFile.getParentFile().exists()) {
            factoryFile.getParentFile().mkdir();
        }
        SerializationUtils.writeObject(instance, factoryFile);
    }

    /**
     * Returns a list of the name of the available user schemes.
     *
     * @return a list of the implemented user schemes
     */
    public ArrayList<String> getUserSchemesNames() {
        return new ArrayList<String>(userSchemes.keySet());
    }

    @Override
    public ExportScheme getExportScheme(String schemeName) {
        ExportScheme exportScheme = userSchemes.get(schemeName);
        if (exportScheme == null) {
            exportScheme = getDefaultExportSchemes().get(schemeName);
        }
        return exportScheme;
    }

    @Override
    public void removeExportScheme(String schemeName) {
        userSchemes.remove(schemeName);
    }

    @Override
    public void addExportScheme(ExportScheme exportScheme) {
        userSchemes.put(exportScheme.getName(), exportScheme);
    }

    @Override
    public ArrayList<String> getImplementedSections() {
        ArrayList<String> result = new ArrayList<String>();
        result.add(PsAnnotationFeature.type);
        result.add(PsInputFilterFeature.type);
        result.add(ReporterProteinFeatures.type);
        result.add(ReporterPeptideFeature.type);
        result.add(ReporterPsmFeatures.type);
        result.add(PsFragmentFeature.type);
        result.add(PsProjectFeature.type);
        result.add(PsPtmScoringFeature.type);
        result.add(PsSearchFeature.type);
        result.add(PsSpectrumCountingFeature.type);
        result.add(PsValidationFeature.type);
        return result;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(String sectionName, boolean includeSubFeatures) {
        if (sectionName.equals(PsAnnotationFeature.type)) {
            return PsAnnotationFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsInputFilterFeature.type)) {
            return PsInputFilterFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(ReporterPeptideFeature.type)) {
            return ReporterPeptideFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsProjectFeature.type)) {
            return PsProjectFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(ReporterProteinFeatures.type)) {
            return ReporterProteinFeatures.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(ReporterPsmFeatures.type)) {
            return ReporterPsmFeatures.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsPtmScoringFeature.type)) {
            return PsPtmScoringFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsSearchFeature.type)) {
            return PsSearchFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsSpectrumCountingFeature.type)) {
            return PsSpectrumCountingFeature.values()[0].getExportFeatures(includeSubFeatures);
        } else if (sectionName.equals(PsValidationFeature.type)) {
            return PsValidationFeature.values()[0].getExportFeatures(includeSubFeatures);
        }
        return new ArrayList<ExportFeature>();
    }

    /**
     * Returns a list of the default export schemes.
     *
     * @return a list of the default export schemes
     */
    public ArrayList<String> getDefaultExportSchemesNames() {
        ArrayList<String> result = new ArrayList<String>(getDefaultExportSchemes().keySet());
        Collections.sort(result);
        return result;
    }

    /**
     * Writes the desired export in text format. If an argument is not needed,
     * provide null (at your own risks).
     *
     * @param exportScheme the scheme of the export
     * @param destinationFile the destination file
     * @param exportFormat the export format
     * @param experiment the experiment corresponding to this project (mandatory
     * for the Project section)
     * @param sample the sample of the project (mandatory for the Project
     * section)
     * @param replicateNumber the replicate number of the project (mandatory for
     * the Project section)
     * @param projectDetails the project details (mandatory for the Project
     * section)
     * @param identification the identification (mandatory for the Protein,
     * Peptide and PSM sections)
     * @param identificationFeaturesGenerator the identification features
     * generator (mandatory for the Protein, Peptide and PSM sections)
     * @param quantificationFeaturesGenerator the object generating the
     * quantification features
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param reporterSettings the reporter settings
     * @param shotgunProtocol the shotgun protocol
     * @param identificationParameters the identification parameters
     * @param proteinKeys the protein keys to export (mandatory for the Protein
     * section)
     * @param peptideKeys the peptide keys to export (mandatory for the Peptide
     * section)
     * @param psmKeys the keys of the PSMs to export (mandatory for the PSM
     * section)
     * @param proteinMatchKey the protein match key when exporting peptides from
     * a single protein match (optional for the Peptide sections)
     * @param nSurroundingAA the number of surrounding amino acids to export
     * (mandatory for the Peptide section)
     * @param spectrumCountingPreferences the spectrum counting preferences
     * (mandatory for the spectrum counting section)
     * @param waitingHandler the waiting handler
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws IOException thrown if an IOException
     * @throws ClassNotFoundException thrown if a ClassNotFoundException
     * @throws InterruptedException thrown if an InterruptedException
     * @throws uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException thrown if an
     * MzMLUnmarshallerException occurs
     * @throws org.apache.commons.math.MathException thrown if a MathException occurs
     */
    public static void writeExport(ExportScheme exportScheme, File destinationFile, ExportFormat exportFormat, String experiment, String sample, int replicateNumber,
            ProjectDetails projectDetails, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, ReporterSettings reporterSettings,
            ShotgunProtocol shotgunProtocol, IdentificationParameters identificationParameters, ArrayList<String> proteinKeys, ArrayList<String> peptideKeys, ArrayList<String> psmKeys,
            String proteinMatchKey, int nSurroundingAA, SpectrumCountingPreferences spectrumCountingPreferences, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException, MathException {

        ExportWriter exportWriter = ExportWriter.getExportWriter(exportFormat, destinationFile, exportScheme.getSeparator(), exportScheme.getSeparationLines());
        if (exportWriter instanceof ExcelWriter) {
            ExcelWriter excelWriter = (ExcelWriter) exportWriter;
            PsExportStyle exportStyle = PsExportStyle.getReportStyle(excelWriter);
            excelWriter.setWorkbookStyle(exportStyle);
        }

        exportWriter.writeMainTitle(exportScheme.getMainTitle());

        for (String sectionName : exportScheme.getSections()) {
            if (exportScheme.isIncludeSectionTitles()) {
                exportWriter.startNewSection(sectionName);
            } else {
                exportWriter.startNewSection();
            }
            if (sectionName.equals(PsAnnotationFeature.type)) {
                PsAnnotationSection section = new PsAnnotationSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identificationParameters.getAnnotationPreferences(), waitingHandler);
            } else if (sectionName.equals(PsInputFilterFeature.type)) {
                PsInputFilterSection section = new PsInputFilterSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identificationParameters.getPeptideAssumptionFilter(), waitingHandler);
            } else if (sectionName.equals(ReporterPeptideFeature.type)) {
                ReporterPeptideSection section = new ReporterPeptideSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identification, identificationFeaturesGenerator, quantificationFeaturesGenerator, reporterIonQuantification, reporterSettings, shotgunProtocol, identificationParameters, peptideKeys, nSurroundingAA, "", exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(PsProjectFeature.type)) {
                PsProjectSection section = new PsProjectSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(experiment, sample, replicateNumber, projectDetails, waitingHandler);
            } else if (sectionName.equals(ReporterProteinFeatures.type)) {
                ReporterProteinSection section = new ReporterProteinSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identification, identificationFeaturesGenerator, quantificationFeaturesGenerator, reporterIonQuantification, reporterSettings, shotgunProtocol, identificationParameters, psmKeys, nSurroundingAA, exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(ReporterPsmFeatures.type)) {
                ReporterPsmSection section = new ReporterPsmSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identification, identificationFeaturesGenerator, quantificationFeaturesGenerator, reporterIonQuantification, reporterSettings, shotgunProtocol, identificationParameters, psmKeys, "", exportScheme.isValidatedOnly(), exportScheme.isIncludeDecoy(), waitingHandler);
            } else if (sectionName.equals(PsPtmScoringFeature.type)) {
                PsPtmScoringSection section = new PsPtmScoringSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identificationParameters.getPtmScoringPreferences(), waitingHandler);
            } else if (sectionName.equals(PsSearchFeature.type)) {
                PsSearchParametersSection section = new PsSearchParametersSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(identificationParameters.getSearchParameters(), waitingHandler);
            } else if (sectionName.equals(PsSpectrumCountingFeature.type)) {
                PsSpectrumCountingSection section = new PsSpectrumCountingSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                section.writeSection(spectrumCountingPreferences, waitingHandler);
            } else if (sectionName.equals(PsValidationFeature.type)) {
                PsValidationSection section = new PsValidationSection(exportScheme.getExportFeatures(sectionName), exportScheme.isIndexes(), exportScheme.isHeader(), exportWriter);
                PSMaps psMaps = new PSMaps();
                psMaps = (PSMaps) identification.getUrParam(psMaps);
                section.writeSection(psMaps, waitingHandler);
            } else {
                throw new UnsupportedOperationException("Section " + sectionName + " not implemented.");
            }
        }

        exportWriter.close();
    }

    /**
     * Writes the documentation related to a report.
     *
     * @param exportScheme the export scheme of the report
     * @param exportFormat the export format chosen by the user
     * @param destinationFile the destination file where to write the
     * documentation
     *
     * @throws IOException if an IOException occurs
     */
    public static void writeDocumentation(ExportScheme exportScheme, ExportFormat exportFormat, File destinationFile) throws IOException {

        ExportWriter exportWriter = ExportWriter.getExportWriter(exportFormat, destinationFile, exportScheme.getSeparator(), exportScheme.getSeparationLines());
        if (exportWriter instanceof ExcelWriter) {
            ExcelWriter excelWriter = (ExcelWriter) exportWriter;
            PsExportStyle exportStyle = PsExportStyle.getReportStyle(excelWriter); //@TODO use another style?
            excelWriter.setWorkbookStyle(exportStyle);
        }

        String mainTitle = exportScheme.getMainTitle();
        if (mainTitle != null) {
            exportWriter.writeMainTitle(mainTitle);
        }
        for (String sectionName : exportScheme.getSections()) {
            exportWriter.startNewSection(sectionName);
            if (exportScheme.isIncludeSectionTitles()) {
                exportWriter.write(sectionName);
                exportWriter.newLine();
            }
            for (ExportFeature exportFeature : exportScheme.getExportFeatures(sectionName)) {
                exportWriter.write(exportFeature.getTitle());
                exportWriter.addSeparator();
                exportWriter.write(exportFeature.getDescription());
                exportWriter.newLine();
            }
        }
        exportWriter.close();
    }

    /**
     * Writes section separation lines using the given writer.
     *
     * @param writer the writer
     * @param nSeparationLines the number of separation lines to write
     * @throws IOException
     */
    private static void writeSeparationLines(BufferedWriter writer, int nSeparationLines) throws IOException {
        for (int i = 1; i <= nSeparationLines; i++) {
            writer.newLine();
        }
    }

    /**
     * Returns the list of implemented reports as command line option.
     *
     * @return the list of implemented reports
     */
    public String getCommandLineOptions() {
        setUpReportList();
        String options = "";
        for (int i = 0; i < implementedReports.size(); i++) {
            if (!options.equals("")) {
                options += ", ";
            }
            options += i + ": " + implementedReports.get(i);
        }
        return options;
    }

    /**
     * Returns the default file name for the export of a report based on the
     * project details.
     *
     * @param experiment the experiment of the project
     * @param sample the sample of the project
     * @param replicate the replicate number
     * @param exportName the name of the report type
     * @return the default file name for the export
     */
    public static String getDefaultReportName(String experiment, String sample, int replicate, String exportName) {
        return experiment + "_" + sample + "_" + replicate + "_" + exportName + ".txt";
    }

    /**
     * Returns the default file name for the export of the documentation of the
     * given report export type.
     *
     * @param exportName the export name
     * @return the default file name for the export
     */
    public static String getDefaultDocumentation(String exportName) {
        return exportName + "_documentation.txt";
    }

    /**
     * Returns the export type based on the number used in command line.
     *
     * @param commandLine the number used in command line option. See
     * getCommandLineOptions().
     * @return the corresponding export name
     */
    public String getExportTypeFromCommandLineOption(int commandLine) {
        if (implementedReports == null) {
            setUpReportList();
        }
        if (commandLine >= implementedReports.size()) {
            throw new IllegalArgumentException("Unrecognized report type: " + commandLine + ". Available reports are: " + getCommandLineOptions() + ".");
        }
        return implementedReports.get(commandLine);
    }

    /**
     * Initiates the sorted list of implemented reports.
     */
    private void setUpReportList() {
        implementedReports = new ArrayList<String>();
        implementedReports.addAll(getDefaultExportSchemesNames());
        ArrayList<String> userReports = new ArrayList<String>(userSchemes.keySet());
        Collections.sort(userReports);
        implementedReports.addAll(userReports);
    }

    /**
     * Returns the default schemes available. The default schemes are here the
     * PeptideShaker default schemes with ratios.
     *
     * @return a list containing the default schemes
     */
    private static HashMap<String, ExportScheme> getDefaultExportSchemes() {

        HashMap<String, ExportScheme> defaultSchemes = new HashMap<String, ExportScheme>();

        for (String schemeName : PSExportFactory.getDefaultExportSchemesNames()) {
            // Add ratios to the default PeptideShaker reports
            ExportScheme exportScheme = PSExportFactory.getDefaultExportScheme(schemeName);
            for (String section : exportScheme.getSections()) {
                boolean protein = false,
                        peptide = false,
                        psm = false;
                for (ExportFeature exportFeature : exportScheme.getExportFeatures(section)) {
                    if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PsProteinFeature) {
                        protein = true;
                    } else if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature) {
                        peptide = true;
                    } else if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature) {
                        psm = true;
                    }
                }
                if (protein) {
                    exportScheme.addExportFeature(section, ReporterProteinFeatures.ratio);
                }
                if (peptide) {
                    exportScheme.addExportFeature(section, ReporterPeptideFeature.raw_ratio);
                    exportScheme.addExportFeature(section, ReporterPeptideFeature.normalized_ratio);
                }
                if (psm) {
                    exportScheme.addExportFeature(section, ReporterPsmFeatures.reporter_mz);
                    exportScheme.addExportFeature(section, ReporterPsmFeatures.reporter_intensity);
                    exportScheme.addExportFeature(section, ReporterPsmFeatures.deisotoped_intensity);
                    exportScheme.addExportFeature(section, ReporterPsmFeatures.ratio);
                }
            }
            // rename the PSM, Peptide and protein sections
            String psSection = eu.isas.peptideshaker.export.exportfeatures.PsProteinFeature.type;
            if (exportScheme.getSections().contains(psSection)) {
                exportScheme.setExportFeatures(ReporterProteinFeatures.type, exportScheme.getExportFeatures(psSection));
                exportScheme.removeSection(psSection);
            }
            psSection = eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature.type;
            if (exportScheme.getSections().contains(psSection)) {
                exportScheme.setExportFeatures(ReporterPeptideFeature.type, exportScheme.getExportFeatures(psSection));
                exportScheme.removeSection(psSection);
            }
            psSection = eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature.type;
            if (exportScheme.getSections().contains(psSection)) {
                exportScheme.setExportFeatures(ReporterPsmFeatures.type, exportScheme.getExportFeatures(psSection));
                exportScheme.removeSection(psSection);
            }
            defaultSchemes.put(schemeName, exportScheme);
        }
        return defaultSchemes;
    }

    /**
     * Returns the file where to save the implemented export schemes.
     *
     * @return the file where to save the implemented export schemes
     */
    public static String getSerializationFile() {
        return SERIALIZATION_FILE;
    }

    /**
     * Returns the folder where to save the implemented export schemes.
     *
     * @return the folder where to save the implemented export schemes
     */
    public static String getSerializationFolder() {
        File tempFile = new File(getSerializationFile());
        return tempFile.getParent();
    }

    /**
     * Sets the folder where to save the implemented export schemes.
     *
     * @param serializationFolder the folder where to save the implemented
     * export schemes
     */
    public static void setSerializationFolder(String serializationFolder) {
        ReporterExportFactory.SERIALIZATION_FILE = serializationFolder + "/reporter_exportFactory.cus";
    }
}
