/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.reporter.export.report;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.SerializationUtils;
import com.compomics.util.io.export.ExportFactory;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.PSExportFactory;
import eu.isas.peptideshaker.export.exportfeatures.AnnotationFeatures;
import eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures;
import eu.isas.peptideshaker.export.exportfeatures.InputFilterFeatures;
import eu.isas.peptideshaker.export.exportfeatures.ProjectFeatures;
import eu.isas.peptideshaker.export.exportfeatures.PtmScoringFeatures;
import eu.isas.peptideshaker.export.exportfeatures.SearchFeatures;
import eu.isas.peptideshaker.export.exportfeatures.SpectrumCountingFeatures;
import eu.isas.peptideshaker.export.exportfeatures.ValidationFeatures;
import eu.isas.peptideshaker.export.sections.AnnotationSection;
import eu.isas.peptideshaker.export.sections.InputFilterSection;
import eu.isas.peptideshaker.export.sections.ProjectSection;
import eu.isas.peptideshaker.export.sections.PtmScoringSection;
import eu.isas.peptideshaker.export.sections.SearchParametersSection;
import eu.isas.peptideshaker.export.sections.SpectrumCountingSection;
import eu.isas.peptideshaker.export.sections.ValidationSection;
import eu.isas.peptideshaker.myparameters.PSMaps;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.export_features.PeptideFeatures;
import eu.isas.reporter.export.report.export_features.ProteinFeatures;
import eu.isas.reporter.export.report.export_features.PsmFeatures;
import eu.isas.reporter.export.report.sections.PeptideSection;
import eu.isas.reporter.export.report.sections.ProteinSection;
import eu.isas.reporter.export.report.sections.PsmSection;
import eu.isas.reporter.myparameters.ReporterPreferences;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The reporter export factory manages the reports available from Reporter
 *
 * @author Marc
 */
public class ReporterExportFactory implements ExportFactory {
    /**
     * The instance of the factory.
     */
    private static ReporterExportFactory instance = null;
    /**
     * User defined factory containing the user schemes.
     */
    private static final String SERIALIZATION_FILE = System.getProperty("user.home") + "/.reporter/exportFactory.cus";
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
        result.add(AnnotationFeatures.type);
        result.add(InputFilterFeatures.type);
        result.add(ProteinFeatures.type);
        result.add(PeptideFeatures.type);
        result.add(PsmFeatures.type);
        result.add(FragmentFeatures.type);
        result.add(ProjectFeatures.type);
        result.add(PtmScoringFeatures.type);
        result.add(SearchFeatures.type);
        result.add(SpectrumCountingFeatures.type);
        result.add(ValidationFeatures.type);
        return result;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(String sectionName) {
        if (sectionName.equals(AnnotationFeatures.type)) {
            return AnnotationFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(InputFilterFeatures.type)) {
            return InputFilterFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(PeptideFeatures.type)) {
            return PeptideFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(ProjectFeatures.type)) {
            return ProjectFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(ProteinFeatures.type)) {
            return ProteinFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(PsmFeatures.type)) {
            return PsmFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(PtmScoringFeatures.type)) {
            return PtmScoringFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(SearchFeatures.type)) {
            return SearchFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(SpectrumCountingFeatures.type)) {
            return SpectrumCountingFeatures.values()[0].getExportFeatures();
        } else if (sectionName.equals(ValidationFeatures.type)) {
            return ValidationFeatures.values()[0].getExportFeatures();
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
     * @param quantificationFeaturesGenerator the object generating the quantification features
     * @param reporterIonQuantification the reporter ion quantification object containing the quantification configuration
     * @param reporterPreferences the reporter preferences
     * @param searchParameters the search parameters (mandatory for the Protein,
     * Peptide, PSM and search parameters sections)
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
     * @param annotationPreferences the annotation preferences (mandatory for
     * the Annotation section)
     * @param idFilter the identification filer (mandatory for the Input Filter
     * section)
     * @param ptmcoringPreferences the PTM scoring preferences (mandatory for
     * the PTM scoring section)
     * @param spectrumCountingPreferences the spectrum counting preferences
     * (mandatory for the spectrum counting section)
     * @param waitingHandler the waiting handler
     * @throws IOException
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static void writeExport(ExportScheme exportScheme, File destinationFile, String experiment, String sample, int replicateNumber,
            ProjectDetails projectDetails, Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, ReporterPreferences reporterPreferences,
            SearchParameters searchParameters, ArrayList<String> proteinKeys, ArrayList<String> peptideKeys, ArrayList<String> psmKeys,
            String proteinMatchKey, int nSurroundingAA, AnnotationPreferences annotationPreferences, IdFilter idFilter,
            PTMScoringPreferences ptmcoringPreferences, SpectrumCountingPreferences spectrumCountingPreferences, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        // @TODO: implement other formats, put sometimes text instead of tables
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile));

        String mainTitle = exportScheme.getMainTitle();
        if (mainTitle != null) {
            writer.write(mainTitle);
            writeSeparationLines(writer, exportScheme.getSeparationLines());
        }

        for (String sectionName : exportScheme.getSections()) {
            if (exportScheme.isIncludeSectionTitles()) {
                writer.write(sectionName);
                writer.newLine();
            }
            if (sectionName.equals(AnnotationFeatures.type)) {
                AnnotationSection section = new AnnotationSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(annotationPreferences, waitingHandler);
            } else if (sectionName.equals(InputFilterFeatures.type)) {
                InputFilterSection section = new InputFilterSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(idFilter, waitingHandler);
            } else if (sectionName.equals(PeptideFeatures.type)) {
                PeptideSection section = new PeptideSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, quantificationFeaturesGenerator, reporterIonQuantification, searchParameters, annotationPreferences, peptideKeys, nSurroundingAA, "", waitingHandler);
            } else if (sectionName.equals(ProjectFeatures.type)) {
                ProjectSection section = new ProjectSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(experiment, sample, replicateNumber, projectDetails, waitingHandler);
            } else if (sectionName.equals(ProteinFeatures.type)) {
                ProteinSection section = new ProteinSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, quantificationFeaturesGenerator, reporterIonQuantification, searchParameters, annotationPreferences, psmKeys, nSurroundingAA, waitingHandler);
            } else if (sectionName.equals(PsmFeatures.type)) {
                PsmSection section = new PsmSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(identification, identificationFeaturesGenerator, quantificationFeaturesGenerator, reporterIonQuantification, reporterPreferences, searchParameters, annotationPreferences, psmKeys, "", waitingHandler);
            } else if (sectionName.equals(PtmScoringFeatures.type)) {
                PtmScoringSection section = new PtmScoringSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(ptmcoringPreferences, waitingHandler);
            } else if (sectionName.equals(SearchFeatures.type)) {
                SearchParametersSection section = new SearchParametersSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(searchParameters, waitingHandler);
            } else if (sectionName.equals(SpectrumCountingFeatures.type)) {
                SpectrumCountingSection section = new SpectrumCountingSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                section.writeSection(spectrumCountingPreferences, waitingHandler);
            } else if (sectionName.equals(ValidationFeatures.type)) {
                ValidationSection section = new ValidationSection(exportScheme.getExportFeatures(sectionName), exportScheme.getSeparator(), exportScheme.isIndexes(), exportScheme.isHeader(), writer);
                PSMaps psMaps = new PSMaps();
                psMaps = (PSMaps) identification.getUrParam(psMaps);
                section.writeSection(psMaps, waitingHandler);
            } else {
                writer.write("Section " + sectionName + " not implemented in the ExportFactory.");
            }

            writeSeparationLines(writer, exportScheme.getSeparationLines());
        }

        writer.close();
    }

    /**
     * Writes the documentation related to a report.
     *
     * @param exportScheme the export scheme of the report
     * @param destinationFile the destination file where to write the
     * documentation
     * @throws IOException
     */
    public static void writeDocumentation(ExportScheme exportScheme, File destinationFile) throws IOException {
        BufferedWriter writer = new BufferedWriter(new FileWriter(destinationFile));

        String mainTitle = exportScheme.getMainTitle();
        if (mainTitle != null) {
            writer.write(mainTitle);
            writeSeparationLines(writer, exportScheme.getSeparationLines());
        }
        for (String sectionName : exportScheme.getSections()) {
            if (exportScheme.isIncludeSectionTitles()) {
                writer.write(sectionName);
                writer.newLine();
            }
            for (ExportFeature exportFeature : exportScheme.getExportFeatures(sectionName)) {
                boolean firstTitle = true;
                for (String title : exportFeature.getTitles()) {
                    if (firstTitle) {
                        firstTitle = false;
                    } else {
                        writer.write(", ");
                    }
                    writer.write(title);
                }
                writer.write(exportScheme.getSeparator());
                writer.write(exportFeature.getDescription());
                writer.newLine();
            }
            writeSeparationLines(writer, exportScheme.getSeparationLines());
        }
        writer.close();
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
     * project details
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
     * Returns the default schemes available. The default schemes are here the PeptideShaker default schemes with ratios.
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
                    if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures) {
                        protein = true;
                    } else if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures) {
                        peptide = true;
                    } else if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PsmFeatures) {
                        psm = true;
                    }
                }
                if (protein) {
                    exportScheme.addExportFeature(section, ProteinFeatures.ratio);
                }
                if (peptide) {
                    exportScheme.addExportFeature(section, PeptideFeatures.raw_ratio);
                    exportScheme.addExportFeature(section, PeptideFeatures.normalized_ratio);
                }
                if (psm) {
                    exportScheme.addExportFeature(section, PsmFeatures.reporter_mz);
                    exportScheme.addExportFeature(section, PsmFeatures.reporter_intensity);
                    exportScheme.addExportFeature(section, PsmFeatures.deisotoped_intensity);
                    exportScheme.addExportFeature(section, PsmFeatures.ratio);
                }
            }
            // rename the PSM, Peptide and protein sections
            String psSection = eu.isas.peptideshaker.export.exportfeatures.ProteinFeatures.type;
            if (exportScheme.getSections().contains(psSection)) {
                exportScheme.setExportFeatures(ProteinFeatures.type, exportScheme.getExportFeatures(psSection));
                exportScheme.removeSection(psSection);
            }
            psSection = eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures.type;
            if (exportScheme.getSections().contains(psSection)) {
                exportScheme.setExportFeatures(PeptideFeatures.type, exportScheme.getExportFeatures(psSection));
                exportScheme.removeSection(psSection);
            }
            psSection = eu.isas.peptideshaker.export.exportfeatures.PsmFeatures.type;
            if (exportScheme.getSections().contains(psSection)) {
                exportScheme.setExportFeatures(PsmFeatures.type, exportScheme.getExportFeatures(psSection));
                exportScheme.removeSection(psSection);
            }
            defaultSchemes.put(schemeName, exportScheme);
        }
        return defaultSchemes;
    }
}
