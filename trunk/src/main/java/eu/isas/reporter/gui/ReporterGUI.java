package eu.isas.reporter.gui;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.gui.UtilitiesGUIDefaults;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.io.ReporterExporter;
import eu.isas.reporter.myparameters.IdentificationDetails;
import eu.isas.reporter.myparameters.ItraqScore;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.utils.Properties;
import java.awt.Toolkit;
import java.io.*;
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
     * Creates new form ReporterGUI.
     */
    public ReporterGUI() {

        // set up the ErrorLog
        setUpLogFile();
        
        // update the look and feel after adding the panels
        setLookAndFeel();
        
        // add desktop shortcut?
        if (!getJarFilePath().equalsIgnoreCase(".")
                && System.getProperty("os.name").lastIndexOf("Windows") != -1
                && new File(getJarFilePath() + "/conf/firstRun").exists()) {

            // @TODO: add support for desktop icons in mac and linux??

            // delete the firstRun file such that the user is not asked the next time around
            new File(getJarFilePath() + "/conf/firstRun").delete();

            int value = JOptionPane.showConfirmDialog(this,
                    "Create a shortcut to Reporter on the desktop?",
                    "Create Desktop Shortcut?",
                    JOptionPane.YES_NO_OPTION,
                    JOptionPane.QUESTION_MESSAGE);

            if (value == JOptionPane.YES_OPTION) {
                addShortcutAtDeskTop();
            }
        }

        initComponents();
        
        // make sure that the scroll panes are see-through
        proteinTableJScrollPane.getViewport().setOpaque(false);

        // set the title of the frame and add the icon
        setTitle("Reporter " + new Properties().getVersion());
        this.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));
        this.setExtendedState(MAXIMIZED_BOTH);

        proteinTable.setAutoCreateRowSorter(true);

        setLocationRelativeTo(null);
        setVisible(true);
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
        repaintResultsTable();
    }

    /**
     * Method called when a change was made in the settings.
     *
     * @param quantificationPreferences
     */
    public void updateResults(QuantificationPreferences quantificationPreferences) {
        parent.setQuantificationPreferences(quantificationPreferences);
        parent.updateResults();
    }

    /**
     * Revalidates and repaints the results table.
     */
    private void repaintResultsTable() {

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                proteinTable.setModel(new ProteinTable());
                proteinTable.revalidate();
                proteinTable.repaint();
            }
        });
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
        resultsPanel = new javax.swing.JPanel();
        tabPanel = new javax.swing.JTabbedPane();
        proteinPanel = new javax.swing.JPanel();
        proteinTableJScrollPane = new javax.swing.JScrollPane();
        proteinTable = new javax.swing.JTable();
        peptidePanel = new javax.swing.JPanel();
        psmPanel = new javax.swing.JPanel();
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

        backgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        resultsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Results"));
        resultsPanel.setOpaque(false);

        proteinPanel.setOpaque(false);

        proteinTableJScrollPane.setOpaque(false);

        proteinTable.setModel(new ProteinTable());
        proteinTable.setOpaque(false);
        proteinTableJScrollPane.setViewportView(proteinTable);

        javax.swing.GroupLayout proteinPanelLayout = new javax.swing.GroupLayout(proteinPanel);
        proteinPanel.setLayout(proteinPanelLayout);
        proteinPanelLayout.setHorizontalGroup(
            proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 684, Short.MAX_VALUE)
        );
        proteinPanelLayout.setVerticalGroup(
            proteinPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinTableJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 519, Short.MAX_VALUE)
        );

        tabPanel.addTab("Proteins", proteinPanel);

        peptidePanel.setOpaque(false);

        javax.swing.GroupLayout peptidePanelLayout = new javax.swing.GroupLayout(peptidePanel);
        peptidePanel.setLayout(peptidePanelLayout);
        peptidePanelLayout.setHorizontalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 684, Short.MAX_VALUE)
        );
        peptidePanelLayout.setVerticalGroup(
            peptidePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 519, Short.MAX_VALUE)
        );

        tabPanel.addTab("Peptides", peptidePanel);

        psmPanel.setOpaque(false);

        javax.swing.GroupLayout psmPanelLayout = new javax.swing.GroupLayout(psmPanel);
        psmPanel.setLayout(psmPanelLayout);
        psmPanelLayout.setHorizontalGroup(
            psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 684, Short.MAX_VALUE)
        );
        psmPanelLayout.setVerticalGroup(
            psmPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 519, Short.MAX_VALUE)
        );

        tabPanel.addTab("PSMs", psmPanel);

        javax.swing.GroupLayout resultsPanelLayout = new javax.swing.GroupLayout(resultsPanel);
        resultsPanel.setLayout(resultsPanelLayout);
        resultsPanelLayout.setHorizontalGroup(
            resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabPanel)
                .addContainerGap())
        );
        resultsPanelLayout.setVerticalGroup(
            resultsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(resultsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(tabPanel)
                .addContainerGap())
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(resultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(resultsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
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
        new NewDialog(this, parent);
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
            ReporterExporter exporter = new ReporterExporter(parent.getExperiment(), "\t");
            try {
                exporter.exportResults(quantification, identification, fileChooser.getSelectedFile().getPath(), null);
                JOptionPane.showMessageDialog(this, "Output complete.", "Output successful.", JOptionPane.INFORMATION_MESSAGE);
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Output error.", "Output error, see log file.", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }//GEN-LAST:event_exportMenuItemActionPerformed

    private void exitMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitMenuItemActionPerformed
        System.exit(0);
    }//GEN-LAST:event_exitMenuItemActionPerformed

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
    private javax.swing.JPanel peptidePanel;
    private javax.swing.JPanel proteinPanel;
    private javax.swing.JTable proteinTable;
    private javax.swing.JScrollPane proteinTableJScrollPane;
    private javax.swing.JPanel psmPanel;
    private javax.swing.JMenu quantificationOptionsMenu;
    private javax.swing.JMenuItem quantificationOptionsMenuItem;
    private javax.swing.JPanel resultsPanel;
    private javax.swing.JMenuItem saveMenuItem;
    private javax.swing.JTabbedPane tabPanel;
    // End of variables declaration//GEN-END:variables

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
                return quantification.getSample(reporterIons.get(pos).getIndex()).getReference() + "/"
                        + quantification.getSample(quantification.getReferenceLabel()).getReference();
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
                    IdentificationDetails identificationDetails = new IdentificationDetails();
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        identificationDetails = (IdentificationDetails) identification.getMatchParameter(peptideKey, identificationDetails);
                        if (identificationDetails.isValidated()) {
                            nPeptides++;
                        }
                    }
                    return nPeptides;
                } else if (column == 4) {
                    int nSpectra = 0;
                    IdentificationDetails identificationDetails = new IdentificationDetails();
                    PeptideMatch peptideMatch;
                    for (String peptideKey : proteinMatch.getPeptideMatches()) {
                        peptideMatch = identification.getPeptideMatch(peptideKey);
                        for (String psmKey : peptideMatch.getSpectrumMatches()) {
                            identificationDetails = (IdentificationDetails) identification.getMatchParameter(psmKey, identificationDetails);
                            if (identificationDetails.isValidated()) {
                                nSpectra++;
                            }
                        }

                    }
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
            if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
                String path = getJarFilePath() + "/conf/Reporter.log";

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
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        String path = this.getClass().getResource("ReporterGUI.class").getPath();

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

        String jarFilePath = getJarFilePath();

        if (!jarFilePath.equalsIgnoreCase(".")) {

            // remove the initial '/' at the start of the line
            if (jarFilePath.startsWith("\\") && !jarFilePath.startsWith("\\\\")) {
                jarFilePath = jarFilePath.substring(1);
            }

            String iconFileLocation = jarFilePath + "\\conf\\reporter.ico";
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
