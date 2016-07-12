package eu.isas.reporter.cli;

import com.compomics.software.settings.PathKey;
import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.io.ProjectImporter;
import eu.isas.reporter.preferences.ReporterPathPreferences;
import eu.isas.reporter.utils.Properties;
import java.io.EOFException;
import java.io.File;
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
import org.apache.commons.compress.archivers.ArchiveException;

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
     * The enzyme factory.
     */
    private EnzymeFactory enzymeFactory = EnzymeFactory.getInstance();
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The command line.
     */
    private CommandLine line;
    /**
     * The compomics reporter methods factory.
     */
    private ReporterMethodFactory methodsFactory = ReporterMethodFactory.getInstance();

    /**
     * Construct a new ReporterCLI runnable from a list of arguments.
     *
     * @param args the command line arguments
     *
     * @throws org.apache.commons.cli.ParseException Exception thrown whenever
     * an error occurred while parsing the command line
     */
    private ReporterCLI(String[] args) throws ParseException {

        // load enzymes
        try {
            enzymeFactory.importEnzymes(Reporter.getEnzymesFile(Reporter.getJarFilePath()));
        } catch (Exception e) {
            System.out.println("An error occurred while loading the enzymes.");
            e.printStackTrace();
        }

        // load species
        try {
            SpeciesFactory speciesFactory = SpeciesFactory.getInstance();
            speciesFactory.initiate(Reporter.getJarFilePath());
        } catch (Exception e) {
            System.out.println("An error occurred while loading the species.");
            e.printStackTrace();
        }

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

        // Initiate the waiting handler
        WaitingHandlerCLIImpl waitingHandlerCLIImpl = new WaitingHandlerCLIImpl();

        // Set processing preferences
        ProcessingPreferences processingPreferences = new ProcessingPreferences();
        processingPreferences.setnThreads(reporterCLIInputBean.getnThreads());

        // Update the identification parameters if changed and save the changes
        IdentificationParameters identificationParameters = reporterCLIInputBean.getIdentificationParameters();
        File parametersFile = reporterCLIInputBean.getIdentificationParametersFile();
        if (parametersFile == null) {
            String name = identificationParameters.getName();
            if (name == null) {
                name = "SearchCLI.par";
            } else {
                name += ".par";
            }
            parametersFile = new File(reporterCLIInputBean.getOutputFile(), name);
            IdentificationParameters.saveIdentificationParameters(identificationParameters, parametersFile);
        }

        // Import the PeptideShaker project
        File selectedFile = reporterCLIInputBean.getPeptideShakerFile();
        try {
            loadPeptideShakerProject(selectedFile, waitingHandlerCLIImpl);
        } catch (Exception e) {
            waitingHandlerCLIImpl.appendReport(selectedFile.getAbsolutePath() + " could not be loaded.", true, true);
            e.printStackTrace();
            try {
                close();
            } catch (Exception eClose) {
                // Ignore
            }
            return 1;
        }

        // Load default quantification settings from the identification
        try {
            ProjectImporter projectImporter = new ProjectImporter();
            projectImporter.importPeptideShakerProject(this, waitingHandlerCLIImpl);
            projectImporter.importReporterProject(this, waitingHandlerCLIImpl);
        } catch (OutOfMemoryError error) {
            System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
            error.printStackTrace();
            String errorText = "PeptideShaker used up all the available memory and had to be stopped.<br>"
                    + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                    + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                    + "Java Options). See also <a href=\"http://compomics.github.io/compomics-utilities/wiki/javatroubleshooting.html\">JavaTroubleShooting</a>.";
            waitingHandlerCLIImpl.appendReport(errorText, true, true);
            return 1;
        } catch (EOFException e) {
            e.printStackTrace();
            String errorText = "An error occurred while reading:\n" + selectedFile + ".\n\n"
                    + "The file is corrupted and cannot be opened anymore.";
            waitingHandlerCLIImpl.appendReport(errorText, true, true);
            return 1;
        } catch (Exception e) {
            e.printStackTrace();
            String errorText = "An error occurred while reading:\n" + selectedFile + ".\n\n"
                    + "Please verify that the PeptideShaker version used to create\n"
                    + "the file is compatible with your version of Reporter.";
            waitingHandlerCLIImpl.appendReport(errorText, true, true);
            return 1;
        }

        // Get reporter ion method
        String selectedMethod = reporterCLIInputBean.getReporterMethod();

        return null;
    }

    /**
     * Loads a PeptideShaker project from the given file.
     *
     * @param peptideShakerFile the PeptideShaker file as .cpsx or zip
     * @param waitingHandler the waiting handler used to display progress to the
     * user
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     * @throws SQLException thrown of SQLException occurs exception thrown
     * whenever an error occurred while interacting with the database
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while saving the project
     * @throws org.apache.commons.compress.archivers.ArchiveException exception
     * thrown whenever an error occurs while untaring the file
     */
    private void loadPeptideShakerProject(File peptideShakerFile, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException, ArchiveException {

        try {
            if (Util.getExtension(peptideShakerFile).equalsIgnoreCase("zip")) {
                loadCpsFromZipFile(peptideShakerFile, Reporter.getMatchesFolder(), waitingHandler);
            } else if (peptideShakerFile != null) {
                loadCpsFile(Reporter.getMatchesFolder(), waitingHandler);
            } else {
                throw new IllegalArgumentException("PeptideShaker project input missing.");
            }
        } catch (SQLException e) {
            waitingHandler.appendReport("An error occurred while reading: " + peptideShakerFile + ". "
                    + "It looks like another instance of PeptideShaker is still connected to the file. "
                    + "Please close all instances of PeptideShaker and try again.", true, true);
            throw e;
        }

        // load fasta file
        if (!loadFastaFile(waitingHandler)) {
            throw new IllegalArgumentException("The FASTA file was not found. Please provide its location in the command line parameters.");
        }

        // load the spectrum files
        if (!loadSpectrumFiles(waitingHandler)) {
            if (identification.getSpectrumFiles().size() > 1) {
                waitingHandler.appendReport("The spectrum files were not found. Please provide their location in the command line parameters.", true, true);
            } else {
                waitingHandler.appendReport("The spectrum file was not found. Please provide its location in the command line parameters.", true, true);
            }
        }

        // Load project specific PTMs
        String error = PeptideShaker.loadModifications(getIdentificationParameters().getSearchParameters());
        if (error != null) {
            System.out.println(error);
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
                + "For further help see http://compomics.github.io/projects/reporter.html and http://compomics.github.io/reporter/wiki/reportercli.html." + System.getProperty("line.separator")
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
                System.exit(0);
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
     * redirects the error stream to the PeptideShaker.log of a given folder.
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
