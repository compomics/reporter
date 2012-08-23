package eu.isas.reporter.gui;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.matches.ProteinQuantification;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.io.ReporterExporter;
import eu.isas.reporter.myparameters.ItraqScore;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.resultpanels.ProteinPanel;
import eu.isas.reporter.utils.Properties;
import java.awt.Toolkit;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.table.DefaultTableModel;
import net.jimmc.jshortcut.JShellLink;
import org.ujmp.core.collections.ArrayIndexList;

/**
 * The main Reporter GUI.
 *
 * @author Marc Vaudel
 */
public class ReporterGUI extends javax.swing.JFrame {

    /**
     * The reporter class which will actually process the data
     */
    private Reporter parent = new Reporter(this);
    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
    /**
     * Mapping of the protein table entries
     */
    private ArrayList<String> proteinTableIndex = new ArrayList<String>();
    /**
     * Mapping of the peptide table entries
     */
    private ArrayList<String> peptideTableIndex = new ArrayList<String>();
    /**
     * Mapping of the psm table entries
     */
    private ArrayList<String> psmTableIndex = new ArrayList<String>();
    /**
     * The currently processed quantification
     */
    private ReporterIonQuantification quantification;
    /**
     * The corresponding identification
     */
    private Identification identification;
    /**
     * The reporter ions used in the method
     */
    private ArrayList<ReporterIon> reporterIons = new ArrayIndexList<ReporterIon>();
    /**
     * A simple progress dialog.
     */
    private ProgressDialogX progressDialog;

    /**
     * Creates new form ReporterGUI.
     */
    public ReporterGUI() {

        // set up the ErrorLog
        setUpLogFile();

        // update the look and feel after adding the panels
        setLookAndFeel();

        // add desktop shortcut?
        if (!parent.getJarFilePath().equalsIgnoreCase(".")
                && System.getProperty("os.name").lastIndexOf("Windows") != -1
                && new File(parent.getJarFilePath() + "/resources/conf/firstRun").exists()) {

            // @TODO: add support for desktop icons in mac and linux??

            // delete the firstRun file such that the user is not asked the next time around
            new File(parent.getJarFilePath() + "/resources/conf/firstRun").delete();

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
            new NewDialog(this, parent);
        } catch (Exception e) {
            parent.catchException(e);
        }
    }

    private void setUpGui() {

        tabPanel.setEnabledAt(1, false);
        tabPanel.setEnabledAt(2, false);
        tabPanel.setEnabledAt(3, false);

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
                parent.setQuantificationPreferences(fQuantificationPreferences);
                parent.compileRatios(progressDialog);
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
            parent.catchException(e);
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
        exportMenuItem = new javax.swing.JMenuItem();
        jSeparator3 = new javax.swing.JPopupMenu.Separator();
        exitMenuItem = new javax.swing.JMenuItem();
        quantificationOptionsMenu = new javax.swing.JMenu();
        quantificationOptionsMenuItem = new javax.swing.JMenuItem();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1280, 750));
        setPreferredSize(new java.awt.Dimension(1278, 883));

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
        fileMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fileMenuActionPerformed(evt);
            }
        });

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

        exportMenuItem.setMnemonic('E');
        exportMenuItem.setText("Export");
        exportMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportMenuItemActionPerformed(evt);
            }
        });
        fileMenu.add(exportMenuItem);
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
        quantificationOptionsMenu.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quantificationOptionsMenuActionPerformed(evt);
            }
        });

        quantificationOptionsMenuItem.setMnemonic('Q');
        quantificationOptionsMenuItem.setText("Quantification Preferences");
        quantificationOptionsMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                quantificationOptionsMenuItemActionPerformed(evt);
            }
        });
        quantificationOptionsMenu.add(quantificationOptionsMenuItem);

        menuBar.add(quantificationOptionsMenu);

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

    private void newMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_newMenuItemActionPerformed
        try {
            new NewDialog(this, parent);
        } catch (Exception e) {
            parent.catchException(e);
        }
    }//GEN-LAST:event_newMenuItemActionPerformed

    private void quantificationOptionsMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quantificationOptionsMenuActionPerformed
        new PreferencesDialog(this, parent.getQuantificationPreferences());
    }//GEN-LAST:event_quantificationOptionsMenuActionPerformed

    private void quantificationOptionsMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_quantificationOptionsMenuItemActionPerformed
        new PreferencesDialog(this, parent.getQuantificationPreferences());
    }//GEN-LAST:event_quantificationOptionsMenuItemActionPerformed

    private void exportMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportMenuItemActionPerformed

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

            exportToCSV(fileChooser.getSelectedFile());
        }
    }//GEN-LAST:event_exportMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

    private void fileMenuActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fileMenuActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fileMenuActionPerformed

    /**
     * @param args the command line arguments
     */
    public static void main(String args[]) {
        new ReporterGUI();
    }
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JMenuItem exitMenuItem;
    private javax.swing.JMenuItem exportMenuItem;
    private javax.swing.JMenu fileMenu;
    private javax.swing.JPopupMenu.Separator jSeparator1;
    private javax.swing.JPopupMenu.Separator jSeparator2;
    private javax.swing.JPopupMenu.Separator jSeparator3;
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

    private void exportToCSV(File file) {

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
                    ReporterExporter exporter = new ReporterExporter(parent.getExperiment(), "\t");
                    exporter.exportResults(quantification, identification, exportFolder.getPath(), progressDialog);

                    if (!progressDialog.isRunCanceled()) {
                        progressDialog.setRunFinished();
                        progressDialog.dispose();
                        JOptionPane.showMessageDialog(ReporterGUI.this, "Output Complete.", "Output successful.", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    progressDialog.dispose();
                    JOptionPane.showMessageDialog(ReporterGUI.this, "Output Error.", "Output error, see log file.", JOptionPane.ERROR_MESSAGE);
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
     * returns the quantification preferences
     *
     * @return the quantification preferences
     */
    public QuantificationPreferences getQuantificationPreferences() {
        return parent.getQuantificationPreferences();
    }

    /**
     * sets the quantification preferences
     *
     * @param quantificationPreferences the quantification preferences
     */
    public void setQuantificationPreferences(QuantificationPreferences quantificationPreferences) {
        parent.setQuantificationPreferences(quantificationPreferences);
    }

    /**
     * Sets the last selected folder
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
        // update the look and feel after adding the panels
        UtilitiesGUIDefaults.setLookAndFeel();
    }

    /**
     * Set up the log file.
     */
    private void setUpLogFile() {

        try {
            if (useLogFile && !parent.getJarFilePath().equalsIgnoreCase(".")) {
                String path = parent.getJarFilePath() + "/resources/conf/Reporter.log";

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
     * Method called to close the program
     *
     * @param status closing status to report
     */
    public void close(int status) {
        this.dispose();
        parent.close(status);
    }

    /**
     * Ask the user if he/she wants to add a shortcut at the desktop.
     */
    private void addShortcutAtDeskTop() {

        String jarFilePath = parent.getJarFilePath();

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
