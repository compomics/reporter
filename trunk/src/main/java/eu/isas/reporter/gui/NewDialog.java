package eu.isas.reporter.gui;

import com.compomics.util.db.ObjectsCache;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.Quantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.JOptionEditorPane;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.reporter.gui.settings.ReporterSettingsDialog;
import eu.isas.reporter.myparameters.ReporterPreferences;
import eu.isas.reporter.myparameters.ReporterSettings;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This panel will be used to load the necessary files and settings to start the
 * analysis.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class NewDialog extends javax.swing.JDialog {

    /**
     * File containing the various reporter methods.
     */
    private final String METHODS_FILE = "resources/conf/reporterMethods.xml";
    /**
     * The current methods file.
     */
    private File methodsFile;
    /**
     * The compomics reporter methods factory.
     */
    private ReporterMethodFactory methodsFactory = ReporterMethodFactory.getInstance();
    /**
     * The reporter class.
     */
    private ReporterGUI reporterGui;
    /**
     * The method selected.
     */
    private ReporterMethod selectedMethod = null;
    /**
     * The cps parent used to manage the data.
     */
    private CpsParent cpsBean = new CpsParent();
    /**
     * The mgf files loaded.
     */
    private ArrayList<File> mgfFiles = new ArrayList<File>();
    /**
     * The reporter settings
     */
    private ReporterSettings reporterSettings;
    /**
     * A simple progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance(100);
    /**
     * List of all sample names.
     */
    private HashMap<String, String> sampleNames = new HashMap<String, String>();
    /**
     * List of reagents used in this reporter method
     */
    private ArrayList<String> reagents = new ArrayList<String>();
    /**
     * List of control samples.
     */
    private ArrayList<String> controlSamples = new ArrayList<String>();
    /**
     * The cache to use for identification and quantification objects.
     */
    private ObjectsCache cache;
    /**
     * Boolean indicating whether the user canceled the project creation
     */
    private boolean cancelled = false;
    /**
     * A reporter ion quantification object containing the input from the user.
     */
    private ReporterIonQuantification reporterIonQuantification = null;

    /**
     * Constructor.
     *
     * @param reporterGui the reporter class
     */
    public NewDialog(ReporterGUI reporterGui) {
        super(reporterGui, true);

        this.reporterGui = reporterGui;

        methodsFile = new File(reporterGui.getJarFilePath(), METHODS_FILE);
        importMethods();

        initComponents();

        setUpGui();

        // load the user preferences
        loadDefaultPreferences();

        if (selectedMethod == null && methodsFactory.getMethodsNames() != null && methodsFactory.getMethodsNames().length > 0) {
            reporterMethodComboBox.setSelectedItem(methodsFactory.getMethodsNames()[0]);
        }

        reporterMethodComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedMethod = getMethod((String) reporterMethodComboBox.getSelectedItem());
                reagents = selectedMethod.getReagentsSortedByMass();
                refresh();
            }
        });

        reporterGui.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));

        refresh();

        pack();
        setLocationRelativeTo(reporterGui);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     */
    private void setUpGui() {
        // make sure that the scroll panes are see-through
        sampleAssignmentJScrollPane.getViewport().setOpaque(false);

        // centrally align the comboboxes
        reporterMethodComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        // disables the user to drag column headers to reorder columns
        sampleAssignmentTable.getTableHeader().setReorderingAllowed(false);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {

        sampleAssignmentTable.getColumnModel().getColumn(0).setMaxWidth(30);
        sampleAssignmentTable.getColumnModel().getColumn(1).setMinWidth(150);
        sampleAssignmentTable.getColumnModel().getColumn(1).setMaxWidth(150);
        sampleAssignmentTable.getColumnModel().getColumn(3).setMinWidth(80);
        sampleAssignmentTable.getColumnModel().getColumn(3).setMaxWidth(80);

        sampleAssignmentTable.getColumnModel().getColumn(3).setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
                null,
                "Yes", "No"));
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
        fileSelectiontPanel = new javax.swing.JPanel();
        spectrumFilesLabel = new javax.swing.JLabel();
        txtSpectraFileLocation = new javax.swing.JTextField();
        idFilesLabel = new javax.swing.JLabel();
        txtIdFileLocation = new javax.swing.JTextField();
        addIdFilesButton = new javax.swing.JButton();
        addSpectraFilesJButton = new javax.swing.JButton();
        databaseFileLabel = new javax.swing.JLabel();
        fastaTxt = new javax.swing.JTextField();
        addDbButton = new javax.swing.JButton();
        samplePanel = new javax.swing.JPanel();
        sampleAssignmentJScrollPane = new javax.swing.JScrollPane();
        sampleAssignmentTable = new javax.swing.JTable();
        reporterMethodLabel = new javax.swing.JLabel();
        reporterMethodComboBox = new javax.swing.JComboBox();
        methodSettingsButton = new javax.swing.JButton();
        advancedSettingsPanel = new javax.swing.JPanel();
        quantPreferencesLabel = new javax.swing.JLabel();
        quantificationPreferencesTxt = new javax.swing.JTextField();
        editQuantPrefsButton = new javax.swing.JButton();
        aboutButton = new javax.swing.JButton();
        reporterPublicationLabel = new javax.swing.JLabel();
        loadButton = new javax.swing.JButton();

        setTitle("New Project");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        fileSelectiontPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Files Selection"));
        fileSelectiontPanel.setOpaque(false);

        spectrumFilesLabel.setText("Spectrum File(s)");

        txtSpectraFileLocation.setEditable(false);
        txtSpectraFileLocation.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        idFilesLabel.setText("Identification File");

        txtIdFileLocation.setEditable(false);
        txtIdFileLocation.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtIdFileLocation.setText("Please import a PeptideShaker project");

        addIdFilesButton.setText("Browse");
        addIdFilesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addIdFilesButtonActionPerformed(evt);
            }
        });

        addSpectraFilesJButton.setText("Browse");
        addSpectraFilesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSpectraFilesJButtonActionPerformed(evt);
            }
        });

        databaseFileLabel.setText("Database File");

        fastaTxt.setEditable(false);
        fastaTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        addDbButton.setText("Browse");
        addDbButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addDbButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout fileSelectiontPanelLayout = new org.jdesktop.layout.GroupLayout(fileSelectiontPanel);
        fileSelectiontPanel.setLayout(fileSelectiontPanelLayout);
        fileSelectiontPanelLayout.setHorizontalGroup(
            fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(fileSelectiontPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(spectrumFilesLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(databaseFileLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(idFilesLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(10, 10, 10)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(txtIdFileLocation)
                    .add(txtSpectraFileLocation, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 504, Short.MAX_VALUE)
                    .add(fastaTxt))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, addSpectraFilesJButton)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, addIdFilesButton)
                    .add(addDbButton))
                .addContainerGap())
        );

        fileSelectiontPanelLayout.linkSize(new java.awt.Component[] {addDbButton, addIdFilesButton, addSpectraFilesJButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        fileSelectiontPanelLayout.setVerticalGroup(
            fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(fileSelectiontPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(idFilesLabel)
                    .add(txtIdFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addIdFilesButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(spectrumFilesLabel)
                    .add(txtSpectraFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addSpectraFilesJButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(databaseFileLabel)
                    .add(fastaTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addDbButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        samplePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sample Assignment"));
        samplePanel.setOpaque(false);

        sampleAssignmentTable.setModel(new AssignementTableModel());
        sampleAssignmentTable.setOpaque(false);
        sampleAssignmentJScrollPane.setViewportView(sampleAssignmentTable);

        reporterMethodLabel.setText("Reporter Method");

        reporterMethodComboBox.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNames()));
        reporterMethodComboBox.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                reporterMethodComboBoxActionPerformed(evt);
            }
        });

        methodSettingsButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit_gray.png"))); // NOI18N
        methodSettingsButton.setToolTipText("Edit Method Settings");
        methodSettingsButton.setBorder(null);
        methodSettingsButton.setBorderPainted(false);
        methodSettingsButton.setContentAreaFilled(false);
        methodSettingsButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/edit.png"))); // NOI18N
        methodSettingsButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                methodSettingsButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                methodSettingsButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                methodSettingsButtonMouseReleased(evt);
            }
        });

        org.jdesktop.layout.GroupLayout samplePanelLayout = new org.jdesktop.layout.GroupLayout(samplePanel);
        samplePanel.setLayout(samplePanelLayout);
        samplePanelLayout.setHorizontalGroup(
            samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(samplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 707, Short.MAX_VALUE)
                    .add(samplePanelLayout.createSequentialGroup()
                        .add(reporterMethodLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(reporterMethodComboBox, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(methodSettingsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        samplePanelLayout.setVerticalGroup(
            samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(samplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(reporterMethodLabel)
                    .add(reporterMethodComboBox, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(methodSettingsButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 190, Short.MAX_VALUE)
                .addContainerGap())
        );

        advancedSettingsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Advanced Settings"));
        advancedSettingsPanel.setOpaque(false);

        quantPreferencesLabel.setText("Quantification");

        quantificationPreferencesTxt.setEditable(false);
        quantificationPreferencesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        quantificationPreferencesTxt.setText("Default Settings");

        editQuantPrefsButton.setText("Edit");
        editQuantPrefsButton.setEnabled(false);
        editQuantPrefsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editQuantPrefsButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout advancedSettingsPanelLayout = new org.jdesktop.layout.GroupLayout(advancedSettingsPanel);
        advancedSettingsPanel.setLayout(advancedSettingsPanelLayout);
        advancedSettingsPanelLayout.setHorizontalGroup(
            advancedSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(quantPreferencesLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(quantificationPreferencesTxt)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(editQuantPrefsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        advancedSettingsPanelLayout.setVerticalGroup(
            advancedSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(advancedSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(quantPreferencesLabel)
                    .add(quantificationPreferencesTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(editQuantPrefsButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        aboutButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/Reporter-shadow.png"))); // NOI18N
        aboutButton.setToolTipText("Open the Reporter web page");
        aboutButton.setBorder(null);
        aboutButton.setBorderPainted(false);
        aboutButton.setContentAreaFilled(false);
        aboutButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                aboutButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                aboutButtonMouseExited(evt);
            }
        });
        aboutButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                aboutButtonActionPerformed(evt);
            }
        });

        reporterPublicationLabel.setText("<html>Please cite Reporter as <a href=\"http://reporter.googlecode.com\">http://reporter.googlecode.com</a></html>");
        reporterPublicationLabel.setToolTipText("Open the Reporter web page");
        reporterPublicationLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                reporterPublicationLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                reporterPublicationLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                reporterPublicationLabelMouseExited(evt);
            }
        });

        loadButton.setBackground(new java.awt.Color(0, 153, 0));
        loadButton.setFont(loadButton.getFont().deriveFont(loadButton.getFont().getStyle() | java.awt.Font.BOLD));
        loadButton.setForeground(new java.awt.Color(255, 255, 255));
        loadButton.setText("Start Quantifying!");
        loadButton.setToolTipText("Click here to start the quantification");
        loadButton.setEnabled(false);
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout backgroundPanelLayout = new org.jdesktop.layout.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(advancedSettingsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(backgroundPanelLayout.createSequentialGroup()
                        .add(aboutButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(reporterPublicationLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 406, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(loadButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 167, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(14, 14, 14))
                    .add(org.jdesktop.layout.GroupLayout.LEADING, fileSelectiontPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, samplePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(fileSelectiontPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(samplePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedSettingsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.CENTER)
                    .add(aboutButton)
                    .add(reporterPublicationLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 45, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(loadButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 53, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Opens a file chooser for adding identification files.
     *
     * @param evt
     */
    private void addIdFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIdFilesButtonActionPerformed

        JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Identification File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter filter = new FileFilter() {
            public boolean accept(File myFile) {
                return myFile.getName().endsWith("cps")
                        || myFile.isDirectory();
            }

            public String getDescription() {
                return "Supported formats: Compomics PeptideShaker (.cps)";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showDialog(this.getParent(), "OK");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File newFile = fileChooser.getSelectedFile();

            if (!newFile.exists()) {
                JOptionPane.showMessageDialog(this, "The file\'" + newFile.getAbsolutePath() + "\' " + "does not exist!",
                        "File Not Found.", JOptionPane.ERROR_MESSAGE);
            } else {
                reporterGui.getLastSelectedFolder().setLastSelectedFolder(newFile.getPath());
                importPeptideShakerFile(newFile);
            }
        }
    }//GEN-LAST:event_addIdFilesButtonActionPerformed

    /**
     * Open a file chooser for adding spectrum files.
     *
     * @param evt
     */
    private void addSpectraFilesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSpectraFilesJButtonActionPerformed

        // @TODO: add mgf validation etc like for PeptideShaker
        JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Spectra File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mgf")
                        || myFile.isDirectory();
            }

            public String getDescription() {
                return "Supported formats: .mgf";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showDialog(this.getParent(), "Add");

        try {
            if (returnVal == JFileChooser.APPROVE_OPTION) {
                for (File newFile : fileChooser.getSelectedFiles()) {
                    if (newFile.isDirectory()) {
                        File[] tempFiles = newFile.listFiles();
                        for (File file : tempFiles) {
                            if (file.getName().toLowerCase().endsWith(".mgf")) {
                                if (!mgfFiles.contains(file)) {
                                    mgfFiles.add(file);
                                    cpsBean.getProjectDetails().addSpectrumFile(file);
                                }
                            }
                        }
                    } else {
                        if (newFile.getName().toLowerCase().endsWith(".mgf")) {
                            if (!mgfFiles.contains(newFile)) {
                                mgfFiles.add(newFile);
                                cpsBean.getProjectDetails().addSpectrumFile(newFile);
                                spectrumFactory.addSpectra(newFile, null); // @TODO: add progress dialog!!
                            }
                        }
                    }

                    reporterGui.getLastSelectedFolder().setLastSelectedFolder(newFile.getPath());
                }

                txtSpectraFileLocation.setText(mgfFiles.size() + " file(s) selected");
            }
        } catch (IOException e) {
            progressDialog.setRunFinished();
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "An error occurred while reading the mgf file.", "Mgf Error", JOptionPane.WARNING_MESSAGE);
        }
    }//GEN-LAST:event_addSpectraFilesJButtonActionPerformed

    /**
     * Clear the sample names and update the method table.
     *
     * @param evt
     */
    private void reporterMethodComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reporterMethodComboBoxActionPerformed
        sampleNames = new HashMap<String, String>();
        selectedMethod = getMethod((String) reporterMethodComboBox.getSelectedItem());
        reagents = selectedMethod.getReagentsSortedByMass();
        refresh();
    }//GEN-LAST:event_reporterMethodComboBoxActionPerformed

    /**
     * Opens a file chooser for selecting the database FASTA file.
     *
     * @param evt
     */
    private void addDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDbButtonActionPerformed
        JFileChooser fileChooser;

//        if (searchParameters != null && searchParameters.getFastaFile() != null && searchParameters.getFastaFile().exists()) {
//            fileChooser = new JFileChooser(searchParameters.getFastaFile());
//        } else {
//            fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());
//        }
        fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder().getLastSelectedFolder());

        fileChooser.setDialogTitle("Select FASTA File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith("fasta")
                        || myFile.getName().toLowerCase().endsWith("fast")
                        || myFile.getName().toLowerCase().endsWith("fas")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Supported formats: FASTA (.fasta)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this.getParent(), "Open");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File fastaFile = fileChooser.getSelectedFile();
            reporterGui.getLastSelectedFolder().setLastSelectedFolder(fastaFile.getAbsolutePath());
            try {
                SequenceFactory.getInstance().loadFastaFile(fastaFile, null); // @TODO: use waiting handler
                getSearchParameters().setFastaFile(fastaFile);
                fastaTxt.setText(fastaFile.getName());
            } catch (FileNotFoundException ex) {
                ex.printStackTrace();
            } catch (IOException ex) {
                ex.printStackTrace();
            } catch (ClassNotFoundException ex) {
                ex.printStackTrace();
            } catch (StringIndexOutOfBoundsException ex) {
                ex.printStackTrace();
            } catch (IllegalArgumentException ex) {
                ex.printStackTrace();
            }
        }
    }//GEN-LAST:event_addDbButtonActionPerformed

    /**
     * Clear the data and close the dialog.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        cancelled = true;
        this.dispose();
    }//GEN-LAST:event_formWindowClosing

    /**
     * Change the cursor icon to a hand cursor.
     *
     * @param evt
     */
    private void methodSettingsButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_methodSettingsButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_methodSettingsButtonMouseEntered

    /**
     * Change the cursor icon back to the default icon.
     *
     * @param evt
     */
    private void methodSettingsButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_methodSettingsButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_methodSettingsButtonMouseExited

    /**
     * Open the MethodSettingsDialog.
     *
     * @param evt
     */
    private void methodSettingsButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_methodSettingsButtonMouseReleased
        new MethodSettingsDialog(this, true);
    }//GEN-LAST:event_methodSettingsButtonMouseReleased

    /**
     * Open the PreferencesDialog.
     *
     * @param evt
     */
    private void editQuantPrefsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editQuantPrefsButtonActionPerformed
        ReporterSettingsDialog reporterSettingsDialog = new ReporterSettingsDialog(this, reporterSettings, cpsBean.getIdentificationParameters().getSearchParameters().getPtmSettings(), getSelectedMethod(), true);
        ReporterSettings newSettings = reporterSettingsDialog.getReporterSettings();
        if (!reporterSettingsDialog.isCanceled() && !reporterSettings.isSameAs(newSettings)) {
            reporterSettings = newSettings;
            ReporterPreferences reporterPreferences = ReporterPreferences.getUserPreferences();
            reporterPreferences.setDefaultSettings(newSettings);
            ReporterPreferences.saveUserPreferences(reporterPreferences);
        }
    }//GEN-LAST:event_editQuantPrefsButtonActionPerformed

    /**
     * Change the cursor icon to a hand cursor.
     *
     * @param evt
     */
    private void aboutButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseEntered

    /**
     * Change the cursor icon back to the default icon.
     *
     * @param evt
     */
    private void aboutButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_aboutButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonMouseExited

    /**
     * Open the Reporter web page. 
     *
     * @param evt 
     */
    private void aboutButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_aboutButtonActionPerformed
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://reporter.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    /**
     * Open the Reporter publication.
     * 
     * @param evt 
     */
    private void reporterPublicationLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_reporterPublicationLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://reporter.googlecode.com");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_reporterPublicationLabelMouseClicked

    /**
     * Change the cursor icon back to the default icon.
     *
     * @param evt
     */
    private void reporterPublicationLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_reporterPublicationLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_reporterPublicationLabelMouseEntered

    /**
     * Change the cursor icon back to the default icon.
     *
     * @param evt
     */
    private void reporterPublicationLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_reporterPublicationLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_reporterPublicationLabelMouseExited

    /**
     * Start loading the data.
     *
     * @param evt
     */
    private void loadButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_loadButtonActionPerformed
        if (validateInput()) {
            reporterIonQuantification = new ReporterIonQuantification(Quantification.QuantificationMethod.REPORTER_IONS);
            for (String key : sampleNames.keySet()) {
                reporterIonQuantification.assignSample(key, new Sample(sampleNames.get(key)));
            }
            reporterIonQuantification.setMethod(selectedMethod);
            reporterIonQuantification.setControlSamples(controlSamples);
            dispose();
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JButton addDbButton;
    private javax.swing.JButton addIdFilesButton;
    private javax.swing.JButton addSpectraFilesJButton;
    private javax.swing.JPanel advancedSettingsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel databaseFileLabel;
    private javax.swing.JButton editQuantPrefsButton;
    private javax.swing.JTextField fastaTxt;
    private javax.swing.JPanel fileSelectiontPanel;
    private javax.swing.JLabel idFilesLabel;
    private javax.swing.JButton loadButton;
    private javax.swing.JButton methodSettingsButton;
    private javax.swing.JLabel quantPreferencesLabel;
    private javax.swing.JTextField quantificationPreferencesTxt;
    private javax.swing.JComboBox reporterMethodComboBox;
    private javax.swing.JLabel reporterMethodLabel;
    private javax.swing.JLabel reporterPublicationLabel;
    private javax.swing.JScrollPane sampleAssignmentJScrollPane;
    private javax.swing.JTable sampleAssignmentTable;
    private javax.swing.JPanel samplePanel;
    private javax.swing.JLabel spectrumFilesLabel;
    private javax.swing.JTextField txtIdFileLocation;
    private javax.swing.JTextField txtSpectraFileLocation;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the reporter method corresponding to the given name.
     *
     * @param methodName the given name
     * @return the corresponding reporter method
     */
    public ReporterMethod getMethod(String methodName) {
        if (methodsFactory.getMethods() == null) {
            importMethods();
        }
        for (ReporterMethod method : methodsFactory.getMethods()) {
            if (methodName == null || method.getName().equals(methodName)) {
                return method;
            }
        }
        return null;
    }

    /**
     * Updates the combo box and table values based on the currently selected
     * quantification method.
     */
    private void refresh() {

        if (selectedMethod != null) {
            reporterMethodComboBox.setSelectedItem(selectedMethod.getName());
        }

        sampleAssignmentTable.setModel(new AssignementTableModel());

        setTableProperties();

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                sampleAssignmentTable.revalidate();
                sampleAssignmentTable.repaint();
            }
        });
    }

    /**
     * Imports the methods from the methods file.
     */
    private void importMethods() {
        try {
            methodsFactory.importMethods(methodsFile);
        } catch (Exception e) {
            e.printStackTrace();
            importMethodsError();
        }
    }

    /**
     * Loads the quantification preferences in the GUI.
     */
    private void loadDefaultPreferences() {
        ReporterPreferences reporterPreferences = ReporterPreferences.getUserPreferences();
        reporterSettings = reporterPreferences.getDefaultSettings();
    }

    /**
     * Method used to import a PeptideShaker file.
     *
     * @param psFile a PeptideShaker file
     */
    private void importPeptideShakerFile(final File psFile) {

        progressDialog = new ProgressDialogX(this, reporterGui,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Importing Project. Please Wait...");

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
                    cpsBean.setCpsFile(psFile);

                    try {
                        cpsBean.loadCpsFile(reporterGui.getJarFilePath(), progressDialog);
                    } catch (SQLException e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(NewDialog.this,
                                "An error occurred while reading:\n" + cpsBean.getCpsFile() + ".\n\n"
                                + "It looks like another instance of PeptideShaker is still connected to the file.\n"
                                + "Please close all instances of PeptideShaker and try again.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                    }

                    progressDialog.setTitle("Loading Gene Mappings. Please Wait...");
                    loadGeneMappings(); // have to load the new gene mappings

                    // @TODO: check if the used gene mapping files are available and download if not?
                    if (progressDialog.isRunCanceled()) {
                        progressDialog.setRunFinished();
                        return;
                    }

                    progressDialog.setTitle("Loading FASTA File. Please Wait...");

                    boolean fileFound;
                    try {
                        fileFound = cpsBean.loadFastaFile(new File(reporterGui.getLastSelectedFolder().getLastSelectedFolder()), progressDialog);
                    } catch (Exception e) {
                        fileFound = false;
                    }

                    if (!fileFound) {
                        JOptionPane.showMessageDialog(NewDialog.this,
                                "An error occurred while reading:\n" + getSearchParameters().getFastaFile() + "."
                                + "\n\nPlease select the file manually.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                    }

                    if (progressDialog.isRunCanceled()) {
                        progressDialog.setRunFinished();
                        return;
                    }

                    progressDialog.setTitle("Loading Spectrum Files. Please Wait...");
                    progressDialog.setPrimaryProgressCounterIndeterminate(false);
                    progressDialog.setMaxPrimaryProgressCounter(getIdentification().getSpectrumFiles().size() + 1);
                    progressDialog.increasePrimaryProgressCounter();

                    int cpt = 0, total = getIdentification().getSpectrumFiles().size();
                    for (String spectrumFileName : getIdentification().getSpectrumFiles()) {

                        progressDialog.setTitle("Loading Spectrum Files (" + ++cpt + " of " + total + "). Please Wait...");
                        progressDialog.increasePrimaryProgressCounter();

                        boolean found;
                        try {
                            found = cpsBean.loadSpectrumFile(spectrumFileName, progressDialog);
                        } catch (Exception e) {
                            found = false;
                        }
                        if (!found) {
                            JOptionPane.showMessageDialog(NewDialog.this,
                                    "Spectrum file not found: \'" + spectrumFileName + "\'."
                                    + "\nPlease select the spectrum file or the folder containing it manually.",
                                    "File Not Found", JOptionPane.ERROR_MESSAGE);
                        }

                        if (progressDialog.isRunCanceled()) {
                            progressDialog.setRunFinished();
                            return;
                        }
                    }

                    loadButton.setEnabled(true);
                    editQuantPrefsButton.setEnabled(true);
                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setRunFinished();

                } catch (OutOfMemoryError error) {
                    System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
                    Runtime.getRuntime().gc();
                    JOptionPane.showMessageDialog(NewDialog.this, JOptionEditorPane.getJOptionEditorPane(
                            "PeptideShaker used up all the available memory and had to be stopped.<br>"
                            + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                            + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                            + "Java Options). See also <a href=\"http://code.google.com/p/compomics-utilities/wiki/JavaTroubleShooting\">JavaTroubleShooting</a>."),
                            "Out Of Memory", JOptionPane.ERROR_MESSAGE);
                    progressDialog.setRunFinished();
                    error.printStackTrace();
                } catch (EOFException e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(NewDialog.this,
                            "An error occurred while reading:\n" + cpsBean.getCpsFile() + ".\n\n"
                            + "The file is corrupted and cannot be opened anymore.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (Exception e) {
                    progressDialog.setRunFinished();
                    JOptionPane.showMessageDialog(NewDialog.this,
                            "An error occurred while reading:\n" + cpsBean.getCpsFile() + ".\n\n"
                            + "Please verify that the PeptideShaker version used to create\n"
                            + "the file is compatible with your version of Reporter.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }

                if (progressDialog.isRunCanceled()) {
                    progressDialog.dispose();
                    return;
                }

                txtSpectraFileLocation.setText(cpsBean.getIdentification().getSpectrumFiles().size() + " files loaded"); //@TODO: allow editing
                fastaTxt.setText(cpsBean.getIdentificationParameters().getSearchParameters().getFastaFile().getName());
                txtIdFileLocation.setText(cpsBean.getCpsFile().getName());

                cache = new ObjectsCache();
                cache.setAutomatedMemoryManagement(true);

                //@TODO: load quantification parameters if existing
                SearchParameters searchParameters = getSearchParameters();

                // try to detect the method used
                for (String ptmName : searchParameters.getPtmSettings().getAllModifications()) {
                    if (ptmName.contains("iTRAQ 4-plex")) {
                        selectedMethod = getMethod("iTRAQ 4-plex");
                        break;
                    } else if (ptmName.contains("iTRAQ 8-plex")) {
                        selectedMethod = getMethod("iTRAQ 8-plex");
                        break;
                    } else if (ptmName.contains("TMT 2-plex")) {
                        selectedMethod = getMethod("TMT 2-plex");
                        break;
                    } else if (ptmName.contains("TMT") && ptmName.contains("6-plex")) {
                        if (searchParameters.getIonSearched1() == PeptideFragmentIon.Y_ION
                                || searchParameters.getIonSearched2() == PeptideFragmentIon.Y_ION) {
                            selectedMethod = getMethod("TMT 6-plex (HCD)");
                        } else {
                            selectedMethod = getMethod("TMT 6-plex (ETD)");
                        }
                        if (reporterSettings.getReporterIonSelectionSettings().getReporterIonsMzTolerance() > 0.0016) {
                            reporterSettings.getReporterIonSelectionSettings().setReporterIonsMzTolerance(0.0016);
                        }
                        break;
                    } else if (ptmName.contains("TMT") && ptmName.contains("10-plex")) {
                        selectedMethod = getMethod("TMT 10-plex");
                        if (reporterSettings.getReporterIonSelectionSettings().getReporterIonsMzTolerance() > 0.0016) {
                            reporterSettings.getReporterIonSelectionSettings().setReporterIonsMzTolerance(0.0016);
                        }
                        break;
                    }
                }

                // no method detected, default to iTRAQ 4plex
                if (selectedMethod == null) {
                    reporterMethodComboBox.setSelectedItem(methodsFactory.getMethodsNames()[0]);
                }

                reagents = selectedMethod.getReagentsSortedByMass();
                sampleNames.clear();
                refresh();

                progressDialog.setRunFinished();
            }
        }.start();
    }

    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        return cpsBean.getProjectDetails();
    }

    /**
     * Returns the search parameters.
     *
     * @return the search parameters
     */
    public SearchParameters getSearchParameters() {
        return cpsBean.getIdentificationParameters().getSearchParameters();
    }

    /**
     * Returns the experiment.
     *
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        return cpsBean.getExperiment();
    }

    /**
     * Returns the sample.
     *
     * @return the sample
     */
    public Sample getSample() {
        return cpsBean.getSample();
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicateNumber
     */
    public int getReplicateNumber() {
        return cpsBean.getReplicateNumber();
    }

    /**
     * Returns the identification displayed.
     *
     * @return the identification displayed
     */
    public Identification getIdentification() {
        return cpsBean.getIdentification();
    }

    /**
     * Imports the gene mapping.
     */
    private void loadGeneMappings() {
        if (!cpsBean.loadGeneMappings(reporterGui.getJarFilePath(), progressDialog)) {
            JOptionPane.showMessageDialog(this, "Unable to load the gene/GO mapping file.", "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Method called whenever an error was encountered while loading the
     * methods.
     */
    private void importMethodsError() {
        JOptionPane.showMessageDialog(this, "\"" + METHODS_FILE + "\" could not be parsed, please select a method file.", "No Spectrum File Selected", JOptionPane.ERROR_MESSAGE);
        JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Methods file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showDialog(this.getParent(), "Add");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File newFile = fileChooser.getSelectedFile();
            try {
                methodsFactory.importMethods(newFile);
                reporterGui.getLastSelectedFolder().setLastSelectedFolder(newFile.getPath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "File " + METHODS_FILE + " not found in conf folder.",
                        "Methods file not found", JOptionPane.WARNING_MESSAGE);
                importMethodsError();
            } catch (XmlPullParserException e) {
                JOptionPane.showMessageDialog(this,
                        "An error occurred while parsing " + METHODS_FILE + " at line " + e.getLineNumber() + ".",
                        "Parsing error", JOptionPane.WARNING_MESSAGE);
                importMethodsError();
            }
        }
    }

    /**
     * Methods which validates the user input (returns false in case of wrong
     * input).
     *
     * @return true if the input can be processed
     */
    private boolean validateInput() {
        // @TODO: validate that the PeptideShaker project has been loaded
        return true;
    }

    /**
     * Sets the spectra files to process.
     *
     * @param files the spectra files to process
     */
    public void addSpectrumFiles(ArrayList<File> files) {
        progressDialog = new ProgressDialogX(this, reporterGui,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Importing Spectra. Please Wait...");
        final ArrayList<File> newMgfFiles = files;

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
                ArrayList<File> error = processSpectrumFiles(newMgfFiles, progressDialog);
                String report = "An error occurred while importing ";
                if (error.size() == 1) {
                    report += error.get(0).getName() + ".";
                } else if (error.size() == newMgfFiles.size()) {
                    report += "the mgf files.";
                } else {
                    report += error.size() + " mgf files of the " + newMgfFiles.size() + " selected.";
                }
                JOptionPane.showMessageDialog(NewDialog.this,
                        report,
                        "File Input Error", JOptionPane.ERROR_MESSAGE);
                txtSpectraFileLocation.setText(mgfFiles.size() + " file(s) selected");
            }
        }.start();
    }

    /**
     * Imports a list of spectrum files and returns a list containing any
     * problematic file.
     *
     * @param spectrumFiles list of spectrum files to process
     * @param progressDialog process dialog displaying progress
     * @return a list of files which could not be processed
     */
    private ArrayList<File> processSpectrumFiles(ArrayList<File> spectrumFiles, ProgressDialogX progressDialog) {
        ArrayList<File> error = new ArrayList<File>();
        int cpt = 1;
        for (File mgfFile : spectrumFiles) {

            if (progressDialog.isRunCanceled()) {
                progressDialog.dispose();
                return null;
            }

            progressDialog.setTitle("Importing Spectrum Files (" + cpt++ + "/" + spectrumFiles.size() + "). Please Wait...");

            try {
                spectrumFactory.addSpectra(mgfFile, progressDialog);
                mgfFiles.add(mgfFile);
            } catch (Exception e) {
                error.add(mgfFile);
            }
        }
        return error;
    }

    /**
     * Returns the selected method.
     *
     * @return the selectedMethod
     */
    public ReporterMethod getSelectedMethod() {
        return selectedMethod;
    }

    /**
     * Set the selected method.
     *
     * @param selectedMethod the selectedMethod to set
     */
    public void setSelectedMethod(ReporterMethod selectedMethod) {
        this.selectedMethod = selectedMethod;
    }

    /**
     * Returns the reagents.
     *
     * @return the reagents
     */
    public ArrayList<String> getReagents() {
        return reagents;
    }

    /**
     * Set the reagents.
     *
     * @param reagents the reagents to set
     */
    public void setReagents(ArrayList<String> reagents) {
        this.reagents = reagents;
    }

    /**
     * Returns the current methods file.
     *
     * @return the methodsFile
     */
    public File getMethodsFile() {
        return methodsFile;
    }

    /**
     * Set the current method file.
     *
     * @param methodsFile the methodsFile to set
     */
    public void setMethodsFile(File methodsFile) {
        this.methodsFile = methodsFile;
    }

    /**
     * @return the reporterGui
     */
    public ReporterGUI getReporterGui() {
        return reporterGui;
    }

    /**
     * Table model for the sample to reporter ion assignment.
     */
    private class AssignementTableModel extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (selectedMethod == null) {
                return 0;
            }
            return reagents.size();
        }

        @Override
        public int getColumnCount() {
            return 4;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Label";
                case 2:
                    return "Sample";
                case 3:
                    return "Ref";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String reagentName = reagents.get(row);
            switch (column) {
                case 0:
                    return (row + 1);
                case 1:
                    ReporterIon reporterIon = selectedMethod.getReporterIon(reagentName);
                    return reporterIon.getName();
                case 2:
                    Sample sample = getSample();
                    if (sampleNames.get(reagentName) == null) {
                        if (sample != null) {
                            sampleNames.put(reagentName, sample.getReference() + " " + reagentName);
                        } else {
                            sampleNames.put(reagentName, "Sample " + reagentName);
                        }
                    }
                    return sampleNames.get(reagentName);
                case 3:
                    return controlSamples.contains(reagentName);
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            String reagentName = reagents.get(row);
            if (column == 2) {
                sampleNames.put(reagentName, aValue.toString());
            } else if (column == 3) {
                if (controlSamples.contains(reagentName)) {
                    controlSamples.remove(reagentName);
                } else {
                    controlSamples.add(reagentName);
                }
            }
            repaint();
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column > 1;
        }
    }

    /**
     * Indicates whether the user canceled the project creation.
     *
     * @return a boolean indicating whether the user canceled the project
     * creation
     */
    public boolean isCancelled() {
        return cancelled;
    }

    /**
     * Returns the cps parent object providing all informations contained in the
     * cps file
     *
     * @return the cps parent object providing all informations contained in the
     * cps file
     */
    public CpsParent getCpsBean() {
        return cpsBean;
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
     * Returns the identification objects cache.
     *
     * @return the identification objects cache
     */
    public ObjectsCache getCache() {
        return cache;
    }

    /**
     * Returns the reporter ion quantification. Note that this object is only
     * set after the user has pressed "Load".
     *
     * @return the reporter ion quantification containing all quantification
     * parameters
     */
    public ReporterIonQuantification getReporterIonQuantification() {
        return reporterIonQuantification;
    }

    /**
     * Update the reagent names in the sample assignment table.
     */
    public void updateReagentNames() {
        sampleAssignmentTable.revalidate();
        sampleAssignmentTable.repaint();
    }
}
