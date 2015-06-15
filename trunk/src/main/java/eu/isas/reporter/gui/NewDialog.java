package eu.isas.reporter.gui;

import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.ions.PeptideFragmentIon;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.SequenceFactory;
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
import eu.isas.reporter.myparameters.ReporterPreferences;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
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
     * The quantification preferences.
     */
    private ReporterPreferences reporterPreferences;
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
        loadUserPreferences();

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
        sameSpectraActionPerformed(null);

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

        reporterLocatinButtonGroup = new javax.swing.ButtonGroup();
        backgroundPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        processingPanel = new javax.swing.JPanel();
        projectPanel = new javax.swing.JPanel();
        projectTxt = new javax.swing.JTextField();
        projectReferenceLabel = new javax.swing.JLabel();
        sampleNameLabel = new javax.swing.JLabel();
        sampleNameTxt = new javax.swing.JTextField();
        replicateLabel = new javax.swing.JLabel();
        replicateNumberTxt = new javax.swing.JTextField();
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
        advancedParamsPanel = new javax.swing.JPanel();
        spectrumAnalysisPanel = new javax.swing.JPanel();
        reporterIonMzToleranceLabel = new javax.swing.JLabel();
        ionToleranceTxt = new javax.swing.JTextField();
        quantOptionsPanel = new javax.swing.JPanel();
        quantPreferencesLabel = new javax.swing.JLabel();
        quantificationPreferencesTxt = new javax.swing.JTextField();
        editQuantPrefsButton = new javax.swing.JButton();
        reporterLocationPanel = new javax.swing.JPanel();
        sameSpectra = new javax.swing.JRadioButton();
        precursorMatching = new javax.swing.JRadioButton();
        mzToleranceLabel = new javax.swing.JLabel();
        mzTolTxt = new javax.swing.JTextField();
        ppmCmb = new javax.swing.JComboBox();
        rtToleranceLabel = new javax.swing.JLabel();
        rtTolTxt = new javax.swing.JTextField();
        loadButton = new javax.swing.JButton();
        exitButton = new javax.swing.JButton();

        setTitle("New Project");
        setResizable(false);
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        processingPanel.setOpaque(false);
        processingPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        projectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Details"));
        projectPanel.setOpaque(false);

        projectTxt.setEditable(false);
        projectTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        projectTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                projectTxtKeyReleased(evt);
            }
        });

        projectReferenceLabel.setText("Project Reference");

        sampleNameLabel.setText("Sample Name");

        sampleNameTxt.setEditable(false);
        sampleNameTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        sampleNameTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleNameTxtActionPerformed(evt);
            }
        });

        replicateLabel.setText("Replicate");

        replicateNumberTxt.setEditable(false);
        replicateNumberTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        replicateNumberTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                replicateNumberTxtActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout projectPanelLayout = new org.jdesktop.layout.GroupLayout(projectPanel);
        projectPanel.setLayout(projectPanelLayout);
        projectPanelLayout.setHorizontalGroup(
            projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(projectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(projectReferenceLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(sampleNameLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(projectTxt)
                    .add(sampleNameTxt))
                .add(26, 26, 26)
                .add(replicateLabel)
                .add(18, 18, 18)
                .add(replicateNumberTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        projectPanelLayout.setVerticalGroup(
            projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(projectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(projectTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(projectReferenceLabel))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(sampleNameLabel)
                    .add(sampleNameTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(replicateLabel)
                    .add(replicateNumberTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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
                    .add(fileSelectiontPanelLayout.createSequentialGroup()
                        .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(spectrumFilesLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(databaseFileLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(10, 10, 10)
                        .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(txtSpectraFileLocation)
                            .add(fastaTxt)))
                    .add(fileSelectiontPanelLayout.createSequentialGroup()
                        .add(idFilesLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(txtIdFileLocation)))
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
                    .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 787, Short.MAX_VALUE)
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
                .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 218, Short.MAX_VALUE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout processingPanelLayout = new org.jdesktop.layout.GroupLayout(processingPanel);
        processingPanel.setLayout(processingPanelLayout);
        processingPanelLayout.setHorizontalGroup(
            processingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(processingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(processingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(samplePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(fileSelectiontPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(projectPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        processingPanelLayout.setVerticalGroup(
            processingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(processingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(fileSelectiontPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 21, Short.MAX_VALUE)
                .add(projectPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(samplePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tabbedPane.addTab("Processing", processingPanel);

        advancedParamsPanel.setOpaque(false);

        spectrumAnalysisPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Analysis"));
        spectrumAnalysisPanel.setOpaque(false);

        reporterIonMzToleranceLabel.setText("Reporter Ion Selection Tolerance [m/z]");

        ionToleranceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        org.jdesktop.layout.GroupLayout spectrumAnalysisPanelLayout = new org.jdesktop.layout.GroupLayout(spectrumAnalysisPanel);
        spectrumAnalysisPanel.setLayout(spectrumAnalysisPanelLayout);
        spectrumAnalysisPanelLayout.setHorizontalGroup(
            spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(reporterIonMzToleranceLabel)
                .add(18, 18, 18)
                .add(ionToleranceTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 102, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(491, Short.MAX_VALUE))
        );
        spectrumAnalysisPanelLayout.setVerticalGroup(
            spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(reporterIonMzToleranceLabel)
                    .add(ionToleranceTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        quantOptionsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Quantification Options"));
        quantOptionsPanel.setOpaque(false);

        quantPreferencesLabel.setText("Quantification Preferences");

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

        org.jdesktop.layout.GroupLayout quantOptionsPanelLayout = new org.jdesktop.layout.GroupLayout(quantOptionsPanel);
        quantOptionsPanel.setLayout(quantOptionsPanelLayout);
        quantOptionsPanelLayout.setHorizontalGroup(
            quantOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(quantOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(quantPreferencesLabel)
                .add(18, 18, 18)
                .add(quantificationPreferencesTxt)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(editQuantPrefsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        quantOptionsPanelLayout.setVerticalGroup(
            quantOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(quantOptionsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(quantOptionsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(quantPreferencesLabel)
                    .add(quantificationPreferencesTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(editQuantPrefsButton))
                .addContainerGap(13, Short.MAX_VALUE))
        );

        reporterLocationPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Reporter Location"));
        reporterLocationPanel.setOpaque(false);

        reporterLocatinButtonGroup.add(sameSpectra);
        sameSpectra.setSelected(true);
        sameSpectra.setText("Same Spectra");
        sameSpectra.setIconTextGap(10);
        sameSpectra.setOpaque(false);
        sameSpectra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sameSpectraActionPerformed(evt);
            }
        });

        reporterLocatinButtonGroup.add(precursorMatching);
        precursorMatching.setText("Precursor Matching");
        precursorMatching.setIconTextGap(10);
        precursorMatching.setOpaque(false);
        precursorMatching.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorMatchingActionPerformed(evt);
            }
        });

        mzToleranceLabel.setText("m/z tolerance");

        mzTolTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        ppmCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ppm", "m/z" }));

        rtToleranceLabel.setText("RT tolerance (s)");

        rtTolTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        rtTolTxt.setText("10");
        rtTolTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rtTolTxtActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout reporterLocationPanelLayout = new org.jdesktop.layout.GroupLayout(reporterLocationPanel);
        reporterLocationPanel.setLayout(reporterLocationPanelLayout);
        reporterLocationPanelLayout.setHorizontalGroup(
            reporterLocationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(reporterLocationPanelLayout.createSequentialGroup()
                .add(reporterLocationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(reporterLocationPanelLayout.createSequentialGroup()
                        .addContainerGap()
                        .add(reporterLocationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(sameSpectra)
                            .add(precursorMatching)))
                    .add(reporterLocationPanelLayout.createSequentialGroup()
                        .add(119, 119, 119)
                        .add(reporterLocationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(rtToleranceLabel)
                            .add(mzToleranceLabel))
                        .add(14, 14, 14)
                        .add(reporterLocationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(reporterLocationPanelLayout.createSequentialGroup()
                                .add(mzTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 106, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(ppmCmb, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(rtTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 106, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        reporterLocationPanelLayout.setVerticalGroup(
            reporterLocationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(reporterLocationPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(sameSpectra)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(precursorMatching)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(reporterLocationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(mzToleranceLabel)
                    .add(mzTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(ppmCmb, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(reporterLocationPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(rtTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(rtToleranceLabel))
                .addContainerGap(17, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout advancedParamsPanelLayout = new org.jdesktop.layout.GroupLayout(advancedParamsPanel);
        advancedParamsPanel.setLayout(advancedParamsPanelLayout);
        advancedParamsPanelLayout.setHorizontalGroup(
            advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(spectrumAnalysisPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(quantOptionsPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(reporterLocationPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        advancedParamsPanelLayout.setVerticalGroup(
            advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(spectrumAnalysisPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(reporterLocationPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(quantOptionsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(261, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Advanced Settings", advancedParamsPanel);

        loadButton.setText("Load");
        loadButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                loadButtonActionPerformed(evt);
            }
        });

        exitButton.setText("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout backgroundPanelLayout = new org.jdesktop.layout.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(tabbedPane)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(loadButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(exitButton)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(new java.awt.Component[] {exitButton, loadButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(tabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(loadButton)
                    .add(exitButton))
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
            .add(backgroundPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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
     * Set the precursor matching type.
     *
     * @param evt
     */
    private void sameSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sameSpectraActionPerformed
        // enable or disable the precursor matching options
        mzToleranceLabel.setEnabled(precursorMatching.isSelected());
        mzTolTxt.setEnabled(precursorMatching.isSelected());
        ppmCmb.setEnabled(precursorMatching.isSelected());
        rtToleranceLabel.setEnabled(precursorMatching.isSelected());
        rtTolTxt.setEnabled(precursorMatching.isSelected());
    }//GEN-LAST:event_sameSpectraActionPerformed

    /**
     * Set the precursor matching type.
     *
     * @param evt
     */
    private void precursorMatchingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorMatchingActionPerformed
        sameSpectraActionPerformed(null);
    }//GEN-LAST:event_precursorMatchingActionPerformed

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
     * Validate the retention time input.
     *
     * @param evt
     */
    private void rtTolTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rtTolTxtActionPerformed
        // @TODO: validate the input
    }//GEN-LAST:event_rtTolTxtActionPerformed

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

//            checkFastaFile(fastaFile);
//            if (searchParameters == null) {
//                searchParameters = new SearchParameters();
//                searchParameters.setEnzyme(EnzymeFactory.getInstance().getEnzyme("Trypsin"));
//            }
//            searchParameters.setFastaFile(fastaFile);
        }
    }//GEN-LAST:event_addDbButtonActionPerformed

    /**
     * Clear the data and close the dialog.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing
        exitButtonActionPerformed(null);
    }//GEN-LAST:event_formWindowClosing

    /**
     * Validate the sample name.
     *
     * @param evt
     */
    private void sampleNameTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleNameTxtActionPerformed
        // @TODO: validate the input
    }//GEN-LAST:event_sampleNameTxtActionPerformed

    /**
     * Validate the project name.
     *
     * @param evt
     */
    private void projectTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_projectTxtKeyReleased
        // @TODO: validate the input
    }//GEN-LAST:event_projectTxtKeyReleased

    /**
     * Validate the input.
     *
     * @param evt
     */
    private void replicateNumberTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_replicateNumberTxtActionPerformed
        validateInput();
    }//GEN-LAST:event_replicateNumberTxtActionPerformed

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
            saveUserPreferences();
            dispose();
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    /**
     * Clear the data and close the dialog.
     *
     * @param evt
     */
    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
        cancelled = true;
        this.dispose();
    }//GEN-LAST:event_exitButtonActionPerformed

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
        new PreferencesDialog(reporterGui, reporterPreferences, cpsBean.getIdentificationParameters().getSearchParameters());
        quantificationPreferencesTxt.setText("User Settings");
    }//GEN-LAST:event_editQuantPrefsButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addDbButton;
    private javax.swing.JButton addIdFilesButton;
    private javax.swing.JButton addSpectraFilesJButton;
    private javax.swing.JPanel advancedParamsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel databaseFileLabel;
    private javax.swing.JButton editQuantPrefsButton;
    private javax.swing.JButton exitButton;
    private javax.swing.JTextField fastaTxt;
    private javax.swing.JPanel fileSelectiontPanel;
    private javax.swing.JLabel idFilesLabel;
    private javax.swing.JTextField ionToleranceTxt;
    private javax.swing.JButton loadButton;
    private javax.swing.JButton methodSettingsButton;
    private javax.swing.JTextField mzTolTxt;
    private javax.swing.JLabel mzToleranceLabel;
    private javax.swing.JComboBox ppmCmb;
    private javax.swing.JRadioButton precursorMatching;
    private javax.swing.JPanel processingPanel;
    private javax.swing.JPanel projectPanel;
    private javax.swing.JLabel projectReferenceLabel;
    private javax.swing.JTextField projectTxt;
    private javax.swing.JPanel quantOptionsPanel;
    private javax.swing.JLabel quantPreferencesLabel;
    private javax.swing.JTextField quantificationPreferencesTxt;
    private javax.swing.JLabel replicateLabel;
    private javax.swing.JTextField replicateNumberTxt;
    private javax.swing.JLabel reporterIonMzToleranceLabel;
    private javax.swing.ButtonGroup reporterLocatinButtonGroup;
    private javax.swing.JPanel reporterLocationPanel;
    private javax.swing.JComboBox reporterMethodComboBox;
    private javax.swing.JLabel reporterMethodLabel;
    private javax.swing.JTextField rtTolTxt;
    private javax.swing.JLabel rtToleranceLabel;
    private javax.swing.JRadioButton sameSpectra;
    private javax.swing.JScrollPane sampleAssignmentJScrollPane;
    private javax.swing.JTable sampleAssignmentTable;
    private javax.swing.JLabel sampleNameLabel;
    private javax.swing.JTextField sampleNameTxt;
    private javax.swing.JPanel samplePanel;
    private javax.swing.JPanel spectrumAnalysisPanel;
    private javax.swing.JLabel spectrumFilesLabel;
    private javax.swing.JTabbedPane tabbedPane;
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
    private void loadUserPreferences() {
        reporterPreferences = ReporterPreferences.getUserPreferences();
        ionToleranceTxt.setText(reporterPreferences.getReporterIonsMzTolerance() + "");
        if (reporterPreferences.isSameSpectra()) {
            sameSpectra.setSelected(true);
            precursorMatching.setSelected(false);
        } else {
            sameSpectra.setSelected(false);
            precursorMatching.setSelected(true);
            mzTolTxt.setText(reporterPreferences.getPrecursorMzTolerance() + "");
            rtTolTxt.setText(reporterPreferences.getPrecursorRTTolerance() + "");
        }
    }

    /**
     * Sets the new quantification preferences.
     */
    private void saveUserPreferences() {
        reporterPreferences.setReporterIonsMzTolerance(new Double(ionToleranceTxt.getText()));
        if (sameSpectra.isSelected()) {
            reporterPreferences.setSameSpectra(true);
        } else {
            reporterPreferences.setSameSpectra(false);
            reporterPreferences.setPrecursorMzTolerance(new Double(mzTolTxt.getText()));
            reporterPreferences.setPrecursorRTTolerance(new Double(rtTolTxt.getText()));
        }
        ReporterPreferences.saveUserPreferences(reporterPreferences);
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

                    // backwards compatibility fix for gene and go references using only latin names
                    boolean genesRemapped = false;
                    String selectedSpecies = cpsBean.getIdentificationParameters().getGenePreferences().getCurrentSpecies();
                    if (selectedSpecies != null) {

                        HashMap<String, HashMap<String, String>> allSpecies = cpsBean.getIdentificationParameters().getGenePreferences().getAllSpeciesMap();
                        HashMap<String, String> tempSpecies = allSpecies.get(cpsBean.getIdentificationParameters().getGenePreferences().getCurrentSpeciesType());

                        if (!tempSpecies.containsKey(selectedSpecies)) {

                            Iterator<String> iterator = tempSpecies.keySet().iterator();
                            boolean keyFound = false;

                            while (iterator.hasNext() && !keyFound) {
                                String tempSpeciesKey = iterator.next();
                                if (tempSpeciesKey.contains(selectedSpecies)) {
                                    cpsBean.getIdentificationParameters().getGenePreferences().setCurrentSpecies(tempSpeciesKey);
                                    keyFound = true;
                                } else if (selectedSpecies.contains(tempSpeciesKey)) { // strange backwards compatibility fix, should not be needed
                                    cpsBean.getIdentificationParameters().getGenePreferences().setCurrentSpecies(tempSpeciesKey);
                                    keyFound = true;
                                }
                            }

                            if (keyFound) {
                                loadGeneMappings(); // have to re-load the gene mappings now that we have the correct species name
                                genesRemapped = true;
                            }
                        }
                    }

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

                projectTxt.setText(getExperiment().getReference());
                sampleNameTxt.setText(getSample().getReference());
                replicateNumberTxt.setText(getReplicateNumber() + "");
                txtIdFileLocation.setText(cpsBean.getCpsFile().getName());

                cache = new ObjectsCache();
                cache.setAutomatedMemoryManagement(true);

                //@TODO: load quantification parameters if existing
                SearchParameters searchParameters = getSearchParameters();
                // try to detect the method used
                for (String ptmName : searchParameters.getModificationProfile().getAllModifications()) {
                    if (ptmName.contains("4plex")) {
                        selectedMethod = getMethod("iTRAQ 4Plex (Default)");
                        break;
                    } else if (ptmName.contains("8plex")) {
                        selectedMethod = getMethod("iTRAQ 8Plex (Default)");
                        break;
                    } else if (ptmName.contains("duplex")) {
                        selectedMethod = getMethod("TMT2 (Default)");
                        break;
                    } else if (ptmName.contains("tmt") && ptmName.contains("6")) {
                        if (searchParameters.getIonSearched1() == PeptideFragmentIon.Y_ION
                                || searchParameters.getIonSearched2() == PeptideFragmentIon.Y_ION) {
                            selectedMethod = getMethod("TMT6 HCD (Default)");
                        } else {
                            selectedMethod = getMethod("TMT6 ETD (Default)");
                        }
                        if (reporterPreferences.getReporterIonsMzTolerance() > 0.0016) {
                            reporterPreferences.setReporterIonsMzTolerance(0.0016);
                        }
                        break;
                    } else if (ptmName.contains("tmt") && ptmName.contains("10")) {
                        selectedMethod = getMethod("TMT10 (Default)");
                        if (reporterPreferences.getReporterIonsMzTolerance() > 0.0016) {
                            reporterPreferences.setReporterIonsMzTolerance(0.0016);
                        }
                        break;
                    } else if (ptmName.contains("itraq")) {
                        selectedMethod = getMethod("iTRAQ 4Plex (Default)");
                        break;
                    }
                }

                // no method detected, default to iTRAQ 4 plex
                if (selectedMethod == null) {
                    reporterMethodComboBox.setSelectedItem(methodsFactory.getMethodsNames()[0]);
                }

                reagents = selectedMethod.getReagentsSortedByMass();

                mzTolTxt.setText(searchParameters.getPrecursorAccuracy() + "");
                if (searchParameters.isPrecursorAccuracyTypePpm()) {
                    ppmCmb.setSelectedIndex(0);
                } else {
                    ppmCmb.setSelectedIndex(1);
                }

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

        // check the ion torerance
        double ionTolerance;
        try {
            ionTolerance = new Double(ionToleranceTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please input a number for the ion tolerance.", "Ion Tolerance Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (ionTolerance <= 0) {
            JOptionPane.showMessageDialog(this, "Please input a positive number for the ion tolerance.", "Ion Tolerance Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        for (String reagent1 : selectedMethod.getReagentNames()) {
            for (String reagent2 : selectedMethod.getReagentNames()) {
                if (!reagent1.equals(reagent2) && Math.abs(selectedMethod.getReagent(reagent1).getReporterIon().getTheoreticMass() - selectedMethod.getReagent(reagent2).getReporterIon().getTheoreticMass()) <= ionTolerance) {
                    JOptionPane.showMessageDialog(this, "The ion tolerance does not allow distinction of " + reagent1 + " and  " + reagent2 + ".", "Ion Tolerance Error", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            }
        }

        if (precursorMatching.isSelected()) {
            try {
                new Double(mzTolTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please input a number for precursor m/z tolerance.", "Matching Tolerance Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            try {
                new Double(rtTolTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Please input a number for precursor RT tolerance.", "RT Tolerance Error", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        try {
            new Integer(replicateNumberTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Please input a number for replicate number.", "Replicate Number Error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
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
     * Returns the reporter preferences.
     *
     * @return the reporter preferences
     */
    public ReporterPreferences getReporterPreferences() {
        return reporterPreferences;
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
