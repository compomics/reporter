package eu.isas.reporter.gui;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.io.ExperimentIO;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private final String METHODS_FILE = "conf/reporterMethods.xml";
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
    private ReporterMethod selectedMethod = getMethod("Method");
    /**
     * The reporter ion reference.
     */
    private int reference = 0;
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
     * The mgf files loaded.
     */
    private ArrayList<File> mgfFiles = new ArrayList<File>();
    /**
     * The identification files loaded.
     */
    private ArrayList<File> idFiles = new ArrayList<File>();
    /**
     * Reporter will take care of the calculation.
     */
    private Reporter reporter;
    /**
     * The quantification preferences.
     */
    private QuantificationPreferences quantificationPreferences;
    /**
     * Compomics experiment saver and opener.
     */
    private ExperimentIO experimentIO = new ExperimentIO();

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

        initComponents();
        
        // make sure that the scroll panes are see-through
        sampleAssignmentJScrollPane.getViewport().setOpaque(false);
        reporterIonsConfigJScrollPane.getViewport().setOpaque(false);
        isotopeCorrectionJScrollPane.getViewport().setOpaque(false);
        
        // set the table properties
        setTableProperties();

        loadPreferences();

        sameSpectra.setSelected(true);

        comboMethod2.setSelectedItem(methodsFactory.getMethodsNames()[0]);
        comboMethod1.setSelectedItem(methodsFactory.getMethodsNames()[0]);

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

        txtConfigurationFileLocation.setText(METHODS_FILE);

        // blank experiment by default, will be overwritten if a compomics project is loaded
        experiment = new MsExperiment("Project Name");
        sample = new Sample("sample");
        experiment.addAnalysisSet(sample, new SampleAnalysisSet(sample, new ProteomicAnalysis(replicateNumber)));
        experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).addIdentificationResults(IdentificationMethod.MS2_IDENTIFICATION, new Ms2Identification());
        Identification identification = experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).getIdentification(IdentificationMethod.MS2_IDENTIFICATION);
        identification.setInMemory(false);
        identification.setAutomatedMemoryManagement(true);
        identification.setSerializationDirectory(reporter.SERIALIZATION_DIRECTORY);
        isotopeCorrectionTable.getColumnModel().getColumn(0).setMaxWidth(50);
        pack();
        setLocationRelativeTo(reporterGui);
        setVisible(true);
    }
    
    /**
     * Set up the properties of the tables.
     */
    private void setTableProperties() {
        sampleAssignmentTable.getColumn("Ref").setCellRenderer(new TrueFalseIconRenderer(
                new ImageIcon(this.getClass().getResource("/icons/selected_green.png")),
                null,
                "Reference", null));
        
        sampleAssignmentTable.getColumn("Ref").setMaxWidth(40);
        sampleAssignmentTable.getColumn("Ref").setMinWidth(40);
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
        txtExperiment = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        sampleNameTxt = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        replicateNumberTxt = new javax.swing.JTextField();
        fileSelectiontPanel = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        txtSpectraFileLocation = new javax.swing.JTextField();
        clearSpectraJButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        txtIdFileLocation = new javax.swing.JTextField();
        addIdFilesButton = new javax.swing.JButton();
        clearIdFilesJButton = new javax.swing.JButton();
        addSpectraFilesJButton = new javax.swing.JButton();
        jLabel10 = new javax.swing.JLabel();
        jTextField1 = new javax.swing.JTextField();
        addDbButton = new javax.swing.JButton();
        clearDbButton = new javax.swing.JButton();
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
        idenificationAnalysisPanel = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        fdrThresholdTxt = new javax.swing.JTextField();
        jLabel8 = new javax.swing.JLabel();
        jLabel9 = new javax.swing.JLabel();
        jLabel16 = new javax.swing.JLabel();
        mascotEvalueTxt = new javax.swing.JTextField();
        jLabel17 = new javax.swing.JLabel();
        omssaEvalueTxt = new javax.swing.JTextField();
        jLabel18 = new javax.swing.JLabel();
        xTandemEvalueTxt = new javax.swing.JTextField();
        jLabel11 = new javax.swing.JLabel();
        sameSpectra = new javax.swing.JRadioButton();
        precursorMatching = new javax.swing.JRadioButton();
        jLabel19 = new javax.swing.JLabel();
        jLabel20 = new javax.swing.JLabel();
        mzTolTxt = new javax.swing.JTextField();
        jLabel21 = new javax.swing.JLabel();
        rtTolTxt = new javax.swing.JTextField();
        jLabel22 = new javax.swing.JLabel();
        identificationPreprocessingPanel = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        nAaMinTxt = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        nAaMaxTxt = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        deltaMassTxt = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();
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

        projectPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Project"));
        projectPanel.setOpaque(false);

        txtExperiment.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtExperiment.setText("Project Name");
        txtExperiment.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtExperimentKeyReleased(evt);
            }
        });

        jLabel1.setText("Project Reference");

        jLabel24.setText("Sample Name");

        sampleNameTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        sampleNameTxt.setText("Sample Name");
        sampleNameTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleNameTxtActionPerformed(evt);
            }
        });

        jLabel25.setText("Replicate");

        replicateNumberTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        replicateNumberTxt.setText("0");

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
                    .add(txtExperiment))
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
                    .add(txtExperiment, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
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
        txtSpectraFileLocation.setText("Please select file(s)");

        clearSpectraJButton.setText("Clear");
        clearSpectraJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSpectraJButtonActionPerformed(evt);
            }
        });

        jLabel3.setText("Identification File(s)");

        txtIdFileLocation.setEditable(false);
        txtIdFileLocation.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtIdFileLocation.setText("Please select file(s)");

        addIdFilesButton.setText("Browse");
        addIdFilesButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addIdFilesButtonActionPerformed(evt);
            }
        });

        clearIdFilesJButton.setText("Clear");
        clearIdFilesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearIdFilesJButtonActionPerformed(evt);
            }
        });

        addSpectraFilesJButton.setText("Browse");
        addSpectraFilesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSpectraFilesJButtonActionPerformed(evt);
            }
        });

        jLabel10.setText("Database File (FASTA)");

        jTextField1.setEditable(false);
        jTextField1.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        jTextField1.setText("Please select a file");

        addDbButton.setText("Browse");

        clearDbButton.setText("Clear");

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
                            .add(jTextField1)))
                    .add(fileSelectiontPanelLayout.createSequentialGroup()
                        .add(jLabel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18)
                        .add(txtIdFileLocation)))
                .add(18, 18, 18)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, addSpectraFilesJButton)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, addIdFilesButton)
                    .add(addDbButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, fileSelectiontPanelLayout.createSequentialGroup()
                            .add(clearIdFilesJButton)
                            .add(1, 1, 1))
                        .add(org.jdesktop.layout.GroupLayout.TRAILING, clearSpectraJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(clearDbButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 68, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );

        fileSelectiontPanelLayout.linkSize(new java.awt.Component[] {addDbButton, addIdFilesButton, addSpectraFilesJButton, clearDbButton, clearIdFilesJButton, clearSpectraJButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        fileSelectiontPanelLayout.setVerticalGroup(
            fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(fileSelectiontPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(txtIdFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addIdFilesButton)
                    .add(clearIdFilesJButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(txtSpectraFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(clearSpectraJButton)
                    .add(addSpectraFilesJButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel10)
                    .add(jTextField1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addDbButton)
                    .add(clearDbButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        samplePanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Sample Assignment"));
        samplePanel.setOpaque(false);

        sampleAssignmentTable.setModel(new AssignementTableModel());
        sampleAssignmentTable.setOpaque(false);
        sampleAssignmentJScrollPane.setViewportView(sampleAssignmentTable);

        jLabel5.setText("Method Selected");

        comboMethod1.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNames()));

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
                .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 214, Short.MAX_VALUE)
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
                .add(projectPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(fileSelectiontPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(samplePanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
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

        org.jdesktop.layout.GroupLayout spectrumAnalysisPanelLayout = new org.jdesktop.layout.GroupLayout(spectrumAnalysisPanel);
        spectrumAnalysisPanel.setLayout(spectrumAnalysisPanelLayout);
        spectrumAnalysisPanelLayout.setHorizontalGroup(
            spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel6)
                .add(18, 18, 18)
                .add(ionToleranceTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 102, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );
        spectrumAnalysisPanelLayout.setVerticalGroup(
            spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(spectrumAnalysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(spectrumAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(ionToleranceTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        idenificationAnalysisPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification Analysis"));
        idenificationAnalysisPanel.setOpaque(false);

        jLabel7.setText("FDR Threshold");

        fdrThresholdTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fdrThresholdTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrThresholdTxtActionPerformed(evt);
            }
        });

        jLabel8.setText("%");

        jLabel9.setText("Maximum E-values");

        jLabel16.setText("Mascot");

        mascotEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        mascotEvalueTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mascotEvalueTxtActionPerformed(evt);
            }
        });

        jLabel17.setText("OMSSA");

        omssaEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel18.setText("X!Tandem");

        xTandemEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

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

        jLabel21.setText("ppm");

        rtTolTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rtTolTxtActionPerformed(evt);
            }
        });

        jLabel22.setText("s");

        org.jdesktop.layout.GroupLayout idenificationAnalysisPanelLayout = new org.jdesktop.layout.GroupLayout(idenificationAnalysisPanel);
        idenificationAnalysisPanel.setLayout(idenificationAnalysisPanelLayout);
        idenificationAnalysisPanelLayout.setHorizontalGroup(
            idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel9)
                    .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel7)
                            .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                                .add(10, 10, 10)
                                .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel18)
                                    .add(jLabel17)
                                    .add(jLabel16))))
                        .add(18, 18, 18)
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, fdrThresholdTxt)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, mascotEvalueTxt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, omssaEvalueTxt)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, xTandemEvalueTxt))))
                .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel8))
                    .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                        .add(169, 169, 169)
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jLabel11)
                            .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                                .add(10, 10, 10)
                                .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(precursorMatching)
                                    .add(sameSpectra)
                                    .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                                        .add(19, 19, 19)
                                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jLabel19)
                                            .add(jLabel20))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                            .add(rtTolTxt)
                                            .add(mzTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 106, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel22)
                                    .add(jLabel21))))))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        idenificationAnalysisPanelLayout.linkSize(new java.awt.Component[] {fdrThresholdTxt, mascotEvalueTxt, omssaEvalueTxt, xTandemEvalueTxt}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        idenificationAnalysisPanelLayout.setVerticalGroup(
            idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                        .add(jLabel11)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(sameSpectra)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(precursorMatching)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel19)
                            .add(mzTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel21))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(rtTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel20)
                            .add(jLabel22)))
                    .add(idenificationAnalysisPanelLayout.createSequentialGroup()
                        .add(jLabel9)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel16)
                            .add(mascotEvalueTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel17)
                            .add(omssaEvalueTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel18)
                            .add(xTandemEvalueTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(18, 18, 18)
                        .add(idenificationAnalysisPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel7)
                            .add(fdrThresholdTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel8))))
                .addContainerGap(168, Short.MAX_VALUE))
        );

        identificationPreprocessingPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification Pre-Processing"));
        identificationPreprocessingPanel.setOpaque(false);

        jLabel12.setText("nAA min");

        nAaMinTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nAaMinTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nAaMinTxtActionPerformed(evt);
            }
        });

        jLabel13.setText("nAA max");

        nAaMaxTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nAaMaxTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nAaMaxTxtActionPerformed(evt);
            }
        });

        jLabel14.setText("Mass Deviation");

        deltaMassTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        deltaMassTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deltaMassTxtActionPerformed(evt);
            }
        });

        jLabel15.setText("ppm");

        org.jdesktop.layout.GroupLayout identificationPreprocessingPanelLayout = new org.jdesktop.layout.GroupLayout(identificationPreprocessingPanel);
        identificationPreprocessingPanel.setLayout(identificationPreprocessingPanelLayout);
        identificationPreprocessingPanelLayout.setHorizontalGroup(
            identificationPreprocessingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(identificationPreprocessingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(identificationPreprocessingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(identificationPreprocessingPanelLayout.createSequentialGroup()
                        .add(identificationPreprocessingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(identificationPreprocessingPanelLayout.createSequentialGroup()
                                .add(jLabel12)
                                .add(18, 18, 18)
                                .add(nAaMinTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(identificationPreprocessingPanelLayout.createSequentialGroup()
                                .add(jLabel13)
                                .add(18, 18, 18)
                                .add(nAaMaxTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 121, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap())
                    .add(identificationPreprocessingPanelLayout.createSequentialGroup()
                        .add(jLabel14)
                        .add(18, 18, 18)
                        .add(deltaMassTxt)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(jLabel15)
                        .add(674, 674, 674))))
        );

        identificationPreprocessingPanelLayout.linkSize(new java.awt.Component[] {deltaMassTxt, nAaMaxTxt, nAaMinTxt}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        identificationPreprocessingPanelLayout.linkSize(new java.awt.Component[] {jLabel12, jLabel13, jLabel14}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        identificationPreprocessingPanelLayout.setVerticalGroup(
            identificationPreprocessingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(identificationPreprocessingPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(identificationPreprocessingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel12)
                    .add(nAaMinTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(identificationPreprocessingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel13)
                    .add(nAaMaxTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(identificationPreprocessingPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel14)
                    .add(deltaMassTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel15))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout advancedParamsPanelLayout = new org.jdesktop.layout.GroupLayout(advancedParamsPanel);
        advancedParamsPanel.setLayout(advancedParamsPanelLayout);
        advancedParamsPanelLayout.setHorizontalGroup(
            advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(idenificationAnalysisPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(identificationPreprocessingPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 810, Short.MAX_VALUE)
                    .add(spectrumAnalysisPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        advancedParamsPanelLayout.setVerticalGroup(
            advancedParamsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedParamsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(spectrumAnalysisPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(identificationPreprocessingPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(idenificationAnalysisPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addContainerGap())
        );

        tabbedPane.addTab("Advanced Parameters", advancedParamsPanel);

        startButton.setText("Load");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        exitJButton.setText("Exit");
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
                        .add(startButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 57, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(exitJButton))
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
                    .add(startButton)
                    .add(exitJButton))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 855, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(backgroundPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 639, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseConfigButtonActionPerformed
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

    private void clearSpectraJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectraJButtonActionPerformed
        mgfFiles = new ArrayList<File>();
        txtSpectraFileLocation.setText("Please select file(s)");
    }//GEN-LAST:event_clearSpectraJButtonActionPerformed

    private void addIdFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIdFilesButtonActionPerformed

        JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Identification File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {

            public boolean accept(File myFile) {
                return myFile.getName().endsWith("dat")
                        || myFile.getName().endsWith("omx")
                        || myFile.getName().endsWith("xml")
                        || myFile.getName().endsWith("cps") // compomics peptide shaker files
                        || myFile.isDirectory();
            }

            public String getDescription() {
                return "Supported formats: Mascot (.dat), OMSSA (.omx), X!Tandem (.xml)";
            }
        };

        fileChooser.setFileFilter(filter);

        int returnVal = fileChooser.showDialog(this.getParent(), "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            for (File newFile : fileChooser.getSelectedFiles()) {
                if (newFile.isDirectory()) {
                    File[] tempFiles = newFile.listFiles();
                    for (File file : tempFiles) {
                        if (file.getName().endsWith("dat")
                                || file.getName().endsWith("omx")
                                || file.getName().endsWith("xml")
                                || file.getName().endsWith("cps")) {
                            if (!idFiles.contains(file)) {
                                idFiles.add(file);
                            }
                        }
                    }
                } else {
                    if (newFile.getName().endsWith("dat")
                            || newFile.getName().endsWith("omx")
                            || newFile.getName().endsWith("xml")
                            || newFile.getName().endsWith("cps")) {
                        if (!idFiles.contains(newFile)) {
                            idFiles.add(newFile);
                        }
                    }
                }

                reporterGui.setLastSelectedFolder(newFile.getPath());
            }
            if (idFiles.size() > 1) {
                for (File file : idFiles) {
                    int fileType = -1;
                    if (file.getName().endsWith(".cps")) {
                        JOptionPane.showMessageDialog(this, "A PeptideShaker file must be imported alone.", "Wrong identification file.", JOptionPane.ERROR_MESSAGE);
                        idFiles = new ArrayList<File>();
                    }
                    if (file.getName().endsWith(".dat")) {
                        if (fileType == -1) {
                            fileType = 0;
                        } else if (fileType == 1 || fileType == 2) {
                            JOptionPane.showMessageDialog(this, "Reporter cannot handle multiple search engine results.\n We advise you to use Peptide-Shaker (peptide-shaker.googlecode.com) to process your indentifications.", "Wrong identification file.", JOptionPane.ERROR_MESSAGE);
                            idFiles = new ArrayList<File>();
                        }
                    }
                    if (file.getName().endsWith(".omx")) {
                        if (fileType == -1) {
                            fileType = 1;
                        } else if (fileType == 0 || fileType == 2) {
                            JOptionPane.showMessageDialog(this, "Reporter cannot handle multiple search engine results.\n We advise you to use Peptide-Shaker (peptide-shaker.googlecode.com) to process your indentifications.", "Wrong identification file.", JOptionPane.ERROR_MESSAGE);
                            idFiles = new ArrayList<File>();
                        }
                    }
                    if (file.getName().endsWith(".xml")) {
                        if (fileType == -1) {
                            fileType = 2;
                        } else if (fileType == 0 || fileType == 1) {
                            JOptionPane.showMessageDialog(this, "Reporter cannot handle multiple search engine results.\n We advise you to use Peptide-Shaker (peptide-shaker.googlecode.com) to process your indentifications.", "Wrong identification file.", JOptionPane.ERROR_MESSAGE);
                            idFiles = new ArrayList<File>();
                        }
                    }
                }
            }
            txtIdFileLocation.setText(idFiles.size() + " file(s) selected.");
            if (idFiles.size() == 1 && idFiles.get(0).getName().endsWith(".cps")) {
                importPeptideShakerFile(idFiles.get(0));
                txtExperiment.setEditable(false);
                sampleNameTxt.setEditable(false);
                replicateNumberTxt.setEditable(false);
            } else {
                txtExperiment.setEditable(true);
                sampleNameTxt.setEditable(true);
                replicateNumberTxt.setEditable(true);
            }
        }
    }//GEN-LAST:event_addIdFilesButtonActionPerformed

    private void clearIdFilesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearIdFilesJButtonActionPerformed
        idFiles = new ArrayList<File>();
        txtIdFileLocation.setText("Please select file(s)");
    }//GEN-LAST:event_clearIdFilesJButtonActionPerformed

    private void startButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_startButtonActionPerformed
        if (validateInput()) {
            savePreferences();
            ReporterIonQuantification reporterIonQuantification = getReporterIonQuantification();
            experiment.getAnalysisSet(sample).getProteomicAnalysis(replicateNumber).addQuantificationResults(reporterIonQuantification.getMethodUsed(), reporterIonQuantification);
            reporter.setExperiment(experiment);
            reporter.setSample(sample);
            reporter.setReplicateNumber(replicateNumber);
            reporter.setQuantificationMethodUsed(reporterIonQuantification.getMethodUsed());
            reporter.loadFiles(idFiles, mgfFiles);
            dispose();
        }
    }//GEN-LAST:event_startButtonActionPerformed

    private void fdrThresholdTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_fdrThresholdTxtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_fdrThresholdTxtActionPerformed

    private void nAaMinTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nAaMinTxtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_nAaMinTxtActionPerformed

    private void nAaMaxTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nAaMaxTxtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_nAaMaxTxtActionPerformed

    private void deltaMassTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_deltaMassTxtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_deltaMassTxtActionPerformed

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

    private void txtExperimentKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_txtExperimentKeyReleased
        experiment.setReference(txtExperiment.getText().trim());
    }//GEN-LAST:event_txtExperimentKeyReleased

    private void sampleNameTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_sampleNameTxtActionPerformed
    }//GEN-LAST:event_sampleNameTxtActionPerformed

    private void rtTolTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_rtTolTxtActionPerformed
        // TODO add your handling code here:
    }//GEN-LAST:event_rtTolTxtActionPerformed

    private void mascotEvalueTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_mascotEvalueTxtActionPerformed
        // TODO add your handling code here:
}//GEN-LAST:event_mascotEvalueTxtActionPerformed

    private void exitJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitJButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_exitJButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addDbButton;
    private javax.swing.JButton addIdFilesButton;
    private javax.swing.JButton addSpectraFilesJButton;
    private javax.swing.JPanel advancedParamsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton browseConfigButton;
    private javax.swing.JButton clearDbButton;
    private javax.swing.JButton clearIdFilesJButton;
    private javax.swing.JButton clearSpectraJButton;
    private javax.swing.JComboBox comboMethod1;
    private javax.swing.JComboBox comboMethod2;
    private javax.swing.JPanel configFilePanel;
    private javax.swing.JPanel configPanel;
    private javax.swing.JTextField deltaMassTxt;
    private javax.swing.JButton exitJButton;
    private javax.swing.JTextField fdrThresholdTxt;
    private javax.swing.JPanel fileSelectiontPanel;
    private javax.swing.JPanel idenificationAnalysisPanel;
    private javax.swing.JPanel identificationPreprocessingPanel;
    private javax.swing.JTextField ionToleranceTxt;
    private javax.swing.JScrollPane isotopeCorrectionJScrollPane;
    private javax.swing.JPanel isotopeCorrectionPanel;
    private javax.swing.JTable isotopeCorrectionTable;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel10;
    private javax.swing.JLabel jLabel11;
    private javax.swing.JLabel jLabel12;
    private javax.swing.JLabel jLabel13;
    private javax.swing.JLabel jLabel14;
    private javax.swing.JLabel jLabel15;
    private javax.swing.JLabel jLabel16;
    private javax.swing.JLabel jLabel17;
    private javax.swing.JLabel jLabel18;
    private javax.swing.JLabel jLabel19;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel20;
    private javax.swing.JLabel jLabel21;
    private javax.swing.JLabel jLabel22;
    private javax.swing.JLabel jLabel24;
    private javax.swing.JLabel jLabel25;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JLabel jLabel9;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JTextField jTextField1;
    private javax.swing.JTextField mascotEvalueTxt;
    private javax.swing.JPanel methodPanel;
    private javax.swing.JTextField mzTolTxt;
    private javax.swing.JTextField nAaMaxTxt;
    private javax.swing.JTextField nAaMinTxt;
    private javax.swing.JTextField omssaEvalueTxt;
    private javax.swing.JRadioButton precursorMatching;
    private javax.swing.JPanel processingPanel;
    private javax.swing.JPanel projectPanel;
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
    private javax.swing.JTextField txtExperiment;
    private javax.swing.JTextField txtIdFileLocation;
    private javax.swing.JTextField txtSpectraFileLocation;
    private javax.swing.JTextField xTandemEvalueTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * returns the quantification method selected.
     *
     * @return the quantification method selected
     */
    private ReporterIonQuantification getReporterIonQuantification() {
        ReporterIonQuantification quantification = new ReporterIonQuantification(selectedMethod.getMethodIndex());
        for (int row = 0; row < sampleAssignmentTable.getRowCount(); row++) {
            quantification.assignSample(selectedMethod.getReporterIons().get(row).getIndex(), new Sample((String) sampleAssignmentTable.getValueAt(row, 1)));
        }
        quantification.setMethod(selectedMethod);
        quantification.setReferenceLabel(selectedMethod.getReporterIons().get(reference).getIndex());
        quantification.setInMemory(false);
        quantification.setAutomatedMemoryManagement(true);
        quantification.setSerializationDirectory(reporter.SERIALIZATION_DIRECTORY);
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
            if (method.getMethodName().equals(methodName)) {
                return method;
            }
        }
        return methodsFactory.getMethods().get(0);
    }

    /**
     * Updates the combo box and tables values based on the currently selected
     * quantification method.
     */
    private void refresh() {
        reference = 0;

        comboMethod1.setSelectedItem(selectedMethod.getMethodName());
        comboMethod2.setSelectedItem(selectedMethod.getMethodName());
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
            methodsFactory.importMethods(new File(METHODS_FILE));
        } catch (Exception e) {
            importMethodsError();
        }
    }

    /**
     * Loads the quantification preferences in the GUI.
     */
    private void loadPreferences() {
        quantificationPreferences = reporter.getQuantificationPreferences();
        ionToleranceTxt.setText(quantificationPreferences.getReporterIonsMzTolerance() + "");
        nAaMinTxt.setText(quantificationPreferences.getnAAmin() + "");
        nAaMaxTxt.setText(quantificationPreferences.getnAAmax() + "");
        deltaMassTxt.setText(quantificationPreferences.getPrecursorMassDeviation() + "");
        mascotEvalueTxt.setText(quantificationPreferences.getMaxEValue(SearchEngine.MASCOT) + "");
        omssaEvalueTxt.setText(quantificationPreferences.getMaxEValue(SearchEngine.OMSSA) + "");
        xTandemEvalueTxt.setText(quantificationPreferences.getMaxEValue(SearchEngine.XTANDEM) + "");
        fdrThresholdTxt.setText(quantificationPreferences.getFdrThreshold() * 100 + "");
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
        quantificationPreferences.setnAAmin(new Integer(nAaMinTxt.getText()));
        quantificationPreferences.setnAAmax(new Integer(nAaMaxTxt.getText()));
        quantificationPreferences.setPrecursorMassDeviation(new Double(deltaMassTxt.getText()));
        quantificationPreferences.setMaxEValue(SearchEngine.MASCOT, new Double(mascotEvalueTxt.getText()));
        quantificationPreferences.setMaxEValue(SearchEngine.OMSSA, new Double(omssaEvalueTxt.getText()));
        quantificationPreferences.setMaxEValue(SearchEngine.XTANDEM, new Double(xTandemEvalueTxt.getText()));
        double threshold = new Double(fdrThresholdTxt.getText()) / 100;
        quantificationPreferences.setFdrThreshold(threshold);
        if (sameSpectra.isSelected()) {
            quantificationPreferences.setSameSpectra(true);
        } else {
            quantificationPreferences.setSameSpectra(false);
            quantificationPreferences.setPrecursorMzTolerance(new Double(mzTolTxt.getText()));
            quantificationPreferences.setPrecursorRTTolerance(new Double(rtTolTxt.getText()));
        }
    }

    /**
     * Method used to import a peptide shaker file.
     *
     * @param psFile a peptide shaker file
     */
    private void importPeptideShakerFile(File psFile) {
        try {
            experiment = experimentIO.loadExperiment(psFile);
            txtExperiment.setText(experiment.getReference());
            txtExperiment.setEditable(false);

            ArrayList<Sample> samples = new ArrayList(experiment.getSamples().values());
            if (samples.size() == 1) {
                sample = samples.get(0);
            } else {
                // @TODO allow the user to chose the desired sample
            }
            sampleNameTxt.setText(sample.getReference());
            sampleNameTxt.setEditable(false);

            ArrayList<Integer> replicates = new ArrayList(experiment.getAnalysisSet(sample).getReplicateNumberList());
            if (replicates.size() == 1) {
                replicateNumber = replicates.get(0);
            } else {
                // @TODO allow the user to chose the desired replicate
            }
            replicateNumberTxt.setText(replicateNumber + "");
            replicateNumberTxt.setEditable(false);
            JOptionPane.showMessageDialog(this,
                    "Experiment successfully loaded.",
                    "Import completed", JOptionPane.WARNING_MESSAGE);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this,
                    "An error occured while reading " + psFile.getName() + ".",
                    "Reading error", JOptionPane.WARNING_MESSAGE);
            e.printStackTrace();
        } catch (ClassNotFoundException e1) {
            JOptionPane.showMessageDialog(this,
                    "An error occured while importing " + psFile.getName() + ". Please verify that the version of Reporter you are using is compatible with the version of Peptide-Shaker which was used to generate the file.",
                    "Import error", JOptionPane.WARNING_MESSAGE);
            e1.printStackTrace();
        }
    }

    /**
     * Method called whenever an error was encountered while loading the methods.
     */
    private void importMethodsError() {
        JOptionPane.showMessageDialog(this, "\"" + METHODS_FILE + "\" could not be found, please select a method file.", "No Spectra File Selected", JOptionPane.ERROR_MESSAGE);
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
        if (mgfFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "please select a spectrum file.", "No Spectra File Selected", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        if (idFiles.isEmpty()) {
            JOptionPane.showMessageDialog(this, "please select an identification file.", "No Identification File Selected", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        double test;
        try {
            test = new Double(ionToleranceTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong Ion Tolerance.", "Please input a number for the ion tolerance.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(fdrThresholdTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong FDR Threshold.", "Please input a number for the fdr threshold.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(mascotEvalueTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong Mascot Threshold.", "Please input a number for the Mascot threshold.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(omssaEvalueTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong OMSSA Threshold.", "Please input a number for the OMSSA threshold.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(xTandemEvalueTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong X!Tandem Threshold.", "Please input a number for the X!Tandem threshold.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(deltaMassTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong mass deviation limit.", "Please input a number for the mass deviation limit.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(nAaMinTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong nAA min.", "Please input a number for minimal amount of amino acid.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(nAaMinTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong nAA max.", "Please input a number for maximal amount of amino acid.", JOptionPane.ERROR_MESSAGE);
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
     * Sets the identification files to process.
     *
     * @param files the identification files to process
     */
    public void setIdFiles(ArrayList<File> files) {
        idFiles = files;
        txtIdFileLocation.setText(idFiles.size() + " file(s) selected.");
    }

    /**
     * Sets the spectra files to process.
     *
     * @param files the spectra files to process
     */
    public void setSpectraFiles(ArrayList<File> files) {
        mgfFiles = files;
        txtSpectraFileLocation.setText(mgfFiles.size() + " file(s) selected.");
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
            return selectedMethod.getReporterIons().size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return "Label";
                case 1:
                    return "Sample";
                case 2:
                    return "Ref";
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
                    if (experiment.getSample(row) == null) {
                        experiment.setSample(row, new Sample("Sample " + (row + 1)));
                    }
                    return experiment.getSample(row).getReference();
                case 2:
                    if (row == reference) {
                        return true;
                    } else {
                        return false;
                    }
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == 1) {
                experiment.setSample(row, new Sample(aValue.toString()));
            } else if (column == 2) {
                if ((Boolean) aValue) {
                    reference = row;
                }
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
                    return selectedMethod.getReporterIons().get(row).theoreticMass;
                default:
                    return "";
            }
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
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
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }
    }
}
