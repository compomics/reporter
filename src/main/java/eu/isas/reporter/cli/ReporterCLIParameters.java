package eu.isas.reporter.cli;

import com.compomics.util.experiment.identification.parameters_cli.IdentificationParametersCLIParams;
import org.apache.commons.cli.Options;

/**
 * Command line option parameters for ReporterCLI.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public enum ReporterCLIParameters {

    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // IMPORTANT: Any change here must be reported in the wiki: 
    // (once the wiki exists)
    ///////////////////////////////////////////////////////////////////////////////////////////////////////////////
    
    ID("id", "The PeptideShaker project (.cpsx or .zip).", true),
    OUT("out", "Output file to save the project.", false),
    ISOTOPES("isotopes", "The isotope correction factors file (.xml file).", false),
    
    METHOD("method", "The reporter ion quantification method as used in the isotopic distribution file.", false),
    
    THREADS("threads", "Number of threads to use for the processing, default: the number of cores on the machine.", false);

    /**
     * Short Id for the CLI parameter.
     */
    public String id;
    /**
     * Explanation for the CLI parameter.
     */
    public String description;
    /**
     * Boolean indicating whether the parameter is mandatory.
     */
    public boolean mandatory;

    /**
     * Constructor.
     *
     * @param id the id
     * @param description the description
     * @param mandatory is the parameter mandatory
     */
    private ReporterCLIParameters(String id, String description, boolean mandatory) {
        this.id = id;
        this.description = description;
        this.mandatory = mandatory;
    }

    /**
     * Creates the options for the command line interface based on the possible
     * values.
     *
     * @param aOptions the options object where the options will be added
     */
    public static void createOptionsCLI(Options aOptions) {
        
        for (ReporterCLIParameters reporterCLIParameters : values()) {
            aOptions.addOption(reporterCLIParameters.id, true, reporterCLIParameters.description);
        }
        
        // Path setup
        aOptions.addOption(PathSettingsCLIParams.ALL.id, true, PathSettingsCLIParams.ALL.description);
    }

    /**
     * Returns the options as a string.
     *
     * @return the options as a string
     */
    public static String getOptionsAsString() {

        String output = "";
        String formatter = "%-25s";

        output += "Mandatory Parameters:\n\n";
        output += "-" + String.format(formatter, ID.id) + " " + ID.description + "\n";
        output += "-" + String.format(formatter, ISOTOPES.id) + " " + ISOTOPES.description + "\n";

        output += "Reporter Ion options:\n\n";
        output += "-" + String.format(formatter, METHOD.id) + " " + METHOD.description + "\n";
                
        output += "\n\nOutput:\n\n";
        output += "-" + String.format(formatter, OUT.id) + " " + OUT.description + "\n";
        
        output += "\n\nProcessing Options:\n\n";
        output += "-" + String.format(formatter, THREADS.id) + " " + THREADS.description + "\n";
        
        output += "\n\nAdvanced Options:\n\n";
//        output += "-" + String.format(formatter, REFERENCE_MASS.id) + " " + REFERENCE_MASS.description + "\n"; TODO
        
        output += "\n\nOptional Temporary Folder:\n\n";
        output += "-" + String.format(formatter, PathSettingsCLIParams.ALL.id) + " " + PathSettingsCLIParams.ALL.description + "\n";

        output += "\n\nOptional Input Parameters:\n\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IDENTIFICATION_PARAMETERS.id) + " " + IdentificationParametersCLIParams.IDENTIFICATION_PARAMETERS.description + "\n";
        
        output += "\n\n\nFor identification parameters options:\nReplace eu.isas.reporter.cmd.ReporterCLI with eu.isas.reportergui.cmd.IdentificationParametersCLI\n\n";

        return output;
    }
    

}
