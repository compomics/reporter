package eu.isas.reporter.io;

import com.compomics.util.db.ObjectsDB;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.settings.ReporterSettings;
import java.awt.Dialog;
import java.io.EOFException;
import java.io.File;
import java.sql.SQLException;
import java.util.ArrayList;
import javax.swing.JOptionPane;

/**
 * Imports a project from a cps file.
 *
 * @author Marc Vaudel
 */
public class ProjectImporter {

    /**
     * The dialog owner if operated from the GUI.
     */
    private Dialog owner;

    /**
     * The last selected folder.
     */
    private LastSelectedFolder lastSelectedFolder;

    /**
     * The cps parent used to load the file.
     */
    private CpsParent cpsParent = null;

    /**
     * The reporter settings loaded from the file.
     */
    private ReporterSettings reporterSettings;

    /**
     * The reporter ion quantification object loaded from the file.
     */
    private ReporterIonQuantification reporterIonQuantification;

    /**
     * Constructor.
     *
     * @param owner the dialog owner if operated from the GUI
     * @param lastSelectedFolder the last selected folder
     * @param cpsFile the file to import the project from
     * @param waitingHandler waiting handler used to display progress and cancel
     * the import
     */
    public ProjectImporter(Dialog owner, LastSelectedFolder lastSelectedFolder, File cpsFile, WaitingHandler waitingHandler) {
        cpsParent = new CpsParent(Reporter.getMatchesFolder());
        this.owner = owner;
        this.lastSelectedFolder = lastSelectedFolder;
        importPeptideShakerFile(cpsFile, waitingHandler);
    }

    /**
     * Constructor.
     *
     * @param lastSelectedFolder the last selected folder
     * @param cpsFile the file to import the project from
     * @param waitingHandler waiting handler used to display progress and cancel
     * the import
     */
    public ProjectImporter(LastSelectedFolder lastSelectedFolder, File cpsFile, WaitingHandler waitingHandler) {
        this(null, lastSelectedFolder, cpsFile, waitingHandler);
    }

    /**
     * Method used to import a PeptideShaker file.
     *
     * @param psFile a PeptideShaker file
     */
    private void importPeptideShakerFile(File cpsFile, WaitingHandler waitingHandler) {

        try {
            cpsParent.setCpsFile(cpsFile);

            try {
                cpsParent.loadCpsFile(Reporter.getMatchesFolder(), waitingHandler);
            } catch (SQLException e) {
                e.printStackTrace();
                String errorText = "An error occurred while reading:\n" + cpsFile + ".\n\n"
                        + "It looks like another instance of PeptideShaker is still connected to the file.\n"
                        + "Please close all instances of PeptideShaker and try again.";
                if (owner != null) {
                    JOptionPane.showMessageDialog(owner,
                            errorText,
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                } else {
                    throw new IllegalArgumentException(errorText);
                }
                return;
            }

            if (waitingHandler.isRunCanceled()) {
                waitingHandler.setRunFinished();
                return;
            }

            waitingHandler.setWaitingText("Loading FASTA File. Please Wait...");

            try {
                cpsParent.loadFastaFile(new File(lastSelectedFolder.getLastSelectedFolder()), waitingHandler);
            } catch (Exception e) {
                    //Ignore
                    e.printStackTrace(); 
            }

            if (waitingHandler.isRunCanceled()) {
                waitingHandler.setRunFinished();
                return;
            }

            Identification identification = cpsParent.getIdentification();
            ArrayList<String> spectrumFiles = identification.getSpectrumFiles();

            waitingHandler.setWaitingText("Loading Spectrum Files. Please Wait...");
            waitingHandler.setPrimaryProgressCounterIndeterminate(true);

            int cpt = 0, total = identification.getSpectrumFiles().size();
            for (String spectrumFileName : spectrumFiles) {

                waitingHandler.setWaitingText("Loading Spectrum Files (" + ++cpt + " of " + total + "). Please Wait...");

                try {
                    cpsParent.loadSpectrumFile(spectrumFileName, waitingHandler);
                } catch (Exception e) {
                    //Ignore
                    e.printStackTrace(); 
                }

                if (waitingHandler.isRunCanceled()) {
                    waitingHandler.setRunFinished();
                    break;
                }
            }
            waitingHandler.setPrimaryProgressCounterIndeterminate(true);
            waitingHandler.setRunFinished();

        } catch (OutOfMemoryError error) {
            System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
            Runtime.getRuntime().gc();
            String errorText = "PeptideShaker used up all the available memory and had to be stopped.<br>"
                    + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                    + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                    + "Java Options). See also <a href=\"http://compomics.github.io/compomics-utilities/wiki/javatroubleshooting.html\">JavaTroubleShooting</a>.";
            if (owner != null) {
                JOptionPane.showMessageDialog(owner,
                        errorText,
                        "Out of Memory", JOptionPane.ERROR_MESSAGE);
            } else {
                throw new IllegalArgumentException(errorText);
            }
            waitingHandler.setRunFinished();
            error.printStackTrace();
            return;
        } catch (EOFException e) {
            e.printStackTrace();
            String errorText = "An error occurred while reading:\n" + cpsFile + ".\n\n"
                    + "The file is corrupted and cannot be opened anymore.";
            if (owner != null) {
                JOptionPane.showMessageDialog(owner,
                        errorText,
                        "Out of Memory", JOptionPane.ERROR_MESSAGE);
            } else {
                throw new IllegalArgumentException(errorText);
            }
            waitingHandler.setRunFinished();
            return;
        } catch (Exception e) {
            e.printStackTrace();
            String errorText = "An error occurred while reading:\n" + cpsFile + ".\n\n"
                    + "Please verify that the PeptideShaker version used to create\n"
                    + "the file is compatible with your version of Reporter.";
            if (owner != null) {
                JOptionPane.showMessageDialog(owner,
                        errorText,
                        "Out of Memory", JOptionPane.ERROR_MESSAGE);
            } else {
                throw new IllegalArgumentException(errorText);
            }
            waitingHandler.setRunFinished();
            return;
        }

        // Load Reporter settings files
        ObjectsDB objectsDB = cpsParent.getIdentification().getIdentificationDB().getObjectsDB();
        try {
            if (objectsDB.hasTable(ProjectSaver.REPORTER_SETTINGS_TABLE_NAME)) {
                try {
                    reporterSettings = (ReporterSettings) objectsDB.retrieveObject(ProjectSaver.REPORTER_SETTINGS_TABLE_NAME, ReporterSettings.class.getName(), true, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    String errorText = "An error occurred while importing the reporter settings.";
                    if (owner != null) {
                        JOptionPane.showMessageDialog(owner,
                                errorText,
                                "Import Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        throw new IllegalArgumentException(errorText);
                    }
                    waitingHandler.setRunFinished();
                    return;
                }
                try {
                    reporterIonQuantification = (ReporterIonQuantification) objectsDB.retrieveObject(ProjectSaver.REPORTER_SETTINGS_TABLE_NAME, ReporterIonQuantification.class.getName(), true, false);
                } catch (Exception e) {
                    e.printStackTrace();
                    String errorText = "An error occurred while importing the reporter settings.";
                    if (owner != null) {
                        JOptionPane.showMessageDialog(owner,
                                errorText,
                                "Import Error", JOptionPane.ERROR_MESSAGE);
                    } else {
                        throw new IllegalArgumentException(errorText);
                    }
                    waitingHandler.setRunFinished();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            String errorText = "An error occurred while importing the quantification details from " + cpsFile + ".";
            if (owner != null) {
                JOptionPane.showMessageDialog(owner,
                        errorText,
                        "Import Error", JOptionPane.ERROR_MESSAGE);
            } else {
                throw new IllegalArgumentException(errorText);
            }
            waitingHandler.setRunFinished();
        }
    }

    /**
     * Returns the cps parent used to import the file.
     *
     * @return the cps parent used to import the file
     */
    public CpsParent getCpsParent() {
        return cpsParent;
    }

    /**
     * Returns the reporter settings loaded from the file.
     *
     * @return the reporter settings loaded from the file
     */
    public ReporterSettings getReporterSettings() {
        return reporterSettings;
    }

    /**
     * Returns the reporter ion quantification object loaded from the file.
     *
     * @return the reporter ion quantification object loaded from the file
     */
    public ReporterIonQuantification getReporterIonQuantification() {
        return reporterIonQuantification;
    }
}
