package eu.isas.reporter.cli;

import com.compomics.software.settings.PathKey;
import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.CommandLineExceptionHandler;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.normalization.NormalizationFactors;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.ProjectExport;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.calculation.QuantificationFeaturesCache;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.normalization.Normalizer;
import eu.isas.reporter.io.ProjectImporter;
import eu.isas.reporter.io.ProjectSaver;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.preferences.ReporterPathPreferences;
import eu.isas.reporter.settings.NormalizationSettings;
import eu.isas.reporter.settings.RatioEstimationSettings;
import eu.isas.reporter.settings.ReporterIonSelectionSettings;
import eu.isas.reporter.settings.ReporterSettings;
import eu.isas.reporter.utils.Properties;
import java.io.EOFException;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.Callable;
import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command line interface for reporter.
 *
 * @author Marc Vaudel
 */
public class ReporterCLI extends CpsParent implements Callable {

    /**
     * The command line parameters.
     */
    private ReporterCLIInputBean reporterCLIInputBean;
    /**
     * The PTM factory.
     */
    private PTMFactory ptmFactory;
    /**
     * The command line.
     */
    private CommandLine line;
    /**
     * The compomics reporter methods factory.
     */
    private ReporterMethodFactory methodsFactory = ReporterMethodFactory.getInstance();
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserPreferences utilitiesUserPreferences;
    /**
     * The mgf files loaded.
     */
    private ArrayList<File> mgfFiles = new ArrayList<File>();
    /**
     * Handler for the exceptions.
     */
    private ExceptionHandler exceptionHandler = new CommandLineExceptionHandler();

    /**
     * Construct a new ReporterCLI runnable from a list of arguments.
     *
     * @param args the command line arguments
     *
     * @throws org.apache.commons.cli.ParseException Exception thrown whenever
     * an error occurred while parsing the command line
     */
    private ReporterCLI(String[] args) throws ParseException {

        // Get command line parameters
        Options lOptions = new Options();
        ReporterCLIParameters.createOptionsCLI(lOptions);

        // Parse the command line
        BasicParser parser = new BasicParser();
        line = parser.parse(lOptions, args);
    }

    /**
     * Indicates whether the command line is valid.
     *
     * @return a boolean indicating whether the command line is valid
     */
    public boolean isValidCommandLine() {
        return ReporterCLIInputBean.isValidStartup(line);
    }

    @Override
    public Object call() throws IOException, ClassNotFoundException {

        // Parse command line
        reporterCLIInputBean = new ReporterCLIInputBean(line);

        // Set path preferences including the log
        PathSettingsCLIInputBean pathSettingsCLIInputBean = reporterCLIInputBean.getPathSettingsCLIInputBean();
        if (pathSettingsCLIInputBean.getLogFolder() != null) {
            redirectErrorStream(pathSettingsCLIInputBean.getLogFolder());
        }
        if (pathSettingsCLIInputBean.hasInput()) {
            PathSettingsCLI pathSettingsCLI = new PathSettingsCLI(pathSettingsCLIInputBean);
            pathSettingsCLI.setPathSettings();
        } else {
            try {
                Reporter.setPathConfiguration();
            } catch (Exception e) {
                System.out.println("An error occurred when setting path configuration. Default paths will be used.");
                e.printStackTrace();
            }
            try {
                ArrayList<PathKey> errorKeys = ReporterPathPreferences.getErrorKeys(Reporter.getJarFilePath());
                if (!errorKeys.isEmpty()) {
                    System.out.println("Unable to write in the following configuration folders. Please use a temporary folder, "
                            + "the path configuration command line, or edit the configuration paths from the graphical interface.");
                    for (PathKey pathKey : errorKeys) {
                        System.out.println(pathKey.getId() + ": " + pathKey.getDescription());
                    }
                }
            } catch (Exception e) {
                System.out.println("Unable to load the path configurations. Default paths will be used.");
            }
        }

        // Load user preferences
        utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();

        // Instantiate factories
        PeptideShaker.instantiateFacories(utilitiesUserPreferences);

        // Load species
        try {
            SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
            speciesFactory.initiate(Reporter.getJarFilePath());
        } catch (Exception e) {
            System.out.println("An error occurred while loading the species.");
            e.printStackTrace();
        }

        // Load default methods
        try {
            methodsFactory.importMethods(Reporter.getMethodsFile());
        } catch (Exception e) {
            System.out.println("An error occurred while loading the methods.");
            e.printStackTrace();
            return 1;
        }

        // Load PTMs 
        ptmFactory = PTMFactory.getInstance();

        // Initiate the waiting handler
        WaitingHandlerCLIImpl waitingHandler = new WaitingHandlerCLIImpl();

        // Set processing preferences
        ProcessingPreferences processingPreferences = new ProcessingPreferences();
        processingPreferences.setnThreads(reporterCLIInputBean.getnThreads());

        // Update the identification parameters if changed and save the changes
        IdentificationParameters tempIdentificationParameters = reporterCLIInputBean.getIdentificationParameters();
        File parametersFile = reporterCLIInputBean.getIdentificationParametersFile();
        if (parametersFile == null) {
            String name = tempIdentificationParameters.getName();
            if (name == null) {
                name = "SearchCLI.par";
            } else {
                name += ".par";
            }
            parametersFile = new File(reporterCLIInputBean.getOutputFile(), name);
            IdentificationParameters.saveIdentificationParameters(tempIdentificationParameters, parametersFile);
        }

        // Load the project from the cps file
        ProjectImporter projectImporter = new ProjectImporter();
        cpsFile = reporterCLIInputBean.getPeptideShakerFile();
        setDbFolder(Reporter.getMatchesFolder());
        try {
            projectImporter.importPeptideShakerProject(this, mgfFiles, waitingHandler);
            projectImporter.importReporterProject(this, waitingHandler);
        } catch (OutOfMemoryError error) {
            System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
            String errorText = "Reporter used up all the available memory and had to be stopped.<br>"
                    + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                    + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                    + "Java Options). See also <a href=\"http://compomics.github.io/projects/compomics-utilities.html\">JavaTroubleShooting</a>.";
            waitingHandler.appendReport(errorText, true, true);
            error.printStackTrace();
            return 1;
        } catch (EOFException e) {
            String errorText = "An error occurred while reading:\n" + cpsFile + ".\n\n"
                    + "The file is corrupted and cannot be opened anymore.";
            waitingHandler.appendReport(errorText, true, true);
            e.printStackTrace();
            return 1;
        } catch (Exception e) {
            String errorText = "An error occurred while reading:\n" + cpsFile + ".\n\n"
                    + "Please verify that the Reporter version used to create\n"
                    + "the file is compatible with your version of Reporter.";
            waitingHandler.appendReport(errorText, true, true);
            e.printStackTrace();
            return 1;
        }
        DisplayPreferences displayPreferences = projectImporter.getDisplayPreferences();

        // Load project specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
        if (error != null) {
            System.out.println(error);
        }

        // Verify that ignored PTMs are recognized
        ArrayList<String> ignoredPtms = reporterCLIInputBean.getIgnoredPtms();
        if (ignoredPtms != null) {
            for (String ptmName : ignoredPtms) {
                PTM ptm = ptmFactory.getPTM(ptmName);
                if (ptm == null) {
                    System.out.println("PTM " + ptmName + " not recognized.");
                    return 1;
                }
            }
        }

        // get previously set quantification settings or defaults from the identification results
        ReporterSettings reporterSettings = projectImporter.getReporterSettings();
        ReporterIonQuantification reporterIonQuantification = projectImporter.getReporterIonQuantification();
        ReporterMethod selectedMethod = reporterIonQuantification.getReporterMethod();

        // Update the method according to the command line
        File methodsFile = reporterCLIInputBean.getIsotopesFile();
        if (methodsFile != null) {
            try {
                methodsFactory.importMethods(methodsFile);
            } catch (Exception e) {
                String errorText = "An error occurred while parsing:\n" + methodsFile + ".\n\n";
                waitingHandler.appendReport(errorText, true, true);
                e.printStackTrace();
                return 1;
            }
        }
        String specifiedMethodName = reporterCLIInputBean.getReporterMethod();
        if (specifiedMethodName != null) {
            selectedMethod = methodsFactory.getReporterMethod(specifiedMethodName);
        }
        if (selectedMethod == null && methodsFactory.getMethodsNames().size() == 1) {
            selectedMethod = methodsFactory.getReporterMethod(methodsFactory.getMethodsNames().get(0));
        }
        if (selectedMethod == null) {
            String errorText = "The reporter quantification methods to use could not be inferred, please specify a method from the isotopic correction file as command line parameter.\n\n";
            waitingHandler.appendReport(errorText, true, true);
            return 1;
        }

        // Set the method
        reporterIonQuantification.setMethod(selectedMethod);

        // Update the quantification settings according to the command line
        updateReporterIonSelectionSettings(reporterSettings.getReporterIonSelectionSettings());
        updateRatioEstimationSettings(reporterSettings.getRatioEstimationSettings());
        updateNormalizationSettings(reporterSettings.getNormalizationSettings());

        // Name samples according to their reagent
        ArrayList<String> reagents = selectedMethod.getReagentsSortedByMass();
        for (String reagent : reagents) {
            reporterIonQuantification.assignSample(reagent, new Sample(reagent));
        }

        // Set reference samples
        ArrayList<Integer> referenceIndexes = reporterCLIInputBean.getReferenceSamples();
        if (referenceIndexes != null) {
            ArrayList<String> referenceSamples = new ArrayList<String>(referenceIndexes.size());
            for (Integer index : referenceIndexes) {
                if (index > reagents.size()) {
                    System.out.println(System.getProperty("line.separator") + "Reference sample index " + index
                            + " is higher than the number of reagents (" + reagents.size() + ")." + System.getProperty("line.separator"));
                    return 1;
                }
                referenceSamples.add(reagents.get(index - 1));
            }
            reporterIonQuantification.setControlSamples(referenceSamples);
        }

        // Create quantification features generator
        QuantificationFeaturesGenerator quantificationFeaturesGenerator = new QuantificationFeaturesGenerator(new QuantificationFeaturesCache(), getIdentification(), getIdentificationFeaturesGenerator(), reporterSettings, reporterIonQuantification,
                identificationParameters.getSearchParameters(), identificationParameters.getSequenceMatchingPreferences());

        // Set Normalization factors
        NormalizationFactors normalizationFactors = reporterIonQuantification.getNormalizationFactors();
        if (!normalizationFactors.hasNormalizationFactors()) {
            try {
                Normalizer normalizer = new Normalizer();
                if (!normalizationFactors.hasPsmNormalisationFactors()) {
                    normalizer.setPsmNormalizationFactors(reporterIonQuantification, reporterSettings.getRatioEstimationSettings(), reporterSettings.getNormalizationSettings(), getIdentificationParameters().getSequenceMatchingPreferences(), getIdentification(), quantificationFeaturesGenerator, processingPreferences, exceptionHandler, waitingHandler);
                }
                if (!normalizationFactors.hasPeptideNormalisationFactors()) {
                    normalizer.setPeptideNormalizationFactors(reporterIonQuantification, reporterSettings.getRatioEstimationSettings(), reporterSettings.getNormalizationSettings(), getIdentificationParameters().getSequenceMatchingPreferences(), getIdentification(), quantificationFeaturesGenerator, processingPreferences, exceptionHandler, waitingHandler);
                }
                if (!normalizationFactors.hasProteinNormalisationFactors()) {
                    normalizer.setProteinNormalizationFactors(reporterIonQuantification, reporterSettings.getRatioEstimationSettings(), reporterSettings.getNormalizationSettings(), getIdentification(), getMetrics(), quantificationFeaturesGenerator, processingPreferences, exceptionHandler, waitingHandler);
                }
            } catch (Exception e) {
                System.out.println(System.getProperty("line.separator") + "An error occurred while estimating the ratios." + System.getProperty("line.separator"));
                e.printStackTrace();
                return 1;
            }
        }

        // Save the project in the cps file
        File destinationFile = reporterCLIInputBean.getOutputFile();
        if (destinationFile == null) {
            destinationFile = cpsFile;
        }
        try {
            ProjectSaver.saveProject(reporterSettings, reporterIonQuantification, displayPreferences, this, waitingHandler);
        } catch (Exception e) {
            System.out.println(System.getProperty("line.separator") + "An error occurred while saving the project." + System.getProperty("line.separator"));
            e.printStackTrace();
            return 1;
        }

        // report export if needed
        ReportCLIInputBean reportCLIInputBean = reporterCLIInputBean.getReportCLIInputBean();

        // see if output folder is set, and if not set to the same folder as the cps file
        if (reportCLIInputBean.getReportOutputFolder() == null) {
            reportCLIInputBean.setReportOutputFolder(destinationFile.getParentFile());
        }

        if (reportCLIInputBean.exportNeeded()) {
            waitingHandler.appendReport("Starting report export.", true, true);

            // Export report(s)
            if (reportCLIInputBean.exportNeeded()) {
                int nSurroundingAAs = 2; //@TODO: this shall not be hard coded //peptideShakerGUI.getDisplayPreferences().getnAASurroundingPeptides()
                for (String reportType : reportCLIInputBean.getReportTypes()) {
                    try {
                        CLIExportMethods.exportReport(reportCLIInputBean, reportType, experiment.getReference(), sample.getReference(), replicateNumber, projectDetails, identification, geneMaps, identificationFeaturesGenerator, quantificationFeaturesGenerator, reporterIonQuantification, reporterSettings, identificationParameters, nSurroundingAAs, spectrumCountingPreferences, waitingHandler);
                    } catch (Exception e) {
                        waitingHandler.appendReport("An error occurred while exporting the " + reportType + ".", true, true);
                        e.printStackTrace();
                        waitingHandler.setRunCanceled();
                    }
                }
            }

            // export documentation
            if (reportCLIInputBean.documentationExportNeeded()) {
                for (String reportType : reportCLIInputBean.getReportTypes()) {
                    try {
                        CLIExportMethods.exportDocumentation(reportCLIInputBean, reportType, waitingHandler);
                    } catch (Exception e) {
                        waitingHandler.appendReport("An error occurred while exporting the documentation for " + reportType + ".", true, true);
                        e.printStackTrace();
                        waitingHandler.setRunCanceled();
                    }
                }
            }
        }

        // export as zip
        File zipFile = reporterCLIInputBean.getZipExport();
        if (zipFile != null) {

            waitingHandler.appendReportEndLine();
            waitingHandler.appendReport("Zipping project.", true, true);

            File parent = zipFile.getParentFile();
            try {
                parent.mkdirs();
            } catch (Exception e) {
                waitingHandler.appendReport("An error occurred while creating folder " + parent.getAbsolutePath() + ".", true, true);
                waitingHandler.setRunCanceled();
            }

            File fastaFile = identificationParameters.getProteinInferencePreferences().getProteinSequenceDatabase();
            ArrayList<File> spectrumFiles = new ArrayList<File>();
            for (String spectrumFileName : getIdentification().getSpectrumFiles()) {
                File spectrumFile = getProjectDetails().getSpectrumFile(spectrumFileName);
                spectrumFiles.add(spectrumFile);
            }

            try {
                ProjectExport.exportProjectAsZip(zipFile, fastaFile, spectrumFiles, cpsFile, waitingHandler);
                final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
                double sizeOfZippedFile = Util.roundDouble(((double) zipFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE), 2);
                waitingHandler.appendReport("Project zipped to \'" + zipFile.getAbsolutePath() + "\' (" + sizeOfZippedFile + " MB)", true, true);
            } catch (FileNotFoundException e) {
                e.printStackTrace();
                waitingHandler.appendReport("An error occurred while attempting to zip project in " + zipFile.getAbsolutePath() + ".", true, true);
                waitingHandler.setRunCanceled();
            } catch (IOException e) {
                e.printStackTrace();
                waitingHandler.appendReport("An error occurred while attempting to zip project in " + zipFile.getAbsolutePath() + ".", true, true);
                waitingHandler.setRunCanceled();
            }
        }

        waitingHandler.appendReportEndLine();

        if (waitingHandler.isRunCanceled()) {
            return 1;
        }

        return 0;
    }

    /**
     * Updates the reporter ion selection settings according to the command line
     * input.
     *
     * @param reporterIonSelectionSettings the reporter ion selection settings
     */
    private void updateReporterIonSelectionSettings(ReporterIonSelectionSettings reporterIonSelectionSettings) {
        if (reporterCLIInputBean.getReporterIonTolerance() != null) {
            reporterIonSelectionSettings.setReporterIonsMzTolerance(reporterCLIInputBean.getReporterIonTolerance());
        }
        if (reporterCLIInputBean.getMostAccurate() != null) {
            reporterIonSelectionSettings.setMostAccurate(reporterCLIInputBean.getMostAccurate());
        }
        if (reporterCLIInputBean.getSameSpectra() != null) {
            reporterIonSelectionSettings.setSameSpectra(reporterCLIInputBean.getSameSpectra());
        }
        if (reporterCLIInputBean.getPrecMzTolerance() != null) {
            reporterIonSelectionSettings.setPrecursorMzTolerance(reporterCLIInputBean.getPrecMzTolerance());
        }
        if (reporterCLIInputBean.getPrecMzTolerancePpm() != null) {
            reporterIonSelectionSettings.setPrecursorMzPpm(reporterCLIInputBean.getPrecMzTolerancePpm());
        }
        if (reporterCLIInputBean.getPrecRtTolerance() != null) {
            reporterIonSelectionSettings.setPrecursorRTTolerance(reporterCLIInputBean.getPrecRtTolerance());
        }
    }

    /**
     * Updates the ratio estimation settings according to the command line
     * input.
     *
     * @param ratioEstimationSettings the ratio estimation settings to update
     */
    private void updateRatioEstimationSettings(RatioEstimationSettings ratioEstimationSettings) {
        if (reporterCLIInputBean.getIgnoreNull() != null) {
            ratioEstimationSettings.setIgnoreNullIntensities(reporterCLIInputBean.getIgnoreNull());
        }
        if (reporterCLIInputBean.getIgnoreMc() != null) {
            ratioEstimationSettings.setIgnoreMissedCleavages(reporterCLIInputBean.getIgnoreMc());
        }
        if (reporterCLIInputBean.getPercentile() != null) {
            ratioEstimationSettings.setPercentile(reporterCLIInputBean.getPercentile());
        }
        if (reporterCLIInputBean.getResolution() != null) {
            ratioEstimationSettings.setRatioResolution(reporterCLIInputBean.getResolution());
        }
        if (reporterCLIInputBean.getMinUnique() != null) {
            ratioEstimationSettings.setMinUnique(reporterCLIInputBean.getMinUnique());
        }
        if (reporterCLIInputBean.getIgnoredPtms() != null) {
            ratioEstimationSettings.emptyPTMList();
            for (String ptmName : reporterCLIInputBean.getIgnoredPtms()) {
                ratioEstimationSettings.addExcludingPtm(ptmName);
            }
        }
        if (reporterCLIInputBean.getValidationPsm() != null) {
            ratioEstimationSettings.setPsmValidationLevel(MatchValidationLevel.getMatchValidationLevel(reporterCLIInputBean.getValidationPsm()));
        }
        if (reporterCLIInputBean.getValidationPeptide() != null) {
            ratioEstimationSettings.setPeptideValidationLevel(MatchValidationLevel.getMatchValidationLevel(reporterCLIInputBean.getValidationPeptide()));
        }
        if (reporterCLIInputBean.getValidationProtein() != null) {
            ratioEstimationSettings.setProteinValidationLevel(MatchValidationLevel.getMatchValidationLevel(reporterCLIInputBean.getValidationProtein()));
        }
    }

    /**
     * Updates the normalization settings according to the command line input.
     *
     * @param normalizationSettings the normalization settings to update
     */
    private void updateNormalizationSettings(NormalizationSettings normalizationSettings) {
        if (reporterCLIInputBean.getPsmNormalizationType() != null) {
            normalizationSettings.setPsmNormalization(reporterCLIInputBean.getPsmNormalizationType());
        }
        if (reporterCLIInputBean.getPeptideNormalizationType() != null) {
            normalizationSettings.setPeptideNormalization(reporterCLIInputBean.getPeptideNormalizationType());
        }
        if (reporterCLIInputBean.getProteinNormalizationType() != null) {
            normalizationSettings.setProteinNormalization(reporterCLIInputBean.getProteinNormalizationType());
        }
        if (reporterCLIInputBean.getStableProteins() != null) {
            normalizationSettings.setStableProteinsFastaFile(reporterCLIInputBean.getStableProteins());
        }
        if (reporterCLIInputBean.getContaminants() != null) {
            normalizationSettings.setContaminantsFastaFile(reporterCLIInputBean.getContaminants());
        }
    }

    /**
     * Close the Reporter instance. Closes file connections and deletes
     * temporary files.
     *
     * @throws IOException thrown of IOException occurs
     * @throws SQLException thrown if SQLException occurs
     */
    public void close() throws IOException, SQLException {
        close(identification);
    }

    /**
     * Close the Reporter instance. Closes file connections and deletes
     * temporary files.
     *
     * @param identification the identification
     *
     * @throws IOException thrown of IOException occurs
     * @throws SQLException thrown if SQLException occurs
     */
    public static void close(Identification identification) throws IOException, SQLException {

        try {
            SpectrumFactory.getInstance().closeFiles();
        } catch (Exception e) {
            e.printStackTrace();
        }
        try {
            SequenceFactory.getInstance().closeFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            if (identification != null) {
                identification.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            DerbyUtil.closeConnection();
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            File matchFolder = Reporter.getMatchesFolder();
            File[] tempFiles = matchFolder.listFiles();

            if (tempFiles != null) {
                for (File currentFile : tempFiles) {
                    boolean deleted = Util.deleteDir(currentFile);
                    if (!deleted) {
                        System.out.println(currentFile.getAbsolutePath() + " could not be deleted!"); // @TODO: better handling of this error?
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {
            TempFilesManager.deleteTempFolders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * SearchCLI header message when printing the usage.
     */
    private static String getHeader() {
        return System.getProperty("line.separator")
                + "ReporterCLI estimates abundance ratios from PeptideShaker projects based on reporter ion quantification." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see http://compomics.github.io/projects/reporter.html and http://compomics.github.io/projects/reporter/wiki/reportercli.html." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/reporter." + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + "\n";
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            ReporterCLI reporterCLI = new ReporterCLI(args);

            if (!reporterCLI.isValidCommandLine()) {
                // Not a valid command line, display the options and exit
                PrintWriter lPrintWriter = new PrintWriter(System.out);
                lPrintWriter.print(System.getProperty("line.separator") + "======================" + System.getProperty("line.separator"));
                lPrintWriter.print("ReporterCLI" + System.getProperty("line.separator"));
                lPrintWriter.print("======================" + System.getProperty("line.separator"));
                lPrintWriter.print(getHeader());
                lPrintWriter.print(ReporterCLIParameters.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();
                System.exit(1);
            } else {
                // Valid command line, start the processing
                reporterCLI.call();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Delete temporary folders
        try {
            TempFilesManager.deleteTempFolders();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Redirects the error stream to the Reporter.log file in the given folder.
     *
     * @param logFolder the folder where to save the log
     */
    public static void redirectErrorStream(File logFolder) {

        try {
            logFolder.mkdirs();
            File file = new File(logFolder, "Reporter.log");
            System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

            System.err.println(System.getProperty("line.separator") + System.getProperty("line.separator") + new Date()
                    + ": Reporter version " + new Properties().getVersion() + ".");
            System.err.println("Memory given to the Java virtual machine: " + Runtime.getRuntime().maxMemory() + " b.");
            System.err.println("Total amount of memory in the Java virtual machine: " + Runtime.getRuntime().totalMemory() + " b.");
            System.err.println("Free memory: " + Runtime.getRuntime().freeMemory() + " b.");
            System.err.println("Java version: " + System.getProperty("java.version") + ".");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
