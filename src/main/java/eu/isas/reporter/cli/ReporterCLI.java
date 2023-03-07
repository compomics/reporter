package eu.isas.reporter.cli;

import com.compomics.software.cli.CommandLineUtils;
import com.compomics.software.cli.CommandParameter;
import com.compomics.util.Util;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.CommandLineExceptionHandler;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.experiment.io.temp.TempFilesManager;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.normalization.NormalizationFactors;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.UtilitiesUserParameters;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.ProjectExport;
import eu.isas.peptideshaker.utils.PsdbParent;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.calculation.QuantificationFeaturesCache;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.normalization.NormalizationType;
import eu.isas.reporter.calculation.normalization.Normalizer;
import eu.isas.reporter.io.ProjectImporter;
import eu.isas.reporter.io.ProjectSaver;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.settings.NormalizationSettings;
import eu.isas.reporter.settings.RatioEstimationSettings;
import eu.isas.reporter.settings.ReporterIonSelectionSettings;
import eu.isas.reporter.settings.ReporterIonsLocationType;
import eu.isas.reporter.settings.ReporterSettings;
import eu.isas.reporter.utils.Properties;
import java.io.EOFException;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.concurrent.Callable;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * Command line interface for reporter.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterCLI extends PsdbParent implements Callable {

    /**
     * The command line parameters.
     */
    private ReporterCLIInputBean reporterCLIInputBean;
    /**
     * The modification factory.
     */
    private ModificationFactory modificationFactory;
    /**
     * The command line.
     */
    private CommandLine line;
    /**
     * The compomics reporter methods factory.
     */
    private ReporterMethodFactory methodsFactory = ReporterMethodFactory.getInstance();
    /**
     * The utilities user parameters.
     */
    private UtilitiesUserParameters utilitiesUserParameters;
    /**
     * The spectrum files loaded.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<>();
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

        // get command line parameters
        Options lOptions = new Options();
        ReporterCLIParameters.createOptionsCLI(lOptions);

        // parse the command line
        DefaultParser parser = new DefaultParser();
        line = parser.parse(lOptions, args);
    }

    /**
     * Indicates whether the command line is valid.
     *
     * @param aLine the command line
     * @return a boolean indicating whether the command line is valid
     */
    public static boolean isValidCommandLine(CommandLine aLine) {

        // PeptideShaker file
        if (!aLine.hasOption(ReporterCLIParameters.ID.id) || ((String) aLine.getOptionValue(ReporterCLIParameters.ID.id)).equals("")) {

            System.out.println(System.getProperty("line.separator") + "PeptideShaker file not specified." + System.getProperty("line.separator"));
            return false;

        } else {

            String arg = aLine.getOptionValue(ReporterCLIParameters.ID.id);
            HashSet<String> supportedFormats = new HashSet<String>(2);
            supportedFormats.add(".psdb");
            supportedFormats.add(".zip");

            if (!CommandParameter.fileExists(ReporterCLIParameters.ID.id, arg, supportedFormats)) {
                return false;
            }

        }

        // The isotopes file
        if (aLine.hasOption(ReporterCLIParameters.ISOTOPES.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.ISOTOPES.id);
            HashSet<String> supportedFormats = new HashSet<>(1);
            supportedFormats.add(".xml");

            if (!CommandParameter.fileExists(ReporterCLIParameters.ISOTOPES.id, arg, supportedFormats)) {
                return false;
            }

        }

        // The reference samples
        if (aLine.hasOption(ReporterCLIParameters.REFERENCE.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.REFERENCE.id);

            try {

                ArrayList<Integer> input = CommandLineUtils.getIntegerListFromString(arg, ",");

                for (Integer reagent : input) {

                    if (!CommandParameter.isPositiveInteger(ReporterCLIParameters.REFERENCE.id, reagent + "", false)) {
                        return false;
                    }

                }

            } catch (Exception e) {

                System.out.println(
                        System.getProperty("line.separator")
                        + "Error parsing the "
                        + ReporterCLIParameters.REFERENCE.id
                        + " option: not a comma separated list of integers."
                        + System.getProperty("line.separator")
                );

                return false;
            }
        }

        // The number of threads
        if (aLine.hasOption(ReporterCLIParameters.THREADS.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.THREADS.id);

            if (!CommandParameter.isPositiveInteger(ReporterCLIParameters.THREADS.id, arg, false)) {
                return false;
            }

        }

        // The ion tolerance
        if (aLine.hasOption(ReporterCLIParameters.ION_TOL.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.ION_TOL.id);

            if (!CommandParameter.isPositiveDouble(ReporterCLIParameters.ION_TOL.id, arg, false)) {
                return false;
            }

        }

        // Most accurate option
        if (aLine.hasOption(ReporterCLIParameters.MOST_ACCURATE.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.MOST_ACCURATE.id);

            if (!CommandParameter.isBooleanInput(ReporterCLIParameters.MOST_ACCURATE.id, arg)) {
                return false;
            }

        }

        // Reporter ions location option
        if (aLine.hasOption(ReporterCLIParameters.REPORTER_IONS_LOCATION.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.REPORTER_IONS_LOCATION.id);

            ArrayList<String> reporterIonsLocationTypes = new ArrayList<String>(ReporterIonsLocationType.values().length);

            for (ReporterIonsLocationType reporterIonsLocationType : ReporterIonsLocationType.values()) {
                reporterIonsLocationTypes.add(reporterIonsLocationType.index + "");
            }

            if (!CommandParameter.isInList(ReporterCLIParameters.REPORTER_IONS_LOCATION.id, arg, reporterIonsLocationTypes)) {
                return false;
            }

        }

        // The precursor ion m/z tolerance
        if (aLine.hasOption(ReporterCLIParameters.PREC_WINDOW_MZ_TOL.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.PREC_WINDOW_MZ_TOL.id);

            if (!CommandParameter.isPositiveDouble(ReporterCLIParameters.PREC_WINDOW_MZ_TOL.id, arg, false)) {
                return false;
            }

        }

        // The precursor ion m/z tolerance unit
        if (aLine.hasOption(ReporterCLIParameters.PREC_WINDOW_MZ_TOL_PPM.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.PREC_WINDOW_MZ_TOL_PPM.id);

            if (!CommandParameter.isBooleanInput(ReporterCLIParameters.PREC_WINDOW_MZ_TOL_PPM.id, arg)) {
                return false;
            }

        }

        // The precursor ion RT tolerance
        if (aLine.hasOption(ReporterCLIParameters.PREC_WINDOW_RT_TOL.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.PREC_WINDOW_RT_TOL.id);

            if (!CommandParameter.isPositiveDouble(ReporterCLIParameters.PREC_WINDOW_RT_TOL.id, arg, false)) {
                return false;
            }

        }

        // The ignore null option
        if (aLine.hasOption(ReporterCLIParameters.IGNORE_NULL.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.IGNORE_NULL.id);

            if (!CommandParameter.isBooleanInput(ReporterCLIParameters.IGNORE_NULL.id, arg)) {
                return false;
            }

        }

        // The ignore missed cleavages option
        if (aLine.hasOption(ReporterCLIParameters.IGNORE_MC.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.IGNORE_MC.id);

            if (!CommandParameter.isBooleanInput(ReporterCLIParameters.IGNORE_MC.id, arg)) {
                return false;
            }

        }

        // The percentile
        if (aLine.hasOption(ReporterCLIParameters.PERCENTILE.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.PERCENTILE.id);

            if (!CommandParameter.isPositiveDouble(ReporterCLIParameters.PERCENTILE.id, arg, false)) {
                return false;
            }

        }

        // The resolution option for ratio estimation
        if (aLine.hasOption(ReporterCLIParameters.RESOLUTION.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.RESOLUTION.id);

            if (!CommandParameter.isPositiveDouble(ReporterCLIParameters.RESOLUTION.id, arg, false)) {
                return false;
            }

        }

        // The number of unique peptides
        if (aLine.hasOption(ReporterCLIParameters.MIN_UNIQUE.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.MIN_UNIQUE.id);

            if (!CommandParameter.isInteger(ReporterCLIParameters.MIN_UNIQUE.id, arg)) {
                return false;
            }

        }

        // The validation levels
        ArrayList<String> validationLevels = new ArrayList<String>(3);
        validationLevels.add("0");
        validationLevels.add("1");
        validationLevels.add("2");

        // PSM
        if (aLine.hasOption(ReporterCLIParameters.VALIDATION_PSM.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.VALIDATION_PSM.id);

            if (!CommandParameter.isInList(ReporterCLIParameters.VALIDATION_PSM.id, arg, validationLevels)) {
                return false;
            }

        }

        // Peptide
        if (aLine.hasOption(ReporterCLIParameters.VALIDATION_PEPTIDE.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.VALIDATION_PEPTIDE.id);

            if (!CommandParameter.isInList(ReporterCLIParameters.VALIDATION_PEPTIDE.id, arg, validationLevels)) {
                return false;
            }

        }

        // Protein
        if (aLine.hasOption(ReporterCLIParameters.VALIDATION_PROTEIN.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.VALIDATION_PROTEIN.id);

            if (!CommandParameter.isInList(ReporterCLIParameters.VALIDATION_PROTEIN.id, arg, validationLevels)) {
                return false;
            }

        }

        // Normalization
        ArrayList<String> normalizationTypes = new ArrayList<String>(NormalizationType.values().length);

        for (NormalizationType normalizationType : NormalizationType.values()) {
            normalizationTypes.add(normalizationType.index + "");
        }

        // PSM
        if (aLine.hasOption(ReporterCLIParameters.NORMALIZATION_PSM.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.NORMALIZATION_PSM.id);

            if (!CommandParameter.isInList(ReporterCLIParameters.NORMALIZATION_PSM.id, arg, normalizationTypes)) {
                return false;
            }

        }

        // Peptide
        if (aLine.hasOption(ReporterCLIParameters.NORMALIZATION_PEPTIDE.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.NORMALIZATION_PEPTIDE.id);

            if (!CommandParameter.isInList(ReporterCLIParameters.NORMALIZATION_PEPTIDE.id, arg, normalizationTypes)) {
                return false;
            }

        }

        // Protein
        if (aLine.hasOption(ReporterCLIParameters.NORMALIZATION_PROTEIN.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.NORMALIZATION_PROTEIN.id);

            if (!CommandParameter.isInList(ReporterCLIParameters.NORMALIZATION_PROTEIN.id, arg, normalizationTypes)) {
                return false;
            }

        }

        // Stable proteins
        if (aLine.hasOption(ReporterCLIParameters.STABLE_PROTEINS.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.STABLE_PROTEINS.id);
            HashSet<String> supportedFormats = new HashSet<String>(1);
            supportedFormats.add(".fasta");

            if (!CommandParameter.fileExists(ReporterCLIParameters.STABLE_PROTEINS.id, arg, supportedFormats)) {
                return false;
            }

        }

        // Contaminants
        if (aLine.hasOption(ReporterCLIParameters.CONTAMINANTS.id)) {

            String arg = aLine.getOptionValue(ReporterCLIParameters.CONTAMINANTS.id);
            HashSet<String> supportedFormats = new HashSet<String>(1);
            supportedFormats.add(".fasta");

            if (!CommandParameter.fileExists(ReporterCLIParameters.CONTAMINANTS.id, arg, supportedFormats)) {
                return false;
            }

        }

        return true;
    }

    @Override
    public Object call() throws IOException, ClassNotFoundException {

        // turn off illegal access log messages
        try {

            Class loggerClass = Class.forName("jdk.internal.module.IllegalAccessLogger");
            Field loggerField = loggerClass.getDeclaredField("logger");
            Class unsafeClass = Class.forName("sun.misc.Unsafe");
            Field unsafeField = unsafeClass.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            Object unsafe = unsafeField.get(null);
            Long offset = (Long) unsafeClass.getMethod("staticFieldOffset", Field.class).invoke(unsafe, loggerField);
            unsafeClass.getMethod("putObjectVolatile", Object.class, long.class, Object.class) //
                    .invoke(unsafe, loggerClass, offset, null);

        } catch (Throwable ex) {
            // ignore, i.e. simply show the warnings...
            //ex.printStackTrace();
        }

        // Parse command line
        reporterCLIInputBean = new ReporterCLIInputBean(line);

        // Load user preferences
        utilitiesUserParameters = utilitiesUserParameters.loadUserParameters();

        // Instantiate factories
        PeptideShaker.instantiateFacories(utilitiesUserParameters);

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
        modificationFactory = ModificationFactory.getInstance();

        // Initiate the waiting handler
        WaitingHandlerCLIImpl waitingHandler = new WaitingHandlerCLIImpl();

        // Set processing preferences
        ProcessingParameters processingParameters = new ProcessingParameters();
        processingParameters.setnThreads(reporterCLIInputBean.getnThreads());

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

        // Load the project from the psdb file
        ProjectImporter projectImporter = new ProjectImporter();
        psdbFile = reporterCLIInputBean.getPeptideShakerFile();
        setDbFolder(Reporter.getMatchesFolder());

        try {

            projectImporter.importPeptideShakerProject(this, spectrumFiles, waitingHandler);
            projectImporter.importReporterProject(this, waitingHandler);

        } catch (OutOfMemoryError error) {

            System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
            String errorText = "Reporter used up all the available memory and had to be stopped.<br>"
                    + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                    + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                    + "Java Options). See also <a href=\"https://compomics.github.io/projects/compomics-utilities/wiki/JavaTroubleShooting.html\">JavaTroubleShooting</a>.";
            waitingHandler.appendReport(errorText, true, true);
            error.printStackTrace();

            return 1;

        } catch (EOFException e) {

            String errorText = "An error occurred while reading:\n" + psdbFile + ".\n\n"
                    + "The file is corrupted and cannot be opened anymore.";
            waitingHandler.appendReport(errorText, true, true);
            e.printStackTrace();

            return 1;

        } catch (Exception e) {

            String errorText = "An error occurred while reading:\n" + psdbFile + ".\n\n"
                    + "Please verify that the Reporter version used to create\n"
                    + "the file is compatible with your version of Reporter.";
            waitingHandler.appendReport(errorText, true, true);
            e.printStackTrace();

            return 1;
        }

        DisplayPreferences displayPreferences = projectImporter.getDisplayPreferences();
        SpectrumProvider spectrumProvider = projectImporter.getSpectrumProvider();

        // Load project specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
        if (error != null) {
            System.out.println(error);
        }

        // Verify that ignored PTMs are recognized
        ArrayList<String> ignoredPtms = reporterCLIInputBean.getIgnoredPtms();
        if (ignoredPtms != null) {

            for (String ptmName : ignoredPtms) {

                Modification modification = modificationFactory.getModification(ptmName);

                if (modification == null) {
                    System.out.println("Modification " + ptmName + " not recognized.");
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

            String errorText = "The reporter quantification methods to use could not be inferred, "
                    + "please specify a method from the isotopic correction file as command line parameter.\n\n";
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
            reporterIonQuantification.assignSample(reagent, reagent);
        }

        // Set reference samples
        ArrayList<Integer> referenceIndexes = reporterCLIInputBean.getReferenceSamples();

        if (referenceIndexes != null) {

            ArrayList<String> referenceSamples = new ArrayList<String>(referenceIndexes.size());

            for (Integer index : referenceIndexes) {

                if (index > reagents.size()) {

                    System.out.println(
                            System.getProperty("line.separator")
                            + "Reference sample index " + index
                            + " is higher than the number of reagents ("
                            + reagents.size()
                            + ")."
                            + System.getProperty("line.separator")
                    );

                    return 1;
                }

                referenceSamples.add(reagents.get(index - 1));
            }

            reporterIonQuantification.setControlSamples(referenceSamples);
        }

        // Create quantification features generator
        QuantificationFeaturesGenerator quantificationFeaturesGenerator = new QuantificationFeaturesGenerator(
                new QuantificationFeaturesCache(),
                getIdentification(),
                getIdentificationFeaturesGenerator(),
                reporterSettings,
                reporterIonQuantification,
                identificationParameters.getSearchParameters(),
                identificationParameters.getSequenceMatchingParameters()
        );

        // Set Normalization factors
        NormalizationFactors normalizationFactors = reporterIonQuantification.getNormalizationFactors();

        if (!normalizationFactors.hasNormalizationFactors()) {

            try {
                Normalizer normalizer = new Normalizer();

                if (!normalizationFactors.hasPsmNormalisationFactors()) {

                    normalizer.setPsmNormalizationFactors(
                            reporterIonQuantification,
                            reporterSettings.getRatioEstimationSettings(),
                            reporterSettings.getNormalizationSettings(),
                            getIdentificationParameters().getSequenceMatchingParameters(),
                            getIdentification(),
                            spectrumProvider,
                            quantificationFeaturesGenerator,
                            processingParameters,
                            getIdentificationParameters().getSearchParameters(),
                            getIdentificationParameters().getFastaParameters(),
                            getIdentificationParameters().getPeptideVariantsParameters(),
                            exceptionHandler,
                            waitingHandler
                    );

                }

                if (!normalizationFactors.hasPeptideNormalisationFactors()) {

                    normalizer.setPeptideNormalizationFactors(
                            reporterIonQuantification,
                            reporterSettings.getRatioEstimationSettings(),
                            reporterSettings.getNormalizationSettings(),
                            getIdentificationParameters().getSequenceMatchingParameters(),
                            getIdentification(),
                            spectrumProvider,
                            quantificationFeaturesGenerator,
                            processingParameters,
                            getIdentificationParameters().getSearchParameters(),
                            getIdentificationParameters().getFastaParameters(),
                            getIdentificationParameters().getPeptideVariantsParameters(),
                            exceptionHandler,
                            waitingHandler
                    );

                }

                if (!normalizationFactors.hasProteinNormalisationFactors()) {

                    normalizer.setProteinNormalizationFactors(
                            reporterIonQuantification,
                            reporterSettings.getRatioEstimationSettings(),
                            reporterSettings.getNormalizationSettings(),
                            getIdentification(),
                            spectrumProvider,
                            getMetrics(),
                            quantificationFeaturesGenerator,
                            processingParameters,
                            getIdentificationParameters().getSearchParameters(),
                            getIdentificationParameters().getFastaParameters(),
                            getIdentificationParameters().getPeptideVariantsParameters(),
                            exceptionHandler,
                            waitingHandler
                    );

                }

            } catch (Exception e) {

                System.out.println(
                        System.getProperty("line.separator")
                        + "An error occurred while estimating the ratios."
                        + System.getProperty("line.separator")
                );

                e.printStackTrace();

                return 1;
            }

        }

        // Save the project in the psdb file
        File destinationFile = reporterCLIInputBean.getOutputFile();
        if (destinationFile == null) {
            destinationFile = psdbFile;
        } else {
            psdbFile = destinationFile;
        }

        try {

            ProjectSaver.saveProject(reporterSettings, reporterIonQuantification, displayPreferences, this, waitingHandler);
            waitingHandler.appendReport("Project saved as " + destinationFile.getAbsolutePath() + ".", true, true);

        } catch (Exception e) {

            System.out.println(
                    System.getProperty("line.separator")
                    + "An error occurred while saving the project."
                    + System.getProperty("line.separator")
            );

            e.printStackTrace();

            return 1;
        }

        // report export if needed
        ReportCLIInputBean reportCLIInputBean = reporterCLIInputBean.getReportCLIInputBean();

        // see if output folder is set, and if not set to the same folder as the psdb file
        if (reportCLIInputBean.getReportOutputFolder() == null) {
            reportCLIInputBean.setReportOutputFolder(destinationFile.getParentFile());
        }

        if (reportCLIInputBean.exportNeeded()) {

            waitingHandler.appendReport("Starting report export.", true, true);

            // export report(s)
            if (reportCLIInputBean.exportNeeded()) {

                for (String reportType : reportCLIInputBean.getReportTypes()) {

                    try {

                        CLIExportMethods.exportReport(
                                reportCLIInputBean,
                                reportType,
                                projectParameters.getProjectUniqueName(),
                                projectDetails,
                                identification,
                                geneMaps,
                                identificationFeaturesGenerator,
                                sequenceProvider,
                                msFileHandler,
                                proteinDetailsProvider,
                                quantificationFeaturesGenerator,
                                reporterIonQuantification,
                                reporterSettings,
                                identificationParameters,
                                displayParameters.getnAASurroundingPeptides(),
                                spectrumCountingParameters,
                                waitingHandler
                        );

                    } catch (Exception e) {

                        waitingHandler.appendReport(
                                "An error occurred while exporting the " + reportType + ".",
                                true,
                                true);

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

                        waitingHandler.appendReport(
                                "An error occurred while exporting the documentation for "
                                + reportType + ".",
                                true,
                                true
                        );

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

                waitingHandler.appendReport(
                        "An error occurred while creating folder "
                        + parent.getAbsolutePath() + ".",
                        true,
                        true
                );

                waitingHandler.setRunCanceled();
            }

            File fastaFile = new File(projectDetails.getFastaFile());

            try {
                ProjectExport.exportProjectAsZip(
                        zipFile,
                        fastaFile,
                        msFileHandler,
                        psdbFile,
                        false,
                        waitingHandler
                );

                final int NUMBER_OF_BYTES_PER_MEGABYTE = 1048576;
                double sizeOfZippedFile = Util.roundDouble(((double) zipFile.length() / NUMBER_OF_BYTES_PER_MEGABYTE), 2);

                waitingHandler.appendReport(
                        "Project zipped to \'"
                        + zipFile.getAbsolutePath()
                        + "\' (" + sizeOfZippedFile + " MB)",
                        true,
                        true
                );

            } catch (Exception e) {

                e.printStackTrace();

                waitingHandler.appendReport(
                        "An error occurred while attempting to zip project in "
                        + zipFile.getAbsolutePath() + ".",
                        true,
                        true
                );

                waitingHandler.setRunCanceled();
            }
        }

        waitingHandler.appendReportEndLine();

        if (waitingHandler.isRunCanceled()) {
            return 1;
        }

        waitingHandler.appendReport("Reporter processing completed.", true, true);

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

        if (reporterCLIInputBean.getReporterIonsLocation() != null) {
            reporterIonSelectionSettings.setReporterIonsLocation(reporterCLIInputBean.getReporterIonsLocation());
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

//    /**
//     * Close the Reporter instance. Closes file connections and deletes
//     * temporary files.
//     *
//     * @throws IOException thrown of IOException occurs
//     * @throws SQLException thrown if SQLException occurs
//     */
//    public void close() throws IOException, SQLException {
//        close(identification);
//    }
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
            if (identification != null) {
                identification.close(false);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        try {

            File matchFolder = Reporter.getMatchesFolder();
            File[] tempFiles = matchFolder.listFiles();

            if (tempFiles != null) {

                for (File currentFile : tempFiles) {

                    boolean deleted = IoUtil.deleteDir(currentFile);

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
                + "ReporterCLI estimates abundance ratios from PeptideShaker projects based on reporter ion quantification."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see https://compomics.github.io/projects/reporter.html "
                + "and https://compomics.github.io/projects/reporter/wiki/reportercli.html."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/reporter."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
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
            // check if there are updates to the paths
            String[] nonPathSettingArgsAsList = PathSettingsCLI.extractAndUpdatePathOptions(args);

            // parse the rest of the options   
            Options nonPathOptions = new Options();
            ReporterCLIParameters.createOptionsCLI(nonPathOptions);
            DefaultParser parser = new DefaultParser();
            CommandLine line = parser.parse(nonPathOptions, nonPathSettingArgsAsList);

            ReporterCLI reporterCLI = new ReporterCLI(args);

            if (!isValidCommandLine(line)) {

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
