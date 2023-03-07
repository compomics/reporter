package eu.isas.reporter.cli;

import com.compomics.software.cli.CommandLineUtils;
import com.compomics.cli.identification_parameters.IdentificationParametersInputBean;
import com.compomics.util.parameters.identification.IdentificationParameters;
import eu.isas.reporter.calculation.normalization.NormalizationType;
import eu.isas.reporter.settings.ReporterIonsLocationType;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import org.apache.commons.cli.CommandLine;

/**
 * This class is used to verify that a command line is valid and parse its
 * parameters.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
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
     * The report export options.
     */
    private ReportCLIInputBean reportCLIInputBean;
    /**
     * The name of the reporter ion method as set in the isotopic distribution
     * file.
     */
    private String reporterMethod = null;
    /**
     * The list of reference samples indexed by ordered reagent mass, 1 is the
     * first reagent.
     */
    private ArrayList<Integer> referenceSamples = null;
    /**
     * The reporter ion tolerance.
     */
    private Double reporterIonTolerance = null;
    /**
     * Boolean indicating whether the most accurate reporter ion should be used.
     */
    private Boolean mostAccurate = null;
    /**
     * The location of the reporter ions.
     */
    private ReporterIonsLocationType reporterIonsLocation = ReporterIonsLocationType.ms2Spectra;
    /**
     * The precursor window m/z tolerance.
     */
    private Double precMzTolerance = null;
    /**
     * Boolean indicating whether the precursor window m/z tolerance is in ppm.
     */
    private Boolean precMzTolerancePpm = null;
    /**
     * The precursor window RT tolerance in seconds.
     */
    private Double precRtTolerance = null;
    /**
     * Boolean indicating whether peptides presenting null intensities should be
     * ignored.
     */
    private Boolean ignoreNull = null;
    /**
     * Boolean indicating whether peptides presenting missed cleavages should be
     * ignored.
     */
    private Boolean ignoreMc = null;
    /**
     * The percentile for ratio window estimation.
     */
    private Double percentile = null;
    /**
     * The resolution for ratio estimation.
     */
    private Double resolution = null;
    /**
     * The minimum number of unique peptides to consider only those for a
     * protein group ratio estimation.
     */
    private Integer minUnique = null;
    /**
     * The list of PTMs to ignore.
     */
    private ArrayList<String> ignoredPtms = null;
    /**
     * The validation level for a PSM to be considered for quantification.
     */
    private Integer validationPsm = null;
    /**
     * The validation level for a peptide to be considered for quantification.
     */
    private Integer validationPeptide = null;
    /**
     * The validation level for a protein to be considered for quantification.
     */
    private Integer validationProtein = null;
    /**
     * The normalization to use for PSMs.
     */
    private NormalizationType psmNormalizationType = null;
    /**
     * The normalization to use for peptides.
     */
    private NormalizationType peptideNormalizationType = null;
    /**
     * The normalization to use for proteins.
     */
    private NormalizationType proteinNormalizationType = null;
    /**
     * FASTA file containing stable proteins.
     */
    private File stableProteins = null;
    /**
     * FASTA file containing contaminants.
     */
    private File contaminants = null;
    /**
     * File where to export the zipped folder.
     */
    private File zipExport = null;

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

        // the output file
        if (aLine.hasOption(ReporterCLIParameters.OUT.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.OUT.id);
            outputFile = new File(arg);
        }

        // the isotopes file
        if (aLine.hasOption(ReporterCLIParameters.ISOTOPES.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.ISOTOPES.id);
            isotopesFile = new File(arg);
        }

        // get the number of threads
        if (aLine.hasOption(ReporterCLIParameters.THREADS.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.THREADS.id);
            nThreads = Integer.parseInt(arg);
        }

        // get the reporter ion method
        if (aLine.hasOption(ReporterCLIParameters.METHOD.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.METHOD.id);
            reporterMethod = arg;
        }

        // get the reference samples
        if (aLine.hasOption(ReporterCLIParameters.REFERENCE.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.REFERENCE.id);
            referenceSamples = CommandLineUtils.getIntegerListFromString(arg, ",");
            Collections.sort(referenceSamples);
        }

        // get the reporter ion m/z tolerance
        if (aLine.hasOption(ReporterCLIParameters.ION_TOL.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.ION_TOL.id);
            Double input = Double.valueOf(arg);
            reporterIonTolerance = input;
        }

        // get the most accurate option
        if (aLine.hasOption(ReporterCLIParameters.MOST_ACCURATE.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.MOST_ACCURATE.id);
            Integer input = Integer.valueOf(arg);
            mostAccurate = input.equals(1);
        }

        // get the same spectra option
        if (aLine.hasOption(ReporterCLIParameters.REPORTER_IONS_LOCATION.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.REPORTER_IONS_LOCATION.id);
            Integer index = Integer.valueOf(arg);
            reporterIonsLocation = ReporterIonsLocationType.getReporterIonsLocationType(index);
        }

        // get the precursor ion m/z tolerance
        if (aLine.hasOption(ReporterCLIParameters.PREC_WINDOW_MZ_TOL.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.PREC_WINDOW_MZ_TOL.id);
            Double input = Double.valueOf(arg);
            precMzTolerance = input;
        }

        // get the precursor ion m/z tolerance unit
        if (aLine.hasOption(ReporterCLIParameters.PREC_WINDOW_MZ_TOL_PPM.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.PREC_WINDOW_MZ_TOL_PPM.id);
            Integer input = Integer.valueOf(arg);
            precMzTolerancePpm = input.equals(1);
        }

        // get the precursor ion RT tolerance
        if (aLine.hasOption(ReporterCLIParameters.PREC_WINDOW_RT_TOL.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.PREC_WINDOW_RT_TOL.id);
            Double input = Double.valueOf(arg);
            precRtTolerance = input;
        }

        // get the ignore null option
        if (aLine.hasOption(ReporterCLIParameters.IGNORE_NULL.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.IGNORE_NULL.id);
            Integer input = Integer.valueOf(arg);
            ignoreNull = input.equals(1);
        }

        // get the ignore missed cleavages option
        if (aLine.hasOption(ReporterCLIParameters.IGNORE_MC.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.IGNORE_MC.id);
            Integer input = Integer.valueOf(arg);
            ignoreMc = input.equals(1);
        }

        // get the percentile option for ratio estimation
        if (aLine.hasOption(ReporterCLIParameters.PERCENTILE.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.PERCENTILE.id);
            Double input = Double.valueOf(arg);
            percentile = input;
        }

        // get the resolution option for ratio estimation
        if (aLine.hasOption(ReporterCLIParameters.RESOLUTION.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.RESOLUTION.id);
            Double input = Double.valueOf(arg);
            resolution = input;
        }

        // get the number of unique peptides
        if (aLine.hasOption(ReporterCLIParameters.MIN_UNIQUE.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.MIN_UNIQUE.id);
            minUnique = Integer.valueOf(arg);
        }

        // get the ignored PTMs
        if (aLine.hasOption(ReporterCLIParameters.IGNORE_PTMS.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.IGNORE_PTMS.id);
            ignoredPtms = CommandLineUtils.splitInput(arg);
        }

        // get the PSM validation level
        if (aLine.hasOption(ReporterCLIParameters.VALIDATION_PSM.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.VALIDATION_PSM.id);
            validationPsm = Integer.valueOf(arg);
        }

        // get the peptide validation level
        if (aLine.hasOption(ReporterCLIParameters.VALIDATION_PEPTIDE.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.VALIDATION_PEPTIDE.id);
            validationPeptide = Integer.valueOf(arg);
        }

        // get the peptide validation level
        if (aLine.hasOption(ReporterCLIParameters.VALIDATION_PROTEIN.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.VALIDATION_PROTEIN.id);
            validationProtein = Integer.valueOf(arg);
        }

        // get the PSM normalization
        if (aLine.hasOption(ReporterCLIParameters.NORMALIZATION_PSM.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.NORMALIZATION_PSM.id);
            Integer index = Integer.valueOf(arg);
            psmNormalizationType = NormalizationType.getNormalizationType(index);
        }

        // get the peptide normalization
        if (aLine.hasOption(ReporterCLIParameters.NORMALIZATION_PEPTIDE.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.NORMALIZATION_PEPTIDE.id);
            Integer index = Integer.valueOf(arg);
            peptideNormalizationType = NormalizationType.getNormalizationType(index);
        }

        // get the protein normalization
        if (aLine.hasOption(ReporterCLIParameters.NORMALIZATION_PROTEIN.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.NORMALIZATION_PROTEIN.id);
            Integer index = Integer.valueOf(arg);
            proteinNormalizationType = NormalizationType.getNormalizationType(index);
        }

        // get the stable proteins
        if (aLine.hasOption(ReporterCLIParameters.STABLE_PROTEINS.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.STABLE_PROTEINS.id);
            stableProteins = new File(arg);
        }

        // get the contaminants
        if (aLine.hasOption(ReporterCLIParameters.CONTAMINANTS.id)) {
            arg = aLine.getOptionValue(ReporterCLIParameters.CONTAMINANTS.id);
            contaminants = new File(arg);
        }

        // zipped export
        if (aLine.hasOption(ReporterCLIParameters.ZIP.id)) {
            zipExport = new File(aLine.getOptionValue(ReporterCLIParameters.ZIP.id));
        }

        // Reports
        reportCLIInputBean = new ReportCLIInputBean(aLine);

        // identification parameters
        identificationParametersInputBean = new IdentificationParametersInputBean(aLine);

        // path settings
        pathSettingsCLIInputBean = new PathSettingsCLIInputBean(aLine);
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
     * Returns the list of reference samples indexed by ordered reagent mass, 1
     * is the first reagent.
     *
     * @return the list of reference samples indexed by ordered reagent mass, 1
     * is the first reagent
     */
    public ArrayList<Integer> getReferenceSamples() {
        return referenceSamples;
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
     * Returns the name of the reporter methods provided by the user.
     *
     * @return the name of the reporter method
     */
    public String getReporterMethod() {
        return reporterMethod;
    }

    /**
     * Returns the tolerance used for the reporter ion matching in the spectrum.
     *
     * @return the tolerance used for the reporter ion matching in the spectrum
     */
    public Double getReporterIonTolerance() {
        return reporterIonTolerance;
    }

    /**
     * Indicates whether the most accurate reporter ion should be used.
     *
     * @return a boolean indicating whether the most accurate reporter ion
     * should be used
     */
    public Boolean getMostAccurate() {
        return mostAccurate;
    }

    /**
     * Indicates whether the quantification peaks are in the same spectra as the
     * identification peaks.
     *
     * @return a boolean indicating whether the quantification peaks are in the
     * same spectra as the identification peaks
     */
    public ReporterIonsLocationType getReporterIonsLocation() {
        return reporterIonsLocation;
    }

    /**
     * Returns the m/z tolerance to use for the precursor ion window.
     *
     * @return the m/z tolerance to use for the precursor ion window
     */
    public Double getPrecMzTolerance() {
        return precMzTolerance;
    }

    /**
     * Indicates whether the precursor ion window m/z tolerance is in ppm.
     *
     * @return a boolean indicating whether the window precursor ion m/z
     * tolerance is in ppm
     */
    public Boolean getPrecMzTolerancePpm() {
        return precMzTolerancePpm;
    }

    /**
     * Returns the RT tolerance to use for the precursor ion window in seconds.
     *
     * @return the RT tolerance to use for the precursor ion window in seconds
     */
    public Double getPrecRtTolerance() {
        return precRtTolerance;
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
     * Returns a boolean indicating whether peptides presenting null intensities
     * should be ignored.
     *
     * @return a boolean indicating whether peptides presenting null intensities
     * should be ignored
     */
    public Boolean getIgnoreNull() {
        return ignoreNull;
    }

    /**
     * Returns a boolean indicating whether peptides presenting missed cleavages
     * should be ignored.
     *
     * @return a boolean indicating whether peptides presenting missed cleavages
     * should be ignored
     */
    public Boolean getIgnoreMc() {
        return ignoreMc;
    }

    /**
     * Returns the percentile for ratio window estimation.
     *
     * @return the percentile for ratio window estimation
     */
    public Double getPercentile() {
        return percentile;
    }

    /**
     * Returns the resolution for ratio estimation.
     *
     * @return the resolution for ratio estimation
     */
    public Double getResolution() {
        return resolution;
    }

    /**
     * Returns the minimum number of unique peptides to consider only those for
     * a protein group ratio estimation.
     *
     * @return the minimum number of unique peptides to consider only those for
     * a protein group ratio estimation
     */
    public Integer getMinUnique() {
        return minUnique;
    }

    /**
     * Returns the list of PTMs to ignore.
     *
     * @return the list of PTMs to ignore
     */
    public ArrayList<String> getIgnoredPtms() {
        return ignoredPtms;
    }

    /**
     * Returns the validation level to use for PSMs.
     *
     * @return the validation level to use for PSMs
     */
    public Integer getValidationPsm() {
        return validationPsm;
    }

    /**
     * Returns the validation level to use for peptides.
     *
     * @return the validation level to use for peptides
     */
    public Integer getValidationPeptide() {
        return validationPeptide;
    }

    /**
     * Returns the validation level to use for proteins.
     *
     * @return the validation level to use for proteins
     */
    public Integer getValidationProtein() {
        return validationProtein;
    }

    /**
     * Returns the PSM normalization.
     *
     * @return the PSM normalization
     */
    public NormalizationType getPsmNormalizationType() {
        return psmNormalizationType;
    }

    /**
     * Returns the peptide normalization.
     *
     * @return the peptide normalization
     */
    public NormalizationType getPeptideNormalizationType() {
        return peptideNormalizationType;
    }

    /**
     * Returns the protein normalization.
     *
     * @return the protein normalization
     */
    public NormalizationType getProteinNormalizationType() {
        return proteinNormalizationType;
    }

    /**
     * Returns the file containing the stable proteins.
     *
     * @return the file containing the stable proteins
     */
    public File getStableProteins() {
        return stableProteins;
    }

    /**
     * Returns the file containing the contaminants.
     *
     * @return the file containing the contaminants
     */
    public File getContaminants() {
        return contaminants;
    }

    /**
     * Returns the file where to export the project as zip file. Null if not
     * set.
     *
     * @return the file where to export the project as zip file
     */
    public File getZipExport() {
        return zipExport;
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
     * Returns the path settings provided by the user.
     *
     * @return the path settings provided by the user
     */
    public PathSettingsCLIInputBean getPathSettingsCLIInputBean() {
        return pathSettingsCLIInputBean;
    }

    /**
     * Returns the report export options required.
     *
     * @return the report export options required
     */
    public ReportCLIInputBean getReportCLIInputBean() {
        return reportCLIInputBean;
    }
}
