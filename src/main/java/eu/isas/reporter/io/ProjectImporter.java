package eu.isas.reporter.io;

import com.compomics.util.Util;
import com.compomics.util.db.ObjectsDB;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.quantification.Quantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.math.clustering.settings.KMeansClusteringSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.calculation.clustering.keys.PeptideClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.ProteinClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.PsmClusterClassKey;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.settings.ClusteringSettings;
import eu.isas.reporter.settings.ReporterIonSelectionSettings;
import eu.isas.reporter.settings.ReporterSettings;
import java.awt.Color;
import java.awt.Dialog;
import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * Imports a project from a cps file.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProjectImporter {

    /**
     * The dialog owner if operated from the GUI.
     */
    private Dialog owner;
    /**
     * The reporter settings loaded from the file.
     */
    private ReporterSettings reporterSettings;
    /**
     * The reporter ion quantification object loaded from the file.
     */
    private ReporterIonQuantification reporterIonQuantification;
    /**
     * The display preferences.
     */
    private DisplayPreferences displayPreferences;
    /**
     * The default reporter ion tolerance for TMT data.
     */
    public static final double DEFAULT_REPORTER_ION_TOLERANCE_TMT = 0.0016;
    /**
     * The default reporter ion tolerance for TMT data.
     */
    public static final double DEFAULT_REPORTER_ION_TOLERANCE_ITRAQ = 0.05;

    /**
     * Constructor.
     *
     * @param owner the dialog owner if operated from the GUI
     */
    public ProjectImporter(Dialog owner) {
        this.owner = owner;
    }

    /**
     * Constructor.
     */
    public ProjectImporter() {
        this(null);
    }

    /**
     * Imports the identification results from a PeptideShaker file.
     *
     * @param cpsParent the cps parent object where the cps file is loaded
     * @param mgfFiles the arraylist to add the detected mgf files to
     * @param waitingHandler a waiting handler to display the progress to the
     * user and allow interrupting the process
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     * @throws SQLException thrown of SQLException occurs exception thrown
     * whenever an error occurred while interacting with the database
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while saving the project
     * @throws org.apache.commons.compress.archivers.ArchiveException exception
     * thrown whenever an error occurs while untaring the file
     */
    public void importPeptideShakerProject(CpsParent cpsParent, ArrayList<File> mgfFiles, WaitingHandler waitingHandler) throws IOException, ClassNotFoundException, SQLException, InterruptedException, ArchiveException {

        File cpsFile = cpsParent.getCpsFile();
        if (Util.getExtension(cpsFile).equalsIgnoreCase("zip")) {
            cpsParent.loadCpsFromZipFile(cpsFile, Reporter.getMatchesFolder(), waitingHandler);
        } else {
            cpsParent.loadCpsFile(Reporter.getMatchesFolder(), waitingHandler);
        }

        if (waitingHandler.isRunCanceled()) {
            waitingHandler.setRunFinished();
            return;
        }

        waitingHandler.setWaitingText("Loading FASTA File. Please Wait...");

        // load fasta file
        if (owner != null) { // GUI
            try {
                cpsParent.loadFastaFile(waitingHandler);
            } catch (Exception e) {
                // Ignore, can be set from the GUI
            }
        } else { // CLI
            if (!cpsParent.loadFastaFile(waitingHandler)) {
                throw new IllegalArgumentException("The FASTA file was not found. Please provide its location in the command line parameters.");
            }
        }

        if (waitingHandler.isRunCanceled()) {
            waitingHandler.setRunFinished();
            return;
        }

        // load the spectrum files
        Identification identification = cpsParent.getIdentification();
        ArrayList<String> spectrumFiles = identification.getSpectrumFiles();
        waitingHandler.setWaitingText("Loading Spectrum Files. Please Wait...");
        waitingHandler.setPrimaryProgressCounterIndeterminate(true);
        int cpt = 0, total = identification.getSpectrumFiles().size();

        for (String spectrumFileName : spectrumFiles) {

            waitingHandler.setWaitingText("Loading Spectrum Files (" + ++cpt + " of " + total + "). Please Wait...");

            if (owner != null) { // GUI
                try {
                    cpsParent.loadSpectrumFile(spectrumFileName, mgfFiles, waitingHandler);
                } catch (Exception e) {
                    // Ignore, can be set from the GUI
                }

                if (waitingHandler.isRunCanceled()) {
                    waitingHandler.setRunFinished();
                    break;
                }

            } else { // CLI
                if (!cpsParent.loadSpectrumFile(spectrumFileName, mgfFiles, waitingHandler)) {
                    throw new IllegalArgumentException(spectrumFileName + " was not found. Please provide its location in the command line parameters.");
                }
            }
        }

        if (waitingHandler.isRunCanceled()) {
            waitingHandler.setRunFinished();
            return;
        }

        waitingHandler.setPrimaryProgressCounterIndeterminate(true);
    }

    /**
     * Loads the Reporter project.
     *
     * @param cpsParent the cps parent object where the cps file is loaded
     * @param waitingHandler a waiting handler to display the progress to the
     * user and allow interrupting the process
     *
     * @throws SQLException exception thrown whenever an error occurs while
     * interacting with the database
     * @throws IOException exception thrown whenever an error occurs while
     * reading or writing a file
     * @throws ClassNotFoundException exception thrown whenever an error
     * occurred while deserializing a file from the database
     * @throws InterruptedException exception thrown if a threading error occurs
     * while interacting with the database
     */
    public void importReporterProject(CpsParent cpsParent, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        // load reporter settings
        Identification identification = cpsParent.getIdentification();
        IdentificationParameters identificationParameters = cpsParent.getIdentificationParameters();
        ObjectsDB objectsDB = identification.getIdentificationDB().getObjectsDB();

        if (objectsDB.hasTable(ProjectSaver.REPORTER_SETTINGS_TABLE_NAME)) {
            waitingHandler.setWaitingText("Loading Quantification Results. Please Wait...");
            reporterSettings = (ReporterSettings) objectsDB.retrieveObject(ProjectSaver.REPORTER_SETTINGS_TABLE_NAME, ReporterSettings.class.getName(), true, false);
            reporterIonQuantification = (ReporterIonQuantification) objectsDB.retrieveObject(ProjectSaver.REPORTER_SETTINGS_TABLE_NAME, ReporterIonQuantification.class.getName(), true, false);
            displayPreferences = (DisplayPreferences) objectsDB.retrieveObject(ProjectSaver.REPORTER_SETTINGS_TABLE_NAME, DisplayPreferences.class.getName(), true, false);
        } else {
            waitingHandler.setWaitingText("Inferring Quantification Parameters. Please Wait...");
        }
        if (reporterIonQuantification == null) {
            reporterIonQuantification = getDefaultReporterIonQuantification(identificationParameters);
        }
        if (reporterSettings == null) {
            reporterSettings = getDefaultReporterSettings(reporterIonQuantification.getReporterMethod(), identificationParameters);
        }
        if (displayPreferences == null) {
            displayPreferences = new DisplayPreferences();
            ClusteringSettings clusteringSettings = getDefaultClusterMetrics(identificationParameters, identification);
            KMeansClusteringSettings kMeansClusteringSettings = new KMeansClusteringSettings();
            clusteringSettings.setKMeansClusteringSettings(kMeansClusteringSettings);
            displayPreferences.setClusteringSettings(clusteringSettings);
        }
    }

    /**
     * Returns the default reporter settings as inferred from the identification
     * parameters.
     *
     * @param reporterMethod the quantification method selected
     * @param identificationParameters the identification parameters
     *
     * @return the default reporter settings
     */
    public static ReporterSettings getDefaultReporterSettings(ReporterMethod reporterMethod, IdentificationParameters identificationParameters) {

        ReporterSettings reporterSettings = new ReporterSettings();
        return getDefaultReporterSettings(reporterMethod, identificationParameters, reporterSettings);
    }

    /**
     * Returns the default reporter settings as inferred from the identification
     * parameters.
     *
     * @param reporterMethod the quantification method selected
     * @param identificationParameters the identification parameters
     * @param reporterSettings the reporter settings
     *
     * @return the default reporter settings
     */
    public static ReporterSettings getDefaultReporterSettings(ReporterMethod reporterMethod, IdentificationParameters identificationParameters, ReporterSettings reporterSettings) {

        ReporterIonSelectionSettings reporterIonSelectionSettings = reporterSettings.getReporterIonSelectionSettings();
        SearchParameters searchParameters = identificationParameters.getSearchParameters();

        // adapt the ion tolerance and selection settings
        if (reporterMethod.getName().contains("iTRAQ")) {
            if (reporterMethod.getName().contains("4")) {
                double massTolerance = searchParameters.getFragmentIonAccuracyInDaltons(ReporterIon.iTRAQ4Plex_117.getTheoreticMz(1));
                reporterIonSelectionSettings.setReporterIonsMzTolerance(massTolerance);
                reporterIonSelectionSettings.setMostAccurate(massTolerance < DEFAULT_REPORTER_ION_TOLERANCE_ITRAQ);
            } else {
                double massTolerance = searchParameters.getFragmentIonAccuracyInDaltons(ReporterIon.iTRAQ8Plex_121.getTheoreticMz(1));
                reporterIonSelectionSettings.setReporterIonsMzTolerance(massTolerance);
                reporterIonSelectionSettings.setMostAccurate(massTolerance < DEFAULT_REPORTER_ION_TOLERANCE_ITRAQ);
            }
        } else if (reporterMethod.getName().contains("TMT")) {
            if (reporterIonSelectionSettings.getReporterIonsMzTolerance() > DEFAULT_REPORTER_ION_TOLERANCE_TMT) {
                reporterIonSelectionSettings.setReporterIonsMzTolerance(DEFAULT_REPORTER_ION_TOLERANCE_TMT);
                reporterIonSelectionSettings.setMostAccurate(true);
            }
        }

        return reporterSettings;
    }

    /**
     * Returns the default reporter ion quantification based on the
     * identification parameters.
     *
     * @param identificationParameters the identification parameters
     *
     * @return the default reporter ion quantification
     */
    public static ReporterIonQuantification getDefaultReporterIonQuantification(IdentificationParameters identificationParameters) {

        ReporterMethod selectedMethod = null;

        SearchParameters searchParameters = identificationParameters.getSearchParameters();
        ReporterMethodFactory reporterMethodFactory = ReporterMethodFactory.getInstance();

        // try to detect the method used
        for (String ptmName : searchParameters.getPtmSettings().getAllModifications()) {
            if (ptmName.contains("iTRAQ 4-plex")) {
                selectedMethod = reporterMethodFactory.getReporterMethod("iTRAQ 4-plex");
                break;
            } else if (ptmName.contains("iTRAQ 8-plex")) {
                selectedMethod = reporterMethodFactory.getReporterMethod("iTRAQ 8-plex");
                break;
            } else if (ptmName.contains("TMT 2-plex")) {
                selectedMethod = reporterMethodFactory.getReporterMethod("TMT 2-plex");
                break;
            } else if (ptmName.contains("TMT") && ptmName.contains("6-plex")) {
                if (searchParameters.getIonSearched1() == PeptideFragmentIon.Y_ION
                        || searchParameters.getIonSearched2() == PeptideFragmentIon.Y_ION) {
                    selectedMethod = reporterMethodFactory.getReporterMethod("TMT 6-plex (HCD)");
                } else {
                    selectedMethod = reporterMethodFactory.getReporterMethod("TMT 6-plex (ETD)");
                }
                break;
            } else if (ptmName.contains("TMT") && ptmName.contains("10-plex")) {
                selectedMethod = reporterMethodFactory.getReporterMethod("TMT 10-plex");
                break;
            }
        }

        ReporterIonQuantification reporterIonQuantification = new ReporterIonQuantification(Quantification.QuantificationMethod.REPORTER_IONS);

        // no method detected, default to the first
        if (selectedMethod != null) {
            reporterIonQuantification.setMethod(selectedMethod);
        }
        
        return reporterIonQuantification;
    }

    /**
     * Returns the cluster metrics for a given project.
     *
     * @param identificationParameters the identification parameters
     * @param identification the identification
     *
     * @return the cluster metrics for a given project
     */
    public static ClusteringSettings getDefaultClusterMetrics(IdentificationParameters identificationParameters, Identification identification) {

        ClusteringSettings clusteringSettings = new ClusteringSettings();

        ArrayList<ProteinClusterClassKey> proteinClasses = new ArrayList<ProteinClusterClassKey>(1);
        ProteinClusterClassKey proteinClusterClassKey = new ProteinClusterClassKey();
        proteinClasses.add(proteinClusterClassKey);
        clusteringSettings.setColor(proteinClusterClassKey.toString(), Color.BLACK);
        ProteinClusterClassKey defaultClusterClass = proteinClusterClassKey;
        proteinClusterClassKey = new ProteinClusterClassKey();
        proteinClusterClassKey.setStarred(Boolean.TRUE);
        proteinClasses.add(proteinClusterClassKey);
        clusteringSettings.setColor(proteinClusterClassKey.toString(), Color.yellow);

        PtmSettings ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
        ArrayList<PeptideClusterClassKey> peptideClasses = new ArrayList<PeptideClusterClassKey>(4);
        PeptideClusterClassKey peptidelusterClassKey = new PeptideClusterClassKey();
        peptideClasses.add(peptidelusterClassKey);
        clusteringSettings.setColor(peptidelusterClassKey.toString(), Color.DARK_GRAY);
        peptidelusterClassKey = new PeptideClusterClassKey();
        peptidelusterClassKey.setStarred(Boolean.TRUE);
        peptideClasses.add(peptidelusterClassKey);
        clusteringSettings.setColor(peptidelusterClassKey.toString(), Color.yellow);
        peptidelusterClassKey = new PeptideClusterClassKey();
        peptidelusterClassKey.setnTerm(Boolean.TRUE);
        peptideClasses.add(peptidelusterClassKey);
        clusteringSettings.setColor(peptidelusterClassKey.toString(), Color.MAGENTA);
        peptidelusterClassKey = new PeptideClusterClassKey();
        peptidelusterClassKey.setcTerm(Boolean.TRUE);
        peptideClasses.add(peptidelusterClassKey);
        clusteringSettings.setColor(peptidelusterClassKey.toString(), Color.CYAN);
        peptidelusterClassKey = new PeptideClusterClassKey();
        peptidelusterClassKey.setNotModified(Boolean.TRUE);
        clusteringSettings.setColor(peptidelusterClassKey.toString(), Color.LIGHT_GRAY);
        peptideClasses.add(peptidelusterClassKey);
        PTMFactory ptmFactory = PTMFactory.getInstance();
        ArrayList<Double> ptmMasses = new ArrayList<Double>();
        HashMap<Double, ArrayList<String>> ptmMap = new HashMap<Double, ArrayList<String>>();
        HashMap<Double, Color> ptmColorMap = new HashMap<Double, Color>();
        for (String ptmName : ptmSettings.getAllNotFixedModifications()) {
            PTM ptm = ptmFactory.getPTM(ptmName);
            Double ptmMass = ptm.getMass();
            ArrayList<String> ptms = ptmMap.get(ptmMass);
            if (ptms == null) {
                ptmMasses.add(ptmMass);
                ptms = new ArrayList<String>(2);
                ptmMap.put(ptmMass, ptms);
                Color ptmColor = ptmSettings.getColor(ptmName);
                ptmColorMap.put(ptmMass, ptmColor);
            }
            ptms.add(ptmName);
        }
        for (Double ptmMass : ptmMasses) {
            ArrayList<String> ptms = ptmMap.get(ptmMass);
            Collections.sort(ptms);
            peptidelusterClassKey = new PeptideClusterClassKey();
            peptidelusterClassKey.setPossiblePtms(ptms);
            peptideClasses.add(peptidelusterClassKey);
            Color color = ptmColorMap.get(ptmMass);
            clusteringSettings.setColor(peptidelusterClassKey.toString(), color);
        }

        ArrayList<PsmClusterClassKey> psmClasses = new ArrayList<PsmClusterClassKey>(1);
        PsmClusterClassKey psmClusterClassKey = new PsmClusterClassKey();
        psmClasses.add(psmClusterClassKey);
        clusteringSettings.setColor(psmClusterClassKey.toString(), Color.GRAY);
        psmClusterClassKey = new PsmClusterClassKey();
        psmClusterClassKey.setStarred(Boolean.TRUE);
        psmClasses.add(psmClusterClassKey);
        clusteringSettings.setColor(psmClusterClassKey.toString(), Color.yellow);
        ArrayList<String> spectrumFiles = identification.getOrderedSpectrumFileNames();
        if (spectrumFiles.size() > 1) {
            for (String spectrumFile : spectrumFiles) {
                psmClusterClassKey = new PsmClusterClassKey();
                psmClusterClassKey.setFile(spectrumFile);
                psmClasses.add(psmClusterClassKey);
                clusteringSettings.setColor(peptidelusterClassKey.toString(), Color.LIGHT_GRAY);
            }
        }

        clusteringSettings.setProteinClassKeys(proteinClasses);
        clusteringSettings.setPeptideClassKeys(peptideClasses);
        clusteringSettings.setPsmClassKeys(psmClasses);
        clusteringSettings.addProteinClass(defaultClusterClass.toString());
        return clusteringSettings;
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

    /**
     * Returns the display preferences.
     *
     * @return the display preferences
     */
    public DisplayPreferences getDisplayPreferences() {
        return displayPreferences;
    }
}
