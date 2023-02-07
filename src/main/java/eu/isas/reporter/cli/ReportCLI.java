package eu.isas.reporter.cli;

import com.compomics.util.experiment.biology.enzymes.EnzymeFactory;
import com.compomics.util.experiment.biology.modifications.Modification;
import com.compomics.util.experiment.biology.modifications.ModificationFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.io.IoUtil;
import com.compomics.util.parameters.UtilitiesUserParameters;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.cmd.PeptideShakerCLI;
import eu.isas.peptideshaker.utils.PsdbParent;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.calculation.QuantificationFeaturesCache;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.io.ProjectImporter;
import eu.isas.reporter.settings.ReporterSettings;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.reflect.Field;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Options;

/**
 * This class performs the command line export of reports in command line.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReportCLI extends PsdbParent {

    /**
     * The report command line options.
     */
    private ReportCLIInputBean reportCLIInputBean;
    /**
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory;
    /**
     * The Progress messaging handler reports the status throughout all Reporter
     * processes.
     */
    private WaitingHandler waitingHandler;
    /**
     * The modification factory.
     */
    private ModificationFactory modificationFactory;
    /**
     * The compomics reporter methods factory.
     */
    private ReporterMethodFactory methodsFactory = ReporterMethodFactory.getInstance();
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserParameters utilitiesUserParameters;
    /**
     * The spectrum files loaded.
     */
    private ArrayList<File> spectrumfFiles = new ArrayList<>();

    /**
     * Construct a new ReportCLI runnable from a ReportCLI Bean. When
     * initialization is successful, calling "run" will open the PeptideShaker
     * project and write the desired output files.
     *
     * @param reportCLIInputBean the input bean
     */
    public ReportCLI(ReportCLIInputBean reportCLIInputBean) {
        this.reportCLIInputBean = reportCLIInputBean;
    }

    /**
     * Calling this method will run the configured Reporter process.
     *
     * @return returns 1 if the process was canceled
     */
    public Object call() {

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

        setDbFolder(PeptideShaker.getMatchesFolder());

        // load user preferences
        utilitiesUserParameters = UtilitiesUserParameters.loadUserParameters();

        // instantiate factories
        PeptideShaker.instantiateFacories(utilitiesUserParameters);
        modificationFactory = ModificationFactory.getInstance();
        enzymeFactory = EnzymeFactory.getInstance();

        // load the species
        loadSpecies();

        // load default methods
        try {
            methodsFactory.importMethods(Reporter.getMethodsFile());
        } catch (Exception e) {
            System.out.println("An error occurred while loading the methods.");
            e.printStackTrace();
            return 1;
        }

        // set waiting handler
        waitingHandler = new WaitingHandlerCLIImpl();

        // Load the project from the psdb file
        ProjectImporter projectImporter = new ProjectImporter();
        File selectedFile = reportCLIInputBean.getPsdbFile();
        this.setPsdbFile(selectedFile);

        try {

            projectImporter.importPeptideShakerProject(this, spectrumfFiles, waitingHandler);
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

        } catch (Exception e) {

            String errorText = "An error occurred while reading:\n" + selectedFile + ".\n\n"
                    + "Please verify that the Reporter version used to create\n"
                    + "the file is compatible with your version of Reporter.";
            waitingHandler.appendReport(errorText, true, true);
            e.printStackTrace();

            return 1;

        }

        // load project-specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());

        if (error != null) {

            System.out.println(error);

        }

        // get previously set quantification settings or defaults from the identification results
        ReporterSettings reporterSettings = projectImporter.getReporterSettings();
        ReporterIonQuantification reporterIonQuantification = projectImporter.getReporterIonQuantification();
        ReporterMethod selectedMethod = reporterIonQuantification.getReporterMethod();

        // verify that ignored PTMs are recognized
        HashSet<String> ignoredPtms = reporterSettings.getRatioEstimationSettings().getExcludingPtms();

        if (ignoredPtms != null) {

            for (String ptmName : ignoredPtms) {

                Modification ptm = modificationFactory.getModification(ptmName);

                if (ptm == null) {

                    System.out.println("PTM " + ptmName + " not recognized.");
                    return 1;

                }

            }

        }

        // create quantification features generator
        QuantificationFeaturesGenerator quantificationFeaturesGenerator = new QuantificationFeaturesGenerator(
                new QuantificationFeaturesCache(),
                getIdentification(),
                getIdentificationFeaturesGenerator(),
                reporterSettings, reporterIonQuantification,
                identificationParameters.getSearchParameters(),
                identificationParameters.getSequenceMatchingParameters()
        );

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
                            spectrumCountingParameters, waitingHandler
                    );

                } catch (Exception e) {

                    waitingHandler.appendReport("An error occurred while exporting the " + reportType + ".", true, true);
                    e.printStackTrace();
                    waitingHandler.setRunCanceled();

                }
            }
        }

        // export documentation(s)
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

        try {

            PeptideShakerCLI.closePeptideShaker(identification);

        } catch (Exception e2) {

            waitingHandler.appendReport("An error occurred while closing Reporter.", true, true);
            e2.printStackTrace();

        }

        if (!waitingHandler.isRunCanceled()) {

            waitingHandler.appendReport("Report export completed.", true, true);
            System.exit(0); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.

            // Note that if a different solution is found, 
            // the DummyFrame has to be closed similar to 
            // the setVisible method in the WelcomeDialog!!
            return 0;

        } else {

            System.exit(1); // @TODO: Find other ways of cancelling the process? If not cancelled searchgui will not stop.

            // Note that if a different solution is found, 
            // the DummyFrame has to be closed similar to 
            // the setVisible method in the WelcomeDialog!!
            return 1;

        }
    }

    /**
     * Reporter report CLI header message when printing the usage.
     */
    private static String getHeader() {

        return System.getProperty("line.separator")
                + "The Reporter report command line takes a psdb file and generates various types of reports."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "For further help see https://compomics.github.io/projects/reporter.html "
                + "and https://compomics.github.io/projects/reporter/wiki/reportercli.html."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "Or contact the developers at https://groups.google.com/group/reporter_software."
                + System.getProperty("line.separator")
                + System.getProperty("line.separator")
                + "----------------------"
                + System.getProperty("line.separator")
                + "OPTIONS"
                + System.getProperty("line.separator")
                + "----------------------" + System.getProperty("line.separator")
                + System.getProperty("line.separator");

    }

    /**
     * Verifies the command line start parameters.
     *
     * @return true if the startup was valid
     */
    private static boolean isValidStartup(CommandLine aLine) throws IOException {

        if (aLine.getOptions().length == 0) {
            return false;
        }

        if (!aLine.hasOption(ReportCLIParams.PSDB_FILE.id)
                || ((String) aLine.getOptionValue(ReportCLIParams.PSDB_FILE.id)).equals("")) {

            System.out.println("\n" + ReportCLIParams.PSDB_FILE.description + " not specified.\n");
            return false;

        } else {

            String fileTxt = aLine.getOptionValue(ReportCLIParams.PSDB_FILE.id);
            File testFile = new File(fileTxt.trim());

            if (!testFile.exists()) {

                System.out.println(
                        "\n"
                        + ReportCLIParams.PSDB_FILE.description
                        + " \'"
                        + testFile.getAbsolutePath()
                        + "\' not found.\n"
                );

                return false;

            }

        }

        if (!aLine.hasOption(ReportCLIParams.EXPORT_FOLDER.id)
                || ((String) aLine.getOptionValue(ReportCLIParams.EXPORT_FOLDER.id)).equals("")) {

            System.out.println(
                    "\n"
                    + ReportCLIParams.EXPORT_FOLDER.description
                    + " not specified.\n"
            );

            return false;

        } else {

            String fileTxt = aLine.getOptionValue(ReportCLIParams.EXPORT_FOLDER.id);
            File testFile = new File(fileTxt.trim());

            if (!testFile.exists()) {

                System.out.println(
                        "\n"
                        + ReportCLIParams.EXPORT_FOLDER.description
                        + " \'"
                        + testFile.getAbsolutePath()
                        + "\' not found.\n"
                );

                return false;

            }

        }

        return true;
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
            ReportCLIParams.createOptionsCLI(nonPathOptions);
            DefaultParser parser = new DefaultParser();
            CommandLine line = parser.parse(nonPathOptions, nonPathSettingArgsAsList);

            if (!isValidStartup(line)) {

                PrintWriter lPrintWriter = new PrintWriter(System.out);

                lPrintWriter.print(
                        System.getProperty("line.separator")
                        + "==============================================="
                        + System.getProperty("line.separator")
                );

                lPrintWriter.print(
                        "Reporter Report Exporter - Command Line"
                        + System.getProperty("line.separator")
                );

                lPrintWriter.print(
                        "==============================================="
                        + System.getProperty("line.separator")
                );

                lPrintWriter.print(getHeader());
                lPrintWriter.print(ReportCLIParams.getOptionsAsString());
                lPrintWriter.flush();
                lPrintWriter.close();

                System.exit(0);

            } else {

                ReportCLIInputBean lCLIBean = new ReportCLIInputBean(line);
                ReportCLI cli = new ReportCLI(lCLIBean);
                cli.call();

            }

        } catch (OutOfMemoryError e) {

            System.out.println(
                    "<CompomicsError>Reporter used up all the memory and had to be stopped. "
                    + "See the Reporter log for details.</CompomicsError>"
            );

            System.err.println("Ran out of memory!");

            System.err.println(
                    "Memory given to the Java virtual machine: "
                    + Runtime.getRuntime().maxMemory() + "."
            );

            System.err.println(
                    "Memory used by the Java virtual machine: "
                    + Runtime.getRuntime().totalMemory() + "."
            );

            System.err.println(
                    "Free memory in the Java virtual machine: "
                    + Runtime.getRuntime().freeMemory() + "."
            );

            e.printStackTrace();
            System.exit(1);

        } catch (Exception e) {

            System.out.print(
                    "<CompomicsError>Reporter processing failed. "
                    + "See the Reporter log for details.</CompomicsError>"
            );

            e.printStackTrace();
            System.exit(1);

        }
    }

    @Override
    public String toString() {

        return "FollowUpCLI{" + ", cliInputBean=" + reportCLIInputBean + '}';

    }

    /**
     * Close the Reporter instance by clearing up factories and cache.
     *
     * @throws IOException thrown if an exception occurred when closing the
     * connection to a file
     * @throws SQLException thrown if an exception occurred when closing the
     * connection to the back-end database
     * @throws java.lang.InterruptedException thrown if a threading error
     * occurred
     */
    public void closePeptideShaker() throws IOException, SQLException, InterruptedException {

        identification.close(false);

        File matchFolder = PeptideShaker.getMatchesFolder();

        File[] tempFiles = matchFolder.listFiles();

        if (tempFiles != null) {
            for (File currentFile : tempFiles) {
                IoUtil.deleteDir(currentFile);
            }
        }

    }

    /**
     * Loads the species from the species file into the species factory.
     */
    private void loadSpecies() {

        try {

            SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
            speciesFactory.initiate(Reporter.getJarFilePath());

        } catch (Exception e) {

            System.out.println("An error occurred while loading the species.");
            e.printStackTrace();

        }

    }
}
