package eu.isas.reporter;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Enzyme;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.Quantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PeptideShakerSettings;
import eu.isas.reporter.io.DataLoader;
import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.gui.ReporterGUI;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.utils.Properties;
import java.io.File;
import java.util.ArrayList;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

/**
 * Reporter performs reporter ion based quantification on MS2 spectra.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class Reporter {

    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * List of caught exceptions.
     */
    private ArrayList<String> exceptionCaught = new ArrayList<String>();
    /**
     * The location of the folder used for serialization of matches.
     */
    public static final String SERIALIZATION_DIRECTORY = "resources/matches";
    /**
     * Modification file.
     */
    private final String MODIFICATIONS_FILE = "resources/conf/reporter_mods.xml";
    /**
     * User modification file.
     */
    private final String USER_MODIFICATIONS_FILE = "resources/conf/reporter_usermods.xml";
    /**
     * The name of the reporter experiment.
     */
    public static final String experimentObjectName = "experiment";
    /**
     * The PeptideShaker settings.
     */
    private PeptideShakerSettings psSettings;

    /**
     * Main method.
     *
     * @param args String[] with the start-up arguments.
     */
    public static void main(String[] args) {
        new Reporter(new ReporterGUI());
    }

    /**
     * Reporter constructor.
     *
     * @param reporterGUI reference to the Reporter GUI
     */
    public Reporter(ReporterGUI reporterGUI) {
        this.reporterGUI = reporterGUI;
        // check if a newer version of Reporter is available
        CompomicsWrapper.checkForNewVersion(new Properties().getVersion(), "Reporter", "reporter");
        loadModifications();
    }
    /**
     * The conducted experiment.
     */
    private MsExperiment experiment = null;
    /**
     * The sample analyzed.
     */
    private Sample sample = null;
    /**
     * The replicate number.
     */
    private int replicateNumber;
    /**
     * The main GUI.
     */
    private ReporterGUI reporterGUI;
    /**
     * The quantification preferences.
     */
    private QuantificationPreferences quantificationPreferences = new QuantificationPreferences();

    /**
     * This method terminates the program.
     *
     * @param aStatus int with the completion status.
     */
    public void close(int aStatus) {
        System.exit(aStatus);
    }

    /**
     * Returns the quantification preferences.
     *
     * @return the quantification preferences
     */
    public QuantificationPreferences getQuantificationPreferences() {
        return quantificationPreferences;
    }

    /**
     * Sets the quantification preferences.
     *
     * @param quantificationPreferences the quantification preferences
     */
    public void setQuantificationPreferences(QuantificationPreferences quantificationPreferences) {
        this.quantificationPreferences = quantificationPreferences;
    }

    /**
     * Returns the experiment conducted.
     *
     * @return the experiment conducted
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * Sets the experiment conducted.
     *
     * @param experiment the experiment conducted
     */
    public void setExperiment(MsExperiment experiment) {
        this.experiment = experiment;
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicate number
     */
    public int getReplicateNumber() {
        return replicateNumber;
    }

    /**
     * Sets the replicate number.
     *
     * @param replicateNumber the replicate number
     */
    public void setReplicateNumber(int replicateNumber) {
        this.replicateNumber = replicateNumber;
    }

    /**
     * Returns the sample analyzed.
     *
     * @return the sample analyzed
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * Sets the sample analyzed.
     *
     * @param sample the sample analyzed
     */
    public void setSample(Sample sample) {
        this.sample = sample;
    }

    /**
     * Loads identification and quantification information from files.
     *
     * @param mgfFiles the quantification files
     * @param waitingHandler
     */
    public void loadFiles(ArrayList<File> mgfFiles, WaitingHandler waitingHandler) {
        DataImporter dataImporter = new DataImporter(waitingHandler, mgfFiles);
        dataImporter.execute();
    }

    /**
     * Returns the processed quantification.
     *
     * @return the processed quantification
     */
    public ReporterIonQuantification getQuantification() {
        return (ReporterIonQuantification) experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getQuantification(Quantification.QuantificationMethod.REPORTER_IONS);
    }

    /**
     * Returns the identification. Null if none loaded.
     *
     * @return the identification
     */
    public Identification getIdentification() {
        if (experiment != null && sample != null && experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber) != null) {
            return experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        }
        return null;
    }

    /**
     * Returns the PeptideShaker settings.
     *
     * @return the PeptideShaker settings
     */
    public PeptideShakerSettings getPSSettings() {
        return psSettings;
    }

    /**
     * Sets the PeptideShaker settings.
     *
     * @param psSettings the peptide shaker settings
     */
    public void setPSSettings(PeptideShakerSettings psSettings) {
        this.psSettings = psSettings;
    }

    /**
     * Compiles the PSM ratios into peptides and proteins.
     *
     * @param waitingHandler a waiting handler displaying the progress. Progress
     * will be displayed on the secondary progress bar if any.
     */
    public void compileRatios(WaitingHandler waitingHandler) {
        try {
            Enzyme enzyme = psSettings.getSearchParameters().getEnzyme();
            ReporterIonQuantification quantification = getQuantification();
            RatioEstimator ratioEstimator = new RatioEstimator(quantification, quantification.getReporterMethod(), quantificationPreferences, enzyme);

            if (waitingHandler != null) {
                waitingHandler.setMaxSecondaryProgressCounter(quantification.getProteinQuantification().size());
                waitingHandler.setSecondaryProgressCounterIndeterminate(false);
            }
            
            // @TODO: has to use batch select/insert!!!

            for (String proteinKey : quantification.getProteinQuantification()) {
                ratioEstimator.estimateProteinRatios(proteinKey);
                if (waitingHandler != null) {
                    waitingHandler.increaseSecondaryProgressCounter();
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                }
            }
        } catch (Exception e) {
            catchException(e);
            waitingHandler.setRunCanceled();
        }
    }

    /**
     * Returns the desired spectrum.
     *
     * @param spectrumKey the key of the spectrum
     * @return the desired spectrum
     */
    public MSnSpectrum getSpectrum(String spectrumKey) {
        String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);
        String spectrumTitle = Spectrum.getSpectrumTitle(spectrumKey);
        try {
            return (MSnSpectrum) spectrumFactory.getSpectrum(spectrumFile, spectrumTitle);
        } catch (Exception e) {
            catchException(e);
            return null;
        }
    }

    /**
     * Method called whenever an exception is caught.
     *
     * @param e the exception caught
     */
    public void catchException(Exception e) {
        e.printStackTrace();
        if (!exceptionCaught.contains(e.getLocalizedMessage())) {
            exceptionCaught.add(e.getLocalizedMessage());
            JOptionPane.showMessageDialog(reporterGUI,
                    "An error occured while reading "
                    + e.getLocalizedMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Loads the modifications from the modification file.
     */
    private void loadModifications() {

        String path = getJarFilePath();

        try {
            ptmFactory.importModifications(new File(path, MODIFICATIONS_FILE), false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            ptmFactory.importModifications(new File(path, USER_MODIFICATIONS_FILE), true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + USER_MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("Reporter.class").getPath(), "Reporter");
    }

    /**
     * Worker used to process files.
     */
    private class DataImporter extends SwingWorker {

        /**
         * The waiting dialog will provide feedback to the user.
         */
        private WaitingHandler waitingHandler;
        /**
         * The identification files.
         */
        private ArrayList<File> idFiles;
        /**
         * The mgf files.
         */
        private ArrayList<File> mgfFiles;

        /**
         * Constructor.
         *
         * @param waitingHandler The waiting dialog will provide feedback to the
         * user
         * @param idFiles The identification files
         * @param mgfFiles The mgf files
         */
        public DataImporter(WaitingHandler waitingHandler, ArrayList<File> mgfFiles) {
            this.waitingHandler = waitingHandler;
            this.mgfFiles = mgfFiles;
        }

        @Override
        protected Object doInBackground() {
            Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            Quantification quantification = getQuantification();
            DataLoader dataLoader = new DataLoader(quantificationPreferences, waitingHandler, identification);
            waitingHandler.appendReport("Building peptides and protein quantification objects.", true, true);

            try {
                quantification.buildPeptidesAndProteinQuantifications(identification, waitingHandler);
            } catch (Exception e) {
                e.printStackTrace();
                waitingHandler.appendReport("An error occured whlie building peptide and protein quantification objects.\n" + e.getLocalizedMessage(), true, true);
                waitingHandler.setRunCanceled();
                return 1;
            }

            if (waitingHandler.isRunCanceled()) {
                return 1;
            }

            dataLoader.loadQuantification(getQuantification(), mgfFiles);
            waitingHandler.increasePrimaryProgressCounter();

            if (waitingHandler.isRunCanceled()) {
                return 1;
            }

            waitingHandler.appendReport("Estimating peptide and protein ratios.", true, true);
            compileRatios(waitingHandler);

            if (!waitingHandler.isRunCanceled()) {
                reporterGUI.displayResults(getQuantification(), getIdentification());
                waitingHandler.appendReportEndLine();
                waitingHandler.appendReport("Quantification Completed.", true, true);
                waitingHandler.setRunFinished();
            }

            return 0;
        }
    }
}
