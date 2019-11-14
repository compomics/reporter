package eu.isas.reporter.cli;

import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.export.ExportFormat;
import com.compomics.util.io.export.ExportScheme;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.quantification.spectrum_counting.SpectrumCountingParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.ReporterExportFactory;
import eu.isas.reporter.settings.ReporterSettings;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.commons.math.MathException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class groups standard methods used by the different command line
 * interfaces.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class CLIExportMethods {

    /**
     * Writes an export according to the command line settings contained in the
     * reportCLIInputBean.
     *
     * @param reportCLIInputBean the command line settings
     * @param reportType the report type
     * @param experiment the experiment of the project
     * @param projectDetails the project details of the project
     * @param identification the identification of the project
     * @param geneMaps the gene maps
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param sequenceProvider the sequence provider
     * @param proteinDetailsProvider the protein details provider
     * @param quantificationFeaturesGenerator the object generating the
     * quantification features
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param reporterSettings the reporter settings
     * @param identificationParameters the identification parameters used
     * @param nSurroundingAA the number of amino acids to export on the side of
     * peptide sequences
     * @param spectrumCountingParameters the spectrum counting parameters
     * @param waitingHandler waiting handler displaying feedback to the user
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     * @throws InterruptedException exception thrown whenever a threading issue
     * occurred while interacting with the database
     * @throws SQLException exception thrown whenever an SQL exception occurred
     * while interacting with the database
     * @throws ClassNotFoundException exception thrown whenever an exception
     * occurred while deserializing an object
     * @throws MzMLUnmarshallerException exception thrown whenever an exception
     * occurred while reading an mzML file
     * @throws org.apache.commons.math.MathException exception thrown whenever
     * an exception occurred while estimating the theoretical coverage of a
     * protein
     */
    public static void exportReport(ReportCLIInputBean reportCLIInputBean, String reportType, String experiment, 
            ProjectDetails projectDetails, Identification identification, GeneMaps geneMaps, IdentificationFeaturesGenerator identificationFeaturesGenerator, 
            SequenceProvider sequenceProvider, ProteinDetailsProvider proteinDetailsProvider, 
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, ReporterSettings reporterSettings,
            IdentificationParameters identificationParameters, int nSurroundingAA, SpectrumCountingParameters spectrumCountingParameters, WaitingHandler waitingHandler)
            throws IOException, IllegalArgumentException, SQLException, ClassNotFoundException,
            InterruptedException, MzMLUnmarshallerException, MathException {
        
        ReporterExportFactory exportFactory = ReporterExportFactory.getInstance();
        ExportScheme exportScheme = exportFactory.getExportScheme(reportType);
        
        String reportName = reportType.replaceAll(" ", "_");
        reportName = ReporterExportFactory.getDefaultReportName(experiment, reportName);
        if (reportCLIInputBean.getReportNamePrefix() != null) {
            reportName = reportCLIInputBean.getReportNamePrefix() + reportName;
        }
        
        File reportFile = new File(reportCLIInputBean.getReportOutputFolder(), ReporterExportFactory.getDefaultReportName(experiment, reportName));

        //@TODO: allow format selection
        ReporterExportFactory.writeExport(exportScheme, reportFile, ExportFormat.text, experiment, projectDetails, identification, 
                identificationFeaturesGenerator, sequenceProvider, proteinDetailsProvider, geneMaps, quantificationFeaturesGenerator, reporterIonQuantification, reporterSettings, identificationParameters,
                null, null, null, null, nSurroundingAA, spectrumCountingParameters, waitingHandler);
    }

    /**
     * Writes the documentation corresponding to an export given the command
     * line arguments.
     *
     * @param reportCLIInputBean the command line arguments
     * @param reportType the type of report of interest
     * @param waitingHandler waiting handler displaying feedback to the user
     *
     * @throws IOException exception thrown whenever an IO exception occurred
     * while reading or writing to a file
     */
    public static void exportDocumentation(ReportCLIInputBean reportCLIInputBean, String reportType, WaitingHandler waitingHandler) throws IOException {
        ReporterExportFactory exportFactory = ReporterExportFactory.getInstance();
        ExportScheme exportScheme = exportFactory.getExportScheme(reportType);
        File reportFile = new File(reportCLIInputBean.getReportOutputFolder(), ReporterExportFactory.getDefaultDocumentation(reportType));

        //@TODO: allow format selection
        ReporterExportFactory.writeDocumentation(exportScheme, ExportFormat.text, reportFile);
    }
}
