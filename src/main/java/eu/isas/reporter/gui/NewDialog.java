package eu.isas.reporter.gui;

import com.compomics.util.FileAndFileFilter;
import com.compomics.util.Util;
import com.compomics.util.db.ObjectsCache;
import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Sample;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.SearchParameters;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.Quantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.parameters.ProcessingPreferencesDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.preferences.ProcessingPreferences;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.gui.settings.ReporterSettingsDialog;
import eu.isas.reporter.io.ProjectImporter;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.settings.ReporterPreferences;
import eu.isas.reporter.settings.ReporterSettings;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
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
    private CpsParent cpsParent;
    /**
     * The mgf files loaded.
     */
    private ArrayList<File> mgfFiles = new ArrayList<File>();
    /**
     * The reporter settings.
     */
    private ReporterSettings reporterSettings;
    /**
     * The display preferences for this project.
     */
    private DisplayPreferences displayPreferences;
    /**
     * The processing preferences.
     */
    private ProcessingPreferences processingPreferences = new ProcessingPreferences();
    /**
     * A simple progress dialog.
     */
    private ProgressDialogX progressDialog;
    /*
     * The welcome dialog parent, can be null.
     */
    private WelcomeDialog welcomeDialog;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();
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
     * The default reporter ion tolerance for TMT data.
     */
    private final double DEFAULT_REPORTER_ION_TOLERANCE_TMT = 0.0016;
    /**
     * The default reporter ion tolerance for iTRAQ data.
     */
    private final double DEFAULT_REPORTER_ION_TOLERANCE_ITRAQ = 0.05;

    /**
     * Constructor.
     *
     * @param reporterGui the reporter class
     * @param modal if the dialog is modal or not
     */
    public NewDialog(ReporterGUI reporterGui, boolean modal) {
        super(reporterGui, modal);

        this.reporterGui = reporterGui;
        this.welcomeDialog = null;

        methodsFile = Reporter.getMethodsFile();
        importMethods();

        initComponents();

        setUpGui();

        // load the user preferences
        loadDefaultPreferences();

        if (selectedMethod == null && methodsFactory.getMethodsNames() != null && methodsFactory.getMethodsNames().size() > 0) {
            reporterMethodComboBox.setSelectedItem(methodsFactory.getReporterMethod(methodsFactory.getMethodsNames().get(0)));
        }

        reporterMethodComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedMethod = methodsFactory.getReporterMethod((String) reporterMethodComboBox.getSelectedItem());
                reagents = selectedMethod.getReagentsSortedByMass();
                refresh();
            }
        });

        reporterGui.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));

        refresh();

        setLocationRelativeTo(reporterGui);
        setVisible(true);
    }

    /**
     * Constructor.
     *
     * @param reporterGui the reporter class
     * @param welcomeDialog the welcome dialog parent frame
     * @param modal if the dialog is modal or not
     */
    public NewDialog(WelcomeDialog welcomeDialog, ReporterGUI reporterGui, boolean modal) {
        super(welcomeDialog, modal);

        this.reporterGui = reporterGui;
        this.welcomeDialog = welcomeDialog;

        methodsFile = Reporter.getMethodsFile();
        importMethods();

        initComponents();

        setUpGui();

        // load the user preferences
        loadDefaultPreferences();

        if (selectedMethod == null && methodsFactory.getMethodsNames() != null && methodsFactory.getMethodsNames().size() > 0) {
            reporterMethodComboBox.setSelectedItem(methodsFactory.getMethodsNames().get(0));
        }

        reporterMethodComboBox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                selectedMethod = methodsFactory.getReporterMethod((String) reporterMethodComboBox.getSelectedItem());
                reagents = selectedMethod.getReagentsSortedByMass();
                refresh();
            }
        });

        reporterGui.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));

        refresh();

        setLocationRelativeTo(welcomeDialog);
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

        // disable the user to drag column headers to reorder columns
        sampleAssignmentTable.getTableHeader().setReorderingAllowed(false);

        processingTxt.setText(processingPreferences.getnThreads() + " cores");
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
        editProcessingButton = new javax.swing.JButton();
        processingTxt = new javax.swing.JTextField();
        processingLbl = new javax.swing.JLabel();
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

        idFilesLabel.setText("Project File");

        txtIdFileLocation.setEditable(false);
        txtIdFileLocation.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        txtIdFileLocation.setText("Please import a project");

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
                    .add(txtSpectraFileLocation, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 531, Short.MAX_VALUE)
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

        reporterMethodComboBox.setModel(new DefaultComboBoxModel(methodsFactory.getMethodsNamesAsArray()));
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
                    .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 734, Short.MAX_VALUE)
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
                .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
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

        editProcessingButton.setText("Edit");
        editProcessingButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                editProcessingButtonActionPerformed(evt);
            }
        });

        processingTxt.setEditable(false);
        processingTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        processingTxt.setText("Default Settings");

        processingLbl.setText("Processing");

        org.jdesktop.layout.GroupLayout advancedSettingsPanelLayout = new org.jdesktop.layout.GroupLayout(advancedSettingsPanel);
        advancedSettingsPanel.setLayout(advancedSettingsPanelLayout);
        advancedSettingsPanelLayout.setHorizontalGroup(
            advancedSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(advancedSettingsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(advancedSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(advancedSettingsPanelLayout.createSequentialGroup()
                        .add(quantPreferencesLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(quantificationPreferencesTxt, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 532, Short.MAX_VALUE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editQuantPrefsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(advancedSettingsPanelLayout.createSequentialGroup()
                        .add(processingLbl, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(processingTxt)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(editProcessingButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
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
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(advancedSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(processingLbl)
                    .add(processingTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(editProcessingButton))
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

        reporterPublicationLabel.setText("<html>Please cite Reporter as <a href=\"http://compomics.github.io/projects/reporter.html\">http://compomics.github.io/projects/reporter.html</a></html>");
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
                    .add(org.jdesktop.layout.GroupLayout.LEADING, backgroundPanelLayout.createSequentialGroup()
                        .add(aboutButton)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .add(reporterPublicationLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 422, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
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
                .add(18, 18, 18)
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
            .add(backgroundPanel, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Opens a file chooser for adding identification files.
     *
     * @param evt
     */
    private void addIdFilesButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addIdFilesButtonActionPerformed

        String cpsFileFilterDescription = "PeptideShaker (.cpsx)";
        //String zipFileFilterDescription = "Zipped PeptideShaker (.zip)"; // @TODO: support zip files
        String lastSelectedFolderPath = reporterGui.getLastSelectedFolder().getLastSelectedFolder();
//        FileAndFileFilter selectedFileAndFilter = Util.getUserSelectedFile(this, new String[]{".cpsx", ".zip"}, 
//                new String[]{cpsFileFilterDescription, zipFileFilterDescription}, "Select Identification File(s)", lastSelectedFolderPath, null, true, false, false, 0);
        FileAndFileFilter selectedFileAndFilter = Util.getUserSelectedFile(this, new String[]{".cpsx"},
                new String[]{cpsFileFilterDescription}, "Select Identification File(s)", lastSelectedFolderPath, null, true, false, false, 0);

        if (selectedFileAndFilter != null) {

            File selectedFile = selectedFileAndFilter.getFile();
            reporterGui.getLastSelectedFolder().setLastSelectedFolder(selectedFile.getParent());

            if (selectedFile.getName().endsWith(".zip")) {
                //importPeptideShakerZipFile(selectedFile); // @TODO: support zip files
            } else if (selectedFile.getName().endsWith(".cpsx")) {
                importPeptideShakerFile(selectedFile);
//                reporterGui.getUserPreferences().addRecentProject(selectedFile); // @TOOD: implement me?
//                reporterGui.updateRecentProjectsList();
            } else {
                JOptionPane.showMessageDialog(this, "Not a PeptideShaker file (.cpsx).", "Unsupported File.", JOptionPane.WARNING_MESSAGE);
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
        final JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Spectrum File(s)");
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

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            if (welcomeDialog != null) {
                progressDialog = new ProgressDialogX(welcomeDialog, reporterGui,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
            } else {
                progressDialog = new ProgressDialogX(this, reporterGui,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
            }

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setWaitingText("Loading Spectrum Files. Please Wait...");

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
                        ArrayList<File> newFiles = new ArrayList<File>();
                        for (File newFile : fileChooser.getSelectedFiles()) {
                            if (newFile.isDirectory()) {
                                File[] tempFiles = newFile.listFiles();
                                for (File file : tempFiles) {
                                    if (file.getName().toLowerCase().endsWith(".mgf")) {
                                        if (!mgfFiles.contains(file) && !newFiles.contains(file)) {
                                            newFiles.add(file);
                                        }
                                    }
                                }
                            } else if (newFile.getName().toLowerCase().endsWith(".mgf")) {
                                if (!mgfFiles.contains(newFile) && !newFiles.contains(newFile)) {
                                    newFiles.add(newFile);
                                }
                            }

                            reporterGui.getLastSelectedFolder().setLastSelectedFolder(newFile.getPath());
                        }

                        int cpt = 0;
                        for (File newFile : newFiles) {
                            progressDialog.setWaitingText("Loading Spectrum Files (" + ++cpt + " of " + newFiles.size() + "). Please Wait...");
                            mgfFiles.add(newFile);
                            cpsParent.getProjectDetails().addSpectrumFile(newFile);
                            spectrumFactory.addSpectra(newFile, progressDialog);
                        }

                        progressDialog.setRunFinished();

                    } catch (IOException e) {
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(NewDialog.this, "An error occurred while reading the mgf file.", "Mgf Error", JOptionPane.WARNING_MESSAGE);
                    }
                    verifySpectrumFiles();
                }
            }.start();
        }
    }//GEN-LAST:event_addSpectraFilesJButtonActionPerformed

    /**
     * Clear the sample names and update the method table.
     *
     * @param evt
     */
    private void reporterMethodComboBoxActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_reporterMethodComboBoxActionPerformed
        sampleNames = new HashMap<String, String>();
        selectedMethod = methodsFactory.getReporterMethod((String) reporterMethodComboBox.getSelectedItem());
        reagents = selectedMethod.getReagentsSortedByMass();

        if (cpsParent != null) {
            // update the reporter settings
            IdentificationParameters identificationParameters = cpsParent.getIdentificationParameters();
            ProjectImporter.getDefaultReporterSettings(selectedMethod, identificationParameters, reporterSettings);
        }

        refresh();
    }//GEN-LAST:event_reporterMethodComboBoxActionPerformed

    /**
     * Opens a file chooser for selecting the database FASTA file.
     *
     * @param evt
     */
    private void addDbButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addDbButtonActionPerformed

//        if (searchParameters != null && searchParameters.getFastaFile() != null && searchParameters.getFastaFile().exists()) {
//            fileChooser = new JFileChooser(searchParameters.getFastaFile());
//        } else {
//            fileChooser = new JFileChooser(peptideShakerGUI.getLastSelectedFolder());
//        }
        final JFileChooser fileChooser = new JFileChooser(reporterGui.getLastSelectedFolder().getLastSelectedFolder());

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
            if (welcomeDialog != null) {
                progressDialog = new ProgressDialogX(welcomeDialog, reporterGui,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
            } else {
                progressDialog = new ProgressDialogX(this, reporterGui,
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                        Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
            }

            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setWaitingText("Loading Fasta File. Please Wait...");

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
                    File fastaFile = fileChooser.getSelectedFile();
                    reporterGui.getLastSelectedFolder().setLastSelectedFolder(fastaFile.getAbsolutePath());
                    try {
                        SequenceFactory.getInstance().loadFastaFile(fastaFile, progressDialog);

                        progressDialog.setRunFinished();

                    } catch (Exception e) {
                        progressDialog.setRunFinished();
                        e.printStackTrace();
                        JOptionPane.showMessageDialog(NewDialog.this, "An error occurred while reading the mgf file.", "Mgf Error", JOptionPane.WARNING_MESSAGE);
                    }
                    verifyFastaFile();
                }
            }.start();
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
        ReporterSettingsDialog reporterSettingsDialog = new ReporterSettingsDialog(this, reporterSettings, cpsParent.getIdentificationParameters().getSearchParameters().getPtmSettings(), getSelectedMethod(), true);
        ReporterSettings newSettings = reporterSettingsDialog.getReporterSettings();
        if (!reporterSettingsDialog.isCanceled()) {
            reporterSettings = newSettings;
            quantificationPreferencesTxt.setText("Custom Settings");
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
        BareBonesBrowserLaunch.openURL("http://compomics.github.io/projects/reporter.html");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    /**
     * Open the Reporter publication.
     *
     * @param evt
     */
    private void reporterPublicationLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_reporterPublicationLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://compomics.github.io/projects/reporter.html");
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

            if (welcomeDialog != null) {
                welcomeDialog.setVisible(false);
            }

            reporterGui.createNewProject(cpsParent, reporterSettings, reporterIonQuantification, processingPreferences, displayPreferences);
            dispose();
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    /**
     * Edit the processing preferences.
     *
     * @param evt
     */
    private void editProcessingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editProcessingButtonActionPerformed
        ProcessingPreferencesDialog processingPreferencesDialog = new ProcessingPreferencesDialog(this, reporterGui, processingPreferences, true);
        if (!processingPreferencesDialog.isCanceled()) {
            processingPreferences = processingPreferencesDialog.getProcessingPreferences();
            processingTxt.setText(processingPreferences.getnThreads() + " cores");
        }
    }//GEN-LAST:event_editProcessingButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton aboutButton;
    private javax.swing.JButton addDbButton;
    private javax.swing.JButton addIdFilesButton;
    private javax.swing.JButton addSpectraFilesJButton;
    private javax.swing.JPanel advancedSettingsPanel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JLabel databaseFileLabel;
    private javax.swing.JButton editProcessingButton;
    private javax.swing.JButton editQuantPrefsButton;
    private javax.swing.JTextField fastaTxt;
    private javax.swing.JPanel fileSelectiontPanel;
    private javax.swing.JLabel idFilesLabel;
    private javax.swing.JButton loadButton;
    private javax.swing.JButton methodSettingsButton;
    private javax.swing.JLabel processingLbl;
    private javax.swing.JTextField processingTxt;
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
     * Verifies that the FASTA file is loaded and updates the corresponding text
     * box.
     *
     * @return a boolean indicating that the FASTA file is loaded
     */
    private boolean verifyFastaFile() {
        SequenceFactory sequenceFactory = SequenceFactory.getInstance();
        File fastaFile = sequenceFactory.getCurrentFastaFile();
        if (fastaFile != null) {
            fastaTxt.setText(fastaFile.getName());
            return true;
        }
        String errorText = "FASTA file not found or incorrectly loaded:\n" + getSearchParameters().getFastaFile().getName()
                + "\nPlease locate it manually.";
        JOptionPane.showMessageDialog(this,
                errorText,
                "Spectrum File(s) Not Found", JOptionPane.WARNING_MESSAGE);
        return false;
    }

    /**
     * Verifies that all spectrum files are loaded and updates the corresponding
     * text box.
     *
     * @return a boolean indicating that all spectrum files are loaded
     */
    private boolean verifySpectrumFiles() {
        String missing = "";
        int nMissing = 0;
        for (String spectrumFileName : cpsParent.getIdentification().getSpectrumFiles()) { // @TODO: check alternative locations as for ps
            boolean found = false;
            for (File spectrumFile : mgfFiles) {
                if (spectrumFile.getName().equals(spectrumFileName)) {
                    found = true;
                }
            }
            if (!found) {
                nMissing++;
                missing += spectrumFileName + "\n";
            }
        }
        if (nMissing > 0) {
            if (nMissing < 11) {
                JOptionPane.showMessageDialog(this, "Spectrum file(s) not found:\n" + missing
                        + "\nPlease locate them manually.", "Spectrum File Not Found", JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(this, "Spectrum files not found.\n"
                        + "Please locate them manually.", "Spectrum File Not Found", JOptionPane.WARNING_MESSAGE);
            }
        }
        if (mgfFiles.size() > 1) {
            txtSpectraFileLocation.setText(mgfFiles.size() + " files loaded"); //@TODO: allow editing
        } else if (mgfFiles.size() == 1) {
            txtSpectraFileLocation.setText(mgfFiles.get(0).getName()); //@TODO: allow editing
        } else {
            txtSpectraFileLocation.setText("");
        }
        return nMissing == 0;
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
     * Method used to import a cps file.
     *
     * @param psFile a cps file
     */
    private void importPeptideShakerFile(final File psFile) {

        if (welcomeDialog != null) {
            progressDialog = new ProgressDialogX(welcomeDialog, reporterGui,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        } else {
            progressDialog = new ProgressDialogX(this, reporterGui,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        }

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

                cpsParent = new CpsParent(Reporter.getMatchesFolder());
                cpsParent.setCpsFile(psFile);
                ProjectImporter projectImporter = new ProjectImporter(NewDialog.this);
                try {
                    projectImporter.importPeptideShakerProject(cpsParent, progressDialog);
                    projectImporter.importReporterProject(cpsParent, progressDialog);
                } catch (OutOfMemoryError error) {
                    System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
                    error.printStackTrace();
                    String errorText = "PeptideShaker used up all the available memory and had to be stopped.<br>"
                            + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                            + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                            + "Java Options). See also <a href=\"http://compomics.github.io/compomics-utilities/wiki/javatroubleshooting.html\">JavaTroubleShooting</a>.";
                    JOptionPane.showMessageDialog(NewDialog.this,
                            errorText,
                            "Out of Memory", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (EOFException e) {
                    e.printStackTrace();
                    String errorText = "An error occurred while reading:\n" + psFile + ".\n\n"
                            + "The file is corrupted and cannot be opened anymore.";
                    JOptionPane.showMessageDialog(NewDialog.this,
                            errorText,
                            "Incomplete file", JOptionPane.ERROR_MESSAGE);
                    return;
                } catch (Exception e) {
                    e.printStackTrace();
                    String errorText = "An error occurred while reading:\n" + psFile + ".\n\n"
                            + "Please verify that the PeptideShaker version used to create\n"
                            + "the file is compatible with your version of Reporter.";
                    JOptionPane.showMessageDialog(NewDialog.this,
                            errorText,
                            "Error", JOptionPane.ERROR_MESSAGE);
                    return;
                }

                if (progressDialog.isRunCanceled()) {
                    progressDialog.dispose();
                    return;
                }

                loadButton.setEnabled(true);
                editQuantPrefsButton.setEnabled(true);
                verifySpectrumFiles();
                verifyFastaFile();
                txtIdFileLocation.setText(cpsParent.getCpsFile().getName());

                // load project specific ptms
                String error = PeptideShaker.loadModifications(getSearchParameters());
                if (error != null) {
                    JOptionPane.showMessageDialog(NewDialog.this,
                            error,
                            "PTM Definition Changed", JOptionPane.WARNING_MESSAGE);
                }

                // set up cache
                cache = new ObjectsCache();
                cache.setAutomatedMemoryManagement(true);

                // set up quantification settings
                reporterSettings = projectImporter.getReporterSettings();
                reporterIonQuantification = projectImporter.getReporterIonQuantification();
                selectedMethod = reporterIonQuantification.getReporterMethod();
                if (selectedMethod == null) {
                    // Default to the first one if not found
                    selectedMethod = methodsFactory.getReporterMethod(methodsFactory.getMethodsNames().get(0));
                }
                reagents = selectedMethod.getReagentsSortedByMass();

                // get the display preferences
                displayPreferences = projectImporter.getDisplayPreferences();

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
        return cpsParent.getProjectDetails();
    }

    /**
     * Returns the search parameters.
     *
     * @return the search parameters
     */
    public SearchParameters getSearchParameters() {
        return cpsParent.getIdentificationParameters().getSearchParameters();
    }

    /**
     * Returns the experiment.
     *
     * @return the experiment
     */
    public MsExperiment getExperiment() {
        return cpsParent.getExperiment();
    }

    /**
     * Returns the sample.
     *
     * @return the sample
     */
    public Sample getSample() {
        return cpsParent.getSample();
    }

    /**
     * Returns the replicate number.
     *
     * @return the replicateNumber
     */
    public int getReplicateNumber() {
        return cpsParent.getReplicateNumber();
    }

    /**
     * Returns the identification displayed.
     *
     * @return the identification displayed
     */
    public Identification getIdentification() {
        return cpsParent.getIdentification();
    }

    /**
     * Method called whenever an error was encountered while loading the
     * methods.
     */
    private void importMethodsError() {

        JOptionPane.showMessageDialog(this, "Default reporter methods file could not be parsed, please select a method file.", "No Spectrum File Selected", JOptionPane.WARNING_MESSAGE);
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
                        "File " + newFile + " could not be parsed.",
                        "Methods file error", JOptionPane.WARNING_MESSAGE);
                importMethodsError();
            } catch (XmlPullParserException e) {
                JOptionPane.showMessageDialog(this,
                        "An error occurred while parsing " + newFile + " at line " + e.getLineNumber() + ".",
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
        // @TODO: validate that the project has been loaded

        // warning for tmt data with low mass accuracy
        if (selectedMethod.getName().contains("TMT")) {
            if (reporterSettings.getReporterIonSelectionSettings().getReporterIonsMzTolerance() > ProjectImporter.DEFAULT_REPORTER_ION_TOLERANCE_TMT) {
                JOptionPane.showMessageDialog(this,
                        "TMT quantification requires high resolution spectra. Please check\n"
                        + "the Reporter Ions Tolerance in the Quantification Settings.",
                        "TMT Resolution Warning", JOptionPane.WARNING_MESSAGE);
                return false;
            }
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
                        "File Input Error", JOptionPane.WARNING_MESSAGE);
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
            if (selectedMethod == null || cpsParent == null) {
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
        return cpsParent;
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
