package eu.isas.reporter.gui;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.general.ExceptionHandler;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.IdFilter;
import com.compomics.util.preferences.PTMScoringPreferences;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import eu.isas.peptideshaker.preferences.FilterPreferences;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.preferences.SpectrumCountingPreferences;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.peptideshaker.utils.Metrics;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.calculation.QuantificationFeaturesCache;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.gui.export.ReportDialog;
import eu.isas.reporter.myparameters.ReporterPreferences;
import eu.isas.reporter.gui.resultpanels.OverviewPanel;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.utils.Properties;
import java.awt.Color;
import java.awt.Toolkit;
import java.io.*;
import java.sql.SQLException;
import java.text.DecimalFormat;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import net.jimmc.jshortcut.JShellLink;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * The main Reporter GUI.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterGUI extends javax.swing.JFrame {

    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
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
     * The exception handler
     */
    private ExceptionHandler exceptionHandler = new ExceptionHandler(this, "http://code.google.com/p/reporter/issues/list");
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();
    /**
     * The cps parent used to manage the data.
     */
    private CpsParent cpsBean = null;
    /**
     * The reporter preferences
     */
    private ReporterPreferences reporterPreferences;
    /**
     * The reporter ion quantification containing the quantification parameters
     */
    private ReporterIonQuantification reporterIonQuantification;
    /**
     * The identification features generator provides identification related
     * metrics on the identified matches
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The display features generator provides display features for the
     * identified matches
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The quantification features generator provides quantification features on
     * the identified matches
     */
    private QuantificationFeaturesGenerator quantificationFeaturesGenerator;
    /**
     * Boolean indicating whether the project has been saved
     */
    private boolean projectSaved = true;
    /**
     * The display preferences
     */
    private DisplayPreferences displayPreferences = new DisplayPreferences();
    /**
     * The horizontal padding used before and after the text in the titled
     * borders. (Needed to make it look as good in Java 7 as it did in Java
     * 6...)
     *
     * @TODO: move to utilities?
     */
    public static String TITLED_BORDER_HORIZONTAL_PADDING = "";
    /**
     * the overview panel
     */
    private OverviewPanel overviewPanel;
    /**
     * The decimal format use for the score and confidence columns.
     */
    private DecimalFormat scoreAndConfidenceDecimalFormat = new DecimalFormat("0");

    /**
     * Creates a new ReporterGUI.
     */
    public ReporterGUI() {

        // set up the ErrorLog
        setUpLogFile();

        // update the look and feel after adding the panels
        setLookAndFeel();

        // load modifications
        loadModifications();

        // load the utilities user preferences
        try {
            utilitiesUserPreferences = UtilitiesUserPreferences.loadUserPreferences();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error occured when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

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

        overviewPanel = new OverviewPanel(this);
        overviewJPanel.add(overviewPanel);

        setUpGui();

        // set the title of the frame and add the icon
        setTitle("Reporter " + new Properties().getVersion());
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));
        this.setExtendedState(MAXIMIZED_BOTH);

        setLocationRelativeTo(null);
        setVisible(true);

        createNewProject();
    }

    /**
     * Loads the modifications from the modification file.
     */
    private void loadModifications() {

        String path = getJarFilePath();

        try {
            ptmFactory.importModifications(new File(path, Reporter.getDefaultModificationFile()), false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + Reporter.getDefaultModificationFile() + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            ptmFactory.importModifications(new File(path, Reporter.getUserModificationFile()), true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + Reporter.getUserModificationFile() + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
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
     * Sets up the GUI.
     */
    private void setUpGui() {
        //@TODO
    }

    /**
     * Closes the currently opened project, open the new project dialog and
     * loads the new project on the interface
     */
    private void createNewProject() {

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Quantifying proteins. Please Wait...");

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
                    if (cpsBean != null) {
                        closeOpenedProject();
                        if (progressDialog.isRunCanceled()) {
                            return;
                        }
                    }
                    projectSaved = true;
                    NewDialog newDialog = new NewDialog(ReporterGUI.this);
                    if (!newDialog.isCancelled()) {
                        cpsBean = newDialog.getCpsBean();
                        identificationFeaturesGenerator = new IdentificationFeaturesGenerator(cpsBean.getIdentification(), cpsBean.getSearchParameters(), cpsBean.getIdFilter(), cpsBean.getMetrics(), cpsBean.getSpectrumCountingPreferences());
                        displayFeaturesGenerator = new DisplayFeaturesGenerator(cpsBean.getSearchParameters().getModificationProfile(), exceptionHandler);
                        displayFeaturesGenerator.setDisplayedPTMs(cpsBean.getDisplayPreferences().getDisplayedPtms());
                        reporterPreferences = newDialog.getReporterPreferences();
                        setDisplayPreferencesFromShakerProject();
                        reporterIonQuantification = newDialog.getReporterIonQuantification();
                        projectSaved = false;
                        quantificationFeaturesGenerator = new QuantificationFeaturesGenerator(new QuantificationFeaturesCache(), cpsBean.getIdentification(), reporterPreferences, reporterIonQuantification, cpsBean.getSearchParameters());
                        displayResults();
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
     * Sets the display preferences based on the currently loaded cps file
     */
    private void setDisplayPreferencesFromShakerProject() {
        displayPreferences = new DisplayPreferences();
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
     * displays the results on the gui
     */
    private void displayResults() throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        if (!reporterIonQuantification.hasNormalisationFactors()) {
            Reporter.setNormalizationFactors(reporterIonQuantification, reporterPreferences, cpsBean.getIdentification(), quantificationFeaturesGenerator, progressDialog);
        }
        overviewPanel.updateDisplay();
    }

    /**
     * Returns the reporter preferences.
     *
     * @return the reporter preferences
     */
    public ReporterPreferences getReporterPreferences() {
        return reporterPreferences;
    }

    /**
     * Returns the identification of the cps file. Null if none loaded.
     *
     * @return the identification of the cps file
     */
    public Identification getIdentification() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getIdentification();
    }

    /**
     * Returns the experiment.
     *
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getExperiment();
    }

    /**
     * Returns the sample.
     *
     * @return the sample
     */
    public Sample getSample() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getSample();
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicateNumber
     */
    public Integer getReplicateNumber() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getReplicateNumber();
    }

    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getProjectDetails();
    }

    /**
     * returns the identification display preferences
     *
     * @return the identification display preferences
     */
    public eu.isas.peptideshaker.preferences.DisplayPreferences getIdentificationDisplayPreferences() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getDisplayPreferences();
    }

    /**
     * Returns the annotation preferences as set by the user.
     *
     * @return the annotation preferences as set by the user
     */
    public AnnotationPreferences getAnnotationPreferences() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getAnnotationPreferences();
    }

    /**
     * Returns the identification filter used when loading the files.
     *
     * @return the identification filter
     */
    public IdFilter getIdFilter() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getIdFilter();
    }

    /**
     * Returns the PTM scoring preferences
     *
     * @return the PTM scoring preferences
     */
    public PTMScoringPreferences getPtmScoringPreferences() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getPtmScoringPreferences();
    }

    /**
     * Returns the identification parameters of the cps file. Null if none
     * loaded.
     *
     * @return the identification parameters of the cps file
     */
    public SearchParameters getSearchParameters() {
        if (cpsBean == null) {
            return null;
        }
        return cpsBean.getSearchParameters();
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
        return cpsBean.getSpectrumCountingPreferences();
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
        return cpsBean.getFilterPreferences();
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
        return cpsBean.getMetrics();
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
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        exportMenu = new javax.swing.JMenu();
        exportQuantificationFeaturesMenuItem = new javax.swing.JMenuItem();
        jMenuItem1 = new javax.swing.JMenuItem();
        quantificationOptionsMenu = new javax.swing.JMenu();
        quantificationOptionsMenuItem = new javax.swing.JMenuItem();
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
            .addComponent(tabPanel, javax.swing.GroupLayout.DEFAULT_SIZE, 862, Short.MAX_VALUE)
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
        fileMenu.add(openMenuItem);
        fileMenu.add(jSeparator2);

        saveMenuItem.setAccelerator(javax.swing.KeyStroke.getKeyStroke(java.awt.event.KeyEvent.VK_S, java.awt.event.InputEvent.CTRL_MASK));
        saveMenuItem.setMnemonic('S');
        saveMenuItem.setText("Save");
        fileMenu.add(saveMenuItem);
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

        exportMenu.setMnemonic('E');
        exportMenu.setText("Export");

        exportQuantificationFeaturesMenuItem.setMnemonic('P');
        exportQuantificationFeaturesMenuItem.setText("Quantification Features");
        exportQuantificationFeaturesMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportQuantificationFeaturesMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportQuantificationFeaturesMenuItem);

        jMenuItem1.setText("Follow-up");
        exportMenu.add(jMenuItem1);

        menuBar.add(exportMenu);

        quantificationOptionsMenu.setMnemonic('E');
        quantificationOptionsMenu.setText("Edit");

        quantificationOptionsMenuItem.setMnemonic('Q');
        quantificationOptionsMenuItem.setText("Quantification Preferences");
        quantificationOptionsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quantificationOptionsMenuItemActionPerformed(evt);
            }
        });
        quantificationOptionsMenu.add(quantificationOptionsMenuItem);

        menuBar.add(quantificationOptionsMenu);

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
        createNewProject();
    }//GEN-LAST:event_newMenuItemActionPerformed

    /**
     * Open the preferences dialog.
     *
     * @param evt
     */
    private void quantificationOptionsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quantificationOptionsMenuItemActionPerformed
        new PreferencesDialog(this, reporterPreferences, cpsBean.getSearchParameters());
    }//GEN-LAST:event_quantificationOptionsMenuItemActionPerformed

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
        new HelpDialog(this, getClass().getResource("/helpFiles/ReporterGUI.html"),
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
        new BugReport(this, lastSelectedFolder, "ReporterGUI", "reporter",
                new Properties().getVersion(),
                new File(getJarFilePath() + "/resources/ReporterGUI.log"));
    }//GEN-LAST:event_logReportMenuActionPerformed

    /**
     * Open the about dialog.
     *
     * @param evt
     */
    private void aboutMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutMenuItemActionPerformed
        new HelpDialog(this, getClass().getResource("/helpFiles/AboutReporterGUI.html"),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                "About ReporterGUI");
    }//GEN-LAST:event_aboutMenuItemActionPerformed

    /**
     * Closes Reporter.
     */
    private void closeReporter() {

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
     * Closes the opened project
     */
    private void closeOpenedProject() {
        //@TODO: check whether the project is saved and close all connections
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
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenu exportMenu;
    private javax.swing.JMenuItem exportQuantificationFeaturesMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JMenuItem jMenuItem1;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JMenuItem logReportMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JMenu quantificationOptionsMenu;
    private javax.swing.JMenuItem quantificationOptionsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JTabbedPane tabPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Sets the last selected folder,
     *
     * @param lastSelectedFolder the lastSelectedFolder to set
     */
    public void setLastSelectedFolder(String lastSelectedFolder) {
        this.lastSelectedFolder = lastSelectedFolder;
    }

    /**
     * Returns the last selected folder.
     *
     * @return the last selected folder
     */
    public String getLastSelectedFolder() {
        return lastSelectedFolder;
    }

    /**
     * Returns the file selected by the user, or null if no file was selected.
     *
     * @param aFileEnding the file type, e.g., .txt
     * @param aFileFormatDescription the file format description, e.g., (Mascot
     * Generic Format) *.mgf
     * @param aDialogTitle the title for the dialog
     * @param openDialog if true an open dialog is shown, false results in a
     * save dialog
     * @return the file selected by the user, or null if no file or folder was
     * selected
     */
    public File getUserSelectedFile(String aFileEnding, String aFileFormatDescription, String aDialogTitle, boolean openDialog) {

        File selectedFile = Util.getUserSelectedFile(this, aFileEnding, aFileFormatDescription, aDialogTitle, lastSelectedFolder, openDialog);

        if (selectedFile != null) {
            if (selectedFile.isDirectory()) {
                lastSelectedFolder = selectedFile.getAbsolutePath();
            } else {
                lastSelectedFolder = selectedFile.getParentFile().getAbsolutePath();
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
        } catch (Exception w) {
        }
    }

    /**
     * Set up the log file.
     */
    private void setUpLogFile() {

        try {
            if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
                String path = getJarFilePath() + "/resources/ReporterGUI.log";

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
                    null, "An error occured when trying to create the Reporter log file.",
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

            String iconFileLocation = jarFilePath + "\\resources\\conf\\reporter.ico";
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
}
