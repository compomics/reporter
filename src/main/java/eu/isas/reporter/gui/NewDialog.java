package eu.isas.reporter.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.ions.impl.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.io.mass_spectrometry.MsFileHandler;
import com.compomics.util.experiment.io.mass_spectrometry.cms.CmsFolder;
import com.compomics.util.experiment.quantification.Quantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.file_handling.FileAndFileFilter;
import com.compomics.util.gui.file_handling.FileChooserUtil;
import static com.compomics.util.gui.parameters.identification.search.SequenceDbDetailsDialog.lastFolderKey;
import com.compomics.util.gui.parameters.tools.ProcessingParametersDialog;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.io.IoUtil;
import com.compomics.util.io.file.LastSelectedFolder;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.parameters.tools.ProcessingParameters;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.preferences.ProjectDetails;
import eu.isas.peptideshaker.utils.PsdbParent;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.gui.settings.ReporterSettingsDialog;
import eu.isas.reporter.io.ProjectImporter;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.settings.ReporterPreferences;
import eu.isas.reporter.settings.ReporterSettings;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.TreeSet;
import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
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
    private ReporterGUI reporterGUI;
    /**
     * The method selected.
     */
    private ReporterMethod selectedMethod = null;
    /**
     * The psdb parent used to manage the data.
     */
    private PsdbParent psdbParent;
    /**
     * The spectrum files loaded.
     */
    private ArrayList<File> spectrumFiles = new ArrayList<File>();
    /**
     * The FASTA file.
     */
    private File fastaFile;
    /**
     * The reporter settings.
     */
    private ReporterSettings reporterSettings;
    /**
     * The display preferences for this project.
     */
    private DisplayPreferences displayPreferences;
    /**
     * The processing parameters.
     */
    private ProcessingParameters processingParameters = new ProcessingParameters();
    /**
     * A simple progress dialog.
     */
    private ProgressDialogX progressDialog;
    /*
     * The welcome dialog parent, can be null.
     */
    private WelcomeDialog welcomeDialog;
    /**
     * List of all sample names.
     */
    private HashMap<String, String> sampleNames = new HashMap<>();
    /**
     * List of reagents used in this reporter method
     */
    private ArrayList<String> reagents = new ArrayList<>();
    /**
     * List of control samples.
     */
    private ArrayList<String> controlSamples = new ArrayList<>();
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
     * The sample assignment table column header tooltips.
     */
    private ArrayList<String> sampleAssignmentTableToolTips;
    /**
     * The project details.
     */
    private ProjectDetails projectDetails = new ProjectDetails();
    /**
     * The handler for mass spectrometry files.
     */
    private MsFileHandler msFileHandler = new MsFileHandler();

    /**
     * Constructor.
     *
     * @param reporterGUI the reporter class
     * @param modal if the dialog is modal or not
     */
    public NewDialog(ReporterGUI reporterGUI, boolean modal) {
        super(reporterGUI, modal);

        this.reporterGUI = reporterGUI;
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
            @Override
            public void actionPerformed(ActionEvent e) {
                selectedMethod = methodsFactory.getReporterMethod((String) reporterMethodComboBox.getSelectedItem());
                reagents = selectedMethod.getReagentsSortedByMass();
                refresh();
            }
        });

        reporterGUI.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")));

        refresh();

        setLocationRelativeTo(reporterGUI);
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

        this.reporterGUI = reporterGui;
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
            @Override
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

        // table header tooltips
        sampleAssignmentTableToolTips = new ArrayList<String>();
        sampleAssignmentTableToolTips.add(null);
        sampleAssignmentTableToolTips.add("The reporter label");
        sampleAssignmentTableToolTips.add("The sample name");
        sampleAssignmentTableToolTips.add("The reference sample(s)");

        // make sure that the scroll panes are see-through
        sampleAssignmentJScrollPane.getViewport().setOpaque(false);

        // centrally align the comboboxes
        reporterMethodComboBox.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        // disable the user to drag column headers to reorder columns
        sampleAssignmentTable.getTableHeader().setReorderingAllowed(false);

        processingTxt.setText(processingParameters.getnThreads() + " cores");
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
                new ImageIcon(this.getClass().getResource("/icons/selected_green-new.png")),
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
        sampleAssignmentTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) sampleAssignmentTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        reporterMethodLabel = new javax.swing.JLabel();
        reporterMethodComboBox = new javax.swing.JComboBox();
        methodSettingsButton = new javax.swing.JButton();
        orderSettingsPanel = new javax.swing.JPanel();
        moveUpButton = new javax.swing.JButton();
        moveTopButton = new javax.swing.JButton();
        moveDownButton = new javax.swing.JButton();
        moveBottomButton = new javax.swing.JButton();
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
        sampleAssignmentTable.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        sampleAssignmentTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                sampleAssignmentTableMouseReleased(evt);
            }
        });
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

        orderSettingsPanel.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        orderSettingsPanel.setOpaque(false);

        moveUpButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowUp_grey.png"))); // NOI18N
        moveUpButton.setToolTipText("Move Up");
        moveUpButton.setBorderPainted(false);
        moveUpButton.setContentAreaFilled(false);
        moveUpButton.setEnabled(false);
        moveUpButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowUp.png"))); // NOI18N
        moveUpButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveUpButtonActionPerformed(evt);
            }
        });

        moveTopButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowUpTop_grey.png"))); // NOI18N
        moveTopButton.setToolTipText("Move to Top");
        moveTopButton.setBorderPainted(false);
        moveTopButton.setContentAreaFilled(false);
        moveTopButton.setEnabled(false);
        moveTopButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowUpTop.png"))); // NOI18N
        moveTopButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveTopButtonActionPerformed(evt);
            }
        });

        moveDownButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowDown_grey.png"))); // NOI18N
        moveDownButton.setToolTipText("Move Down");
        moveDownButton.setBorderPainted(false);
        moveDownButton.setContentAreaFilled(false);
        moveDownButton.setEnabled(false);
        moveDownButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowDown.png"))); // NOI18N
        moveDownButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveDownButtonActionPerformed(evt);
            }
        });

        moveBottomButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowDownBottom_grey.png"))); // NOI18N
        moveBottomButton.setToolTipText("Move to Bottom");
        moveBottomButton.setBorderPainted(false);
        moveBottomButton.setContentAreaFilled(false);
        moveBottomButton.setEnabled(false);
        moveBottomButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowDownBottom.png"))); // NOI18N
        moveBottomButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                moveBottomButtonActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout orderSettingsPanelLayout = new org.jdesktop.layout.GroupLayout(orderSettingsPanel);
        orderSettingsPanel.setLayout(orderSettingsPanelLayout);
        orderSettingsPanelLayout.setHorizontalGroup(
            orderSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, orderSettingsPanelLayout.createSequentialGroup()
                .add(0, 0, Short.MAX_VALUE)
                .add(orderSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, moveUpButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, moveDownButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, moveBottomButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 36, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                    .add(moveTopButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 36, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
        );
        orderSettingsPanelLayout.setVerticalGroup(
            orderSettingsPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, orderSettingsPanelLayout.createSequentialGroup()
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .add(moveTopButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(moveUpButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(moveDownButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(moveBottomButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 20, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        org.jdesktop.layout.GroupLayout samplePanelLayout = new org.jdesktop.layout.GroupLayout(samplePanel);
        samplePanel.setLayout(samplePanelLayout);
        samplePanelLayout.setHorizontalGroup(
            samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, samplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .add(samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(samplePanelLayout.createSequentialGroup()
                        .add(reporterMethodLabel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 120, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                        .add(reporterMethodComboBox, 0, 533, Short.MAX_VALUE))
                    .add(sampleAssignmentJScrollPane))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(samplePanelLayout.createSequentialGroup()
                        .add(methodSettingsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 66, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addContainerGap())
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, samplePanelLayout.createSequentialGroup()
                        .add(orderSettingsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .add(18, 18, 18))))
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
                .add(samplePanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(sampleAssignmentJScrollPane, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 205, Short.MAX_VALUE)
                    .add(samplePanelLayout.createSequentialGroup()
                        .add(0, 0, Short.MAX_VALUE)
                        .add(orderSettingsPanel, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)))
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

        String psdbFileFilterDescription = "PeptideShaker Database (.psdb)";
        String zipFileFilterDescription = "Zipped PeptideShaker (.zip)";
        String lastSelectedFolderPath = reporterGUI.getLastSelectedFolder().getLastSelectedFolder();
        FileAndFileFilter selectedFileAndFilter = FileChooserUtil.getUserSelectedFile(this, new String[]{".psdb", ".zip"},
                new String[]{psdbFileFilterDescription, zipFileFilterDescription}, "Open PeptideShaker Project", lastSelectedFolderPath, null, true, false, false, 0);

        if (selectedFileAndFilter != null) {

            File selectedFile = selectedFileAndFilter.getFile();
            reporterGUI.getLastSelectedFolder().setLastSelectedFolder(selectedFile.getParent());

            if (selectedFile.getName().toLowerCase().endsWith(".zip")) {
//                setVisible(false); // @TODO: support zip files
//                reporterGUI.setVisible(true);
//                reporterGUI.importPeptideShakerZipFile(selectedFile);
//                dispose();
            } else if (selectedFile.getName().toLowerCase().endsWith(".psdb")) {
                importPeptideShakerFile(selectedFile);
//                reporterGUI.getUserParameters().addRecentProject(selectedFile); // @TOOD: implement me?
//                reporterGUI.updateRecentProjectsList();
                LastSelectedFolder lastSelectedFolder = reporterGUI.getLastSelectedFolder();
                lastSelectedFolder.setLastSelectedFolder(selectedFile.getAbsolutePath());
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Not a PeptideShaker file (.psdb).",
                        "Unsupported File.",
                        JOptionPane.WARNING_MESSAGE
                );
            }
        }
    }//GEN-LAST:event_addIdFilesButtonActionPerformed

    /**
     * Open a file chooser for adding spectrum files.
     *
     * @param evt
     */
    private void addSpectraFilesJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addSpectraFilesJButtonActionPerformed

        JFileChooser fileChooser = new JFileChooser(reporterGUI.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Spectrum File(s)");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setMultiSelectionEnabled(true);

        FileFilter filter = new FileFilter() {
            @Override
            public boolean accept(File myFile) {
                return myFile.getName().toLowerCase().endsWith(".mgf")
                        || myFile.getName().toLowerCase().endsWith(".mgf.gz")
                        || myFile.getName().toLowerCase().endsWith(".mzml")
                        || myFile.getName().toLowerCase().endsWith(".mzml.gz")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "mgf or mzML (.mgf, .mg.gz, .mzml, .mzml.gz)";
            }
        };

        fileChooser.setFileFilter(filter);
        int returnVal = fileChooser.showDialog(this, "Add");

        if (returnVal == JFileChooser.APPROVE_OPTION) {

            // get the files
            ArrayList<File> selectedFiles = new ArrayList<>();

            for (File newFile : fileChooser.getSelectedFiles()) {

                if (newFile.isDirectory()) {

                    File[] tempFiles = newFile.listFiles();

                    for (File file : tempFiles) {

                        if (file.getName().toLowerCase().endsWith(".mgf")
                                || file.getName().toLowerCase().endsWith(".mgf.gz")
                                || file.getName().toLowerCase().endsWith(".mzml")
                                || file.getName().toLowerCase().endsWith(".mzml.gz")) {

                            selectedFiles.add(file);

                        }
                    }
                } else {

                    selectedFiles.add(newFile);

                }
            }

            // Load the files
            progressDialog = new ProgressDialogX(
                    this,
                    reporterGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/peptide-shaker-orange.gif")),
                    true
            );
            progressDialog.setPrimaryProgressCounterIndeterminate(true);
            progressDialog.setTitle("Loading Files. Please Wait...");

            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        progressDialog.setVisible(true);
                    } catch (IndexOutOfBoundsException e) {
                        // ignore
                    }
                }
            }, "ProgressDialog").start();

            new Thread("loadingThread") {
                @Override
                public void run() {

                    boolean allLoaded = true;

                    for (File file : selectedFiles) {

                        try {

                            File folder = CmsFolder.getParentFolder() == null ? file.getParentFile() : new File(CmsFolder.getParentFolder());

                            msFileHandler.register(file, folder, progressDialog);

                        } catch (Exception e) {

                            progressDialog.setRunCanceled();

                            allLoaded = false;

                            JOptionPane.showMessageDialog(
                                    null,
                                    "An error occurred while reading the following file.\n"
                                    + file.getAbsolutePath() + "\n\nError:\n" + e.getLocalizedMessage(),
                                    "File error",
                                    JOptionPane.ERROR_MESSAGE
                            );

                            e.printStackTrace();
                        }
                    }

                    progressDialog.setRunFinished();

                    if (allLoaded) {

                        spectrumFiles.addAll(selectedFiles);
                        txtSpectraFileLocation.setText(spectrumFiles.size() + " file(s) selected");
                        validateInput();

                    }

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
        sampleNames = new HashMap<>();
        selectedMethod = methodsFactory.getReporterMethod((String) reporterMethodComboBox.getSelectedItem());
        reagents = selectedMethod.getReagentsSortedByMass();

        if (psdbParent != null) {
            // update the reporter settings
            IdentificationParameters identificationParameters = psdbParent.getIdentificationParameters();
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

        File startLocation = fastaFile;

        if (startLocation == null && reporterGUI.getUtilitiesUserParameters().getDbFolder() != null && reporterGUI.getUtilitiesUserParameters().getDbFolder().exists()) {
            startLocation = reporterGUI.getUtilitiesUserParameters().getDbFolder();
        }

        if (startLocation == null) {
            startLocation = new File(reporterGUI.getLastSelectedFolder().getLastSelectedFolder());
        }

        JFileChooser fc = new JFileChooser(startLocation);

        FileFilter filter = new FileFilter() {

            @Override
            public boolean accept(File myFile) {

                return myFile.getName().toLowerCase().endsWith("fasta")
                        || myFile.isDirectory();
            }

            @Override
            public String getDescription() {
                return "FASTA (.fasta)";
            }

        };

        fc.setFileFilter(filter);
        int result = fc.showOpenDialog(this);

        if (result == JFileChooser.APPROVE_OPTION) {

            fastaFile = fc.getSelectedFile();
            File folder = fastaFile.getParentFile();
            reporterGUI.getUtilitiesUserParameters().setDbFolder(folder);
            reporterGUI.getLastSelectedFolder().setLastSelectedFolder(lastFolderKey, folder.getAbsolutePath());

            fastaTxt.setText(fastaFile.getName());
            psdbParent.getProjectDetails().setFastaFile(fastaFile);

            if (fastaFile.getName().contains(" ")) {
                JOptionPane.showMessageDialog(this, "Your FASTA file name contains white space and ougth to be renamed.", "File Name Warning", JOptionPane.WARNING_MESSAGE);
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
        ReporterSettingsDialog reporterSettingsDialog = new ReporterSettingsDialog(this, reporterSettings, psdbParent.getIdentificationParameters().getSearchParameters().getModificationParameters(), getSelectedMethod(), true);
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
        BareBonesBrowserLaunch.openURL("https://compomics.github.io/projects/reporter.html");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_aboutButtonActionPerformed

    /**
     * Open the Reporter publication.
     *
     * @param evt
     */
    private void reporterPublicationLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_reporterPublicationLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("https://compomics.github.io/projects/reporter.html");
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
                reporterIonQuantification.assignSample(key, sampleNames.get(key));
            }
            reporterIonQuantification.setMethod(selectedMethod);
            reporterIonQuantification.setControlSamples(controlSamples);

            if (welcomeDialog != null) {
                welcomeDialog.setVisible(false);
            }

            // set the user defined reagents order
            displayPreferences.setReagents(reagents);

            reporterGUI.createNewProject(psdbParent, reporterSettings, reporterIonQuantification, processingParameters, displayPreferences);
            dispose();
        }
    }//GEN-LAST:event_loadButtonActionPerformed

    /**
     * Edit the processing preferences.
     *
     * @param evt
     */
    private void editProcessingButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_editProcessingButtonActionPerformed
        ProcessingParametersDialog processingParametersDialog = new ProcessingParametersDialog(this, reporterGUI, processingParameters, true);
        if (!processingParametersDialog.isCanceled()) {
            processingParameters = processingParametersDialog.getProcessingParameters();
            processingTxt.setText(processingParameters.getnThreads() + " cores");
        }
    }//GEN-LAST:event_editProcessingButtonActionPerformed

    /**
     * Move the selected row up.
     *
     * @param evt
     */
    private void moveUpButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveUpButtonActionPerformed
        int[] selectedRows = sampleAssignmentTable.getSelectedRows();

        if (selectedRows.length > 0 && selectedRows[0] > 0) {
            String toMove = reagents.get(selectedRows[0]);
            String toReplace = reagents.get(selectedRows[0] - 1);
            reagents.set(selectedRows[0] - 1, toMove);
            reagents.set(selectedRows[0], toReplace);
            sampleAssignmentTable.setRowSelectionInterval(selectedRows[0] - 1, selectedRows[0] - 1 + selectedRows.length - 1);
            resetTableIndexes();
            sampleAssignmentTableMouseReleased(null);
        }
    }//GEN-LAST:event_moveUpButtonActionPerformed

    /**
     * Move the selected row to the top.
     *
     * @param evt
     */
    private void moveTopButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveTopButtonActionPerformed
        int[] selectedRows = sampleAssignmentTable.getSelectedRows();

        if (selectedRows.length > 0 && selectedRows[0] > 0) {
            String toMove = reagents.get(selectedRows[0]);
            reagents.remove(selectedRows[0]);
            reagents.add(0, toMove);
            sampleAssignmentTable.setRowSelectionInterval(0, selectedRows.length - 1);
            resetTableIndexes();
            sampleAssignmentTableMouseReleased(null);
        }
    }//GEN-LAST:event_moveTopButtonActionPerformed

    /**
     * Move the selected row down.
     *
     * @param evt
     */
    private void moveDownButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveDownButtonActionPerformed
        int[] selectedRows = sampleAssignmentTable.getSelectedRows();

        if (selectedRows.length > 0 && selectedRows[selectedRows.length - 1] < sampleAssignmentTable.getRowCount() - 1) {
            String toMove = reagents.get(selectedRows[0]);
            String toReplace = reagents.get(selectedRows[0] + 1);
            reagents.set(selectedRows[0] + 1, toMove);
            reagents.set(selectedRows[0], toReplace);
            sampleAssignmentTable.setRowSelectionInterval(selectedRows[0] + 1, selectedRows[0] + selectedRows.length);
            resetTableIndexes();
            sampleAssignmentTableMouseReleased(null);
        }
    }//GEN-LAST:event_moveDownButtonActionPerformed

    /**
     * Move the selected row to the bottom.
     *
     * @param evt
     */
    private void moveBottomButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_moveBottomButtonActionPerformed
        int[] selectedRows = sampleAssignmentTable.getSelectedRows();

        if (selectedRows.length > 0 && selectedRows[selectedRows.length - 1] < sampleAssignmentTable.getRowCount() - 1) {
            String toMove = reagents.get(selectedRows[0]);
            reagents.remove(selectedRows[0]);
            reagents.add(toMove);
            sampleAssignmentTable.setRowSelectionInterval(sampleAssignmentTable.getRowCount() - selectedRows.length, sampleAssignmentTable.getRowCount() - 1);
            resetTableIndexes();
            sampleAssignmentTableMouseReleased(null);
        }
    }//GEN-LAST:event_moveBottomButtonActionPerformed

    /**
     * Enable/disable the move options based on which rows that are selected.
     *
     * @param evt the mouse event
     */
    private void sampleAssignmentTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_sampleAssignmentTableMouseReleased
        int selectedRows[] = sampleAssignmentTable.getSelectedRows();

        if (selectedRows.length > 0) {
            moveUpButton.setEnabled(selectedRows[0] > 0);
            moveTopButton.setEnabled(selectedRows[0] > 0);
            moveDownButton.setEnabled(selectedRows[selectedRows.length - 1] < sampleAssignmentTable.getRowCount() - 1);
            moveBottomButton.setEnabled(selectedRows[selectedRows.length - 1] < sampleAssignmentTable.getRowCount() - 1);
        } else {
            moveUpButton.setEnabled(false);
            moveTopButton.setEnabled(false);
            moveDownButton.setEnabled(false);
            moveBottomButton.setEnabled(false);
        }
    }//GEN-LAST:event_sampleAssignmentTableMouseReleased

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
    private javax.swing.JButton moveBottomButton;
    private javax.swing.JButton moveDownButton;
    private javax.swing.JButton moveTopButton;
    private javax.swing.JButton moveUpButton;
    private javax.swing.JPanel orderSettingsPanel;
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

        String fastaFilePath = psdbParent.getProjectDetails().getFastaFile();

        if (fastaFilePath != null) {
            fastaTxt.setText(fastaFilePath);
            return true;
        }

        String errorText = "FASTA file not found or incorrectly loaded:\n" + psdbParent.getProjectDetails().getFastaFile()
                + "\nPlease locate it manually.";

        JOptionPane.showMessageDialog(this,
                errorText,
                "FASTA File(s) Not Found", JOptionPane.WARNING_MESSAGE);

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

        TreeSet<String> msFiles = new TreeSet<>(psdbParent.getIdentification().getSpectrumIdentification().keySet());

        for (String spectrumFileName : msFiles) { // @TODO: check alternative locations as for ps

            boolean found = false;

            for (File spectrumFile : spectrumFiles) {
                if (IoUtil.removeExtension(spectrumFile.getName()).equalsIgnoreCase(spectrumFileName)) {
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
                JOptionPane.showMessageDialog(
                        this,
                        "Spectrum file(s) not found:\n" + missing + "\nPlease locate them manually.",
                        "Spectrum File Not Found",
                        JOptionPane.WARNING_MESSAGE);
            } else {
                JOptionPane.showMessageDialog(
                        this,
                        "Spectrum files not found.\n" + "Please locate them manually.",
                        "Spectrum File Not Found",
                        JOptionPane.WARNING_MESSAGE
                );
            }

        }

        if (spectrumFiles.size() > 1) {
            txtSpectraFileLocation.setText(spectrumFiles.size() + " files loaded"); //@TODO: allow editing
        } else if (spectrumFiles.size() == 1) {
            txtSpectraFileLocation.setText(spectrumFiles.get(0).getName()); //@TODO: allow editing
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
            @Override
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
     * Method used to import a psdb file.
     *
     * @param psFile a psdb file
     */
    private void importPeptideShakerFile(final File psFile) {

        if (welcomeDialog != null) {
            progressDialog = new ProgressDialogX(welcomeDialog, reporterGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        } else {
            progressDialog = new ProgressDialogX(this, reporterGUI,
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                    Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")), true);
        }

        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Importing Project. Please Wait...");

        new Thread(new Runnable() {
            @Override
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

                psdbParent = new PsdbParent(Reporter.getMatchesFolder());
                psdbParent.setPsdbFile(psFile);
                ProjectImporter projectImporter = new ProjectImporter(NewDialog.this);
                try {
                    projectImporter.importPeptideShakerProject(psdbParent, spectrumFiles, progressDialog);
                    projectImporter.importReporterProject(psdbParent, progressDialog);
                } catch (OutOfMemoryError error) {
                    System.out.println("Ran out of memory! (runtime.maxMemory(): " + Runtime.getRuntime().maxMemory() + ")");
                    error.printStackTrace();
                    String errorText = "PeptideShaker used up all the available memory and had to be stopped.<br>"
                            + "Memory boundaries are changed in the the Welcome Dialog (Settings<br>"
                            + "& Help > Settings > Java Memory Settings) or in the Edit menu (Edit<br>"
                            + "Java Options). See also <a href=\"https://compomics.github.io/projects/compomics-utilities/wiki/JavaTroubleShooting.html\">JavaTroubleShooting</a>.";
                    JOptionPane.showMessageDialog(NewDialog.this,
                            errorText,
                            "Out of Memory", JOptionPane.ERROR_MESSAGE);
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
                txtIdFileLocation.setText(psdbParent.getPsdbFile().getName());

                // load project specific ptms
                String error = PeptideShaker.loadModifications(getSearchParameters());
                if (error != null) {
                    JOptionPane.showMessageDialog(NewDialog.this,
                            error,
                            "PTM Definition Changed", JOptionPane.WARNING_MESSAGE);
                }

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
        return psdbParent.getProjectDetails();
    }

    /**
     * Returns the search parameters.
     *
     * @return the search parameters
     */
    public SearchParameters getSearchParameters() {
        return psdbParent.getIdentificationParameters().getSearchParameters();
    }

    /**
     * Returns the identification displayed.
     *
     * @return the identification displayed
     */
    public Identification getIdentification() {
        return psdbParent.getIdentification();
    }

    /**
     * Method called whenever an error was encountered while loading the
     * methods.
     */
    private void importMethodsError() {

        JOptionPane.showMessageDialog(this, "Default reporter methods file could not be parsed, please select a method file.", "No Spectrum File Selected", JOptionPane.WARNING_MESSAGE);
        JFileChooser fileChooser = new JFileChooser(reporterGUI.getLastSelectedFolder().getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Methods file");
        fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileChooser.setMultiSelectionEnabled(false);

        int returnVal = fileChooser.showDialog(this.getParent(), "Add");
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File newFile = fileChooser.getSelectedFile();
            try {
                methodsFactory.importMethods(newFile);
                reporterGUI.getLastSelectedFolder().setLastSelectedFolder(newFile.getPath());
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

        if (fastaTxt.getText().isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "FASTA file not selected.",
                    "FASTA File Missing", JOptionPane.WARNING_MESSAGE);
            return false;
        }

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
        return reporterGUI;
    }

    /**
     * Table model for the sample to reporter ion assignment.
     */
    private class AssignementTableModel extends DefaultTableModel {

        @Override
        public int getRowCount() {
            if (selectedMethod == null || psdbParent == null) {
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
                    String projectName = psdbParent.getProjectParameters().getProjectUniqueName();
                    if (sampleNames.get(reagentName) == null) {
                        if (projectName != null) {
                            sampleNames.put(reagentName, projectName + " " + reagentName);
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
     * Returns the psdb parent object providing all information contained in the
     * psdb file
     *
     * @return the psdb parent object providing all information contained in the
     * psdb file
     */
    public PsdbParent getPsdbBean() {
        return psdbParent;
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

    /**
     * Resets the table indexes.
     */
    private void resetTableIndexes() {
        for (int i = 0; i < sampleAssignmentTable.getRowCount(); i++) {
            sampleAssignmentTable.setValueAt((i + 1), i, 0);
        }
    }
}
