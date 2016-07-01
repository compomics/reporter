package eu.isas.reporter.cli;

import com.compomics.software.settings.PathKey;
import com.compomics.util.experiment.biology.EnzymeFactory;
import com.compomics.util.experiment.biology.taxonomy.SpeciesFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.gui.filehandling.TempFilesManager;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.preferences.ReporterPathPreferences;
import eu.isas.reporter.utils.Properties;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
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
public class ReporterCLI implements Callable {

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

        // Start processing
        return null;
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
