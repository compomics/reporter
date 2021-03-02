package eu.isas.reporter.export.report;

import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.export.ExportFactory;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.io.export.ExportWriter;
import com.compomics.util.io.export.writers.ExcelWriter;
import com.compomics.util.io.json.JsonMarshaller;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
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
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.export_features.ReporterPeptideFeature;
import eu.isas.reporter.export.report.export_features.ReporterProteinFeatures;
import eu.isas.reporter.export.report.export_features.ReporterPsmFeatures;
import eu.isas.reporter.export.report.sections.ReporterPeptideSection;
import eu.isas.reporter.export.report.sections.ReporterProteinSection;
import eu.isas.reporter.export.report.sections.ReporterPsmSection;
import eu.isas.reporter.settings.ReporterSettings;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.math.MathException;

/**
 * The reporter export factory manages the reports available from Reporter.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterExportFactory implements ExportFactory {

    /**
     * The instance of the factory.
     */
    private static ReporterExportFactory instance = null;
    /**
     * User defined JSON file containing the user schemes.
     */
    private static String JSON_FILE = System.getProperty("user.home") + "/.reporter/exportFactory.cus";
    /**
     * The user export schemes.
     */
    private HashMap<String, ExportScheme> userSchemes = new HashMap<>();
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

                File savedFile = new File(JSON_FILE);
                instance = loadFromFile(savedFile);

            } catch (Exception e) {

                e.getMessage(); // print the message to the error log
                instance = new ReporterExportFactory();

                try {

                    saveFactory(instance);

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
     * @param reporterExportFactory the export factory
     * @throws IOException exception thrown whenever an error occurred while
     * saving the ptmFactory
     */
    public static void saveFactory(ReporterExportFactory reporterExportFactory) throws IOException {

        File factoryFile = new File(JSON_FILE);

        if (!factoryFile.getParentFile().exists()) {
            factoryFile.getParentFile().mkdir();
        }

        JsonMarshaller jsonMarshaller = new JsonMarshaller();
        jsonMarshaller.saveObjectToJson(reporterExportFactory, factoryFile);
    }

    /**
     * Loads an export factory from a file. The file must be an export of the
     * factory in the json format.
     *
     * @param file the file to load
     *
     * @return the export factory saved in file
     *
     * @throws IOException exception thrown whenever an error occurred while
     * loading the file
     */
    public static ReporterExportFactory loadFromFile(File file) throws IOException {

        JsonMarshaller jsonMarshaller = new JsonMarshaller();
        ReporterExportFactory result = (ReporterExportFactory) jsonMarshaller.fromJson(ReporterExportFactory.class, file);

        return result;

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

        ArrayList<String> result = new ArrayList<>();
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

        switch (sectionName) {

            case PsAnnotationFeature.type:
                return PsAnnotationFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsInputFilterFeature.type:
                return PsInputFilterFeature.values()[0].getExportFeatures(includeSubFeatures);

            case ReporterPeptideFeature.type:
                return ReporterPeptideFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsProjectFeature.type:
                return PsProjectFeature.values()[0].getExportFeatures(includeSubFeatures);

            case ReporterProteinFeatures.type:
                return ReporterProteinFeatures.values()[0].getExportFeatures(includeSubFeatures);

            case ReporterPsmFeatures.type:
                return ReporterPsmFeatures.values()[0].getExportFeatures(includeSubFeatures);

            case PsPtmScoringFeature.type:
                return PsPtmScoringFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsSearchFeature.type:
                return PsSearchFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsSpectrumCountingFeature.type:
                return PsSpectrumCountingFeature.values()[0].getExportFeatures(includeSubFeatures);

            case PsValidationFeature.type:
                return PsValidationFeature.values()[0].getExportFeatures(includeSubFeatures);

            default:
                break;
        }

        return new ArrayList<ExportFeature>();
    }

    /**
     * Returns a list of the default export schemes.
     *
     * @return a list of the default export schemes
     */
    public ArrayList<String> getDefaultExportSchemesNames() {

        ArrayList<String> result = new ArrayList<>(getDefaultExportSchemes().keySet());
        Collections.sort(result);

        return result;
    }

    /**
     * Writes the desired export in text format.If an argument is not needed,
     * provide null (at your own risks).
     *
     * @param exportScheme the scheme of the export
     * @param destinationFile the destination file
     * @param exportFormat the export format
     * @param experiment the experiment corresponding to this project (mandatory
     * for the Project section)
     * @param projectDetails the project details (mandatory for the Project
     * section)
     * @param identification the identification (mandatory for the Protein,
     * Peptide and PSM sections)
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param spectrumProvider the spectrum provider
     * @param identificationFeaturesGenerator the identification features
     * generator (mandatory for the Protein, Peptide and PSM sections)
     * @param geneMaps the gene maps
     * @param quantificationFeaturesGenerator the object generating the
     * quantification features
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param reporterSettings the reporter settings
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
     * @param spectrumCountingParameters the spectrum counting preferences
     * (mandatory for the spectrum counting section)
     * @param waitingHandler the waiting handler
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while interacting with the database
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an exception occurred while estimating the theoretical coverage of a
     * protein
     */
    public static void writeExport(
            ExportScheme exportScheme,
            File destinationFile,
            ExportFormat exportFormat,
            String experiment,
            ProjectDetails projectDetails,
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            ProteinDetailsProvider proteinDetailsProvider,
            GeneMaps geneMaps,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            ReporterIonQuantification reporterIonQuantification,
            ReporterSettings reporterSettings,
            IdentificationParameters identificationParameters,
            ArrayList<String> proteinKeys,
            long[] peptideKeys,
            long[] psmKeys,
            String proteinMatchKey,
            int nSurroundingAA,
            SpectrumCountingParameters spectrumCountingParameters,
            WaitingHandler waitingHandler
    ) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MathException {

        ExportWriter exportWriter = ExportWriter.getExportWriter(
                exportFormat,
                destinationFile,
                exportScheme.getSeparator(),
                exportScheme.getSeparationLines(),
                false
        );

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

            switch (sectionName) {

                case PsAnnotationFeature.type:

                    PsAnnotationSection psAnnotationSection = new PsAnnotationSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    psAnnotationSection.writeSection(identificationParameters.getAnnotationParameters(), waitingHandler);

                    break;

                case PsInputFilterFeature.type:

                    PsInputFilterSection psInputFilterSection = new PsInputFilterSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    psInputFilterSection.writeSection(identificationParameters.getPeptideAssumptionFilter(), waitingHandler);

                    break;

                case ReporterPeptideFeature.type:

                    ReporterPeptideSection reporterPeptideSection = new ReporterPeptideSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    reporterPeptideSection.writeSection(
                            identification,
                            identificationFeaturesGenerator,
                            sequenceProvider,
                            spectrumProvider,
                            proteinDetailsProvider,
                            quantificationFeaturesGenerator,
                            reporterIonQuantification,
                            reporterSettings,
                            identificationParameters,
                            peptideKeys,
                            nSurroundingAA,
                            "",
                            exportScheme.isValidatedOnly(),
                            exportScheme.isIncludeDecoy(),
                            waitingHandler
                    );

                    break;

                case PsProjectFeature.type:

                    PsProjectSection psProjectSection = new PsProjectSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    psProjectSection.writeSection(
                            experiment,
                            projectDetails,
                            waitingHandler
                    );

                    break;

                case ReporterProteinFeatures.type:

                    ReporterProteinSection reporterProteinSection = new ReporterProteinSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    reporterProteinSection.writeSection(
                            identification,
                            identificationFeaturesGenerator,
                            sequenceProvider,
                            spectrumProvider,
                            proteinDetailsProvider,
                            geneMaps,
                            quantificationFeaturesGenerator,
                            reporterIonQuantification,
                            reporterSettings,
                            identificationParameters,
                            psmKeys,
                            nSurroundingAA,
                            exportScheme.isValidatedOnly(),
                            exportScheme.isIncludeDecoy(),
                            waitingHandler
                    );

                    break;

                case ReporterPsmFeatures.type:

                    ReporterPsmSection reporterPsmSection = new ReporterPsmSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    reporterPsmSection.writeSection(
                            identification,
                            identificationFeaturesGenerator,
                            sequenceProvider,
                            spectrumProvider,
                            proteinDetailsProvider,
                            quantificationFeaturesGenerator,
                            reporterIonQuantification,
                            reporterSettings,
                            identificationParameters,
                            psmKeys,
                            "",
                            nSurroundingAA,
                            exportScheme.isValidatedOnly(),
                            exportScheme.isIncludeDecoy(),
                            waitingHandler
                    );

                    break;

                case PsPtmScoringFeature.type:

                    PsPtmScoringSection psPtmScoringSection = new PsPtmScoringSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    psPtmScoringSection.writeSection(
                            identificationParameters.getModificationLocalizationParameters(),
                            waitingHandler
                    );

                    break;

                case PsSearchFeature.type:

                    PsSearchParametersSection psSearchParametersSection = new PsSearchParametersSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    psSearchParametersSection.writeSection(
                            identificationParameters.getSearchParameters(),
                            projectDetails,
                            waitingHandler
                    );

                    break;

                case PsSpectrumCountingFeature.type:

                    PsSpectrumCountingSection psSpectrumCountingSection = new PsSpectrumCountingSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    psSpectrumCountingSection.writeSection(
                            spectrumCountingParameters,
                            waitingHandler
                    );

                    break;

                case PsValidationFeature.type:

                    PsValidationSection psValidationSection = new PsValidationSection(
                            exportScheme.getExportFeatures(sectionName),
                            exportScheme.isIndexes(),
                            exportScheme.isHeader(),
                            exportWriter
                    );

                    PSMaps psMaps = new PSMaps();
                    psMaps = (PSMaps) identification.getUrParam(psMaps);
                    psValidationSection.writeSection(psMaps, identificationParameters, waitingHandler);

                    break;

                default:

                    throw new UnsupportedOperationException(
                            "Section "
                            + sectionName
                            + " not implemented."
                    );

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
    public static void writeDocumentation(
            ExportScheme exportScheme,
            ExportFormat exportFormat,
            File destinationFile
    ) throws IOException {

        ExportWriter exportWriter = ExportWriter.getExportWriter(
                exportFormat,
                destinationFile,
                exportScheme.getSeparator(),
                exportScheme.getSeparationLines(),
                false
        );

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
    private static void writeSeparationLines(
            BufferedWriter writer,
            int nSeparationLines
    ) throws IOException {

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
     * project details
     *
     * @param experiment the experiment of the project
     * @param exportName the name of the report type
     *
     * @return the default file name for the export
     */
    public static String getDefaultReportName(String experiment, String exportName) {
        return experiment + "_" + exportName + ".txt";
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
            throw new IllegalArgumentException(
                    "Unrecognized report type: "
                    + commandLine
                    + ". Available reports are: "
                    + getCommandLineOptions()
                    + "."
            );
        }

        return implementedReports.get(commandLine);
    }

    /**
     * Initiates the sorted list of implemented reports.
     */
    private void setUpReportList() {

        implementedReports = new ArrayList<String>();
        implementedReports.addAll(getDefaultExportSchemesNames());
        ArrayList<String> userReports = new ArrayList<>(userSchemes.keySet());
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

        HashMap<String, ExportScheme> defaultSchemes = new HashMap<>();

        for (String schemeName : PSExportFactory.getDefaultExportSchemesNames()) {

            // Add ratios to the default PeptideShaker reports
            ExportScheme exportScheme = PSExportFactory.getDefaultExportScheme(schemeName);

            for (String section : exportScheme.getSections()) {

                boolean protein = false, peptide = false, psm = false;

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

                    exportScheme.addExportFeature(section, ReporterProteinFeatures.raw_unique_ratio);
                    exportScheme.addExportFeature(section, ReporterProteinFeatures.raw_shared_ratio);
                    exportScheme.addExportFeature(section, ReporterProteinFeatures.raw_ratio);
                    exportScheme.addExportFeature(section, ReporterProteinFeatures.unique_ratio);
                    exportScheme.addExportFeature(section, ReporterProteinFeatures.shared_ratio);
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
    public static String getJsonFile() {
        return JSON_FILE;
    }

    /**
     * Returns the folder where to save the implemented export schemes.
     *
     * @return the folder where to save the implemented export schemes
     */
    public static String getJsonFolder() {
        File tempFile = new File(getJsonFile());
        return tempFile.getParent();
    }

    /**
     * Sets the folder where to save the implemented export schemes.
     *
     * @param jsonFolder the folder where to save the implemented export schemes
     */
    public static void setJsonFolder(String jsonFolder) {
        ReporterExportFactory.JSON_FILE = jsonFolder + "/reporter_exportFactory.cus";
    }
}
