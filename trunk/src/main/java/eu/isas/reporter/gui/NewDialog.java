package eu.isas.reporter.gui;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.ProteomicAnalysis;
import com.compomics.util.experiment.SampleAnalysisSet;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.identification.IdentificationMethod;
import com.compomics.util.experiment.identification.advocates.SearchEngine;
import com.compomics.util.experiment.identification.identifications.Ms2Identification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.io.UtilitiesInput;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.filechooser.FileFilter;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import org.xmlpull.v1.XmlPullParserException;

/**
 * This panel will be used to load the necessary files and settings to start the analysis
 *
 * @author Marc Vaudel
 */
public class NewDialog extends javax.swing.JDialog {

    /**
     * File containing the various reporter methods
     */
    private final String METHODS_FILE = "conf/reporterMethods.xml";
    /**
     * modification file
     */
    private final String MODIFICATIONS_FILE = "conf/reporter_mods.xml";
    /**
     * user modification file
     */
    private final String USER_MODIFICATIONS_FILE = "conf/reporter_usermods.xml";
    /**
     * The compomics reporter methods factory
     */
    private ReporterMethodFactory methodsFactory = ReporterMethodFactory.getInstance();
    /**
     * The reporter class
     */
    private ReporterGUI parent;
    /**
     * The method selected
     */
    private ReporterMethod selectedMethod = getMethod("Method");
    /**
     * The  reporter ion reference
     */
    private int reference = 0;
    /**
     * The experiment conducted
     */
    private MsExperiment experiment;
    /**
     * The sample analyzed
     */
    private Sample sample;
    /**
     * The replicate number
     */
    private int replicateNumber;
    /**
     * The mgf files loaded
     */
    private ArrayList<File> mgfFiles = new ArrayList<File>();
    /**
     * The identification files loaded
     */
    private ArrayList<File> idFiles = new ArrayList<File>();
    /**
     * Reporter will take care of the calculation
     */
    private Reporter reporter;
    /**
     * The quantification preferences
     */
    private QuantificationPreferences quantificationPreferences;

    /**
     * constructor
     *
     * @param parent the reporter class
     */
    public NewDialog(ReporterGUI parent, Reporter reporter) {
        super(parent, true);

        this.parent = parent;
        this.reporter = reporter;

        initComponents();

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
        isotopeCorrectionTable.getColumnModel().getColumn(0).setMaxWidth(50);
        pack();
        setVisible(true);
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jPanel3 = new javax.swing.JPanel();
        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        txtSpectraFileLocation = new javax.swing.JTextField();
        clearSpectraJButton = new javax.swing.JButton();
        jLabel3 = new javax.swing.JLabel();
        txtIdFileLocation = new javax.swing.JTextField();
        editIdFilesJButton = new javax.swing.JButton();
        addIdFilesButton = new javax.swing.JButton();
        clearIdFilesJButton = new javax.swing.JButton();
        addSpectraFilesJButton = new javax.swing.JButton();
        editSpectraFilesJButton = new javax.swing.JButton();
        exitButton1 = new javax.swing.JButton();
        startButton = new javax.swing.JButton();
        jPanel8 = new javax.swing.JPanel();
        jScrollPane3 = new javax.swing.JScrollPane();
        sampleAssignmentTable = new javax.swing.JTable();
        jLabel5 = new javax.swing.JLabel();
        comboMethod1 = new javax.swing.JComboBox();
        jPanel13 = new javax.swing.JPanel();
        txtExperiment = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
        jLabel24 = new javax.swing.JLabel();
        sampleNameTxt = new javax.swing.JTextField();
        jLabel25 = new javax.swing.JLabel();
        replicateNumberTxt = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jPanel5 = new javax.swing.JPanel();
        jScrollPane1 = new javax.swing.JScrollPane();
        reporterIonConfigurationTable = new javax.swing.JTable();
        jPanel6 = new javax.swing.JPanel();
        jScrollPane2 = new javax.swing.JScrollPane();
        isotopeCorrectionTable = new javax.swing.JTable();
        jPanel7 = new javax.swing.JPanel();
        txtConfigurationFileLocation = new javax.swing.JTextField();
        browseConfigButton = new javax.swing.JButton();
        saveConfigButton = new javax.swing.JButton();
        saveAsConfigButton = new javax.swing.JButton();
        exitButton2 = new javax.swing.JButton();
        jPanel14 = new javax.swing.JPanel();
        jLabel4 = new javax.swing.JLabel();
        comboMethod2 = new javax.swing.JComboBox();
        jPanel9 = new javax.swing.JPanel();
        jPanel10 = new javax.swing.JPanel();
        jLabel6 = new javax.swing.JLabel();
        ionToleranceTxt = new javax.swing.JTextField();
        exitButton3 = new javax.swing.JButton();
        jPanel11 = new javax.swing.JPanel();
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
        jPanel12 = new javax.swing.JPanel();
        jLabel12 = new javax.swing.JLabel();
        nAaMinTxt = new javax.swing.JTextField();
        jLabel13 = new javax.swing.JLabel();
        nAaMaxTxt = new javax.swing.JTextField();
        jLabel14 = new javax.swing.JLabel();
        deltaMassTxt = new javax.swing.JTextField();
        jLabel15 = new javax.swing.JLabel();

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

        jPanel1.setPreferredSize(new java.awt.Dimension(800, 600));

        jPanel2.setBorder(javax.swing.BorderFactory.createTitledBorder("Files Selection"));

        jLabel2.setText("Spectra Files:");

        txtSpectraFileLocation.setEditable(false);
        txtSpectraFileLocation.setText("Please select file(s)");

        clearSpectraJButton.setText("Clear");
        clearSpectraJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                clearSpectraJButtonActionPerformed(evt);
            }
        });

        jLabel3.setText("Identification Files:");

        txtIdFileLocation.setEditable(false);
        txtIdFileLocation.setText("Please select file(s)");

        editIdFilesJButton.setText("Edit");
        editIdFilesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editIdFilesJButtonActionPerformed(evt);
            }
        });

        addIdFilesButton.setText("Add");
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

        addSpectraFilesJButton.setText("Add");
        addSpectraFilesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addSpectraFilesJButtonActionPerformed(evt);
            }
        });

        editSpectraFilesJButton.setText("Edit");
        editSpectraFilesJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editSpectraFilesJButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jLabel2)
                        .add(44, 44, 44)
                        .add(txtSpectraFileLocation, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(jLabel3)
                        .add(18, 18, 18)
                        .add(txtIdFileLocation, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 577, Short.MAX_VALUE)))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(addSpectraFilesJButton, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 67, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editSpectraFilesJButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 67, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(clearSpectraJButton))
                    .add(jPanel2Layout.createSequentialGroup()
                        .add(addIdFilesButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editIdFilesJButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(clearIdFilesJButton)))
                .addContainerGap())
        );

        jPanel2Layout.linkSize(new java.awt.Component[] {addIdFilesButton, addSpectraFilesJButton, clearIdFilesJButton, clearSpectraJButton, editIdFilesJButton, editSpectraFilesJButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel3)
                    .add(txtIdFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(addIdFilesButton)
                    .add(editIdFilesJButton)
                    .add(clearIdFilesJButton))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel2)
                    .add(txtSpectraFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(clearSpectraJButton)
                    .add(editSpectraFilesJButton)
                    .add(addSpectraFilesJButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        exitButton1.setText("Exit");
        exitButton1.setMaximumSize(new java.awt.Dimension(57, 23));
        exitButton1.setMinimumSize(new java.awt.Dimension(57, 23));
        exitButton1.setPreferredSize(new java.awt.Dimension(57, 23));
        exitButton1.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButton1ActionPerformed(evt);
            }
        });

        startButton.setText("Load");
        startButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                startButtonActionPerformed(evt);
            }
        });

        jPanel8.setBorder(javax.swing.BorderFactory.createTitledBorder("Sample Assignement"));

        sampleAssignmentTable.setModel(new AssignementTableModel());
        jScrollPane3.setViewportView(sampleAssignmentTable);

        jLabel5.setText("Method Selected:");

        comboMethod1.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNames()));

        org.jdesktop.layout.GroupLayout jPanel8Layout = new org.jdesktop.layout.GroupLayout(jPanel8);
        jPanel8.setLayout(jPanel8Layout);
        jPanel8Layout.setHorizontalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 905, Short.MAX_VALUE)
                    .add(jPanel8Layout.createSequentialGroup()
                        .add(jLabel5)
                        .add(26, 26, 26)
                        .add(comboMethod1, 0, 795, Short.MAX_VALUE)))
                .addContainerGap())
        );
        jPanel8Layout.setVerticalGroup(
            jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel8Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel8Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel5)
                    .add(comboMethod1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .add(18, 18, 18)
                .add(jScrollPane3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 165, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel13.setBorder(javax.swing.BorderFactory.createTitledBorder("Project"));

        txtExperiment.setText("Project Name");
        txtExperiment.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                txtExperimentKeyReleased(evt);
            }
        });

        jLabel1.setText("Experiment Name:");

        jLabel24.setText("Sample Name:");

        sampleNameTxt.setText("Sample Name");
        sampleNameTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sampleNameTxtActionPerformed(evt);
            }
        });

        jLabel25.setText("Replicate Number:");

        replicateNumberTxt.setText("0");

        org.jdesktop.layout.GroupLayout jPanel13Layout = new org.jdesktop.layout.GroupLayout(jPanel13);
        jPanel13.setLayout(jPanel13Layout);
        jPanel13Layout.setHorizontalGroup(
            jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel1)
                    .add(jLabel24)
                    .add(jLabel25))
                .add(20, 20, 20)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sampleNameTxt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 797, Short.MAX_VALUE)
                    .add(txtExperiment, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 797, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, replicateNumberTxt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 797, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel13Layout.setVerticalGroup(
            jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel13Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(txtExperiment, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel1))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel24)
                    .add(sampleNameTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel13Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel25)
                    .add(replicateNumberTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel13, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                        .add(startButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 57, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(exitButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 57, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );
        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel13, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .add(9, 9, 9)
                .add(jPanel8, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(exitButton1, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(startButton))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Processing", jPanel1);

        jPanel5.setBorder(javax.swing.BorderFactory.createTitledBorder("Reporter Ions Configuration"));

        reporterIonConfigurationTable.setModel(new IonTableModel());
        reporterIonConfigurationTable.setName(""); // NOI18N
        jScrollPane1.setViewportView(reporterIonConfigurationTable);

        org.jdesktop.layout.GroupLayout jPanel5Layout = new org.jdesktop.layout.GroupLayout(jPanel5);
        jPanel5.setLayout(jPanel5Layout);
        jPanel5Layout.setHorizontalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 905, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel5Layout.setVerticalGroup(
            jPanel5Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel5Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 140, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel6.setBorder(javax.swing.BorderFactory.createTitledBorder("Isotope Correction"));

        isotopeCorrectionTable.setModel(new CorrectionTableModel());
        jScrollPane2.setViewportView(isotopeCorrectionTable);

        org.jdesktop.layout.GroupLayout jPanel6Layout = new org.jdesktop.layout.GroupLayout(jPanel6);
        jPanel6.setLayout(jPanel6Layout);
        jPanel6Layout.setHorizontalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 905, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel6Layout.setVerticalGroup(
            jPanel6Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel6Layout.createSequentialGroup()
                .addContainerGap()
                .add(jScrollPane2, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 94, Short.MAX_VALUE)
                .addContainerGap())
        );

        jPanel7.setBorder(javax.swing.BorderFactory.createTitledBorder("Configuration File"));

        txtConfigurationFileLocation.setEditable(false);

        browseConfigButton.setText("Browse");
        browseConfigButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                browseConfigButtonActionPerformed(evt);
            }
        });

        saveConfigButton.setText("Save");

        saveAsConfigButton.setText("Save As");

        org.jdesktop.layout.GroupLayout jPanel7Layout = new org.jdesktop.layout.GroupLayout(jPanel7);
        jPanel7.setLayout(jPanel7Layout);
        jPanel7Layout.setHorizontalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(txtConfigurationFileLocation, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 674, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(browseConfigButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(saveConfigButton)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(saveAsConfigButton)
                .addContainerGap())
        );

        jPanel7Layout.linkSize(new java.awt.Component[] {browseConfigButton, saveAsConfigButton, saveConfigButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel7Layout.setVerticalGroup(
            jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel7Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel7Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(txtConfigurationFileLocation, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(saveConfigButton)
                    .add(browseConfigButton)
                    .add(saveAsConfigButton))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        exitButton2.setText("Exit");
        exitButton2.setMaximumSize(new java.awt.Dimension(57, 23));
        exitButton2.setMinimumSize(new java.awt.Dimension(57, 23));
        exitButton2.setPreferredSize(new java.awt.Dimension(57, 23));
        exitButton2.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButton2ActionPerformed(evt);
            }
        });

        jPanel14.setBorder(javax.swing.BorderFactory.createTitledBorder("Method"));

        jLabel4.setText("Method Selected:");

        comboMethod2.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNames()));

        org.jdesktop.layout.GroupLayout jPanel14Layout = new org.jdesktop.layout.GroupLayout(jPanel14);
        jPanel14.setLayout(jPanel14Layout);
        jPanel14Layout.setHorizontalGroup(
            jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel4)
                .add(18, 18, 18)
                .add(comboMethod2, 0, 803, Short.MAX_VALUE)
                .addContainerGap())
        );
        jPanel14Layout.setVerticalGroup(
            jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel14Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel14Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel4)
                    .add(comboMethod2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel7, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel5, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel14, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(exitButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 57, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel14, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel5, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel6, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel7, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(exitButton2, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Configuration", jPanel4);

        jPanel10.setBorder(javax.swing.BorderFactory.createTitledBorder("Spectrum Analysis"));

        jLabel6.setText("Ion Selection Tolerance [m/z]:");

        ionToleranceTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        org.jdesktop.layout.GroupLayout jPanel10Layout = new org.jdesktop.layout.GroupLayout(jPanel10);
        jPanel10.setLayout(jPanel10Layout);
        jPanel10Layout.setHorizontalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel6)
                .add(18, 18, 18)
                .add(ionToleranceTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 102, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(651, Short.MAX_VALUE))
        );
        jPanel10Layout.setVerticalGroup(
            jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel10Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel10Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel6)
                    .add(ionToleranceTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        exitButton3.setText("Exit");
        exitButton3.setMaximumSize(new java.awt.Dimension(57, 23));
        exitButton3.setMinimumSize(new java.awt.Dimension(57, 23));
        exitButton3.setPreferredSize(new java.awt.Dimension(57, 23));
        exitButton3.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButton3ActionPerformed(evt);
            }
        });

        jPanel11.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification Analysis"));

        jLabel7.setText("FDR Threshold:");

        fdrThresholdTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        fdrThresholdTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                fdrThresholdTxtActionPerformed(evt);
            }
        });

        jLabel8.setText("%");

        jLabel9.setText("Maximal E-values:");

        jLabel16.setText("Mascot:");

        mascotEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        mascotEvalueTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                mascotEvalueTxtActionPerformed(evt);
            }
        });

        jLabel17.setText("OMSSA:");

        omssaEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel18.setText("X!Tandem:");

        xTandemEvalueTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel11.setText("Link to Quantification:");

        sameSpectra.setText("Same Spectra");
        sameSpectra.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                sameSpectraActionPerformed(evt);
            }
        });

        precursorMatching.setText("Precursor Matching:");
        precursorMatching.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                precursorMatchingActionPerformed(evt);
            }
        });

        jLabel19.setText("m/z tolerance:");

        jLabel20.setText("RT tolerance:");

        jLabel21.setText("ppm");

        rtTolTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                rtTolTxtActionPerformed(evt);
            }
        });

        jLabel22.setText("s");

        org.jdesktop.layout.GroupLayout jPanel11Layout = new org.jdesktop.layout.GroupLayout(jPanel11);
        jPanel11.setLayout(jPanel11Layout);
        jPanel11Layout.setHorizontalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel9)
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jLabel7)
                            .add(jPanel11Layout.createSequentialGroup()
                                .add(10, 10, 10)
                                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel18)
                                    .add(jLabel17)
                                    .add(jLabel16))))
                        .add(18, 18, 18)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, fdrThresholdTxt)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, mascotEvalueTxt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, omssaEvalueTxt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 124, Short.MAX_VALUE)
                            .add(org.jdesktop.layout.GroupLayout.TRAILING, xTandemEvalueTxt))))
                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel11Layout.createSequentialGroup()
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel8))
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(169, 169, 169)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                            .add(jLabel11)
                            .add(jPanel11Layout.createSequentialGroup()
                                .add(10, 10, 10)
                                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(precursorMatching)
                                    .add(sameSpectra)
                                    .add(jPanel11Layout.createSequentialGroup()
                                        .add(19, 19, 19)
                                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                            .add(jLabel19)
                                            .add(jLabel20))
                                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                                            .add(rtTolTxt)
                                            .add(mzTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 106, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))))
                                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                                    .add(jLabel22)
                                    .add(jLabel21))))))
                .add(198, 198, 198))
        );

        jPanel11Layout.linkSize(new java.awt.Component[] {fdrThresholdTxt, mascotEvalueTxt, omssaEvalueTxt, xTandemEvalueTxt}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel11Layout.setVerticalGroup(
            jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel11Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(jLabel11)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(sameSpectra)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(precursorMatching)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel19)
                            .add(mzTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel21))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(rtTolTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel20)
                            .add(jLabel22)))
                    .add(jPanel11Layout.createSequentialGroup()
                        .add(jLabel9)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel16)
                            .add(mascotEvalueTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel17)
                            .add(omssaEvalueTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel18)
                            .add(xTandemEvalueTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(18, 18, 18)
                        .add(jPanel11Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                            .add(jLabel7)
                            .add(fdrThresholdTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .add(jLabel8))))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel12.setBorder(javax.swing.BorderFactory.createTitledBorder("Identification Pre-Processing"));

        jLabel12.setText("nAA min:");

        nAaMinTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nAaMinTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nAaMinTxtActionPerformed(evt);
            }
        });

        jLabel13.setText("nAA max:");

        nAaMaxTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        nAaMaxTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nAaMaxTxtActionPerformed(evt);
            }
        });

        jLabel14.setText("Mass Deviation:");

        deltaMassTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        deltaMassTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                deltaMassTxtActionPerformed(evt);
            }
        });

        jLabel15.setText("ppm");

        org.jdesktop.layout.GroupLayout jPanel12Layout = new org.jdesktop.layout.GroupLayout(jPanel12);
        jPanel12.setLayout(jPanel12Layout);
        jPanel12Layout.setHorizontalGroup(
            jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel12Layout.createSequentialGroup()
                        .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                            .add(jPanel12Layout.createSequentialGroup()
                                .add(jLabel12)
                                .add(18, 18, 18)
                                .add(nAaMinTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                            .add(jPanel12Layout.createSequentialGroup()
                                .add(jLabel13)
                                .add(18, 18, 18)
                                .add(nAaMaxTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 121, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
                        .addContainerGap(700, Short.MAX_VALUE))
                    .add(jPanel12Layout.createSequentialGroup()
                        .add(jLabel14)
                        .add(18, 18, 18)
                        .add(deltaMassTxt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(jLabel15)
                        .add(676, 676, 676))))
        );

        jPanel12Layout.linkSize(new java.awt.Component[] {deltaMassTxt, nAaMaxTxt, nAaMinTxt}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel12Layout.linkSize(new java.awt.Component[] {jLabel12, jLabel13, jLabel14}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel12Layout.setVerticalGroup(
            jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel12Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel12)
                    .add(nAaMinTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel13)
                    .add(nAaMaxTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel12Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel14)
                    .add(deltaMassTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(jLabel15))
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout jPanel9Layout = new org.jdesktop.layout.GroupLayout(jPanel9);
        jPanel9.setLayout(jPanel9Layout);
        jPanel9Layout.setHorizontalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jPanel12, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(jPanel10, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, exitButton3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel11, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 941, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel9Layout.setVerticalGroup(
            jPanel9Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel9Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel10, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel12, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel11, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, 119, Short.MAX_VALUE)
                .add(exitButton3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        jTabbedPane1.addTab("Advanced Parameters", jPanel9);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 966, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 582, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void browseConfigButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_browseConfigButtonActionPerformed
        JFileChooser fileChooser = new JFileChooser(parent.getLastSelectedFolder());
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
                parent.setLastSelectedFolder(newFile.getPath());
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

    private void editIdFilesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editIdFilesJButtonActionPerformed
}//GEN-LAST:event_editIdFilesJButtonActionPerformed

    private void exitButton1ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButton1ActionPerformed
        this.dispose();
    }//GEN-LAST:event_exitButton1ActionPerformed

    private void exitButton2ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButton2ActionPerformed
        this.dispose();
    }//GEN-LAST:event_exitButton2ActionPerformed

    private void exitButton3ActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButton3ActionPerformed
        this.dispose();
    }//GEN-LAST:event_exitButton3ActionPerformed

    private void clearSpectraJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_clearSpectraJButtonActionPerformed
        mgfFiles = new ArrayList<File>();
        txtSpectraFileLocation.setText("Please select file(s)");
    }//GEN-LAST:event_clearSpectraJButtonActionPerformed

    private void addIdFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIdFilesButtonActionPerformed

        JFileChooser fileChooser = new JFileChooser(parent.getLastSelectedFolder());
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

                parent.setLastSelectedFolder(newFile.getPath());
            }
            if (idFiles.size() > 1) {
                for (File file : idFiles) {
                    if (file.getName().endsWith(".cps")) {
                        JOptionPane.showMessageDialog(this, "A PeptideShaker file must be imported alone.", "Wrong identification file.", JOptionPane.ERROR_MESSAGE);
                        idFiles = new ArrayList<File>();
                    }
                }
            }
            txtIdFileLocation.setText(idFiles.size() + " file(s) selected.");
            if (idFiles.size() == 1 && idFiles.get(0).getName().endsWith(".cps")) {
                importPeptideShakerFile(idFiles.get(0));
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

        JFileChooser fileChooser = new JFileChooser(parent.getLastSelectedFolder());
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

                parent.setLastSelectedFolder(newFile.getPath());
            }

            txtSpectraFileLocation.setText(mgfFiles.size() + " file(s) selected.");
        }
    }//GEN-LAST:event_addSpectraFilesJButtonActionPerformed

    private void editSpectraFilesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editSpectraFilesJButtonActionPerformed
    }//GEN-LAST:event_editSpectraFilesJButtonActionPerformed

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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addIdFilesButton;
    private javax.swing.JButton addSpectraFilesJButton;
    private javax.swing.JButton browseConfigButton;
    private javax.swing.JButton clearIdFilesJButton;
    private javax.swing.JButton clearSpectraJButton;
    private javax.swing.JComboBox comboMethod1;
    private javax.swing.JComboBox comboMethod2;
    private javax.swing.JTextField deltaMassTxt;
    private javax.swing.JButton editIdFilesJButton;
    private javax.swing.JButton editSpectraFilesJButton;
    private javax.swing.JButton exitButton1;
    private javax.swing.JButton exitButton2;
    private javax.swing.JButton exitButton3;
    private javax.swing.JTextField fdrThresholdTxt;
    private javax.swing.JTextField ionToleranceTxt;
    private javax.swing.JTable isotopeCorrectionTable;
    private javax.swing.JLabel jLabel1;
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
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel10;
    private javax.swing.JPanel jPanel11;
    private javax.swing.JPanel jPanel12;
    private javax.swing.JPanel jPanel13;
    private javax.swing.JPanel jPanel14;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JPanel jPanel5;
    private javax.swing.JPanel jPanel6;
    private javax.swing.JPanel jPanel7;
    private javax.swing.JPanel jPanel8;
    private javax.swing.JPanel jPanel9;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane3;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField mascotEvalueTxt;
    private javax.swing.JTextField mzTolTxt;
    private javax.swing.JTextField nAaMaxTxt;
    private javax.swing.JTextField nAaMinTxt;
    private javax.swing.JTextField omssaEvalueTxt;
    private javax.swing.JRadioButton precursorMatching;
    private javax.swing.JTextField replicateNumberTxt;
    private javax.swing.JTable reporterIonConfigurationTable;
    private javax.swing.JTextField rtTolTxt;
    private javax.swing.JRadioButton sameSpectra;
    private javax.swing.JTable sampleAssignmentTable;
    private javax.swing.JTextField sampleNameTxt;
    private javax.swing.JButton saveAsConfigButton;
    private javax.swing.JButton saveConfigButton;
    private javax.swing.JButton startButton;
    private javax.swing.JTextField txtConfigurationFileLocation;
    private javax.swing.JTextField txtExperiment;
    private javax.swing.JTextField txtIdFileLocation;
    private javax.swing.JTextField txtSpectraFileLocation;
    private javax.swing.JTextField xTandemEvalueTxt;
    // End of variables declaration//GEN-END:variables

    /**
     * returns the quantification method selected
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
        return quantification;
    }

    /**
     * Returns the reporter method corresponding to the given name
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
     * Imports the methods from the methods file
     */
    private void importMethods() {
        try {
            methodsFactory.importMethods(new File(METHODS_FILE));
        } catch (Exception e) {
            importMethodsError();
        }
    }

    /**
     * Loads the quantification preferences in the GUI
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
        fdrThresholdTxt.setText(quantificationPreferences.getFdrThreshold()*100 + "");
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
     * Sets the new quantification preferences
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
     * Method used to import a peptide shaker file
     * @param psFile a peptide shaker file
     */
    private void importPeptideShakerFile(File psFile) {
        UtilitiesInput importer = new UtilitiesInput();
        experiment = importer.importExperiment(psFile);
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
    }

    /**
     * Method called whenever an error was encountered while loading the methods
     */
    private void importMethodsError() {
        JOptionPane.showMessageDialog(this, "\"" + METHODS_FILE + "\" could not be found, please select a method file.", "No Spectra File Selected", JOptionPane.ERROR_MESSAGE);
        JFileChooser fileChooser = new JFileChooser(parent.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Methods file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showDialog(this.getParent(), "Add");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File newFile = fileChooser.getSelectedFile();
            try {
                methodsFactory.importMethods(newFile);
                parent.setLastSelectedFolder(newFile.getPath());
            } catch (IOException e) {
                JOptionPane.showMessageDialog(null,
                        "File " + METHODS_FILE + " not found in conf folder.",
                        "Methods file not found", JOptionPane.WARNING_MESSAGE);
                importMethodsError();
            } catch (XmlPullParserException e) {
                JOptionPane.showMessageDialog(null,
                        "An error occured while parsing " + METHODS_FILE + " at line " + e.getLineNumber() + ".",
                        "Parsing error", JOptionPane.WARNING_MESSAGE);
                importMethodsError();
            }
        }
    }

    /**
     * Methods which validates the user input (returns false in case of wrong input)
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
     * Sets the identification files to process
     *
     * @param files the identification files to process
     */
    public void setIdFiles(ArrayList<File> files) {
        idFiles = files;
        txtIdFileLocation.setText(idFiles.size() + " file(s) selected.");
    }

    /**
     * Sets the spectra files to process
     *
     * @param files the spectra files to process
     */
    public void setSpectraFiles(ArrayList<File> files) {
        mgfFiles = files;
        txtSpectraFileLocation.setText(mgfFiles.size() + " file(s) selected.");
    }

    /**
     * returns the last selected folder
     *
     * @return the last selected folder
     */
    public String getLastSelectedFolder() {
        return parent.getLastSelectedFolder();
    }

    /**
     * sets the last selected folder
     *
     * @param lastSelectedFolder the last selected folder
     */
    public void setLastSelectedFolder(String lastSelectedFolder) {
        parent.setLastSelectedFolder(lastSelectedFolder);
    }

    /**
     * Table model for the sample to reporter ion assignment
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
                    return "Normalization Reference";
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
     * Table model for the reporter ions table
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
     * Table model for the correction factors table
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
