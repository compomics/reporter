package eu.isas.reporter;

import com.compomics.util.examples.BareBonesBrowserLaunch;
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
import com.compomics.util.gui.waiting.WaitingHandler;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import eu.isas.peptideshaker.myparameters.PSSettings;
import eu.isas.reporter.io.DataLoader;
import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.gui.ReporterGUI;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.UnknownHostException;
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
    private final String MODIFICATIONS_FILE = "conf/reporter_mods.xml";
    /**
     * User modification file.
     */
    private final String USER_MODIFICATIONS_FILE = "conf/reporter_usermods.xml";
    /**
     * The name of the reporter experiment
     */
    public static final String experimentObjectName = "experiment";
    /**
     * The PeptideShaker settings
     */
    private PSSettings psSettings;

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
     * @param reporterGUI referenec to the Reporter GUI
     */
    public Reporter(ReporterGUI reporterGUI) {
        this.reporterGUI = reporterGUI;
        // check if a newer version of Reporter is available
        //checkForNewVersion(new Properties().getVersion()); // @TODO: re-add later!
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
     * Check if a newer version of reporter is available.
     *
     * @param currentVersion the version number of the currently running
     * reporter
     */
    private static void checkForNewVersion(String currentVersion) {

        try {
            boolean deprecatedOrDeleted = false;
            URL downloadPage = new URL(
                    "http://code.google.com/p/reporter/downloads/detail?name=Reporter-"
                    + currentVersion + ".zip");

            if ((java.net.HttpURLConnection) downloadPage.openConnection() != null) {

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
                        if (inputLine.lastIndexOf("Deprecated") != -1
                                && inputLine.lastIndexOf("Deprecated Downloads") == -1
                                && inputLine.lastIndexOf("Deprecated downloads") == -1) {
                            deprecatedOrDeleted = true;
                        }
                    }

                    in.close();
                }

                // informs the user about an updated version of the tool, unless the user
                // is running a beta version
                if (deprecatedOrDeleted && currentVersion.lastIndexOf("beta") == -1) {
                    int option = JOptionPane.showConfirmDialog(null,
                            "A newer version of Reporter is available.\n"
                            + "Do you want to upgrade?",
                            "Upgrade Available",
                            JOptionPane.YES_NO_CANCEL_OPTION);
                    if (option == JOptionPane.YES_OPTION) {
                        BareBonesBrowserLaunch.openURL("http://reporter.googlecode.com/");
                        System.exit(0);
                    } else if (option == JOptionPane.CANCEL_OPTION) {
                        System.exit(0);
                    }
                }
            }
        } catch (UnknownHostException e) {
            // ignore exception
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
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
     */
    public void loadFiles(ArrayList<File> mgfFiles) {

        WaitingDialog waitingDialog = new WaitingDialog(reporterGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                false, null, true); //@TODO: put and tips
        DataImporter dataImporter = new DataImporter(waitingDialog, mgfFiles);
        dataImporter.execute();
        waitingDialog.setLocationRelativeTo(reporterGUI);
        waitingDialog.setVisible(true);
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
     * Returns the PeptideShaker settings
     *
     * @return the PeptideShaker settings
     */
    public PSSettings getPSSettings() {
        return psSettings;
    }

    /**
     * Sets the PeptideShaker settings
     *
     * @param psSettings the peptide shaker settings
     */
    public void setPSSettings(PSSettings psSettings) {
        this.psSettings = psSettings;
    }

    /**
     * Compiles the PSM ratios into peptides and proteins
     * @param waitingHandler a waiting handler displaying the progress. Progress will be displayed on the secondary progress bar if any.
     */
    public void compileRatios(WaitingHandler waitingHandler) {
            try {
                Enzyme enzyme = psSettings.getSearchParameters().getEnzyme();
                ReporterIonQuantification quantification = getQuantification();
                RatioEstimator ratioEstimator = new RatioEstimator(quantification, quantification.getReporterMethod(), quantificationPreferences, enzyme);

                if (waitingHandler != null) {
                    waitingHandler.setMaxSecondaryProgressValue(quantification.getProteinQuantification().size());
                    waitingHandler.setSecondaryProgressDialogIndeterminate(false);
                }
                for (String proteinKey : quantification.getProteinQuantification()) {
                    ratioEstimator.estimateProteinRatios(proteinKey);
                    if (waitingHandler != null) {
                        waitingHandler.increaseSecondaryProgressValue();
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
        String path = this.getClass().getResource("Reporter.class").getPath();

        if (path.lastIndexOf("/Reporter-") != -1) {
            path = path.substring(5, path.lastIndexOf("/Reporter-"));
            path = path.replace("%20", " ");
            path = path.replace("%5b", "[");
            path = path.replace("%5d", "]");

            if (System.getProperty("os.name").lastIndexOf("Windows") != -1) {
                path = path.replace("/", "\\");
            }
        } else {
            path = ".";
        }

        return path;
    }

    /**
     * Worker used to process files.
     */
    private class DataImporter extends SwingWorker {

        /**
         * The waiting dialog will provide feedback to the user.
         */
        private WaitingDialog waitingDialog;
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
         * @param waitingDialog The waiting dialog will provide feedback to the
         * user
         * @param idFiles The identification files
         * @param mgfFiles The mgf files
         */
        public DataImporter(WaitingDialog waitingDialog, ArrayList<File> mgfFiles) {
            this.waitingDialog = waitingDialog;
            this.mgfFiles = mgfFiles;
        }

        @Override
        protected Object doInBackground() {
            Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            Quantification quantification = getQuantification();
            DataLoader dataLoader = new DataLoader(quantificationPreferences, waitingDialog, identification);
            waitingDialog.appendReport("Building peptides and protein quantification objects.");
            waitingDialog.setMaxProgressValue(3);

            try {
                quantification.buildPeptidesAndProteinQuantifications(identification, waitingDialog);
            } catch (Exception e) {
                e.printStackTrace();
                waitingDialog.appendReport("An error occured whlie building peptide and protein quantification objects.\n" + e.getLocalizedMessage());
                waitingDialog.setRunCanceled();
                return 1;
            }

            dataLoader.loadQuantification(getQuantification(), mgfFiles);
            waitingDialog.increaseProgressValue();
            waitingDialog.appendReport("Estimating peptide and protein ratios");
            compileRatios(waitingDialog);
            if (!waitingDialog.isRunCanceled()) {
                reporterGUI.displayResults(getQuantification(), getIdentification());
                waitingDialog.setRunFinished();
            }
            return 0;
        }
    }
}
