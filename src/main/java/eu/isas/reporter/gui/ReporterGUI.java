package eu.isas.reporter.gui;

import com.compomics.software.CompomicsWrapper;
import com.compomics.software.autoupdater.MavenJarFile;
import com.compomics.software.dialogs.JavaHomeOrMemoryDialogParent;
import com.compomics.software.dialogs.JavaSettingsDialog;
import com.compomics.util.Util;
import com.compomics.util.db.DerbyUtil;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.exceptions.exception_handlers.FrameExceptionHandler;
import com.compomics.util.experiment.ShotgunProtocol;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.normalization.NormalizationFactors;
import com.compomics.util.gui.PrivacySettingsDialog;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.parameters.ProcessingPreferencesDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.math.clustering.KMeansClustering;
import com.compomics.util.math.clustering.settings.KMeansClusteringSettings;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.LastSelectedFolder;
import com.compomics.util.preferences.ProcessingPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.ReporterWrapper;
import eu.isas.reporter.calculation.QuantificationFeaturesCache;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.clustering.ClusterBuilder;
import eu.isas.reporter.calculation.normalization.Normalizer;
import eu.isas.reporter.gui.export.ReportDialog;
import eu.isas.reporter.gui.resultpanels.OverviewPanel;
import eu.isas.reporter.gui.settings.display.ClusteringSettingsDialog;
import eu.isas.reporter.io.ProjectSaver;
import eu.isas.reporter.settings.ReporterSettings;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.settings.ClusteringSettings;
import eu.isas.reporter.utils.Properties;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Frame;
import java.awt.Toolkit;
import java.io.*;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import javax.swing.Box;
import javax.swing.JOptionPane;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import net.jimmc.jshortcut.JShellLink;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The main Reporter GUI.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterGUI extends javax.swing.JFrame implements JavaHomeOrMemoryDialogParent {

    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private LastSelectedFolder lastSelectedFolder = new LastSelectedFolder();
    /**
     * A simple progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserPreferences utilitiesUserPreferences = null;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance(100);
    /**
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance(100000);
    /**
     * The exception handler.
     */
    private ExceptionHandler exceptionHandler = new FrameExceptionHandler(this, "https://github.com/compomics/reporter/issues");
    /**
     * The cps parent used to manage the data.
     */
    private CpsParent cpsParent = null;
    /**
     * The reporter settings
     */
    private ReporterSettings reporterSettings;
    /**
     * The reporter ion quantification containing the quantification parameters.
     */
    private ReporterIonQuantification reporterIonQuantification;
    /**
     * The identification features generator provides identification related
     * metrics on the identified matches.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The display features generator provides display features for the
     * identified matches.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The quantification features generator provides quantification features on
     * the identified matches.
     */
    private QuantificationFeaturesGenerator quantificationFeaturesGenerator;
    /**
     * Boolean indicating whether the project has been saved.
     */
    private boolean projectSaved = true;
    /**
     * The display preferences.
     */
    private DisplayPreferences displayPreferences;
    /**
     * The processing preferences.
     */
    private ProcessingPreferences processingPreferences = new ProcessingPreferences();
    /**
     * The horizontal padding used before and after the text in the titled
     * borders. (Needed to make it look as good in Java 7 as it did in Java
     * 6...)
     */
    public static String TITLED_BORDER_HORIZONTAL_PADDING = ""; // @TODO: move to utilities?
    /**
     * the overview panel
     */
    private OverviewPanel overviewPanel;
    /**
     * The decimal format use for the score and confidence columns.
     */
    private DecimalFormat scoreAndConfidenceDecimalFormat = new DecimalFormat("0");
    /**
     * The k-means clustering results.
     */
    private KMeansClustering kMeansClutering;
    /**
     * List of the currently selected proteins.
     */
    private ArrayList<String> selectedProteins = new ArrayList<String>();
    /**
     * The Jump To panel.
     */
    private JumpToPanel jumpToPanel;
    /**
     * The cluster builder.
     */
    private ClusterBuilder clusterBuilder;

    /**
     * Creates a new ReporterGUI.
     */
    public ReporterGUI() {

        // set up the ErrorLog
        setUpLogFile();

        // update the look and feel after adding the panels
        setLookAndFeel();

        // load the utilities user preferences
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occurred when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        // check for new version
        boolean newVersion = false;
        if (!getJarFilePath().equalsIgnoreCase(".") && utilitiesUserPreferences.isAutoUpdate()) {
            newVersion = checkForNewVersion();
        }

        if (!newVersion) {

            // set this version as the default Reporter version
            if (!getJarFilePath().equalsIgnoreCase(".")) {
                utilitiesUserPreferences.setReporterPath(new File(getJarFilePath(), "Reporter-" + new Properties().getVersion() + ".jar").getAbsolutePath());
                UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
            }

            // add desktop shortcut?
            if (!getJarFilePath().equalsIgnoreCase(".")
                    && System.getProperty("os.name").lastIndexOf("Windows") != -1
                    && new File(getJarFilePath() + "/resources/conf/firstRun").exists()) {

                // @TODO: add support for desktop icons in mac and linux??
                // delete the firstRun file such that the user is not asked the next time around
                new File(getJarFilePath() + "/resources/conf/firstRun").delete();

                int value = JOptionPane.showConfirmDialog(null,
                        "Create a shortcut to Reporter on the desktop?",
                        "Create Desktop Shortcut?",
                        JOptionPane.YES_NO_OPTION,
                        JOptionPane.QUESTION_MESSAGE);

                if (value == JOptionPane.YES_OPTION) {
                    addShortcutAtDeskTop();
                }
            }

            initComponents();
            
            categoriesMenuItem.setVisible(false);
            jSeparator4.setVisible(false);

            overviewPanel = new OverviewPanel(this);
            overviewJPanel.add(overviewPanel);

            jumpToPanel = new JumpToPanel(this);
            jumpToPanel.setEnabled(false);

            menuBar.add(Box.createHorizontalGlue());

            menuBar.add(jumpToPanel);

            // set the title of the frame and add the icon
            setTitle("Reporter " + new Properties().getVersion());
            this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));
            this.setExtendedState(MAXIMIZED_BOTH);

            overviewPanel.autoResizeComponents();

            // check for 64 bit java and for at least 4 gb memory 
            boolean java64bit = CompomicsWrapper.is64BitJava();
            boolean memoryOk = (utilitiesUserPreferences.getMemoryPreference() >= 4000);
            String javaVersion = System.getProperty("java.version");
            boolean javaVersionWarning = javaVersion.startsWith("1.5") || javaVersion.startsWith("1.6");

            new WelcomeDialog(this, !java64bit || !memoryOk, javaVersionWarning, true);
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("ReporterGUI.class").getPath(), "Reporter");
    }

    /**
     * Closes the currently opened project, open the new project dialog and
     * loads the new project on the interface
     *
     * @param cpsParent the cps parent
     * @param reporterSettings the reporter settings
     * @param reporterIonQuantification the reporter quantification settings
     * @param processingPreferences the processing preferences
     * @param displayPreferences the display preferences
     */
    public void createNewProject(CpsParent cpsParent, ReporterSettings reporterSettings, ReporterIonQuantification reporterIonQuantification, ProcessingPreferences processingPreferences, DisplayPreferences displayPreferences) {

        if (cpsParent != null) {
            closeOpenedProject();
        }
        projectSaved = true;

        setLocationRelativeTo(null);
        setVisible(true);

        this.cpsParent = cpsParent;
        this.reporterSettings = reporterSettings;
        this.reporterIonQuantification = reporterIonQuantification;
        this.processingPreferences = processingPreferences;
        this.displayPreferences = displayPreferences;

        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(getIdentification(), cpsParent.getShotgunProtocol(),
                cpsParent.getIdentificationParameters(), cpsParent.getMetrics(), cpsParent.getSpectrumCountingPreferences());
        displayFeaturesGenerator = new DisplayFeaturesGenerator(cpsParent.getIdentificationParameters().getSearchParameters().getPtmSettings(), exceptionHandler);
        displayFeaturesGenerator.setDisplayedPTMs(cpsParent.getDisplayPreferences().getDisplayedPtms());
        selectedProteins = new ArrayList<String>();

        projectSaved = false;
        quantificationFeaturesGenerator = new QuantificationFeaturesGenerator(new QuantificationFeaturesCache(), getIdentification(), getIdentificationFeaturesGenerator(), reporterSettings, reporterIonQuantification,
                cpsParent.getIdentificationParameters().getSearchParameters(), cpsParent.getIdentificationParameters().getSequenceMatchingPreferences());

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Quantifying Proteins. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("ImportThread") {
            @Override
            public void run() {
                try {
                    displayResults(progressDialog);
                } catch (Exception e) {
                    catchException(e);
                    progressDialog.setRunCanceled();
                } finally {
                    progressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * Returns the identification features generator.
     *
     * @return the identification features generator
     */
    public IdentificationFeaturesGenerator getIdentificationFeaturesGenerator() {
        return identificationFeaturesGenerator;
    }

    /**
     * Returns the display features generator.
     *
     * @return the display features generator
     */
    public DisplayFeaturesGenerator getDisplayFeaturesGenerator() {
        return displayFeaturesGenerator;
    }

    /**
     * Returns the quantification features generator.
     *
     * @return the quantification features generator
     */
    public QuantificationFeaturesGenerator getQuantificationFeaturesGenerator() {
        return quantificationFeaturesGenerator;
    }

    /**
     * Returns the reporter ion quantification.
     *
     * @return the reporter ion quantification
     */
    public ReporterIonQuantification getReporterIonQuantification() {
        return reporterIonQuantification;
    }

    /**
     * Displays the results on the GUI.
     *
     * @param waitingHandler the waiting handler
     */
    private void displayResults(WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        // Set Normalization factors
        NormalizationFactors normalizationFactors = reporterIonQuantification.getNormalizationFactors();
        if (!normalizationFactors.hasNormalizationFactors()) {
            Normalizer normalizer = new Normalizer();
            if (!normalizationFactors.hasPsmNormalisationFactors()) {
                normalizer.setPsmNormalizationFactors(reporterIonQuantification, reporterSettings.getRatioEstimationSettings(), reporterSettings.getNormalizationSettings(), getIdentificationParameters().getSequenceMatchingPreferences(), getIdentification(), quantificationFeaturesGenerator, processingPreferences, exceptionHandler, progressDialog);
            }
            if (!normalizationFactors.hasPeptideNormalisationFactors()) {
                normalizer.setPeptideNormalizationFactors(reporterIonQuantification, reporterSettings.getRatioEstimationSettings(), reporterSettings.getNormalizationSettings(), getIdentificationParameters().getSequenceMatchingPreferences(), getIdentification(), quantificationFeaturesGenerator, processingPreferences, exceptionHandler, progressDialog);
            }
            if (!normalizationFactors.hasProteinNormalisationFactors()) {
                normalizer.setProteinNormalizationFactors(reporterIonQuantification, reporterSettings.getRatioEstimationSettings(), reporterSettings.getNormalizationSettings(), getIdentification(), getMetrics(), quantificationFeaturesGenerator, processingPreferences, exceptionHandler, progressDialog);
            }
        }

        // cluster the profiles of the selected entities
        clusterBuilder = new ClusterBuilder();
        kMeansClutering = clusterBuilder.clusterProfiles(getIdentification(), getIdentificationParameters(), getMetrics(), reporterIonQuantification, quantificationFeaturesGenerator, displayPreferences, true, progressDialog);

        if (waitingHandler.isRunCanceled()) {
            return;
        }

        overviewPanel.updateDisplay();

        saveMenuItem.setEnabled(true);
        saveAsMenuItem.setEnabled(true);
        exportMenu.setEnabled(true);

        jumpToPanel.setEnabled(true);
        jumpToPanel.setType(JumpToPanel.JumpType.proteinAndPeptides);
        ArrayList<String> filteredProteinKeysArray = new ArrayList<String>(clusterBuilder.getFilteredProteins());
        jumpToPanel.setProteinKeys(filteredProteinKeysArray);
    }

    /**
     * Returns the cluster builder.
     * 
     * @return the cluster builder
     */
    public ClusterBuilder getClusterBuilder() {
        return clusterBuilder;
    }
    
    /**
     * Returns the reporter settings.
     *
     * @return the reporter settings
     */
    public ReporterSettings getReporterSettings() {
        return reporterSettings;
    }

    /**
     * Returns the identification of the cps file. Null if none loaded.
     *
     * @return the identification of the cps file
     */
    public Identification getIdentification() {
        if (cpsParent == null) {
            return null;
        }
        return cpsParent.getIdentification();
    }

    /**
     * Returns the gene maps of the loaded project. Null if none.
     *
     * @return the gene maps
     */
    public GeneMaps getGeneMaps() {
        if (cpsParent == null) {
            return null;
        }
        return cpsParent.getGeneMaps();
    }

    /**
     * Returns the experiment.
     *
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        if (cpsParent == null) {
            return null;
        }
        return cpsParent.getExperiment();
    }

    /**
     * Returns the sample.
     *
     * @return the sample
     */
    public Sample getSample() {
        if (cpsParent == null) {
            return null;
        }
        return cpsParent.getSample();
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicateNumber
     */
    public Integer getReplicateNumber() {
        if (cpsParent == null) {
            return null;
        }
        return cpsParent.getReplicateNumber();
    }

    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        if (cpsParent == null) {
            return null;
        }
        return cpsParent.getProjectDetails();
    }

    /**
     * returns the identification display preferences
     *
     * @return the identification display preferences
     */
    public eu.isas.peptideshaker.preferences.DisplayPreferences getIdentificationDisplayPreferences() {
        if (cpsParent == null || cpsParent.getDisplayPreferences() == null) { //@TODO: this is null with the online version of PeptideShaker
            return new eu.isas.peptideshaker.preferences.DisplayPreferences();
        }
        return cpsParent.getDisplayPreferences();
    }

    /**
     * Returns the exception handler.
     *
     * @return the exception handler
     */
    public ExceptionHandler getExceptionHandler() {
        return exceptionHandler;
    }

    /**
     * Returns the spectrum counting preferences.
     *
     * @return the spectrum counting preferences
     */
    public SpectrumCountingPreferences getSpectrumCountingPreferences() {
        return cpsParent.getSpectrumCountingPreferences();
    }

    /**
     * Returns the display preferences.
     *
     * @return the display preferences
     */
    public DisplayPreferences getDisplayPreferences() {
        return displayPreferences;
    }

    /**
     * Returns the filter preferences.
     *
     * @return the filter preferences
     */
    public FilterPreferences getFilterPreferences() {
        return cpsParent.getFilterPreferences();
    }

    /**
     * Get the sparklines color.
     *
     * @return the sparklineColor
     */
    public Color getSparklineColor() {
        return utilitiesUserPreferences.getSparklineColor();
    }

    /**
     * Get the non-validated sparklines color.
     *
     * @return the non-validated sparklineColor
     */
    public Color getSparklineColorNonValidated() {
        return utilitiesUserPreferences.getSparklineColorNonValidated();
    }

    /**
     * Get the not found sparklines color.
     *
     * @return the not found sparklineColor
     */
    public Color getSparklineColorNotFound() {
        return utilitiesUserPreferences.getSparklineColorNotFound();
    }

    /**
     * Get the not doubtful sparklines color.
     *
     * @return the doubtful sparklineColor
     */
    public Color getSparklineColorDoubtful() {
        return utilitiesUserPreferences.getSparklineColorDoubtful();
    }

    /**
     * Returns the decimal format used for the score and confidence columns.
     *
     * @return the decimal format used for the score and confidence columns
     */
    public DecimalFormat getScoreAndConfidenceDecimalFormat() {
        return scoreAndConfidenceDecimalFormat;
    }

    /**
     * Returns the metrics saved while loading the files.
     *
     * @return the metrics saved while loading the files
     */
    public Metrics getMetrics() {
        return cpsParent.getMetrics();
    }

    /**
     * Returns the shotgun protocol.
     *
     * @return the shotgun protocol
     */
    public ShotgunProtocol getShotgunProtocol() {
        if (cpsParent == null) {
            return null;
        }
        return cpsParent.getShotgunProtocol();
    }

    /**
     * Returns the identification parameters.
     *
     * @return the identification parameters
     */
    public IdentificationParameters getIdentificationParameters() {
        if (cpsParent == null) {
            return null;
        }
        return cpsParent.getIdentificationParameters();
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundPanel = new javax.swing.JPanel();
        tabPanel = new javax.swing.JTabbedPane();
        overviewJPanel = new javax.swing.JPanel();
        menuBar = new javax.swing.JMenuBar();
        fileMenu = new javax.swing.JMenu();
        newMenuItem = new javax.swing.JMenuItem();
        jSeparator1 = new javax.swing.JPopupMenu.Separator();
        openMenuItem = new javax.swing.JMenuItem();
        jSeparator2 = new javax.swing.JPopupMenu.Separator();
        saveMenuItem = new javax.swing.JMenuItem();
        saveAsMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        quantificationOptionsMenu = new javax.swing.JMenu();
        categoriesMenuItem = new javax.swing.JMenuItem();
        jSeparator4 = new javax.swing.JPopupMenu.Separator();
        processingSettingsMenuItem = new javax.swing.JMenuItem();
        javaOptionsMenuItem = new javax.swing.JMenuItem();
        privacyMenuItem = new javax.swing.JMenuItem();
        exportMenu = new javax.swing.JMenu();
        exportQuantificationFeaturesMenuItem = new javax.swing.JMenuItem();
        exportFollowUpJMenuItem = new javax.swing.JMenuItem();
        helpMenu = new javax.swing.JMenu();
        helpMenuItem = new javax.swing.JMenuItem();
        jSeparator17 = new javax.swing.JPopupMenu.Separator();
        logReportMenu = new javax.swing.JMenuItem();
        jSeparator16 = new javax.swing.JPopupMenu.Separator();
        aboutMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1280, 750));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        tabPanel.setTabPlacement(javax.swing.JTabbedPane.RIGHT);

        overviewJPanel.setOpaque(false);
        overviewJPanel.setLayout(new javax.swing.BoxLayout(overviewJPanel, javax.swing.BoxLayout.LINE_AXIS));
        tabPanel.addTab("Overview", overviewJPanel);

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 1278, Short.MAX_VALUE)
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(tabPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 864, Short.MAX_VALUE)
        );

        fileMenu.setMnemonic('F');
        fileMenu.setText("File");

        newMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_N, java.awt.event.InputEvent.CTRL_MASK));
        newMenuItem.setMnemonic('N');
        newMenuItem.setText("New");
        newMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                newMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(newMenuItem);
        fileMenu.add(jSeparator1);

        openMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_O, java.awt.event.InputEvent.CTRL_MASK));
        openMenuItem.setMnemonic('O');
        openMenuItem.setText("Open");
        openMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                openMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(openMenuItem);
        fileMenu.add(jSeparator2);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save");
        saveMenuItem.setEnabled(false);
        saveMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveMenuItem);

        saveAsMenuItem.setText("Save As...");
        saveAsMenuItem.setEnabled(false);
        saveAsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(saveAsMenuItem);
        fileMenu.add(jSeparator3);

        exitMenuItem.setMnemonic('x');
        exitMenuItem.setText("Exit");
        exitMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exitMenuItem);

        menuBar.add(fileMenu);

        quantificationOptionsMenu.setMnemonic('E');
        quantificationOptionsMenu.setText("Edit");

        categoriesMenuItem.setText("Categories");
        categoriesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                categoriesMenuItemActionPerformed(evt);
            }
        });
        quantificationOptionsMenu.add(categoriesMenuItem);
        quantificationOptionsMenu.add(jSeparator4);

        processingSettingsMenuItem.setText("Processing Settings");
        processingSettingsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                processingSettingsMenuItemActionPerformed(evt);
            }
        });
        quantificationOptionsMenu.add(processingSettingsMenuItem);

        javaOptionsMenuItem.setText("Java Settings");
        javaOptionsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                javaOptionsMenuItemActionPerformed(evt);
            }
        });
        quantificationOptionsMenu.add(javaOptionsMenuItem);

        privacyMenuItem.setText("Privacy Settings");
        privacyMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                privacyMenuItemActionPerformed(evt);
            }
        });
        quantificationOptionsMenu.add(privacyMenuItem);

        menuBar.add(quantificationOptionsMenu);

        exportMenu.setMnemonic('E');
        exportMenu.setText("Export");
        exportMenu.setEnabled(false);

        exportQuantificationFeaturesMenuItem.setMnemonic('P');
        exportQuantificationFeaturesMenuItem.setText("Quantification Features");
        exportQuantificationFeaturesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportQuantificationFeaturesMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportQuantificationFeaturesMenuItem);

        exportFollowUpJMenuItem.setText("Follow Up Analysis");
        exportFollowUpJMenuItem.setEnabled(false);
        exportMenu.add(exportFollowUpJMenuItem);

        menuBar.add(exportMenu);

        helpMenu.setMnemonic('H');
        helpMenu.setText("Help");

        helpMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_F1, 0));
        helpMenuItem.setMnemonic('H');
        helpMenuItem.setText("Help");
        helpMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                helpMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(helpMenuItem);
        helpMenu.add(jSeparator17);

        logReportMenu.setMnemonic('B');
        logReportMenu.setText("Bug Report");
        logReportMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                logReportMenuActionPerformed(evt);
            }
        });
        helpMenu.add(logReportMenu);
        helpMenu.add(jSeparator16);

        aboutMenuItem.setMnemonic('A');
        aboutMenuItem.setText("About");
        aboutMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutMenuItemActionPerformed(evt);
            }
        });
        helpMenu.add(aboutMenuItem);

        menuBar.add(helpMenu);

        setJMenuBar(menuBar);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Opens the new dialog.
     *
     * @param evt
     */
    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        new NewDialog(this, true);
    }//GEN-LAST:event_newMenuItemActionPerformed

    /**
     * Export the proteins to a tab separated text file.
     *
     * @param evt
     */
    private void exportQuantificationFeaturesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportQuantificationFeaturesMenuItemActionPerformed
        new ReportDialog(this);
    }//GEN-LAST:event_exportQuantificationFeaturesMenuItemActionPerformed

    /**
     * Close Reporter.
     *
     * @param evt
     */
    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        closeReporter();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    /**
     * Close Reporter.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        closeReporter();
    }//GEN-LAST:event_formWindowClosing

    /**
     * Open the help dialog.
     *
     * @param evt
     */
    private void helpMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_helpMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/helpFiles/Reporter.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                "ReporterGUI - Help");
    }//GEN-LAST:event_helpMenuItemActionPerformed

    /**
     * Open the BugReport dialog.
     *
     * @param evt
     */
    private void logReportMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_logReportMenuActionPerformed
        new BugReport(this, lastSelectedFolder, "Reporter", "reporter",
                new Properties().getVersion(), "reporter_software", "Reporter",
                new File(getJarFilePath() + "/resources/Reporter.log"));
    }//GEN-LAST:event_logReportMenuActionPerformed

    /**
     * Open the about dialog.
     *
     * @param evt
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/helpFiles/AboutReporter.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                "About ReporterGUI");
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    private void privacyMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_privacyMenuItemActionPerformed
        new PrivacySettingsDialog(this, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));
    }//GEN-LAST:event_privacyMenuItemActionPerformed

    /**
     * Open the Java Settings dialog.
     *
     * @param evt
     */
    private void javaOptionsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_javaOptionsMenuItemActionPerformed
        new JavaSettingsDialog(this, this, null, "Reporter", true);
    }//GEN-LAST:event_javaOptionsMenuItemActionPerformed

    /**
     * Save the current project.
     *
     * @param evt
     */
    private void saveMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveMenuItemActionPerformed
        if (cpsParent.getCpsFile() != null && cpsParent.getCpsFile().exists()) {
            saveProject(false, false);
        } else {
            saveProjectAs(false, false);
        }
    }//GEN-LAST:event_saveMenuItemActionPerformed

    /**
     * Create a new project.
     *
     * @param evt
     */
    private void openMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_openMenuItemActionPerformed
        new NewDialog(this, true); // @TODO: implement open project option
    }//GEN-LAST:event_openMenuItemActionPerformed

    /**
     * Saves the quantification details in the cpsx file.
     *
     * @param evt
     */
    private void saveAsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsMenuItemActionPerformed
        saveProjectAs(false, false);
    }//GEN-LAST:event_saveAsMenuItemActionPerformed

    private void processingSettingsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_processingSettingsMenuItemActionPerformed
        ProcessingPreferencesDialog processingPreferencesDialog = new ProcessingPreferencesDialog(this, processingPreferences, true);
        if (!processingPreferencesDialog.isCanceled()) {
            processingPreferences = processingPreferencesDialog.getProcessingPreferences();
        }
    }//GEN-LAST:event_processingSettingsMenuItemActionPerformed

    private void categoriesMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_categoriesMenuItemActionPerformed
        ClusteringSettingsDialog clusteringSettingsDialog = new ClusteringSettingsDialog(this, displayPreferences.getClusteringSettings(), true);
        if (!clusteringSettingsDialog.isCanceled()) { //@TODO: check whether the settings changed
            KMeansClusteringSettings kMeansClusteringSettings = displayPreferences.getClusteringSettings().getKMeansClusteringSettings();
            ClusteringSettings newSettings = clusteringSettingsDialog.getClusteringSettings();
            newSettings.setKMeansClusteringSettings(kMeansClusteringSettings);
            displayPreferences.setClusteringSettings(newSettings);
            recluster(kMeansClusteringSettings.getnClusters(), true);
        }
    }//GEN-LAST:event_categoriesMenuItemActionPerformed

    /**
     * Saves the quantification details in the cpsx file.
     *
     * @param aCloseWhenDone if true, Reporter closes after saving
     * @param aExportToZipWhenDone if true, the project is also saved as a zip
     * file
     */
    private void saveProject(boolean aCloseWhenDone, boolean aExportToZipWhenDone) { // @TODO: implement aExportToZipWhenDone?

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Saving. Please Wait...");

        final boolean closeWhenDone = aCloseWhenDone;

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("SaveThread") {
            @Override
            public void run() {
                try {
                    progressDialog.setWaitingText("Saving Results. Please Wait...");
                    ProjectSaver.saveProject(reporterSettings, reporterIonQuantification, displayPreferences, cpsParent, progressDialog);
                    if (!progressDialog.isRunCanceled()) {
                        if (closeWhenDone) {
                            closeReporter();
                        } else {
                            progressDialog.setRunFinished();
                            JOptionPane.showMessageDialog(ReporterGUI.this, "Project successfully saved.", "Save Successful", JOptionPane.INFORMATION_MESSAGE);
                            projectSaved = true;
                        }
                    }
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    e.printStackTrace();
                    catchException(e);
                }
            }
        }.start();
    }

    /**
     * Save the project to the currentPSFile location.
     *
     * @param closeWhenDone if true, Reporter closes when done saving
     * @param aExportToZipWhenDone if true, the project is also saved as a zip
     * file
     */
    public void saveProjectAs(boolean closeWhenDone, boolean aExportToZipWhenDone) {
        File selectedFile = getUserSelectedFile(cpsParent.getExperiment().getReference(), ".cpsx", "Compomics Peptide Shaker format (*.cpsx)", "Save As...", false);
        cpsParent.setCpsFile(selectedFile);
        if (selectedFile != null) {
            saveProject(closeWhenDone, aExportToZipWhenDone);
        }
    }

    /**
     * Closes Reporter.
     */
    public void closeReporter() {

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true);
        progressDialog.setTitle("Closing. Please Wait...");
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setUnstoppable(true);

        final ReporterGUI finalRef = this;

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    // turn off the self updating table models
                    overviewPanel.deactivateSelfUpdatingTableModels();

                    closeOpenedProject();

                    if (progressDialog.isRunCanceled()) {
                        return;
                    }

                    spectrumFactory.closeFiles();
                    sequenceFactory.closeFile();
                    saveUserPreferences();

                    // close the progress dialog
                    if (!progressDialog.isRunCanceled()) {
                        progressDialog.setRunFinished();
                    }

                    // hide the gui
                    finalRef.setVisible(false);

                    // clear the data and database folder
                    clearData(true);

                    // close the jvm
                    System.exit(0);

                } catch (Exception e) {
                    e.printStackTrace();
                    catchException(e);
                }
            }
        });
    }

    /**
     * Closes the opened project.
     */
    private void closeOpenedProject() {
        //@TODO: check whether the project is saved and close all connections
    }

    /**
     * Clear the data from the previous experiment.
     *
     * @param clearDatabaseFolder decides if the database folder is to be
     * cleared or not
     */
    public void clearData(boolean clearDatabaseFolder) {

        if (cpsParent != null) {
            cpsParent.setProjectDetails(null);
        }

        try {
            spectrumFactory.closeFiles();
            sequenceFactory.closeFile();
            spectrumFactory.clearFactory();
            sequenceFactory.clearFactory();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }

        if (clearDatabaseFolder) {
            clearDatabaseFolder();
        }

        if (cpsParent != null) {
            cpsParent.setCpsFile(null);
        }
    }

    /**
     * Clears the database folder.
     */
    private void clearDatabaseFolder() {

        boolean databaseClosed = true;

        // closeFiles the database connection
        if (getIdentification() != null) {

            try {
                getIdentification().close();
                cpsParent.setIdentification(null);
            } catch (SQLException e) {
                databaseClosed = false;
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to close the database.", "Database Error", JOptionPane.WARNING_MESSAGE);
            }
        }

        // empty the matches folder
        if (databaseClosed) {

            File matchFolder = Reporter.getMatchesFolder();

            if (matchFolder.exists()) {

                DerbyUtil.closeConnection();

                File[] tempFiles = matchFolder.listFiles();

                if (tempFiles != null) {
                    for (File currentFile : tempFiles) {
                        Util.deleteDir(currentFile);
                    }
                }

                if (matchFolder.listFiles() != null && matchFolder.listFiles().length > 0) {
                    JOptionPane.showMessageDialog(null, "Failed to empty the database folder:\n" + matchFolder.getPath() + ".",
                            "Database Cleanup Failed", JOptionPane.WARNING_MESSAGE);
                }
            }
        }
    }

    /**
     * Method called whenever an exception is caught.
     *
     * @param e the exception caught
     */
    public void catchException(Exception e) {
        exceptionHandler.catchException(e);
    }

    /**
     * Saves the user preferences.
     */
    public void saveUserPreferences() {
        UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
    }

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new ReporterGUI();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JMenuItem aboutMenuItem;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JMenuItem categoriesMenuItem;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportFollowUpJMenuItem;
    private javax.swing.JMenu exportMenu;
    private javax.swing.JMenuItem exportQuantificationFeaturesMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JPopupMenu.Separator jSeparator4;
    private javax.swing.JMenuItem javaOptionsMenuItem;
    private javax.swing.JMenuItem logReportMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JMenuItem privacyMenuItem;
    private javax.swing.JMenuItem processingSettingsMenuItem;
    private javax.swing.JMenu quantificationOptionsMenu;
    private javax.swing.JMenuItem saveAsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JTabbedPane tabPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the last selected folder.
     *
     * @return the last selected folder
     */
    public LastSelectedFolder getLastSelectedFolder() {
        if (lastSelectedFolder == null) {
            lastSelectedFolder = new LastSelectedFolder();
            utilitiesUserPreferences.setLastSelectedFolder(lastSelectedFolder);
        }
        return lastSelectedFolder;
    }

    /**
     * Set the last selected folder.
     *
     * @param lastSelectedFolder the folder to set
     */
    public void setLastSelectedFolder(LastSelectedFolder lastSelectedFolder) {
        this.lastSelectedFolder = lastSelectedFolder;
    }

    /**
     * Returns the file selected by the user, or null if no file was selected.
     *
     * @param aSuggestedFileName the suggested file name, can be null
     * @param aFileEnding the file type, e.g., .txt
     * @param aFileFormatDescription the file format description, e.g., (Mascot
     * Generic Format) *.mgf
     * @param aDialogTitle the title for the dialog
     * @param openDialog if true an open dialog is shown, false results in a
     * save dialog
     * @return the file selected by the user, or null if no file or folder was
     * selected
     */
    public File getUserSelectedFile(String aSuggestedFileName, String aFileEnding, String aFileFormatDescription, String aDialogTitle, boolean openDialog) {

        File selectedFile = Util.getUserSelectedFile(this, aFileEnding, aFileFormatDescription, aDialogTitle, lastSelectedFolder.getLastSelectedFolder(), aSuggestedFileName, openDialog);

        if (selectedFile != null) {
            if (selectedFile.isDirectory()) {
                lastSelectedFolder.setLastSelectedFolder(selectedFile.getAbsolutePath());
            } else {
                lastSelectedFolder.setLastSelectedFolder(selectedFile.getParentFile().getAbsolutePath());
            }
        }

        return selectedFile;
    }

    /**
     * Sets the look and feel of Reporter.
     * <p/>
     * Note that the GUI has been created with the following look and feel in
     * mind. If using a different look and feel you might need to tweak the GUI
     * to get the best appearance.
     */
    private static void setLookAndFeel() {
        try {
            // update the look and feel after adding the panels
            UtilitiesGUIDefaults.setLookAndFeel();

            // fix for the scroll bar thumb disappearing...
            LookAndFeel lookAndFeel = UIManager.getLookAndFeel();
            UIDefaults defaults = lookAndFeel.getDefaults();
            defaults.put("ScrollBar.minimumThumbSize", new Dimension(30, 30));
        } catch (Exception w) {
        }
    }

    /**
     * Set up the log file.
     */
    private void setUpLogFile() {
        try {
            if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
                String path = getJarFilePath() + "/resources/Reporter.log";

                File file = new File(path);
                System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));
                System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                // creates a new log file if it does not exist
                if (!file.exists()) {
                    file.createNewFile();

                    FileWriter w = new FileWriter(file);
                    BufferedWriter bw = new BufferedWriter(w);

                    bw.close();
                    w.close();
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(
                    null, "An error occurred when trying to create the Reporter log file.",
                    "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }
    }

    /**
     * Ask the user if he/she wants to add a shortcut at the desktop.
     */
    private void addShortcutAtDeskTop() {

        String jarFilePath = getJarFilePath();

        if (!jarFilePath.equalsIgnoreCase(".")) {

            // remove the initial '/' at the start of the line
            if (jarFilePath.startsWith("\\") && !jarFilePath.startsWith("\\\\")) {
                jarFilePath = jarFilePath.substring(1);
            }

            String iconFileLocation = jarFilePath + "\\resources\\reporter.ico";
            String jarFileLocation = jarFilePath + "\\Reporter-" + new Properties().getVersion() + ".jar";

            try {
                JShellLink link = new JShellLink();
                link.setFolder(JShellLink.getDirectory("desktop"));
                link.setName("Reporter " + new Properties().getVersion());
                link.setIconLocation(iconFileLocation);
                link.setPath(jarFileLocation);
                link.save();
            } catch (Exception e) {
                System.out.println("An error occurred when trying to create a desktop shortcut...");
                e.printStackTrace();
            }
        }
    }

    /**
     * Closes and restarts Reporter. Does not work inside the IDE of course.
     */
    public void restart() {
        if (this.getExtendedState() == Frame.ICONIFIED || !this.isActive()) {
            this.setExtendedState(Frame.MAXIMIZED_BOTH);
        }

        // @TODO: ask if the user wants to save unsaved data
        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true);
        progressDialog.getProgressBar().setStringPainted(false);
        progressDialog.getProgressBar().setIndeterminate(true);
        progressDialog.setTitle("Closing. Please Wait...");

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("RestartThread") {
            @Override
            public void run() {
                try {
                    spectrumFactory.closeFiles();
                    sequenceFactory.closeFile();
                    //cpsBean.saveUserPreferences();
                    //PeptideShakerGUI.this.clearData(true, true); // @TODO: clear data
                } catch (Exception e) {
                    e.printStackTrace();
                    catchException(e);
                }
                progressDialog.setRunFinished();
                ReporterGUI.this.dispose();

                // @TODO: pass the current project to the new instance of Reporter.
                new ReporterWrapper(null);
                System.exit(0); // have to close the current java process (as a new one is started on the line above)
            }
        }.start();
    }

    /**
     * Returns the user preferences.
     *
     * @return the user preferences
     */
    public UtilitiesUserPreferences getUtilitiesUserPreferences() {
        return utilitiesUserPreferences;
    }

    /**
     * Check for new version.
     *
     * @return true if a new version is to be downloaded
     */
    public boolean checkForNewVersion() {
        try {
            File jarFile = new File(ReporterGUI.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath());
            MavenJarFile oldMavenJarFile = new MavenJarFile(jarFile.toURI());
            URL jarRepository = new URL("http", "genesis.ugent.be", new StringBuilder().append("/maven2/").toString());

            return CompomicsWrapper.checkForNewDeployedVersion(
                    "Reporter", oldMavenJarFile, jarRepository, "reporter.ico",
                    false, true, true, Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        } catch (UnknownHostException ex) {
            // no internet connection
            System.out.println("Checking for new version failed. No internet connection.");
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (URISyntaxException e) {
            e.printStackTrace();
            return false;
        } catch (Exception e) {
            System.out.println("Checking for new version failed. Unknown error.");
            return false;
        }
    }

    /**
     * Returns the k-means clustering results.
     *
     * @return the k-means clustering results
     */
    public KMeansClustering getkMeansClutering() {
        return kMeansClutering;
    }

    /**
     * Set the number of clusters and recluster.
     *
     * @param numberOfClusters the new number of clusters
     * @param loadData if true, the data is (re-)loaded
     */
    public void recluster(int numberOfClusters, final boolean loadData) {

        displayPreferences.getClusteringSettings().getKMeansClusteringSettings().setnClusters(numberOfClusters);

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("ClusterThread") {
            @Override
            public void run() {
                try {
                    kMeansClutering = clusterBuilder.clusterProfiles(getIdentification(), getIdentificationParameters(), getMetrics(), reporterIonQuantification, quantificationFeaturesGenerator, displayPreferences, loadData, progressDialog);

                    if (!progressDialog.isRunCanceled()) {
                        overviewPanel.updateDisplay();
                    }
                } catch (Exception e) {
                    catchException(e);
                    progressDialog.setRunCanceled();
                } finally {
                    progressDialog.setRunFinished();
                }
            }
        }.start();
    }

    /**
     * Returns the list of selected proteins.
     *
     * @return the list of selected proteins
     */
    public ArrayList<String> getSelectedProteins() {
        return selectedProteins;
    }

    /**
     * If a chart is maximized, then minimize it. If not, do nothing.
     */
    public void minimizeChart() {
        overviewPanel.minimizeChart();
    }
    
    /**
     * Set the list of selected proteins.
     *
     * @param selectedProteins the list of selected proteins
     * @param updateSelection if true, the selection is updated in the GUI
     * @param clearSelection if true, the current selection will be removed
     */
    public void setSelectedProteins(ArrayList<String> selectedProteins, boolean updateSelection, boolean clearSelection) {
        this.selectedProteins = selectedProteins;
        if (updateSelection) {         
            overviewPanel.updateSelection(clearSelection);
        }
    }
}
