package eu.isas.reporter.cli;

import com.compomics.cli.identification_parameters.IdentificationParametersCLIParams;
import eu.isas.reporter.calculation.normalization.NormalizationType;
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
    ID("id", "The PeptideShaker project (.psdb or .zip).", true),
    OUT("out", "Output file to save the project.", false),
    ISOTOPES("isotopes", "The isotope correction factors file (.xml file). Default values used if not provided. It is strongly advised to provide the values corresponding to the labelling kit used during the experiment.", false),
    METHOD("method", "The reporter ion quantification method to use from the isotopic methods file in case multiple methods are listed in the file. Inferred from the identification parameters if not provided.", false),
    REFERENCE("ref_samples", "The reference sample(s) as a comma separated list of integers where each sample is represented by its reagent ordered by mass starting from 1. Ex: 1,3 represent reagents 144 and 116 with iTRAQ 4-plex. Default: no reference.", false),
    ION_TOL("ion_tol", "The reporter ion m/z tolerance. Default value inferred from the identification parameters and reporter method.", false),
    MOST_ACCURATE("most_accurate", "Indicates whether the ion within the m/z tolerance with the most accurate m/z should be selected (1: yes, 0: no). If no, the most intense ion will be selected. Default value inferred from the identification parameters.", false),
    SAME_SPECTRA("same_spectra", "Indicates whether reporter ions are in the same spectra as the identification fragment ions (1: yes, 0: no). If no, the spectra from prescursor in an m/z and RT window around the identified precursor will be used. Default is 1.", false),
    PREC_WINDOW_MZ_TOL("prec_window_mz_tol", "If " + SAME_SPECTRA.name() + " is set to 0, the m/z tolerance to use. Default is 1.", false),
    PREC_WINDOW_MZ_TOL_PPM("prec_window_mz_tol_ppm", "If " + SAME_SPECTRA.name() + " is set to 0, indicates whether the m/z tolerance to use is in ppm (1: yes, 0: no). Default is 1.", false),
    PREC_WINDOW_RT_TOL("prec_window_rt_tol", "If " + SAME_SPECTRA.name() + " is set to 0, the rt tolerance in seconds to use. Default is 10. Will be used only if available in the spectrum file.", false),
    IGNORE_NULL("ignore_null", "Ignore spectra where null intensities are found for at least one of the reporter ions (1: yes, 0: no). Default is 0.", false),
    IGNORE_MC("ignore_mc", "Ignore peptides with missed cleavages (1: yes, 0: no). Default is 0.", false),
    PERCENTILE("percentile", "Share of ratios to consider for the likelihood estimator window setting in percent. Default is 68%.", false),
    RESOLUTION("resolution", "Resolution to use for ratios calculation. Default is 0.01.", false),
    MIN_UNIQUE("min_unique", "Minimum number of unique peptides required to consider only those for a protein group ratio estimation, ignored if negative. Default is 3.", false),
    IGNORE_PTMS("ignore_ptms", "Ignore peptides carrying PTMs from this comma separated list. The lists of implemented and searched PTMs are available in the identification parameters.", false),
    VALIDATION_PSM("validation_psm", "Validation level for a PSM to be considered for quantification (0: all, 1: validated, 2: confident). Default is 1.", false),
    VALIDATION_PEPTIDE("validation_peptide", "Validation level for a peptide to be considered for quantification (0: all, 1: validated, 2: confident). Default is 1.", false),
    VALIDATION_PROTEIN("validation_protein", "Validation level for a protein to be considered for quantification (0: all, 1: validated, 2: confident). Default is 1.", false),
    NORMALIZATION_PSM("normalization_psm", "Normalization at the PSM level (" + NormalizationType.getCommandLineDescription() + "). Default is 0.", false),
    NORMALIZATION_PEPTIDE("normalization_peptide", "Normalization at the peptide level (" + NormalizationType.getCommandLineDescription() + "). Default is 1.", false),
    NORMALIZATION_PROTEIN("normalization_protein", "Normalization at the protein level (" + NormalizationType.getCommandLineDescription() + "). Default is 1.", false),
    STABLE_PROTEINS("stable_proteins", "Path to a FASTA file containing proteins to consider most stable between samples.", false),
    CONTAMINANTS("contaminants", "Path to a FASTA file containing proteins to consider as contaminants. Default is resources/crap.fasta.", false),
    ZIP("zip", "Exports the entire project as a zip file in the file specified.", false),
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

        // report options
        ReportCLIParams.createOptionsCLI(aOptions);

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

        output += "Mandatory Parameters:\n";
        output += "-" + String.format(formatter, ID.id) + " " + ID.description + "\n";
        output += "-" + String.format(formatter, ISOTOPES.id) + " " + ISOTOPES.description + "\n";
        output += "-" + String.format(formatter, METHOD.id) + " " + METHOD.description + "\n";

        output += "\n\nOutput:\n";
        output += "-" + String.format(formatter, OUT.id) + " " + OUT.description + "\n";

        output += "\n\nOptional Export Parameters:\n";
        output += "-" + String.format(formatter, ZIP.id) + " " + ZIP.description + "\n";

        output += "\n\nProcessing Options:\n";
        output += "-" + String.format(formatter, THREADS.id) + " " + THREADS.description + "\n";

        output += "\n\nAdvanced Options:\n";
//        output += "-" + String.format(formatter, REFERENCE_MASS.id) + " " + REFERENCE_MASS.description + "\n"; TODO

        output += "\n\nOptional Temporary Folder:\n";
        output += "-" + String.format(formatter, PathSettingsCLIParams.ALL.id) + " " + PathSettingsCLIParams.ALL.description + "\n";

        output += "\n\nOptional Input Parameters:\n";
        output += "-" + String.format(formatter, IdentificationParametersCLIParams.IDENTIFICATION_PARAMETERS.id) + " " + IdentificationParametersCLIParams.IDENTIFICATION_PARAMETERS.description + "\n";

        output += "\n\n\nFor identification parameters options:\nReplace eu.isas.reporter.cmd.ReporterCLI with eu.isas.reportergui.cmd.IdentificationParametersCLI\n\n";
        output += "\nFor report export options:\nReplace eu.isas.reporter.cli.ReporterCLI with eu.isas.reporter.cli.ReportCLI\n";

        return output;
    }
}
