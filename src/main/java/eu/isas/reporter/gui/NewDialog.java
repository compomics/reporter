package eu.isas.reporter.gui;

import com.compomics.util.Util;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.SequenceFactory;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.Quantification.QuantificationMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.SampleSelection;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PeptideShakerSettings;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import eu.isas.reporter.utils.Properties;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
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
    private ReporterMethod selectedMethod;
    /**
     * The experiment conducted.
     */
    private MsExperiment experiment;
    /**
     * The sample analyzed.
     */
    private Sample sample;
    /**
     * The replicate number.
     */
    private int replicateNumber;
    /**
     * The currently loaded PeptideShaker file.
     */
    private File currentPSFile;
    /**
     * The mgf files loaded.
     */
    private ArrayList<File> mgfFiles = new ArrayList<File>();
    /**
     * Reporter will take care of the calculation.
     */
    private Reporter reporter;
    /**
     * The quantification preferences.
     */
    private QuantificationPreferences quantificationPreferences;
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
    private HashMap<Integer, String> sampleNames = new HashMap<Integer, String>();
    /**
     * The cache to use for identification and quantification objects.
     */
    private ObjectsCache cache;
    /**
     * The waiting dialog.
     */
    private WaitingDialog waitingDialog;

    /**
     * Constructor.
     *
     * @param reporterGui the reporter class
     * @param reporter
     */
    public NewDialog(ReporterGUI reporterGui, Reporter reporter) {
        super(reporterGui, true);

        this.reporterGui = reporterGui;
        this.reporter = reporter;

        importMethods();

        initComponents();

        // make sure that the scroll panes are see-through
        sampleAssignmentJScrollPane.getViewport().setOpaque(false);
        reporterIonsConfigJScrollPane.getViewport().setOpaque(false);
        isotopeCorrectionJScrollPane.getViewport().setOpaque(false);

        // set the table properties
        setTableProperties();

        loadPreferences();

        sameSpectra.setSelected(true);

        if (selectedMethod == null) {
            comboMethod2.setSelectedItem(methodsFactory.getMethodsNames()[0]);
            comboMethod1.setSelectedItem(methodsFactory.getMethodsNames()[0]);
        }

        // centrally align the comboboxes
        comboMethod1.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        comboMethod2.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        // disables the user to drag column headers to reorder columns
        sampleAssignmentTable.getTableHeader().setReorderingAllowed(false);
        reporterIonConfigurationTable.getTableHeader().setReorderingAllowed(false);
        isotopeCorrectionTable.getTableHeader().setReorderingAllowed(false);

        comboMethod2.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectedMethod = getMethod((String) comboMethod2.getSelectedItem());
                refresh();
            }
        });
        comboMethod1.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                selectedMethod = getMethod((String) comboMethod1.getSelectedItem());
                refresh();
            }
        });

        txtConfigurationFileLocation.setText(reporterGui.getReporter().getJarFilePath() + File.separator + METHODS_FILE);

        isotopeCorrectionTable.getColumnModel().getColumn(0).setMaxWidth(50);
        pack();
        setLocationRelativeTo(reporterGui);
        setVisible(true);
    }

    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {
//        sampleAssignmentTable.getColumn("Ref").setCellRenderer(new TrueFalseIconRenderer(
//                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
//                null,
//                "Reference", null));
//
//        sampleAssignmentTable.getColumn("Ref").setMaxWidth(40);
//        sampleAssignmentTable.getColumn("Ref").setMinWidth(40);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        backgroundPanel = new javax.swing.JPanel();
        tabbedPane = new javax.swing.JTabbedPane();
        processingPanel = new javax.swing.JPanel();
        projectPanel = new javax.swing.JPanel();
        experimentTxt = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        sampleNameTxt = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        replicateNumberTxt = new javax.swing.JTextField();
        fileSelectiontPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        txtSpectraFileLocation = new javax.swing.JTextField();
        jLabel3 = new javax.swing.JLabel();
        txtIdFileLocation = new javax.swing.JTextField();
        addIdFilesButton = new javax.swing.JButton();
        addSpectraFilesJButton = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        fastaTxt = new javax.swing.JTextField();
        addDbButton = new javax.swing.JButton();
        samplePanel = new javax.swing.JPanel();
        sampleAssignmentJScrollPane = new javax.swing.JScrollPane();
        sampleAssignmentTable = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        comboMethod1 = new javax.swing.JComboBox();
        configPanel = new javax.swing.JPanel();
        reporterIonsConfigPanel = new javax.swing.JPanel();
        reporterIonsConfigJScrollPane = new javax.swing.JScrollPane();
        reporterIonConfigurationTable = new javax.swing.JTable();
        isotopeCorrectionPanel = new javax.swing.JPanel();
        isotopeCorrectionJScrollPane = new javax.swing.JScrollPane();
        isotopeCorrectionTable = new javax.swing.JTable();
        configFilePanel = new javax.swing.JPanel();
        txtConfigurationFileLocation = new javax.swing.JTextField();
        browseConfigButton = new javax.swing.JButton();
        saveConfigButton = new javax.swing.JButton();
        saveAsConfigButton = new javax.swing.JButton();
        methodPanel = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        comboMethod2 = new javax.swing.JComboBox();
        advancedParamsPanel = new javax.swing.JPanel();
        spectrumAnalysisPanel = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        ionToleranceTxt = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        sameSpectra = new javax.swing.JRadioButton();
        precursorMatching = new javax.swing.JRadioButton();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        rtTolTxt = new javax.swing.JTextField();
        mzTolTxt = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        ppmCmb = new javax.swing.JComboBox();
        jPanel1 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        editPreferencesButton = new javax.swing.JButton();
        quantificationPreferencesTxt = new javax.swing.JTextField();
        startButton = new javax.swing.JButton();
        exitJButton = new javax.swing.JButton();

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );
        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 100, Short.MAX_VALUE)
        );

        setTitle("New Project");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        processingPanel.setOpaque(false);
        processingPanel.setPreferredSize(new java.awt.Dimension(800, 600));

        projectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project Details"));
        projectPanel.setOpaque(false);

        experimentTxt.setEditable(false);
        experimentTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        experimentTxt.setEnabled(false);
        experimentTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                experimentTxtKeyReleased(evt);
            }
        });

        jLabel1.setText("Project Reference");

        jLabel24.setText("Sample Name");

        sampleNameTxt.setEditable(false);
        sampleNameTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        sampleNameTxt.setEnabled(false);
        sampleNameTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleNameTxtActionPerformed(evt);
            }
        });

        jLabel25.setText("Replicate");

        replicateNumberTxt.setEditable(false);
        replicateNumberTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        replicateNumberTxt.setEnabled(false);

        org.jdesktop.layout.GroupLayout projectPanelLayout = new org.jdesktop.layout.GroupLayout(projectPanel);
        projectPanel.setLayout(projectPanelLayout);
        projectPanelLayout.setHorizontalGroup(
            projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(projectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel24, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(20, 20, 20)
                .add(projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sampleNameTxt)
                    .add(experimentTxt))
                .add(26, 26, 26)
                .add(jLabel25)
                .add(18, 18, 18)
                .add(replicateNumberTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        projectPanelLayout.setVerticalGroup(
            projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(projectPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(experimentTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(projectPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel24)
                    .add(sampleNameTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel25)
                    .add(replicateNumberTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        fileSelectiontPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Files Selection"));
        fileSelectiontPanel.setOpaque(false);

        jLabel2.setText("Spectrum File(s)");

        txtSpectraFileLocation.setEditable(false);
        txtSpectraFileLocation.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtSpectraFileLocation.setEnabled(false);

        jLabel3.setText("Identification File");

        txtIdFileLocation.setEditable(false);
        txtIdFileLocation.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtIdFileLocation.setText("Please import a PeptideShaker project");

        addIdFilesButton.setText("Browse");
        addIdFilesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addIdFilesButtonActionPerformed(evt);
            }
        });

        addSpectraFilesJButton.setText("Add");
        addSpectraFilesJButton.setEnabled(false);
        addSpectraFilesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSpectraFilesJButtonActionPerformed(evt);
            }
        });

        jLabel10.setText("Database File");

        fastaTxt.setEditable(false);
        fastaTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fastaTxt.setEnabled(false);

        addDbButton.setText("Browse");
        addDbButton.setEnabled(false);

        org.jdesktop.layout.GroupLayout fileSelectiontPanelLayout = new org.jdesktop.layout.GroupLayout(fileSelectiontPanel);
        fileSelectiontPanel.setLayout(fileSelectiontPanelLayout);
        fileSelectiontPanelLayout.setHorizontalGroup(
            fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(fileSelectiontPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(fileSelectiontPanelLayout.createSequentialGroup()
                        .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(18, 18, 18)
                        .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(txtSpectraFileLocation)
                            .add(fastaTxt)))
                    .add(fileSelectiontPanelLayout.createSequentialGroup()
                        .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
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
                    .add(jLabel3)
                    .add(txtIdFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addIdFilesButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(txtSpectraFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addSpectraFilesJButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(fastaTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addDbButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        samplePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sample Assignment"));
        samplePanel.setOpaque(false);

        sampleAssignmentTable.setModel(new AssignementTableModel());
        sampleAssignmentTable.setOpaque(false);
        sampleAssignmentJScrollPane.setViewportView(sampleAssignmentTable);

        jLabel5.setText("Method Selected");

        comboMethod1.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNames()));
        comboMethod1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                comboMethod1ActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout samplePanelLayout = new org.jdesktop.layout.GroupLayout(samplePanel);
        samplePanel.setLayout(samplePanelLayout);
        samplePanelLayout.setHorizontalGroup(
            samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(samplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 778, Short.MAX_VALUE)
                    .add(samplePanelLayout.createSequentialGroup()
                        .add(jLabel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(comboMethod1, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addContainerGap())
        );
        samplePanelLayout.setVerticalGroup(
            samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(samplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(comboMethod1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 211, Short.MAX_VALUE)
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
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 9, Short.MAX_VALUE)
                .add(projectPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(samplePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        tabbedPane.addTab("Processing", processingPanel);

        configPanel.setOpaque(false);

        reporterIonsConfigPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Reporter Ions Configuration"));
        reporterIonsConfigPanel.setOpaque(false);

        reporterIonConfigurationTable.setModel(new IonTableModel());
        reporterIonConfigurationTable.setName(""); // NOI18N
        reporterIonConfigurationTable.setOpaque(false);
        reporterIonsConfigJScrollPane.setViewportView(reporterIonConfigurationTable);

        org.jdesktop.layout.GroupLayout reporterIonsConfigPanelLayout = new org.jdesktop.layout.GroupLayout(reporterIonsConfigPanel);
        reporterIonsConfigPanel.setLayout(reporterIonsConfigPanelLayout);
        reporterIonsConfigPanelLayout.setHorizontalGroup(
            reporterIonsConfigPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(reporterIonsConfigPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(reporterIonsConfigJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 778, Short.MAX_VALUE)
                .addContainerGap())
        );
        reporterIonsConfigPanelLayout.setVerticalGroup(
            reporterIonsConfigPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(reporterIonsConfigPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(reporterIonsConfigJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                .addContainerGap())
        );

        isotopeCorrectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Isotope Correction"));
        isotopeCorrectionPanel.setOpaque(false);

        isotopeCorrectionTable.setModel(new CorrectionTableModel());
        isotopeCorrectionTable.setOpaque(false);
        isotopeCorrectionJScrollPane.setViewportView(isotopeCorrectionTable);

        org.jdesktop.layout.GroupLayout isotopeCorrectionPanelLayout = new org.jdesktop.layout.GroupLayout(isotopeCorrectionPanel);
        isotopeCorrectionPanel.setLayout(isotopeCorrectionPanelLayout);
        isotopeCorrectionPanelLayout.setHorizontalGroup(
            isotopeCorrectionPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(isotopeCorrectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(isotopeCorrectionJScrollPane)
                .addContainerGap())
        );
        isotopeCorrectionPanelLayout.setVerticalGroup(
            isotopeCorrectionPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(isotopeCorrectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(isotopeCorrectionJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                .addContainerGap())
        );

        configFilePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Configuration File"));
        configFilePanel.setOpaque(false);

        txtConfigurationFileLocation.setEditable(false);

        browseConfigButton.setText("Browse");
        browseConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseConfigButtonActionPerformed(evt);
            }
        });

        saveConfigButton.setText("Save");

        saveAsConfigButton.setText("Save As");

        org.jdesktop.layout.GroupLayout configFilePanelLayout = new org.jdesktop.layout.GroupLayout(configFilePanel);
        configFilePanel.setLayout(configFilePanelLayout);
        configFilePanelLayout.setHorizontalGroup(
            configFilePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(configFilePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(txtConfigurationFileLocation)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(browseConfigButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(saveConfigButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(saveAsConfigButton)
                .addContainerGap())
        );

        configFilePanelLayout.linkSize(new java.awt.Component[] {browseConfigButton, saveAsConfigButton, saveConfigButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        configFilePanelLayout.setVerticalGroup(
            configFilePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(configFilePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(configFilePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(txtConfigurationFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(saveConfigButton)
                    .add(browseConfigButton)
                    .add(saveAsConfigButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        methodPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Method"));
        methodPanel.setOpaque(false);

        jLabel4.setText("Method Selected");

        comboMethod2.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNames()));

        org.jdesktop.layout.GroupLayout methodPanelLayout = new org.jdesktop.layout.GroupLayout(methodPanel);
        methodPanel.setLayout(methodPanelLayout);
        methodPanelLayout.setHorizontalGroup(
            methodPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(methodPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(18, 18, 18)
                .add(comboMethod2, 0, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );
        methodPanelLayout.setVerticalGroup(
            methodPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(methodPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(methodPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(comboMethod2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout configPanelLayout = new org.jdesktop.layout.GroupLayout(configPanel);
        configPanel.setLayout(configPanelLayout);
        configPanelLayout.setHorizontalGroup(
            configPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(configPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(configPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(configFilePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(methodPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(reporterIonsConfigPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(isotopeCorrectionPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        configPanelLayout.setVerticalGroup(
            configPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(configPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(methodPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(reporterIonsConfigPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(isotopeCorrectionPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(configFilePanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(12, 12, 12))
        );

        tabbedPane.addTab("Configuration", configPanel);

        advancedParamsPanel.setOpaque(false);

        spectrumAnalysisPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Analysis"));
        spectrumAnalysisPanel.setOpaque(false);

        jLabel6.setText("Reporter Ion Selection Tolerance [m/z]");

        ionToleranceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel11.setText("Link to Quantification");

        sameSpectra.setText("Same Spectra");
        sameSpectra.setIconTextGap(10);
        sameSpectra.setOpaque(false);
        sameSpectra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sameSpectraActionPerformed(evt);
            }
        });

        precursorMatching.setText("Precursor Matching:");
        precursorMatching.setIconTextGap(10);
        precursorMatching.setOpaque(false);
        precursorMatching.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorMatchingActionPerformed(evt);
            }
        });

        jLabel19.setText("m/z tolerance");

        jLabel20.setText("RT tolerance");

        rtTolTxt.setText("10");
        rtTolTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rtTolTxtActionPerformed(evt);
            }
        });

        jLabel22.setText("s");

        ppmCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "ppm", "m/z" }));

        org.jdesktop.layout.GroupLayout spectrumAnalysisPanelLayout = new org.jdesktop.layout.GroupLayout(spectrumAnalysisPanel);
        spectrumAnalysisPanel.setLayout(spectrumAnalysisPanelLayout);
        spectrumAnalysisPanelLayout.setHorizontalGroup(
            spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel6)
                .add(18, 18, 18)
                .add(ionToleranceTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 102, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 211, Short.MAX_VALUE)
                .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel11)
                    .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                        .add(10, 10, 10)
                        .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(precursorMatching)
                            .add(sameSpectra)
                            .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                                .add(19, 19, 19)
                                .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel19)
                                    .add(jLabel20))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                    .add(rtTolTxt)
                                    .add(mzTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 106, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel22)
                            .add(ppmCmb, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap())
        );
        spectrumAnalysisPanelLayout.setVerticalGroup(
            spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                        .add(jLabel11)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(sameSpectra)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(precursorMatching)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel19)
                            .add(mzTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(ppmCmb, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(rtTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel20)
                            .add(jLabel22)))
                    .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                        .add(jLabel6)
                        .add(ionToleranceTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel1.setBorder(javax.swing.BorderFactory.createTitledBorder("Quantification Options"));
        jPanel1.setOpaque(false);

        jLabel7.setText("Quantification Preferences");

        editPreferencesButton.setText("Edit");
        editPreferencesButton.setPreferredSize(new java.awt.Dimension(57, 23));
        editPreferencesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editPreferencesButtonActionPerformed(evt);
            }
        });

        quantificationPreferencesTxt.setEditable(false);
        quantificationPreferencesTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        quantificationPreferencesTxt.setText("Default Settings");

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel7)
                .add(18, 18, 18)
                .add(quantificationPreferencesTxt)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(editPreferencesButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(editPreferencesButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(quantificationPreferencesTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout advancedParamsPanelLayout = new org.jdesktop.layout.GroupLayout(advancedParamsPanel);
        advancedParamsPanel.setLayout(advancedParamsPanelLayout);
        advancedParamsPanelLayout.setHorizontalGroup(
            advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(spectrumAnalysisPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        advancedParamsPanelLayout.setVerticalGroup(
            advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(spectrumAnalysisPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(307, Short.MAX_VALUE))
        );

        tabbedPane.addTab("Advanced Parameters", advancedParamsPanel);

        startButton.setText("Load");
        startButton.setPreferredSize(new java.awt.Dimension(57, 23));
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        exitJButton.setText("Exit");
        exitJButton.setPreferredSize(new java.awt.Dimension(57, 23));
        exitJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitJButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout backgroundPanelLayout = new org.jdesktop.layout.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(backgroundPanelLayout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(startButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(exitJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(tabbedPane))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(new java.awt.Component[] {exitJButton, startButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(tabbedPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 588, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(backgroundPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(startButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(exitJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
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

    private void browseConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseConfigButtonActionPerformed

        if (txtConfigurationFileLocation.getText().length() > 0) {
            reporterGui.setLastSelectedFolder(txtConfigurationFileLocation.getText());
        }

        JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Configuration file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showDialog(this.getParent(), "Add");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File newFile = fileChooser.getSelectedFile();
            try {
                methodsFactory.importMethods(newFile);
                comboMethod2.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNames()));
                comboMethod1.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNames()));
                selectedMethod = getMethod((String) comboMethod2.getSelectedItem());
                refresh();
                txtConfigurationFileLocation.setText(newFile.getAbsolutePath());
                reporterGui.setLastSelectedFolder(newFile.getPath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "File " + METHODS_FILE + " not found in conf folder.",
                        "Methods file not found", JOptionPane.WARNING_MESSAGE);
            } catch (XmlPullParserException e) {
                JOptionPane.showMessageDialog(null,
                        "An error occured while parsing " + METHODS_FILE + " at line " + e.getLineNumber() + ".",
                        "Parsing error", JOptionPane.WARNING_MESSAGE);
            }
        }
}//GEN-LAST:event_browseConfigButtonActionPerformed

    private void addIdFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIdFilesButtonActionPerformed

        JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Identification File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        FileFilter filter = new FileFilter() {

            public boolean accept(File myFile) {
                return myFile.getName().endsWith("cps")
                        || myFile.isDirectory();
            }

            public String getDescription() {
                return "Supported formats: PeptideShaker (.cps)";
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
                reporterGui.setLastSelectedFolder(newFile.getPath());
                importPeptideShakerFile(newFile);
            }
        }
    }//GEN-LAST:event_addIdFilesButtonActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (validateInput()) {

            dispose();
            waitingDialog = new WaitingDialog(reporterGui,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                    false, null, "Quantifying", "Reporter", new Properties().getVersion(), true); //@TODO: put and tips
            waitingDialog.setLocationRelativeTo(this);
            waitingDialog.setMaxProgressValue(5);

            final ReporterGUI finalRef = reporterGui;


            new Thread(new Runnable() {

                public void run() {
                    try {
                        waitingDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("DisplayThread") {

                @Override
                public void run() {

                    try {
                        waitingDialog.appendReport("Preparing for the quantification.", true, true);
                        waitingDialog.increaseProgressValue();
                        savePreferences();
                        ReporterIonQuantification reporterIonQuantification = getReporterIonQuantification();
                        experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).addQuantificationResults(reporterIonQuantification.getMethodUsed(), reporterIonQuantification);
                        reporter.setExperiment(experiment);
                        reporter.setSample(sample);
                        reporter.setReplicateNumber(replicateNumber);
                        waitingDialog.increaseProgressValue();
                        reporter.loadFiles(mgfFiles, waitingDialog);
                    } catch (Exception e) {
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(finalRef,
                                "An error occured while creating the project:\n"
                                + e.getLocalizedMessage(),
                                "Error",
                                JOptionPane.ERROR_MESSAGE);
                    }
                }
            }.start();
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void sameSpectraActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sameSpectraActionPerformed
        precursorMatching.setSelected(!sameSpectra.isSelected());
    }//GEN-LAST:event_sameSpectraActionPerformed

    private void precursorMatchingActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_precursorMatchingActionPerformed
        sameSpectra.setSelected(!precursorMatching.isSelected());
    }//GEN-LAST:event_precursorMatchingActionPerformed

    private void addSpectraFilesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSpectraFilesJButtonActionPerformed

        JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Spectra File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {

            public boolean accept(File myFile) {
                return myFile.getName().endsWith("mgf")
                        || myFile.isDirectory();
            }

            public String getDescription() {
                return "Supported formats: .mgf";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showDialog(this.getParent(), "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            for (File newFile : fileChooser.getSelectedFiles()) {
                if (newFile.isDirectory()) {
                    File[] tempFiles = newFile.listFiles();
                    for (File file : tempFiles) {
                        if (file.getName().endsWith("mgf")) {
                            if (!mgfFiles.contains(file)) {
                                mgfFiles.add(file);
                            }
                        }
                    }
                } else {
                    if (newFile.getName().endsWith("mgf")) {
                        if (!mgfFiles.contains(newFile)) {
                            mgfFiles.add(newFile);
                        }
                    }
                }

                reporterGui.setLastSelectedFolder(newFile.getPath());
            }

            txtSpectraFileLocation.setText(mgfFiles.size() + " file(s) selected.");
        }
    }//GEN-LAST:event_addSpectraFilesJButtonActionPerformed

    private void experimentTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_experimentTxtKeyReleased
        experiment.setReference(experimentTxt.getText().trim());
    }//GEN-LAST:event_experimentTxtKeyReleased

    private void sampleNameTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleNameTxtActionPerformed
    }//GEN-LAST:event_sampleNameTxtActionPerformed

    private void rtTolTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rtTolTxtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rtTolTxtActionPerformed

    private void exitJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitJButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_exitJButtonActionPerformed

    private void editPreferencesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editPreferencesButtonActionPerformed
        new PreferencesDialog(reporterGui, quantificationPreferences);
        quantificationPreferencesTxt.setText("User Settings");
    }//GEN-LAST:event_editPreferencesButtonActionPerformed

    private void comboMethod1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_comboMethod1ActionPerformed
        sampleNames = new HashMap<Integer, String>();
    }//GEN-LAST:event_comboMethod1ActionPerformed
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addDbButton;
    private javax.swing.JButton addIdFilesButton;
    private javax.swing.JButton addSpectraFilesJButton;
    private javax.swing.JPanel advancedParamsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton browseConfigButton;
    private javax.swing.JComboBox comboMethod1;
    private javax.swing.JComboBox comboMethod2;
    private javax.swing.JPanel configFilePanel;
    private javax.swing.JPanel configPanel;
    private javax.swing.JButton editPreferencesButton;
    private javax.swing.JButton exitJButton;
    private javax.swing.JTextField experimentTxt;
    private javax.swing.JTextField fastaTxt;
    private javax.swing.JPanel fileSelectiontPanel;
    private javax.swing.JTextField ionToleranceTxt;
    private javax.swing.JScrollPane isotopeCorrectionJScrollPane;
    private javax.swing.JPanel isotopeCorrectionPanel;
    private javax.swing.JTable isotopeCorrectionTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel methodPanel;
    private javax.swing.JTextField mzTolTxt;
    private javax.swing.JComboBox ppmCmb;
    private javax.swing.JRadioButton precursorMatching;
    private javax.swing.JPanel processingPanel;
    private javax.swing.JPanel projectPanel;
    private javax.swing.JTextField quantificationPreferencesTxt;
    private javax.swing.JTextField replicateNumberTxt;
    private javax.swing.JTable reporterIonConfigurationTable;
    private javax.swing.JScrollPane reporterIonsConfigJScrollPane;
    private javax.swing.JPanel reporterIonsConfigPanel;
    private javax.swing.JTextField rtTolTxt;
    private javax.swing.JRadioButton sameSpectra;
    private javax.swing.JScrollPane sampleAssignmentJScrollPane;
    private javax.swing.JTable sampleAssignmentTable;
    private javax.swing.JTextField sampleNameTxt;
    private javax.swing.JPanel samplePanel;
    private javax.swing.JButton saveAsConfigButton;
    private javax.swing.JButton saveConfigButton;
    private javax.swing.JPanel spectrumAnalysisPanel;
    private javax.swing.JButton startButton;
    private javax.swing.JTabbedPane tabbedPane;
    private javax.swing.JTextField txtConfigurationFileLocation;
    private javax.swing.JTextField txtIdFileLocation;
    private javax.swing.JTextField txtSpectraFileLocation;
    // End of variables declaration//GEN-END:variables

    /**
     * Returns the quantification method selected.
     *
     * @return the quantification method selected
     */
    private ReporterIonQuantification getReporterIonQuantification() throws SQLException {
        ReporterIonQuantification quantification = new ReporterIonQuantification(QuantificationMethod.REPORTER_IONS);
        quantification.establishConnection(Reporter.SERIALIZATION_DIRECTORY, ReporterIonQuantification.getDefaultReference(experiment.getReference(), sample.getReference(), replicateNumber), true, cache);
        for (int row = 0; row < sampleAssignmentTable.getRowCount(); row++) {
            quantification.assignSample(selectedMethod.getReporterIons().get(row).getIndex(), new Sample((String) sampleAssignmentTable.getValueAt(row, 1)));
        }
        quantification.setMethod(selectedMethod);
        return quantification;
    }

    /**
     * Returns the reporter method corresponding to the given name.
     *
     * @param methodName the given name
     * @return the corresponding reporter method
     */
    private ReporterMethod getMethod(String methodName) {
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
            comboMethod1.setSelectedItem(selectedMethod.getName());
            comboMethod2.setSelectedItem(selectedMethod.getName());
        }

        sampleAssignmentTable.setModel(new AssignementTableModel());

        setTableProperties();

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                sampleAssignmentTable.revalidate();
                sampleAssignmentTable.repaint();

                reporterIonConfigurationTable.revalidate();
                reporterIonConfigurationTable.repaint();

                isotopeCorrectionTable.revalidate();
                isotopeCorrectionTable.repaint();
            }
        });
    }

    /**
     * Imports the methods from the methods file.
     */
    private void importMethods() {
        try {
            methodsFactory.importMethods(new File(reporterGui.getReporter().getJarFilePath() + File.separator + METHODS_FILE));
        } catch (Exception e) {
            e.printStackTrace();
            importMethodsError();
        }
    }

    /**
     * Loads the quantification preferences in the GUI.
     */
    private void loadPreferences() {
        quantificationPreferences = reporter.getQuantificationPreferences();
        ionToleranceTxt.setText(quantificationPreferences.getReporterIonsMzTolerance() + "");
        if (quantificationPreferences.isSameSpectra()) {
            sameSpectra.setSelected(true);
            precursorMatching.setSelected(false);
        } else {
            sameSpectra.setSelected(false);
            precursorMatching.setSelected(true);
            mzTolTxt.setText(quantificationPreferences.getPrecursorMzTolerance() + "");
            rtTolTxt.setText(quantificationPreferences.getPrecursorRTTolerance() + "");
        }
    }

    /**
     * Sets the new quantification preferences.
     */
    private void savePreferences() {
        quantificationPreferences.setReporterIonsMzTolerance(new Double(ionToleranceTxt.getText()));
        if (sameSpectra.isSelected()) {
            quantificationPreferences.setSameSpectra(true);
        } else {
            quantificationPreferences.setSameSpectra(false);
            quantificationPreferences.setPrecursorMzTolerance(new Double(mzTolTxt.getText()));
            quantificationPreferences.setPrecursorRTTolerance(new Double(rtTolTxt.getText()));
        }
    }

    /**
     * Method used to import a PeptideShaker file.
     *
     * @param psFile a PeptideShaker file
     */
    private void importPeptideShakerFile(File psFile) {
        currentPSFile = psFile;

        progressDialog = new ProgressDialogX(this, reporterGui,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        progressDialog.setIndeterminate(true);
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
                    // reset enzymes, ptms and preferences. Close any open connection to an identification database
                    if (reporter.getIdentification() != null) {
                        reporter.getIdentification().close();
                    }

                    File experimentFile = new File(Reporter.SERIALIZATION_DIRECTORY, Reporter.experimentObjectName);
                    File matchFolder = new File(Reporter.SERIALIZATION_DIRECTORY);

                    for (File file : matchFolder.listFiles()) {
                        if (file.isDirectory()) {
                            Util.deleteDir(file);
                        } else {
                            file.delete();
                        }
                    }

                    final int BUFFER = 2048;
                    byte data[] = new byte[BUFFER];
                    FileInputStream fi = new FileInputStream(currentPSFile);
                    BufferedInputStream bis = new BufferedInputStream(fi, BUFFER);
                    ArchiveInputStream tarInput = new ArchiveStreamFactory().createArchiveInputStream(bis);
                    ArchiveEntry archiveEntry;
                    progressDialog.setMaxProgressValue(100);
                    progressDialog.setValue(0);
                    progressDialog.setIndeterminate(false);
                    long fileLength = currentPSFile.length();

                    while ((archiveEntry = tarInput.getNextEntry()) != null) {
                        File destinationFile = new File(archiveEntry.getName());
                        File destinationFolder = destinationFile.getParentFile();
                        if (!destinationFolder.exists()) {
                            destinationFolder.mkdirs();
                        }
                        FileOutputStream fos = new FileOutputStream(destinationFile);
                        BufferedOutputStream bos = new BufferedOutputStream(fos);
                        int count;
                        while ((count = tarInput.read(data, 0, BUFFER)) != -1 && !progressDialog.isRunCanceled()) {
                            bos.write(data, 0, count);
                        }
                        bos.close();
                        fos.close();
                        int progress = (int) (100 * tarInput.getBytesRead() / fileLength);
                        progressDialog.setValue(progress);
                        if (progressDialog.isRunCanceled()) {
                            progressDialog.dispose();
                            return;
                        }
                    }
                    progressDialog.setIndeterminate(true);
                    fi.close();
                    bis.close();
                    fi.close();

                    experiment = ExperimentIO.loadExperiment(experimentFile);

                    Sample tempSample = null;

                    PeptideShakerSettings psSettings = new PeptideShakerSettings();
                    psSettings = (PeptideShakerSettings) experiment.getUrParam(psSettings);
                    reporter.setPSSettings(psSettings);

                    if (progressDialog.isRunCanceled()) {
                        progressDialog.dispose();
                        return;
                    }
                    
                    
                    
                    progressDialog.setIndeterminate(true);
                    progressDialog.setTitle("Importing Experiment Details. Please Wait...");

                    if (progressDialog.isRunCanceled()) {
                        progressDialog.dispose();
                        return;
                    }

                    ArrayList<Sample> samples = new ArrayList(experiment.getSamples().values());

                    if (samples.size() == 1) {
                        tempSample = samples.get(0);
                    } else {
                        String[] sampleNames = new String[samples.size()];
                        for (int i = 0; i < sampleNames.length; i++) {
                            sampleNames[i] = samples.get(i).getReference();
                        }
                        SampleSelection sampleSelection = new SampleSelection(null, true, sampleNames, "sample");
                        sampleSelection.setVisible(true);
                        String choice = sampleSelection.getChoice();
                        for (Sample sampleTemp : samples) {
                            if (sampleTemp.getReference().equals(choice)) {
                                tempSample = sampleTemp;
                                break;
                            }
                        }
                    }

                    sample = tempSample;

                    if (progressDialog.isRunCanceled()) {
                        progressDialog.dispose();
                        return;
                    }

                    ArrayList<Integer> replicates = new ArrayList(experiment.getAnalysisSet(tempSample).getReplicateNumberList());

                    int tempReplicate;

                    if (replicates.size() == 1) {
                        tempReplicate = replicates.get(0);
                    } else {
                        String[] replicateNames = new String[replicates.size()];
                        for (int i = 0; i < replicateNames.length; i++) {
                            replicateNames[i] = samples.get(i).getReference();
                        }
                        SampleSelection sampleSelection = new SampleSelection(null, true, replicateNames, "replicate");
                        sampleSelection.setVisible(true);
                        Integer choice = new Integer(sampleSelection.getChoice());
                        tempReplicate = choice;
                    }

                    replicateNumber = tempReplicate;

                    if (progressDialog.isRunCanceled()) {
                        progressDialog.dispose();
                        return;
                    }
                    
                    
                    

                    progressDialog.setTitle("Loading FASTA File. Please Wait...");

                    try {
                        File providedFastaLocation = psSettings.getSearchParameters().getFastaFile();
                        String fileName = providedFastaLocation.getName();
                        File projectFolder = currentPSFile.getParentFile();
                        File dataFolder = new File(projectFolder, "data");

                        // try to locate the FASTA file
                        if (providedFastaLocation.exists()) {
                            SequenceFactory.getInstance().loadFastaFile(providedFastaLocation);
                            fastaTxt.setText(fileName);
                        } else if (new File(projectFolder, fileName).exists()) {
                            SequenceFactory.getInstance().loadFastaFile(new File(projectFolder, fileName));
                            psSettings.getSearchParameters().setFastaFile(new File(projectFolder, fileName));
                            fastaTxt.setText(fileName);
                        } else if (new File(dataFolder, fileName).exists()) {
                            SequenceFactory.getInstance().loadFastaFile(new File(dataFolder, fileName));
                            psSettings.getSearchParameters().setFastaFile(new File(dataFolder, fileName));
                            fastaTxt.setText(fileName);
                        } else {
                            JOptionPane.showMessageDialog(NewDialog.this,
                                    psSettings.getSearchParameters().getFastaFile() + " could not be found."
                                    + "\n\nPlease select the FASTA file manually.",
                                    "File Input Error", JOptionPane.ERROR_MESSAGE);
                            fastaTxt.setText("Please Select Fasta File(s)");
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(NewDialog.this,
                                "An error occured while reading:\n" + psSettings.getSearchParameters().getFastaFile() + "."
                                + "\n\nPlease select the FASTA file manually.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                        fastaTxt.setText("Please Select Fasta File(s)");
                    }

                    if (progressDialog.isRunCanceled()) {
                        progressDialog.dispose();
                        return;
                    }

                    ArrayList<String> names = new ArrayList<String>();
                    ArrayList<String> missing = new ArrayList<String>();
                    Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
                    ArrayList<String> spectrumFiles = identification.getSpectrumFiles();
                    ArrayList<File> mgfFiles = new ArrayList<File>();

                    progressDialog.setTitle("Locating Spectrum Files. Please Wait...");
                    progressDialog.setIndeterminate(false);
                    progressDialog.setMaxProgressValue(spectrumFiles.size() + 1);
                    progressDialog.increaseProgressValue();

                    try {
                        for (String filePath : spectrumFiles) {

                            progressDialog.increaseProgressValue();

                            File newFile, providedSpectrumLocation = new File(filePath);
                            String fileName = providedSpectrumLocation.getName();
                            File projectFolder = currentPSFile.getParentFile();
                            File dataFolder = new File(projectFolder, "data");

                            // try to locate the spectrum file
                            if (providedSpectrumLocation.exists() && !names.contains(providedSpectrumLocation.getName())) {
                                names.add(providedSpectrumLocation.getName());
                                mgfFiles.add(providedSpectrumLocation);
                            } else if (new File(projectFolder, fileName).exists() && !names.contains(new File(projectFolder, fileName).getName())) {
                                newFile = new File(projectFolder, fileName);
                                names.add(newFile.getName());
                                mgfFiles.add(newFile);
                            } else if (new File(dataFolder, fileName).exists() && !names.contains(new File(dataFolder, fileName).getName())) {
                                newFile = new File(dataFolder, fileName);
                                names.add(newFile.getName());
                                mgfFiles.add(newFile);
                            } else {
                                missing.add(providedSpectrumLocation.getName());
                            }
                        }
                        if (!missing.isEmpty()) {
                            if (missing.size() <= 3) {
                                String report = "";
                                int cpt = 0;
                                Collections.sort(missing);
                                for (String name : missing) {
                                    if (cpt > 0) {
                                        if (cpt == missing.size() - 1) {
                                            report += " and ";
                                        } else {
                                            report += ", ";
                                        }
                                    }
                                    cpt++;
                                    report += name;
                                }
                                if (cpt == 1) {
                                    report += " was";
                                } else {
                                    report += " were";
                                }
                                report += " not found. Please import ";
                                if (cpt == 1) {
                                    report += "it";
                                } else {
                                    report += "them";
                                }
                                report += " manually."; // Dare you say I don't make efforts for user friendliness!
                                JOptionPane.showMessageDialog(NewDialog.this,
                                        report,
                                        "File(s) missing", JOptionPane.ERROR_MESSAGE);
                            } else if (names.isEmpty()) {
                                JOptionPane.showMessageDialog(NewDialog.this,
                                        "The mgf files could not be located, please import them manually.",
                                        "File(s) missing", JOptionPane.ERROR_MESSAGE);
                            } else {
                                JOptionPane.showMessageDialog(NewDialog.this,
                                        missing.size() + " mgf files could not be located, please import them manually.",
                                        "File(s) missing", JOptionPane.ERROR_MESSAGE);
                            }
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(NewDialog.this,
                                "An error occured while looking for the spectrum files. Please locate the files manually.",
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                        e.printStackTrace();
                    }



                    progressDialog.setTitle("Importing Spectrum Files. Please Wait...");
                    progressDialog.setIndeterminate(true);

                    ArrayList<File> error = processSpectrumFiles(mgfFiles, progressDialog);

                    if (!error.isEmpty()) {
                        String report = "An error occurred while importing ";
                        if (error.size() <= 3) {
                            int cpt = 0;
                            Collections.sort(error);
                            for (File errorFile : error) {
                                if (cpt > 0) {
                                    if (cpt == error.size() - 1) {
                                        report += " and ";
                                    } else {
                                        report += ", ";
                                    }
                                }
                                String fileName = errorFile.getName();
                                cpt++;
                                report += fileName;
                            }
                        } else {
                            report += error.size() + " mgf files";
                        }
                        if (error.size() > 1) {
                            report += ". Please import them manually.";
                        } else {
                            report += ". Please import it manually.";
                        }
                        JOptionPane.showMessageDialog(NewDialog.this,
                                report,
                                "File Input Error", JOptionPane.ERROR_MESSAGE);
                    }

                    if (!missing.isEmpty() || !error.isEmpty()) {
                        txtSpectraFileLocation.setText("Please select the mgf file(s).");
                    } else {
                        String report = "";
                        if (names.size() <= 3) {
                            int cpt = 0;
                            Collections.sort(names);
                            for (String name : names) {
                                if (cpt > 0) {
                                    if (cpt == names.size() - 1) {
                                        report += " and ";
                                    } else {
                                        report += ", ";
                                    }
                                }
                                cpt++;
                                report += name;
                            }
                        } else {
                            report += names.size() + " files selected.";
                        }
                        txtSpectraFileLocation.setText(report);
                    }

                    if (progressDialog.isRunCanceled()) {
                        progressDialog.dispose();
                        return;
                    }

                    experimentTxt.setText(experiment.getReference());
                    sampleNameTxt.setText(sample.getReference());
                    replicateNumberTxt.setText(replicateNumber + "");
                    txtIdFileLocation.setText(currentPSFile.getName());

                    cache = new ObjectsCache();
                    cache.setAutomatedMemoryManagement(true);
                    identification.establishConnection(Reporter.SERIALIZATION_DIRECTORY, false, cache);

                    if (!testIdentificationConnection()) {
                        progressDialog.setRunCanceled();
                    }
                    
                    if (identification.getSpectrumIdentificationMap() == null) {
                        // 0.18 version, needs update of the spectrum mapping
                        identification.updateSpectrumMapping();
                    }

                    if (!progressDialog.isRunCanceled()) {
                        progressDialog.setIndeterminate(true);
                        progressDialog.setTitle("Loading Proteins. Please Wait...");
                        identification.loadProteinMatches(progressDialog);
                        progressDialog.setTitle("Loading Protein Details. Please Wait...");
                        identification.loadProteinMatchParameters(new PSParameter(), progressDialog);
                        progressDialog.setTitle("Loading Peptide Details. Please Wait...");
                        identification.loadPeptideMatchParameters(new PSParameter(), progressDialog);
                        progressDialog.setIndeterminate(true);
                    }


                    // try to detect the method used

                    // @TODO: this list only contains the variable mods. and thus defaults to iTRAQ 4plex for TMT...
                    ArrayList<String> foundModifications = psSettings.getMetrics().getFoundModifications();
                    for (String ptm : foundModifications) {
                        if (ptm.toLowerCase().contains("8plex")) {
                            selectedMethod = getMethod("iTRAQ 8Plex");
                        } else if (ptm.toLowerCase().contains("duplex")) {
                            selectedMethod = getMethod("TMT2");
                        } else if (ptm.toLowerCase().contains("6-plex")) {
                            selectedMethod = getMethod("TMT6");
                        } else if (ptm.toLowerCase().contains("itraq")) {
                            selectedMethod = getMethod("iTRAQ 4Plex");
                        }
                    }

                    // no method detected, default to iTRAQ 4 plex
                    if (selectedMethod == null) {
                        comboMethod2.setSelectedItem(methodsFactory.getMethodsNames()[0]);
                        comboMethod1.setSelectedItem(methodsFactory.getMethodsNames()[0]);
                    }

                    mzTolTxt.setText(psSettings.getSearchParameters().getPrecursorAccuracy() + "");
                    if (psSettings.getSearchParameters().isPrecursorAccuracyTypePpm()) {
                        ppmCmb.setSelectedIndex(0);
                    } else {
                        ppmCmb.setSelectedIndex(1);
                    }

                    sampleNames.clear();
                    refresh();

                    progressDialog.dispose();

                } catch (OutOfMemoryError error) {
                    System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
                    Runtime.getRuntime().gc();
                    JOptionPane.showMessageDialog(null,
                            "The task used up all the available memory and had to be stopped.\n"
                            + "Memory boundaries are set in ../resources/conf/JavaOptions.txt.",
                            "Out Of Memory Error",
                            JOptionPane.ERROR_MESSAGE);

                    progressDialog.dispose();

                    error.printStackTrace();
                } catch (EOFException e) {

                    progressDialog.dispose();

                    JOptionPane.showMessageDialog(NewDialog.this,
                            "An error occured while reading:\n" + currentPSFile + ".\n\n"
                            + "The file is corrupted and cannot be opened anymore.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                } catch (Exception e) {

                    progressDialog.dispose();

                    JOptionPane.showMessageDialog(NewDialog.this,
                            "An error occured while reading:\n" + currentPSFile + ".\n\n"
                            + "Please verify that the compomics-utilities version used to create\n"
                            + "the file is compatible with your version of PeptideShaker.",
                            "File Input Error", JOptionPane.ERROR_MESSAGE);
                    e.printStackTrace();
                }
            }
        }.start();
    }

    /**
     * Tests the connection to the identification database.
     */
    private boolean testIdentificationConnection() {
        try {
            Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
            String proteinKey = identification.getProteinIdentification().get(0);
            ProteinMatch testMatch = identification.getProteinMatch(proteinKey);
            if (testMatch == null) {
                throw new IllegalArgumentException("Test protein " + proteinKey + " not found.");
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(NewDialog.this,
                    "A problem occurred while connecting to the database.",
                    "Database connection error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
    }

    /**
     * Method called whenever an error was encountered while loading the
     * methods.
     */
    private void importMethodsError() {
        JOptionPane.showMessageDialog(this, "\"" + METHODS_FILE + "\" could not be found, please select a method file.", "No Spectrum File Selected", JOptionPane.ERROR_MESSAGE);
        JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Methods file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showDialog(this.getParent(), "Add");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File newFile = fileChooser.getSelectedFile();
            try {
                methodsFactory.importMethods(newFile);
                reporterGui.setLastSelectedFolder(newFile.getPath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "File " + METHODS_FILE + " not found in conf folder.",
                        "Methods file not found", JOptionPane.WARNING_MESSAGE);
                importMethodsError();
            } catch (XmlPullParserException e) {
                JOptionPane.showMessageDialog(this,
                        "An error occured while parsing " + METHODS_FILE + " at line " + e.getLineNumber() + ".",
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
        double test;
        try {
            test = new Double(ionToleranceTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong Ion Tolerance.", "Please input a number for the ion tolerance.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (precursorMatching.isSelected()) {
            try {
                test = new Double(mzTolTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Wrong spectrum matching m/z tolerance.", "Please input a number for precursor m/z tolerance.", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            try {
                test = new Double(rtTolTxt.getText().trim());
            } catch (Exception e) {
                JOptionPane.showMessageDialog(this, "Wrong spectrum matching RT tolerance.", "Please input a number for precursor RT tolerance.", JOptionPane.ERROR_MESSAGE);
                return false;
            }
        }
        try {
            test = new Integer(replicateNumberTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong replicate number.", "Please input a number for replicate number.", JOptionPane.ERROR_MESSAGE);
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
        progressDialog.setIndeterminate(true);
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
                txtSpectraFileLocation.setText(mgfFiles.size() + " file(s) selected.");
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
        int cpt = 0;
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
     * Returns the last selected folder.
     *
     * @return the last selected folder
     */
    public String getLastSelectedFolder() {
        return reporterGui.getLastSelectedFolder();
    }

    /**
     * Sets the last selected folder.
     *
     * @param lastSelectedFolder the last selected folder
     */
    public void setLastSelectedFolder(String lastSelectedFolder) {
        reporterGui.setLastSelectedFolder(lastSelectedFolder);
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
            return selectedMethod.getReporterIons().size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Label";
                case 1:
                    return "Sample";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return selectedMethod.getReporterIons().get(row).getName();
                case 1:
                    int index = selectedMethod.getReporterIons().get(row).getIndex();

                    if (sampleNames.get(row) == null) {
                        if (sample != null) {
                            sampleNames.put(row, sample.getReference() + " " + index);
                        } else {
                            sampleNames.put(row, "Sample " + index);
                        }
                    }
                    return sampleNames.get(row);
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == 1) {
                sampleNames.put(row, aValue.toString());
            }
            repaint();
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }
    }

    /**
     * Table model for the reporter ions table.
     */
    private class IonTableModel extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (selectedMethod == null) {
                return 0;
            }
            return selectedMethod.getReporterIons().size();
        }

        @Override
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Name";
                case 1:
                    return "Expected Mass";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return selectedMethod.getReporterIons().get(row).getName();
                case 1:
                    return selectedMethod.getReporterIons().get(row).getTheoreticMass();
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            // @TODO: implement me!!
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }
    }

    /**
     * Table model for the correction factors table.
     */
    private class CorrectionTableModel extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (selectedMethod == null) {
                return 0;
            }
            return selectedMethod.getCorrectionFactors().size();
        }

        @Override
        public int getColumnCount() {
            return 5;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Ion Id";
                case 1:
                    return "% of -2";
                case 2:
                    return "% of -1";
                case 3:
                    return "% of +1";
                case 4:
                    return "% of +2";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            switch (column) {
                case 0:
                    return selectedMethod.getCorrectionFactors().get(row).getIonId();
                case 1:
                    return selectedMethod.getCorrectionFactors().get(row).getMinus2();
                case 2:
                    return selectedMethod.getCorrectionFactors().get(row).getMinus1();
                case 3:
                    return selectedMethod.getCorrectionFactors().get(row).getPlus1();
                case 4:
                    return selectedMethod.getCorrectionFactors().get(row).getPlus2();
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            // @TODO: implement me!!
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }
    }
}
