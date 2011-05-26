package eu.isas.reporter;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import eu.isas.reporter.calculation.DataLoader;
import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.gui.ReporterGUI;
import eu.isas.reporter.gui.WaitingDialog;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.utils.Properties;
import java.io.File;
import java.util.ArrayList;
import javax.swing.SwingWorker;

/**
 * Reporter performs reporter ion based quantification on MS2 spectra
 *
 * @author Marc Vaudel
 */
public class Reporter {

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
     */
    public Reporter(ReporterGUI reporterGUI) {
        this.reporterGUI = reporterGUI;
        // check if a newer version of Reporter is available
        checkForNewVersion(new Properties().getVersion());
    }
    /**
     * The conducted experiment
     */
    private MsExperiment experiment;
    /**
     * The sample analyzed
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;
    /**
     * The quantification method used
     */
    private int quantificationMethodUsed;
    /**
     * The main GUI
     */
    private ReporterGUI reporterGUI;

    /**
     * Check if a newer version of reporter is available.
     *
     * @param currentVersion the version number of the currently running reporter
     */
    private static void checkForNewVersion(String currentVersion) {
        /*
        try {
        boolean deprecatedOrDeleted = false;
        URL downloadPage = new URL(
        "http://code.google.com/p/reporter/downloads/detail?name=reporter-" +
        currentVersion + ".zip");
        int respons = ((java.net.HttpURLConnection) downloadPage.openConnection()).getResponseCode();

        // 404 means that the file no longer exists, which means that
        // the running version is no longer available for download,
        // which again means that a never version is available.
        if (respons == 404) {
        deprecatedOrDeleted = true;
        } else {

        // also need to check if the available running version has been
        // deprecated (but not deleted)
        BufferedReader in = new BufferedReader(
        new InputStreamReader(downloadPage.openStream()));

        String inputLine;

        while ((inputLine = in.readLine()) != null && !deprecatedOrDeleted) {
        if (inputLine.lastIndexOf("Deprecated") != -1 &&
        inputLine.lastIndexOf("Deprecated Downloads") == -1 &&
        inputLine.lastIndexOf("Deprecated downloads") == -1) {
        deprecatedOrDeleted = true;
        }
        }

        in.close();
        }

        // informs the user about an updated version of the converter, unless the user
        // is running a beta version
        if (deprecatedOrDeleted && currentVersion.lastIndexOf("beta") == -1) {
        int option = JOptionPane.showConfirmDialog(null,
        "A newer version of reporter is available.\n" +
        "Do you want to upgrade?",
        "Upgrade Available",
        JOptionPane.YES_NO_CANCEL_OPTION);
        if (option == JOptionPane.YES_OPTION) {
        BareBonesBrowserLaunch.openURL("http://reporter.googlecode.com/");
        System.exit(0);
        } else if (option == JOptionPane.CANCEL_OPTION) {
        System.exit(0);
        }
        }
        } catch (MalformedURLException e) {
        e.printStackTrace();
        } catch (IOException e) {
        e.printStackTrace();
        }*/
    }

    /**
     * This method terminates the program.
     *
     * @param aStatus int with the completion status.
     */
    public void close(int aStatus) {
        System.exit(aStatus);
    }
    /**
     * The quantification preferences
     */
    private QuantificationPreferences quantificationPreferences = new QuantificationPreferences();

    /**
     * returns the quantification preferences
     * @return the quantification preferences
     */
    public QuantificationPreferences getQuantificationPreferences() {
        return quantificationPreferences;
    }

    /**
     * sets the quantification preferences
     * @param quantificationPreferences the quantification preferences
     */
    public void setQuantificationPreferences(QuantificationPreferences quantificationPreferences) {
        this.quantificationPreferences = quantificationPreferences;
    }

    /**
     * Returns the experiment conducted
     * @return the experiment conducted
     */
    public MsExperiment getExperiment() {
        return experiment;
    }

    /**
     * Sets  the experiment conducted
     * @param experiment the experiment conducted
     */
    public void setExperiment(MsExperiment experiment) {
        this.experiment = experiment;
    }

    /**
     * Returns the replicate number
     * @return the replicate number
     */
    public int getReplicateNumber() {
        return replicateNumber;
    }

    /**
     * Sets the replicate number
     * @param replicateNumber the replicate number
     */
    public void setReplicateNumber(int replicateNumber) {
        this.replicateNumber = replicateNumber;
    }

    /**
     * Returns the sample analyzed
     * @return the sample analyzed
     */
    public Sample getSample() {
        return sample;
    }

    /**
     * Sets the sample analyzed
     * @param sample the sample analyzed
     */
    public void setSample(Sample sample) {
        this.sample = sample;
    }

    /**
     * returns the quantification method used
     * @return the quantification method used
     */
    public int getQuantificationMethodUsed() {
        return quantificationMethodUsed;
    }

    /**
     * sets the quantification method used
     * @param quantificationMethodUsed the quantification method used
     */
    public void setQuantificationMethodUsed(int quantificationMethodUsed) {
        this.quantificationMethodUsed = quantificationMethodUsed;
    }

    /**
     * Loads identification and quantification information from files
     * @param idFiles   the identification files
     * @param mgfFiles  the quantification files
     */
    public void loadFiles(ArrayList<File> idFiles, ArrayList<File> mgfFiles) {
        WaitingDialog waitingDialog = new WaitingDialog(reporterGUI, true, this);
        DataImporter dataImporter = new DataImporter(waitingDialog, idFiles, mgfFiles);
        dataImporter.execute();
        waitingDialog.setVisible(true);
    }

    /**
     * displays the results in the main gui
     */
    public void updateResults() {
        RatiosCompilator ratiosCompilator = new RatiosCompilator();
        ratiosCompilator.execute();
        // @TODO add a progress bar? May be necessary for big experiments...
    }

    /**
     * Returns the processed quantification
     * @return the processed quantification
     */
    public ReporterIonQuantification getQuantification() {
        return (ReporterIonQuantification) experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getQuantification(quantificationMethodUsed);
    }

    public Identification getIdentification() {
        return experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
    }

    /**
     * worker used to compile ratios
     */
    private class RatiosCompilator extends SwingWorker {

        @Override
        protected Object doInBackground() throws Exception {
            try {
                ReporterIonQuantification quantification = getQuantification();
                RatioEstimator ratioEstimator = new RatioEstimator(quantification.getReporterMethod(), quantification.getReferenceLabel(), quantificationPreferences);
                for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification().values()) {
                    ratioEstimator.estimateRatios(proteinQuantification);
                }
                reporterGUI.displayResults(quantification, getIdentification());
            } catch (Exception e) {
                e.printStackTrace();
            }
            return 0;
        }
    }

    /**
     * worker used to process files
     */
    private class DataImporter extends SwingWorker {

        /**
         * The waiting dialog will provide feedback to the user
         */
        private WaitingDialog waitingDialog;
        /**
         * The identification files
         */
        private ArrayList<File> idFiles;
        /**
         * The mgf files
         */
        private ArrayList<File> mgfFiles;

        /**
         * Constructor
         * @param waitingDialog The waiting dialog will provide feedback to the user
         * @param idFiles       The identification files
         * @param mgfFiles      The mgf files
         */
        public DataImporter(WaitingDialog waitingDialog, ArrayList<File> idFiles, ArrayList<File> mgfFiles) {
            this.waitingDialog = waitingDialog;
            this.idFiles = idFiles;
            this.mgfFiles = mgfFiles;
        }

        @Override
        protected Object doInBackground() throws Exception {
            DataLoader dataLoader = new DataLoader(quantificationPreferences, waitingDialog, experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION));
            if (idFiles.size() > 1 || !idFiles.get(0).getName().endsWith(".cps")) {
                dataLoader.loadIdentifications(idFiles);
                dataLoader.loadQuantification(getQuantification(), mgfFiles);
            } else {
                dataLoader.processPeptideShakerInput(getQuantification(), mgfFiles);
            }
            waitingDialog.setRunFinished();
            return 0;
        }
    }
}
