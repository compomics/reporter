package eu.isas.reporter.gui;

import com.compomics.software.CompomicsWrapper;
import com.compomics.util.Util;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.matches.ProteinQuantification;
import com.compomics.util.general.ExceptionHandler;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.error_handlers.BugReport;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.UtilitiesUserPreferences;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.io.ReporterExporter;
import eu.isas.reporter.myparameters.ItraqScore;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.resultpanels.ProteinPanel;
import eu.isas.reporter.utils.Properties;
import java.awt.Toolkit;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import net.jimmc.jshortcut.JShellLink;
import org.ujmp.core.collections.ArrayIndexList;

/**
 * The main Reporter GUI.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterGUI extends javax.swing.JFrame {

    /**
     * The reporter class which will actually process the data.
     */
    private Reporter reporter = new Reporter(this);
    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
    /**
     * Mapping of the protein table entries.
     */
    private ArrayList<String> proteinTableIndex = new ArrayList<String>();
    /**
     * Mapping of the peptide table entries.
     */
    private ArrayList<String> peptideTableIndex = new ArrayList<String>();
    /**
     * Mapping of the PSM table entries.
     */
    private ArrayList<String> psmTableIndex = new ArrayList<String>();
    /**
     * The currently processed quantification.
     */
    private ReporterIonQuantification quantification;
    /**
     * The corresponding identification.
     */
    private Identification identification;
    /**
     * The reporter ions used in the method.
     */
    private ArrayList<ReporterIon> reporterIons = new ArrayIndexList<ReporterIon>();
    /**
     * A simple progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The utilities user preferences.
     */
    private UtilitiesUserPreferences utilitiesUserPreferences = null;
    /**
     * The location of the folder used for serialization of matches.
     */
    public final static String SERIALIZATION_DIRECTORY = "resources/matches";
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
    private ExceptionHandler exceptionHandler = new ExceptionHandler(this);

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
            JOptionPane.showMessageDialog(null, "An error occured when reading the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        // set this version as the default Reporter version
        if (!getJarFilePath().equalsIgnoreCase(".")) {

            utilitiesUserPreferences.setPeptideShakerPath(new File(getJarFilePath(), "Reporter-" + new Properties().getVersion() + ".jar").getAbsolutePath());

            try {
                UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "An error occured when saving the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
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

        ProteinPanel proteinPanel = new ProteinPanel(this);
        proteinsJPanel.add(proteinPanel);

        setUpGui();


        // set the title of the frame and add the icon
        setTitle("Reporter " + new Properties().getVersion());
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));
        this.setExtendedState(MAXIMIZED_BOTH);

        setLocationRelativeTo(null);
        setVisible(true);

        try {
            new NewDialog(this, reporter);
        } catch (Exception e) {
            reporter.catchException(e);
        }
    }

    /**
     * Sets up the GUI.
     */
    private void setUpGui() {
        tabPanel.setEnabledAt(1, false);
        tabPanel.setEnabledAt(2, false);
        tabPanel.setEnabledAt(3, false);
    }

    /**
     * Returns a reference to the parent Reporter object.
     *
     * @return a reference to the parent Reporter object
     */
    public Reporter getReporter() {
        return reporter;
    }

    /**
     * Displays results to the user.
     *
     * @param quantification The quantification computed
     * @param identification The corresponding identification
     */
    public void displayResults(ReporterIonQuantification quantification, Identification identification) {
        this.quantification = quantification;
        this.identification = identification;
        reporterIons = quantification.getReporterMethod().getReporterIons();
        updateProteinMap();
    }

    /**
     * Method called when a change was made in the settings.
     *
     * @param quantificationPreferences
     */
    public void updateResults(QuantificationPreferences quantificationPreferences) {

        final QuantificationPreferences fQuantificationPreferences = quantificationPreferences;
        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true);
        progressDialog.setTitle("Updating. Please Wait...");
        progressDialog.setIndeterminate(true);
        progressDialog.setUnstoppable(false);

        new Thread(new Runnable() {
            public void run() {
                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }
            }
        }, "ProgressDialog").start();

        new Thread("CloseThread") {
            @Override
            public void run() {
                reporter.setQuantificationPreferences(fQuantificationPreferences);
                reporter.compileRatios(progressDialog);
                displayResults(quantification, identification);
                progressDialog.setVisible(false);
            }
        }.start();
    }

    /**
     * Updates the maps for results display.
     */
    private void updateProteinMap() {
        try {
            // create the new protein table index ordered by quantification quality.
            proteinTableIndex = new ArrayList<String>();
            HashMap<Double, ArrayList<String>> proteinKeys = new HashMap<Double, ArrayList<String>>();
            ArrayList<Double> scores = new ArrayList<Double>();
            ProteinQuantification proteinQuantification;
            ItraqScore itraqScore = new ItraqScore();
            double score;
            for (String proteinKey : quantification.getProteinQuantification()) {
                proteinQuantification = quantification.getProteinMatch(proteinKey);
                itraqScore = (ItraqScore) proteinQuantification.getUrParam(itraqScore);
                score = -itraqScore.getMinScore();
                if (!proteinKeys.containsKey(score)) {
                    proteinKeys.put(score, new ArrayList<String>());
                    scores.add(score);
                }
                proteinKeys.get(score).add(proteinKey);
            }
            Collections.sort(scores);
            for (double currentScore : scores) {
                proteinTableIndex.addAll(proteinKeys.get(currentScore));
            }
        } catch (Exception e) {
            reporter.catchException(e);
        }
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
        proteinsJPanel = new javax.swing.JPanel();
        peptidesJPanel = new javax.swing.JPanel();
        psmsJPanel = new javax.swing.JPanel();
        ptmsJPanel = new javax.swing.JPanel();
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
        exportProteinsMenuItem = new javax.swing.JMenuItem();
        exportAllMenuItem = new javax.swing.JMenuItem();
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

        proteinsJPanel.setOpaque(false);
        proteinsJPanel.setLayout(new javax.swing.BoxLayout(proteinsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        tabPanel.addTab("Proteins", proteinsJPanel);

        peptidesJPanel.setOpaque(false);
        peptidesJPanel.setLayout(new javax.swing.BoxLayout(peptidesJPanel, javax.swing.BoxLayout.LINE_AXIS));
        tabPanel.addTab("Peptides", peptidesJPanel);

        psmsJPanel.setOpaque(false);
        psmsJPanel.setLayout(new javax.swing.BoxLayout(psmsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        tabPanel.addTab("PSMs", psmsJPanel);

        ptmsJPanel.setOpaque(false);
        ptmsJPanel.setLayout(new javax.swing.BoxLayout(ptmsJPanel, javax.swing.BoxLayout.LINE_AXIS));
        tabPanel.addTab("Modifications", ptmsJPanel);

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

        exportProteinsMenuItem.setMnemonic('P');
        exportProteinsMenuItem.setText("Export Proteins");
        exportProteinsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProteinsMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportProteinsMenuItem);

        exportAllMenuItem.setMnemonic('A');
        exportAllMenuItem.setText("Export All");
        exportAllMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportAllMenuItemActionPerformed(evt);
            }
        });
        exportMenu.add(exportAllMenuItem);

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
        try {
            new NewDialog(this, reporter);
        } catch (Exception e) {
            reporter.catchException(e);
        }
    }//GEN-LAST:event_newMenuItemActionPerformed

    /**
     * Open the preferences dialog.
     *
     * @param evt
     */
    private void quantificationOptionsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quantificationOptionsMenuItemActionPerformed
        new PreferencesDialog(this, reporter.getQuantificationPreferences());
    }//GEN-LAST:event_quantificationOptionsMenuItemActionPerformed

    /**
     * Export protein level to CSV.
     *
     * @param evt
     */
    private void exportProteinsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProteinsMenuItemActionPerformed
        export(true);
    }//GEN-LAST:event_exportProteinsMenuItemActionPerformed

    /**
     * Close Reporter.
     *
     * @param evt
     */
    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        closeReporter();
    }//GEN-LAST:event_exitMenuItemActionPerformed

    /**
     * Export everything to CSV.
     *
     * @param evt
     */
    private void exportAllMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportAllMenuItemActionPerformed
        export(false);
    }//GEN-LAST:event_exportAllMenuItemActionPerformed

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
        progressDialog.setIndeterminate(true);
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
                    File serializationFolder = new File(getJarFilePath(), ReporterGUI.SERIALIZATION_DIRECTORY);
                    String[] files = serializationFolder.list();

                    if (files != null) {
                        progressDialog.setIndeterminate(false);
                        progressDialog.setMaxProgressValue(files.length);
                    }

                    // close the files and save the user preferences
                    if (!progressDialog.isRunCanceled()) {
                        spectrumFactory.closeFiles();
                        sequenceFactory.closeFile();
                        saveUserPreferences();
                    }

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
     * Clear the data from the previous experiment.
     *
     * @param clearDatabaseFolder decides if the database folder is to be
     * cleared or not
     */
    public void clearData(boolean clearDatabaseFolder) {

        // reset the preferences
        //selectedProteinKey = NO_SELECTION;
        //selectedPeptideKey = NO_SELECTION;
        //selectedPsmKey = NO_SELECTION;

        //projectDetails = null;
        //spectrumAnnotator = new SpectrumAnnotator();

        try {
            spectrumFactory.closeFiles();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }
        try {
            sequenceFactory.closeFile();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }

        try {
            spectrumFactory.clearFactory();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }

        try {
            sequenceFactory.clearFactory();
        } catch (Exception e) {
            e.printStackTrace();
            catchException(e);
        }

        exceptionHandler = new ExceptionHandler(this);

        if (clearDatabaseFolder) {
            clearDatabaseFolder();
        }

        //resetFeatureGenerator();

        // set up the tabs/panels
        //setUpPanels(true);

        // repaint the panels
        //repaintPanels();

        // select the overview tab
        //allTabsJTabbedPane.setSelectedIndex(OVER_VIEW_TAB_INDEX);
        //currentPSFile = null;
        //dataSaved = false;
    }
    
    /**
     * Clears the database folder.
     */
    private void clearDatabaseFolder() {

        boolean databaseClosed = true;

        // close the database connection
        if (identification != null) {

            try {
                identification.close();
                identification = null;
            } catch (SQLException e) {
                databaseClosed = false;
                e.printStackTrace();
                JOptionPane.showMessageDialog(null, "Failed to close the database.", "Database Error", JOptionPane.WARNING_MESSAGE);
            }
        }

        // empty the matches folder
        if (databaseClosed) {
            File matchFolder = new File(getJarFilePath(), Reporter.SERIALIZATION_DIRECTORY);
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
        try {
            UtilitiesUserPreferences.saveUserPreferences(utilitiesUserPreferences);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "An error occured when saving the user preferences.", "File Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
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
     * Export to CSV.
     *
     * @param proteinOnly export protein level only
     */
    private void export(boolean proteinOnly) {

        JFileChooser fileChooser = new JFileChooser(getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Export Folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnVal = fileChooser.showSaveDialog(this);

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            File tempDir = fileChooser.getSelectedFile();

            if (!tempDir.exists()) {
                int value = JOptionPane.showConfirmDialog(this, "The folder \'" + tempDir.getAbsolutePath() + "\' does not exist.\n"
                        + "Do you want to create it?", "Create Folder?", JOptionPane.YES_NO_OPTION);
                if (value == JOptionPane.NO_OPTION) {
                    return;
                } else { // yes option selected
                    boolean success = tempDir.mkdir();

                    if (!success) {
                        JOptionPane.showMessageDialog(this, "Failed to create the folder. Please create it manually and then select it.",
                                "File Error", JOptionPane.INFORMATION_MESSAGE);
                        return;
                    }
                }
            }

            exportToCSV(fileChooser.getSelectedFile(), proteinOnly);
        }
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
    private javax.swing.JMenuItem exportAllMenuItem;
    private javax.swing.JMenu exportMenu;
    private javax.swing.JMenuItem exportProteinsMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JMenu helpMenu;
    private javax.swing.JMenuItem helpMenuItem;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator16;
    private javax.swing.JPopupMenu.Separator jSeparator17;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
    private javax.swing.JMenuItem logReportMenu;
    private javax.swing.JMenuBar menuBar;
    private javax.swing.JMenuItem newMenuItem;
    private javax.swing.JMenuItem openMenuItem;
    private javax.swing.JPanel peptidesJPanel;
    private javax.swing.JPanel proteinsJPanel;
    private javax.swing.JPanel psmsJPanel;
    private javax.swing.JPanel ptmsJPanel;
    private javax.swing.JMenu quantificationOptionsMenu;
    private javax.swing.JMenuItem quantificationOptionsMenuItem;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JTabbedPane tabPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Export the results to a CSV file.
     *
     * @param file the file to export to
     * @param aProteinsOnly if true, only the protein level will be exported
     */
    private void exportToCSV(File file, boolean aProteinsOnly) {

        final boolean proteinsOnly = aProteinsOnly;

        progressDialog = new ProgressDialogX(this,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        progressDialog.setIndeterminate(true);
        progressDialog.setTitle("Exporting Project. Please Wait...");
        final File exportFolder = file;

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
                    ReporterExporter exporter = new ReporterExporter(reporter.getExperiment(), "\t");
                    exporter.exportResults(quantification, identification, exportFolder.getPath(), proteinsOnly, progressDialog);

                    if (!progressDialog.isRunCanceled()) {
                        progressDialog.setRunFinished();
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(ReporterGUI.this, "Export Complete.", "Export successful.", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(ReporterGUI.this, "Export Error.", "Export error, see log file.", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Table model for the protein result table
     */
    private class ProteinTable extends DefaultTableModel {

        /**
         * Number of columns without counting quantification results
         */
        private static final int nC = 7;

        @Override
        public int getRowCount() {
            return proteinTableIndex.size();
        }

        @Override
        public int getColumnCount() {
            return nC + reporterIons.size();
        }

        @Override
        public String getColumnName(int column) {
            if (column == 1) {
                return "Protein";
            } else if (column == 2) {
                return "Other protein(s)";
            } else if (column == 3) {
                return "# Peptides";
            } else if (column == 4) {
                return "# Spectra identified";
            } else if (column == 5) {
                return "# Spectra quantified";
            } else if (column == 6) {
                return "emPAI";
            } else if (column > nC - 1 && column < nC - 1 + reporterIons.size()) {
                int pos = column - nC + 1;
                return quantification.getSample(reporterIons.get(pos).getIndex()).getReference();
            } else if (column == nC - 1 + reporterIons.size()) {
                return "Quality";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                ProteinQuantification proteinQuantification = quantification.getProteinMatch(proteinTableIndex.get(row));
                ProteinMatch proteinMatch = identification.getProteinMatch(proteinTableIndex.get(row));
                if (column == 0) {
                    return row + 1;
                } else if (column == 1) {
                    return proteinMatch.getMainMatch();
                } else if (column == 2) {
                    String result = "";
                    String mainKey = proteinMatch.getMainMatch();
                    for (String key : proteinMatch.getTheoreticProteinsAccessions()) {
                        if (!key.equals(mainKey)) {
                            result += key + " ";
                        }
                    }
                    return result;
                } else if (column == 3) {
                    int nPeptides = 0;
                    return nPeptides;
                } else if (column == 4) {
                    int nSpectra = 0;
                    return nSpectra;
                } else if (column == 5) {
                    return " ";
                } else if (column == 6) {
                    return " ";
                } else if (column > nC - 1 && column < nC - 1 + reporterIons.size()) {
                    int pos = column - nC + 1;
                    return proteinQuantification.getRatios().get(reporterIons.get(pos).getIndex()).getRatio();
                } else if (column == nC - 1 + reporterIons.size()) {
                    ItraqScore itraqScore = (ItraqScore) proteinQuantification.getUrParam(new ItraqScore());
                    return itraqScore.getMinScore();
                } else {
                    return " ";
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            for (int i = 0; i < getRowCount(); i++) {
                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }
            return (new Double(0.0)).getClass();
        }
    }

    /**
     * Returns the quantification preferences.
     *
     * @return the quantification preferences
     */
    public QuantificationPreferences getQuantificationPreferences() {
        return reporter.getQuantificationPreferences();
    }

    /**
     * Sets the quantification preferences.
     *
     * @param quantificationPreferences the quantification preferences
     */
    public void setQuantificationPreferences(QuantificationPreferences quantificationPreferences) {
        reporter.setQuantificationPreferences(quantificationPreferences);
    }

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
     * Method called to close the program.
     *
     * @param status closing status to report
     */
    public void close(int status) {
        this.dispose();
        reporter.close(status);
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
