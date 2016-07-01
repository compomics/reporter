package eu.isas.reporter.cli;

import com.compomics.software.cli.CommandParameter;
import com.compomics.util.Util;
import com.compomics.util.experiment.identification.parameters_cli.IdentificationParametersInputBean;
import com.compomics.util.preferences.IdentificationParameters;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import org.apache.commons.cli.CommandLine;

/**
 * This class is used to verify that a command line is valid and parse its
 * parameters.
 *
 * @author Marc Vaudel
 */
public class ReporterCLIInputBean {

    /**
     * The file containing the identification results.
     */
    private File peptideShakerFile;
    /**
     * File where to save the output.
     */
    private File outputFile = null;
    /**
     * The file containing the isotope correction factors.
     */
    private File isotopesFile = null;
    /**
     * The number of threads.
     */
    private int nThreads = Runtime.getRuntime().availableProcessors();
    /**
     * The identification parameters input.
     */
    private IdentificationParametersInputBean identificationParametersInputBean;
    /**
     * The path settings.
     */
    private PathSettingsCLIInputBean pathSettingsCLIInputBean;

    /**
     * Parses the arguments of a command line.
     *
     * @param aLine the command line
     * 
     * @throws IOException thrown if an error occurred while reading the FASTA
     * file
     * @throws ClassNotFoundException thrown if the search parameters cannot be
     * converted
     */
    public ReporterCLIInputBean(CommandLine aLine) throws IOException, ClassNotFoundException {

        // PeptideShaker file
        String arg = aLine.getOptionValue(ReporterCLIParameters.ID.id);
        peptideShakerFile = new File(arg);

        // The output file
        if (aLine.hasOption(ReporterCLIParameters.OUT.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.OUT.id);
            outputFile = new File(arg);
        }

        // The isotopes file
        if (aLine.hasOption(ReporterCLIParameters.ISOTOPES.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.ISOTOPES.id);
            isotopesFile = new File(arg);
        }

        // get the number of threads
        if (aLine.hasOption(ReporterCLIParameters.THREADS.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.THREADS.id);
            nThreads = new Integer(arg);
        }

        // identification parameters
        identificationParametersInputBean = new IdentificationParametersInputBean(aLine);

        // path settings
        pathSettingsCLIInputBean = new PathSettingsCLIInputBean(aLine);

    }

    /**
     * Verifies that the command line is valid.
     *
     * @param aLine the command line to validate
     *
     * @return a boolean indicating whether the command line is valid.
     */
    public static boolean isValidStartup(CommandLine aLine) {

        // PeptideShaker file
        if (!aLine.hasOption(ReporterCLIParameters.ID.id) || ((String) aLine.getOptionValue(ReporterCLIParameters.ID.id)).equals("")) {
            System.out.println(System.getProperty("line.separator") + "PeptideShaker file not specified." + System.getProperty("line.separator"));
            return false;
        } else {
            String arg = aLine.getOptionValue(ReporterCLIParameters.ID.id);
            File input = new File(arg);
            if (!input.exists()) {
                System.out.println(System.getProperty("line.separator") + "PeptideShaker file \'" + input.getName() + "\' not found." + System.getProperty("line.separator"));
            }
            String extension = Util.getExtension(input);
            if (!extension.equals("cpsx") || !extension.equals("zip")) {
                System.out.println(System.getProperty("line.separator") + "Format \'" + extension + "\' not supported for PeptideShaker file." + System.getProperty("line.separator"));
            }
        }

        // The isotopes file
        if (aLine.hasOption(ReporterCLIParameters.ISOTOPES.id)) {
            String arg = aLine.getOptionValue(ReporterCLIParameters.ISOTOPES.id);
            File input = new File(arg);
            if (!input.exists()) {
                System.out.println(System.getProperty("line.separator") + "Isotope correction factors file \'" + input.getName() + "\' not found." + System.getProperty("line.separator"));
            }
            String extension = Util.getExtension(input);
            if (!extension.equals("xml")) {
                System.out.println(System.getProperty("line.separator") + "Format \'" + extension + "\' not supported for Isotope correction factors file." + System.getProperty("line.separator"));
            }
        }

        // get the number of threads
        if (aLine.hasOption(ReporterCLIParameters.THREADS.id)) {
            String arg = aLine.getOptionValue(ReporterCLIParameters.THREADS.id);
            if (!CommandParameter.isPositiveInteger(ReporterCLIParameters.THREADS.id, arg, false)) {
                return false;
            }
        }
        
        return true;
    }

    /**
     * Returns the PeptideShaker file.
     * 
     * @return the PeptideShaker file
     */
    public File getPeptideShakerFile() {
        return peptideShakerFile;
    }

    /**
     * Returns the output file where to save the project, null if not set.
     * 
     * @return the output file
     */
    public File getOutputFile() {
        return outputFile;
    }

    /**
     * Returns the isotope correction file, null if not set.
     * 
     * @return the isotope correction file
     */
    public File getIsotopesFile() {
        return isotopesFile;
    }

    /**
     * Returns the number of threads to use.
     * 
     * @return the number of threads to use
     */
    public int getnThreads() {
        return nThreads;
    }

    /**
     * Returns the identification parameters.
     *
     * @return the identification parameters
     */
    public IdentificationParameters getIdentificationParameters() {
        return identificationParametersInputBean.getIdentificationParameters();
    }

    /**
     * Returns the identification parameters file.
     *
     * @return the identification parameters file
     */
    public File getIdentificationParametersFile() {
        if (identificationParametersInputBean.getDestinationFile() != null) {
            return identificationParametersInputBean.getDestinationFile();
        } else {
            return identificationParametersInputBean.getInputFile();
        }
    }

    /**
     * Returns the path settings provided by the user.
     *
     * @return the path settings provided by the user
     */
    public PathSettingsCLIInputBean getPathSettingsCLIInputBean() {
        return pathSettingsCLIInputBean;
    }
}
