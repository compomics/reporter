package eu.isas.reporter.gui;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.calculation.Ignorer;
import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.ItraqScore;
import eu.isas.reporter.gui.qcpanels.PeptideCharts;
import eu.isas.reporter.gui.qcpanels.PeptideScoreCharts;
import eu.isas.reporter.gui.qcpanels.ProteinCharts;
import eu.isas.reporter.gui.qcpanels.ProteinScoreCharts;
import eu.isas.reporter.gui.qcpanels.SpectrumCharts;
import eu.isas.reporter.gui.qcpanels.SpectrumScoreCharts;
import eu.isas.reporter.io.ReporterExporter;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 * This panel will be used to display the quantification results
 *
 * @author Marc Vaudel
 */
public class ResultPanel extends javax.swing.JPanel {

    /**
     * index for obsolete chart display
     */
    private static final int DISP_OBSOLETE = -1;
    /**
     * spectrum chart index
     */
    private static final int DISP_SPECTRUM = 0;
    /**
     * peptide chart index
     */
    private static final int DISP_PEPTIDE = 1;
    /**
     * protein chart index
     */
    private static final int DISP_PROTEIN = 2;
    /**
     * spectrum score chart index
     */
    private static final int DISP_SPECTRUM_SCORE = 3;
    /**
     * peptide score chart index
     */
    private static final int DISP_PEPTIDE_SCORE = 4;
    /**
     * protein score chart index
     */
    private static final int DISP_PROTEIN_SCORE = 5;
    /**
     * index indicating which chart is being displayed
     */
    private int displayed;

    /**
     * The currently processed quantification
     */
    private ReporterIonQuantification quantification;
    /**
     * List of the reporter ions used for this quantification
     */
    private ArrayList<ReporterIon> reporterIons;
    /**
     * The reporter class
     */
    private Reporter parent;

    /**
     * The experiment conducted
     */
    private MsExperiment experiment;
    /**
     * a ratio estimator
     */
    private RatioEstimator ratioEstimator;
    /**
     * list of the indexes of the displayed lines
     */
    private ArrayList<TableKey> tableIndex = new ArrayList<TableKey>();
    /**
     * The ignorer used
     */
    private Ignorer ignorer;


    /**
     * The jpanel encompassing the right-hand side chart panels
     */
    private JPanel verticalChartPanel;
    /**
     * The protein chart panel
     */
    private ProteinCharts proteinCharts;
    /**
     * The peptide chart panel
     */
    private PeptideCharts peptideCharts;
    /**
     * The spectrum chart panel
     */
    private SpectrumCharts spectrumCharts;
    /**
     * The protein score chart panel
     */
    private ProteinScoreCharts proteinScoreCharts;
    /**
     * The peptide score chart panel
     */
    private PeptideScoreCharts peptideScoreCharts;
    /**
     * The spectrum score chart panel
     */
    private SpectrumScoreCharts spectrumScoreCharts;

    /**
     * pop-up used for the table
     */
    private JPopupMenu tablePopUp;

    /**
     * Constructor
     * @param parent            The reporter class
     * @param quantification    The quantification under processing
     * @param experiment        The experiment conducted
     * @param ignorer           The ignorer to use
     */
    public ResultPanel(Reporter parent, ReporterIonQuantification quantification, MsExperiment experiment, Ignorer ignorer) {
        this.experiment = experiment;
        this.parent = parent;
        this.quantification = quantification;
        this.ignorer = ignorer;
        this.reporterIons = quantification.getReporterMethod().getReporterIons();
        createTableIndex();

        initComponents();
        parent.getMainFrame().setTitle(parent.getMainFrame().getTitle() + " - " + experiment.getReference());

        resultTable.setAutoCreateRowSorter(false);
        resultTable.getColumnModel().getColumn(0).setMaxWidth(10);
        resultTable.getColumnModel().getColumn(1).setMaxWidth(10);
        resultTable.setDefaultRenderer(String.class, new ReporterCellRenderer());
        resultTable.setDefaultRenderer(Integer.class, new ReporterCellRenderer());
        resultTable.setDefaultRenderer(Double.class, new ReporterCellRenderer());

        // disables the dragging column headers to reorder columns
        resultTable.getTableHeader().setReorderingAllowed(false);

        // centrally align the combobox
        separatorCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        if (validateInput()) {
            ignorer.setRatioMin(getRatioMin());
            ignorer.setRatioMax(getRatioMax());
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), ignorer);
            estimateAllProteinRatios();
        }

        verticalChartPanel = new JPanel();
        proteinCharts = new ProteinCharts(quantification, getResolution());
        updateProteinCharts();

        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));
        chartPanel.add(verticalChartPanel);

        proteinScoreCharts = new ProteinScoreCharts(quantification);
        peptideScoreCharts = new PeptideScoreCharts(quantification);
        spectrumScoreCharts = new SpectrumScoreCharts(quantification);
        displayed = DISP_PROTEIN;

        JMenuItem ignoreItem = new JMenuItem("Ignore");
        ignoreItem.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        ignore();
                    }
                });
        JMenuItem accountItem = new JMenuItem("Account");
        accountItem.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        account();
                    }
                });
        JMenuItem sortItem = new JMenuItem("Sort");
        sortItem.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        sort();
                    }
                });
        JMenuItem detailsItem = new JMenuItem("Details");
        detailsItem.addActionListener(
                new ActionListener() {

                    public void actionPerformed(ActionEvent e) {
                        // @TODO: implement me...
                    }
                });
        tablePopUp = new JPopupMenu();
        tablePopUp.add(ignoreItem);
        tablePopUp.add(accountItem);
        tablePopUp.add(sortItem);
        tablePopUp.add(detailsItem);
    }

    /**
     * Method which creates the table index
     */
    private void createTableIndex() {
        for (int i = 0; i < quantification.getProteinQuantification().size(); i++) {
            tableIndex.add(new TableKey(TableKey.PROTEIN, i, 0, 0));
        }
    }

    /** This method is called from within the constructor to
     * initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is
     * always regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        jTabbedPane1 = new javax.swing.JTabbedPane();
        jPanel1 = new javax.swing.JPanel();
        exitButton = new javax.swing.JButton();
        saveAsButton = new javax.swing.JButton();
        jSplitPane1 = new javax.swing.JSplitPane();
        jScrollPane1 = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        chartPanel = new javax.swing.JPanel();
        jPanel2 = new javax.swing.JPanel();
        jPanel3 = new javax.swing.JPanel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        ratioResolutionTxt = new javax.swing.JTextField();
        kTxt = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        ratioMaxTxt = new javax.swing.JTextField();
        ratioMinTxt = new javax.swing.JTextField();
        jPanel4 = new javax.swing.JPanel();
        jLabel7 = new javax.swing.JLabel();
        separatorCmb = new javax.swing.JComboBox();

        exitButton.setText("Exit");
        exitButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exitButtonActionPerformed(evt);
            }
        });

        saveAsButton.setText("Save As");
        saveAsButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                saveAsButtonActionPerformed(evt);
            }
        });

        jSplitPane1.setDividerLocation(500);
        jSplitPane1.setResizeWeight(0.5);

        resultTable.setModel(new ResultTable());
        resultTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resultTableMouseClicked(evt);
            }
        });
        resultTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                resultTableKeyReleased(evt);
            }
        });
        jScrollPane1.setViewportView(resultTable);

        jSplitPane1.setLeftComponent(jScrollPane1);

        org.jdesktop.layout.GroupLayout chartPanelLayout = new org.jdesktop.layout.GroupLayout(chartPanel);
        chartPanel.setLayout(chartPanelLayout);
        chartPanelLayout.setHorizontalGroup(
            chartPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 537, Short.MAX_VALUE)
        );
        chartPanelLayout.setVerticalGroup(
            chartPanelLayout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(0, 770, Short.MAX_VALUE)
        );

        jSplitPane1.setRightComponent(chartPanel);

        org.jdesktop.layout.GroupLayout jPanel1Layout = new org.jdesktop.layout.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                        .add(saveAsButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 80, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                        .add(exitButton, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 84, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                    .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1043, Short.MAX_VALUE))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(new java.awt.Component[] {exitButton, saveAsButton}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .add(jSplitPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 772, Short.MAX_VALUE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.UNRELATED)
                .add(jPanel1Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(exitButton)
                    .add(saveAsButton))
                .addContainerGap())
        );

        jTabbedPane1.addTab("Quantification Results", jPanel1);

        jPanel3.setBorder(javax.swing.BorderFactory.createTitledBorder("Ratio Estimation"));

        jLabel2.setText("Ratio Resolution:");

        jLabel3.setText("Estimator Window:");

        ratioResolutionTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ratioResolutionTxt.setText("0.01");
        ratioResolutionTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ratioResolutionTxtActionPerformed(evt);
            }
        });

        kTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        kTxt.setText("1.48");
        kTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                kTxtActionPerformed(evt);
            }
        });

        jLabel4.setText("* MAD");

        jLabel5.setText("Ratio Max:");

        jLabel6.setText("Ratio Min:");

        ratioMaxTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ratioMaxTxt.setText("100");
        ratioMaxTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ratioMaxTxtActionPerformed(evt);
            }
        });

        ratioMinTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        ratioMinTxt.setText("0.01");
        ratioMinTxt.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ratioMinTxtActionPerformed(evt);
            }
        });

        org.jdesktop.layout.GroupLayout jPanel3Layout = new org.jdesktop.layout.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jLabel3))
                .add(18, 18, 18)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING, false)
                    .add(ratioResolutionTxt)
                    .add(kTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 76, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jLabel4)
                .add(130, 130, 130)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel5)
                    .add(jLabel6))
                .add(18, 18, 18)
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING, false)
                    .add(ratioMinTxt)
                    .add(ratioMaxTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 65, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(526, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(new java.awt.Component[] {kTxt, ratioMaxTxt, ratioMinTxt, ratioResolutionTxt}, org.jdesktop.layout.GroupLayout.HORIZONTAL);

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
                    .add(jLabel2)
                    .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                        .add(jPanel3Layout.createSequentialGroup()
                            .add(ratioMaxTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(ratioMinTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                        .add(jPanel3Layout.createSequentialGroup()
                            .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(ratioResolutionTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(jLabel5))
                            .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                            .add(jPanel3Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                                .add(kTxt, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                                .add(jLabel4)
                                .add(jLabel6)
                                .add(jLabel3)))))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("File Export"));

        jLabel7.setText("Exported Column Separator:");

        separatorCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Tab", "Comma", "Point", "§" }));

        org.jdesktop.layout.GroupLayout jPanel4Layout = new org.jdesktop.layout.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jLabel7)
                .add(18, 18, 18)
                .add(separatorCmb, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, 86, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(780, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel4Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.BASELINE)
                    .add(jLabel7)
                    .add(separatorCmb, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        org.jdesktop.layout.GroupLayout jPanel2Layout = new org.jdesktop.layout.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(org.jdesktop.layout.GroupLayout.TRAILING, jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.TRAILING)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel3, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .add(org.jdesktop.layout.GroupLayout.LEADING, jPanel4, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .add(jPanel3, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(org.jdesktop.layout.LayoutStyle.RELATED)
                .add(jPanel4, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, org.jdesktop.layout.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(647, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Quantification Settings", jPanel2);

        org.jdesktop.layout.GroupLayout layout = new org.jdesktop.layout.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 1068, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(org.jdesktop.layout.GroupLayout.LEADING)
            .add(jTabbedPane1, org.jdesktop.layout.GroupLayout.DEFAULT_SIZE, 856, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
        parent.close(0);
    }//GEN-LAST:event_exitButtonActionPerformed

    private void ratioResolutionTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ratioResolutionTxtActionPerformed
        if (validateInput()) {
            ignorer.setRatioMin(getRatioMin());
            ignorer.setRatioMax(getRatioMax());
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), ignorer);
            estimateAllProteinRatios();
        }
    }//GEN-LAST:event_ratioResolutionTxtActionPerformed

    private void kTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_kTxtActionPerformed
        if (validateInput()) {
            ignorer.setRatioMin(getRatioMin());
            ignorer.setRatioMax(getRatioMax());
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), ignorer);
            estimateAllProteinRatios();
        }
    }//GEN-LAST:event_kTxtActionPerformed

    private void resultTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTableMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON1) {
            tableLeftClicked();
        } else if (evt.getButton() == MouseEvent.BUTTON3) {
            resultTable.changeSelection(resultTable.rowAtPoint(evt.getPoint()), resultTable.columnAtPoint(evt.getPoint()), false, false);
            tablePopUp.show(resultTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_resultTableMouseClicked

    private void ratioMaxTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ratioMaxTxtActionPerformed
        if (validateInput()) {
            ignorer.setRatioMin(getRatioMin());
            ignorer.setRatioMax(getRatioMax());
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), ignorer);
            estimateAllProteinRatios();
        }
    }//GEN-LAST:event_ratioMaxTxtActionPerformed

    private void ratioMinTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ratioMinTxtActionPerformed
        if (validateInput()) {
            ignorer.setRatioMin(getRatioMin());
            ignorer.setRatioMax(getRatioMax());
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), ignorer);
            estimateAllProteinRatios();
        }
    }//GEN-LAST:event_ratioMinTxtActionPerformed

    private void saveAsButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_saveAsButtonActionPerformed

        JFileChooser fileChooser = new JFileChooser(parent.getLastSelectedFolder());
        fileChooser.setDialogTitle("Select Export Folder");
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);

        int returnVal = fileChooser.showSaveDialog(this);
        if (returnVal == JFileChooser.APPROVE_OPTION) {
            ReporterExporter exporter = new ReporterExporter(experiment, getSeparator());
            exporter.exportResults(quantification, fileChooser.getSelectedFile().getPath());
            parent.setLastSelectedFolder(fileChooser.getSelectedFile().getPath());
        }
    }//GEN-LAST:event_saveAsButtonActionPerformed

    private void resultTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_resultTableKeyReleased
            tableLeftClicked();
    }//GEN-LAST:event_resultTableKeyReleased
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel chartPanel;
    private javax.swing.JButton exitButton;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel7;
    private javax.swing.JPanel jPanel1;
    private javax.swing.JPanel jPanel2;
    private javax.swing.JPanel jPanel3;
    private javax.swing.JPanel jPanel4;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JSplitPane jSplitPane1;
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField kTxt;
    private javax.swing.JTextField ratioMaxTxt;
    private javax.swing.JTextField ratioMinTxt;
    private javax.swing.JTextField ratioResolutionTxt;
    private javax.swing.JTable resultTable;
    private javax.swing.JButton saveAsButton;
    private javax.swing.JComboBox separatorCmb;
    // End of variables declaration//GEN-END:variables

    /**
     * Method which estimates all protein ratios
     */
    private void estimateAllProteinRatios() {
        for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification()) {
            ratioEstimator.estimateRatios(proteinQuantification);
        }
    }

    /**
     * getter for k
     *
     * @return the input value for k
     */
    private double getK() {
        return new Double(kTxt.getText().trim());
    }

    /**
     * Returns the resolution.
     *
     * @return the resolution
     */
    private double getResolution() {
        return new Double(ratioResolutionTxt.getText().trim());
    }

    /**
     * Returns the minimum ratio.
     *
     * @return the minimum ratio
     */
    private double getRatioMin() {
        return new Double(ratioMinTxt.getText().trim());
    }

    /**
     * Returns the maximum ratio.
     *
     * @return the maximum ratio
     */
    private double getRatioMax() {
        return new Double(ratioMaxTxt.getText().trim());
    }

    /**
     * Validates the input. Returns false if not valid.
     *
     * @return false if not valid, true otherwise
     */
    private boolean validateInput() {
        try {
            new Double(ratioResolutionTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong Ratio Resolution.", "Please input a number for the ratio calculation resolution.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            new Double(kTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong Window Width.", "Please input a number for the estimator window width.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            new Double(ratioMinTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong Ratio Min.", "Please input a number for the minimal ratio.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            new Double(ratioMaxTxt.getText().trim());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Wrong Ratio Max.", "Please input a number for the maximal ratio.", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * returns the separator for export to csv files
     *
     * @return the user selected separator
     */
    private String getSeparator() {
        String cmbResult = (String) separatorCmb.getSelectedItem();
        if (cmbResult.equals("Comma")) {
            return ",";
        } else if (cmbResult.equals("Point")) {
            return ".";
        } else if (cmbResult.equals("§")) {
            return "§";
        } else {
            return "\t";
        }
    }

    /**
     * method used to ignore the selected row
     */
    private void ignore() {
        int row = resultTable.getSelectedRow();
        int column = resultTable.getSelectedColumn();
        TableKey tableKey = tableIndex.get(row);
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        if (tableKey.lineType == TableKey.PROTEIN) {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getUrParam(ignoredRatios);
        } else if (tableKey.lineType == TableKey.PEPTIDE) {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getUrParam(ignoredRatios);
        } else {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                    tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(ignoredRatios);
        }
        if (column > 5 && column < 5 + reporterIons.size()) {
            int pos = column - 5;
            ignoredRatios.ignore(reporterIons.get(pos).getIndex());
        } else {
            for (ReporterIon reporterIon : reporterIons) {
                ignoredRatios.ignore(reporterIon.getIndex());
            }
        }
        ratioEstimator.estimateRatios(quantification.getProteinQuantification().get(tableKey.proteinIndex));
        repaintResultsTable();
        proteinCharts = new ProteinCharts(quantification, getResolution());
        displayed = DISP_OBSOLETE;
        tableLeftClicked();
    }

    /**
     * method used to account the selected row
     */
    private void account() {
        int row = resultTable.getSelectedRow();
        int column = resultTable.getSelectedColumn();
        TableKey tableKey = tableIndex.get(row);
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        if (tableKey.lineType == TableKey.PROTEIN) {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getUrParam(ignoredRatios);
        } else if (tableKey.lineType == TableKey.PEPTIDE) {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getUrParam(ignoredRatios);
        } else {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                    tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(ignoredRatios);
        }
        if (column > 5 && column < 5 + reporterIons.size()) {
            int pos = column - 5;
            ignoredRatios.account(reporterIons.get(pos).getIndex());
        } else {
            for (ReporterIon reporterIon : reporterIons) {
                ignoredRatios.account(reporterIon.getIndex());
            }
        }
        ratioEstimator.estimateRatios(quantification.getProteinQuantification().get(tableKey.proteinIndex));
        repaintResultsTable();
        proteinCharts = new ProteinCharts(quantification, getResolution());
        displayed = DISP_OBSOLETE;
        tableLeftClicked();
    }

    /**
     * method called whenever the table is left-clicked
     */
    private void tableLeftClicked() {
        int row = resultTable.getSelectedRow();

        if (row < tableIndex.size()) {

            TableKey tableKey = tableIndex.get(row);
            int column = resultTable.getSelectedColumn();
            if (column == 0 && resultTable.getValueAt(row, column).equals("+")) {
                expandProtein(row);
            } else if (column == 1 && resultTable.getValueAt(row, column).equals("+")) {
                expandPeptide(row);
            } else if (column == 0 && resultTable.getValueAt(row, column).equals("-")) {
                concatProtein(row);
            } else if (column == 1 && resultTable.getValueAt(row, column).equals("-")) {
                concatPeptide(row);
            } else if (column == 2 && tableKey.lineType == TableKey.PROTEIN) {
                ProteinQuantification proteinQuantification = quantification.getProteinQuantification().get(tableKey.proteinIndex);
                if (displayed == DISP_PROTEIN) {
                    proteinCharts.setProtein(proteinQuantification);
                    verticalChartPanel.validate();
                } else {
                    updateProteinCharts();
                }
                displayed = DISP_PROTEIN;
            } else if (column == 3) {
                if (peptideCharts == null) {
                    peptideCharts = new PeptideCharts(tableKey.proteinIndex, quantification, getResolution());
                }
                ProteinQuantification proteinQuantification = quantification.getProteinQuantification().get(tableKey.proteinIndex);
                if (tableKey.lineType == TableKey.PROTEIN) {
                    peptideCharts = new PeptideCharts(tableKey.proteinIndex, quantification, getResolution());
                    updatePeptideCharts();
                } else if (tableKey.lineType == TableKey.PEPTIDE) {
                    if (displayed == DISP_PEPTIDE && peptideCharts.getProteinIndex() == tableKey.proteinIndex) {
                        peptideCharts.setPeptide(proteinQuantification.getPeptideQuantification().get(tableKey.peptideIndex));
                        verticalChartPanel.validate();
                    } else {
                        peptideCharts = new PeptideCharts(tableKey.proteinIndex, quantification, getResolution());
                        updatePeptideCharts();
                    }
                }
                displayed = DISP_PEPTIDE;
            } else if (column == 5) {
                PeptideQuantification peptideQuantification = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex);
                if (tableKey.lineType == TableKey.PEPTIDE) {
                    spectrumCharts = new SpectrumCharts(tableKey.proteinIndex, tableKey.peptideIndex, quantification, getResolution());
                    updateSpectrumCharts();
                } else if (tableKey.lineType == TableKey.SPECTRUM) {
                    if (displayed == DISP_SPECTRUM && spectrumCharts.getProteinIndex() == tableKey.proteinIndex && spectrumCharts.getPeptideIndex() == tableKey.peptideIndex) {
                        spectrumCharts.setSpectrum(peptideQuantification.getSpectrumQuantification().get(tableKey.spectrumIndex));
                        verticalChartPanel.validate();
                    } else {
                        spectrumCharts = new SpectrumCharts(tableKey.proteinIndex, tableKey.peptideIndex, quantification, getResolution());
                        updateSpectrumCharts();
                    }
                    displayed = DISP_SPECTRUM;
                }
            } else if (column == 5 + reporterIons.size()) {
                if (tableKey.lineType == TableKey.PROTEIN) {
                    if (displayed != DISP_PROTEIN_SCORE) {
                        proteinScoreCharts = new ProteinScoreCharts(quantification);
                        verticalChartPanel.removeAll();
                        verticalChartPanel.setLayout(new BoxLayout(verticalChartPanel, BoxLayout.Y_AXIS));
                        verticalChartPanel.add(Box.createHorizontalStrut(10));
                        verticalChartPanel.add(proteinScoreCharts.getChart(true));
                        verticalChartPanel.add(Box.createHorizontalStrut(10));
                        verticalChartPanel.validate();
                    }
                    proteinScoreCharts.setProtein(quantification.getProteinQuantification().get(tableKey.proteinIndex));
                    displayed = DISP_PROTEIN_SCORE;
                } else if (tableKey.lineType == TableKey.PEPTIDE) {
                    if (displayed != DISP_PEPTIDE_SCORE) {
                        peptideScoreCharts = new PeptideScoreCharts(quantification);
                        verticalChartPanel.removeAll();
                        verticalChartPanel.setLayout(new BoxLayout(verticalChartPanel, BoxLayout.Y_AXIS));
                        verticalChartPanel.add(Box.createHorizontalStrut(10));
                        verticalChartPanel.add(peptideScoreCharts.getChart(true));
                        verticalChartPanel.add(Box.createHorizontalStrut(10));
                        verticalChartPanel.validate();
                    }
                    peptideScoreCharts.setPeptide(quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex));
                    displayed = DISP_PEPTIDE_SCORE;
                } else if (tableKey.lineType == TableKey.SPECTRUM) {
                    if (displayed != DISP_SPECTRUM_SCORE) {
                        spectrumScoreCharts = new SpectrumScoreCharts(quantification);
                        verticalChartPanel.removeAll();
                        verticalChartPanel.setLayout(new BoxLayout(verticalChartPanel, BoxLayout.Y_AXIS));
                        verticalChartPanel.add(Box.createHorizontalStrut(10));
                        verticalChartPanel.add(spectrumScoreCharts.getChart(true));
                        verticalChartPanel.add(Box.createHorizontalStrut(10));
                        verticalChartPanel.validate();
                    }
                    spectrumScoreCharts.setSpectrum(quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                            tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex));
                    verticalChartPanel.validate();
                    displayed = DISP_SPECTRUM_SCORE;
                }
            }
            this.repaint();
        }
    }

    /**
     * Update the peptide charts.
     */
    private void updatePeptideCharts() {
        verticalChartPanel.removeAll();
        verticalChartPanel.setLayout(new BoxLayout(verticalChartPanel, BoxLayout.Y_AXIS));

        int chartCounter = 0;

        for (ReporterIon ion : reporterIons) {
            if (ion.getIndex() != quantification.getReferenceLabel()) {

                chartCounter++;

                verticalChartPanel.add(Box.createHorizontalStrut(10));

                boolean showLegend = true;

                // hide the chart legends for all but the last chart
                if (chartCounter < reporterIons.size() - 1) {
                    showLegend = false;
                }

                verticalChartPanel.add(peptideCharts.getChart(ion.getIndex(), showLegend));
            }
            verticalChartPanel.add(Box.createHorizontalStrut(10));
        }
        verticalChartPanel.validate();
    }

    /**
     * Update the protein charts.
     */
    private void updateProteinCharts() {
        verticalChartPanel.removeAll();
        verticalChartPanel.setLayout(new BoxLayout(verticalChartPanel, BoxLayout.Y_AXIS));

        int chartCounter = 0;

        for (ReporterIon ion : reporterIons) {
            if (ion.getIndex() != quantification.getReferenceLabel()) {

                chartCounter++;

                verticalChartPanel.add(Box.createHorizontalStrut(10));

                boolean showLegend = true;

                // hide the chart legends for all but the last chart
                if (chartCounter < reporterIons.size() - 1) {
                    showLegend = false;
                }

                verticalChartPanel.add(proteinCharts.getChart(ion.getIndex(), showLegend));
            }
            verticalChartPanel.add(Box.createHorizontalStrut(10));
        }
        verticalChartPanel.validate();
    }

    /**
     * Update the spectrum charts.
     */
    private void updateSpectrumCharts() {
        verticalChartPanel.removeAll();
        verticalChartPanel.setLayout(new BoxLayout(verticalChartPanel, BoxLayout.Y_AXIS));

        int chartCounter = 0;

        for (ReporterIon ion : reporterIons) {
            if (ion.getIndex() != quantification.getReferenceLabel()) {

                chartCounter++;

                verticalChartPanel.add(Box.createHorizontalStrut(10));

                boolean showLegend = true;

                // hide the chart legends for all but the last chart
                if (chartCounter < reporterIons.size() - 1) {
                    showLegend = false;
                }

                verticalChartPanel.add(spectrumCharts.getChart(ion.getIndex(), showLegend));
            }
            verticalChartPanel.add(Box.createHorizontalStrut(10));
        }
        verticalChartPanel.validate();
    }

    /**
     * method used to expand a protein line
     *
     * @param row the selected row
     */
    private void expandProtein(int row) {
        TableKey tableKey = tableIndex.get(row);
        int nPeptides = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().size();
        for (int i = nPeptides - 1; i >= 0; i--) {
            tableIndex.add(row + 1, new TableKey(TableKey.PEPTIDE, tableKey.proteinIndex, i, 0));
        }
        repaintResultsTable();
    }

    /**
     * Method used to expand a peptide line
     *
     * @param row the selected row
     */
    private void expandPeptide(int row) {
        TableKey tableKey = tableIndex.get(row);
        int nSpectra = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().size();
        for (int i = nSpectra - 1; i >= 0; i--) {
            tableIndex.add(row + 1, new TableKey(TableKey.SPECTRUM, tableKey.proteinIndex, tableKey.peptideIndex, i));
        }
        repaintResultsTable();
    }

    /**
     * method used to reduce a protein line
     *
     * @param row the selected row
     */
    private void concatProtein(int row) {
        while (tableIndex.get(row + 1).lineType != TableKey.PROTEIN) {
            tableIndex.remove(row + 1);
            if (row == tableIndex.size() - 1) {
                break;
            }
        }
        repaintResultsTable();
    }

    /**
     * method used to reduce a peptide line
     *
     * @param row the selected row
     */
    private void concatPeptide(int row) {
        while (tableIndex.get(row + 1).lineType != TableKey.PEPTIDE
                && tableIndex.get(row + 1).lineType != TableKey.PROTEIN) {
            tableIndex.remove(row + 1);
            if (row == tableIndex.size() - 1) {
                break;
            }
        }
        repaintResultsTable();
    }

    /**
     * Revalidates and repaints the results table.
     */
    private void repaintResultsTable() {

        SwingUtilities.invokeLater(new Runnable() {

            public void run() {
                resultTable.revalidate();
                resultTable.repaint();
            }
        });
    }

    /**
     * Methods which sorts the table according to the protein column
     */
    private void sortProteins() {
        HashMap<String, ArrayList<TableKey>> tempMap = new HashMap<String, ArrayList<TableKey>>();
        ArrayList<String> accessions = new ArrayList<String>();
        String accession;
        for (TableKey key : tableIndex) {
            accession = quantification.getProteinQuantification().get(key.proteinIndex).getProteinMatch().getTheoreticProtein().getAccession();
            if (!tempMap.containsKey(accession)) {
                tempMap.put(accession, new ArrayList<TableKey>());
                accessions.add(accession);
            }
            tempMap.get(accession).add(key);
        }
        Collections.sort(accessions);

        tableIndex = new ArrayList<TableKey>();
        for (String key : accessions) {
            tableIndex.addAll(tempMap.get(key));
        }
        repaintResultsTable();
    }

    /**
     * Method which sorts the table according to the number of peptides found per protein
     */
    private void sortProteinsNPeptides() {
        HashMap<Integer, ArrayList<TableKey>> tempMap = new HashMap<Integer, ArrayList<TableKey>>();
        ArrayList<Integer> nPeptides = new ArrayList<Integer>();
        int nPeptide;
        for (TableKey key : tableIndex) {
            nPeptide = quantification.getProteinQuantification().get(key.proteinIndex).getPeptideQuantification().size();
            if (!tempMap.containsKey(nPeptide)) {
                tempMap.put(nPeptide, new ArrayList<TableKey>());
                nPeptides.add(nPeptide);
            }
            tempMap.get(nPeptide).add(key);
        }
        Collections.sort(nPeptides);

        tableIndex = new ArrayList<TableKey>();
        for (int i = nPeptides.size() - 1; i >= 0; i--) {
            tableIndex.addAll(tempMap.get(nPeptides.get(i)));
        }
        repaintResultsTable();
    }

    /**
     * Method used to sort the table according to the peptide column
     */
    private void sortPeptides() {
        ArrayList<String> sequences = new ArrayList<String>();
        HashMap<String, ArrayList<TableKey>> tempMap = new HashMap<String, ArrayList<TableKey>>();
        ArrayList<TableKey> tempIndex = new ArrayList<TableKey>();
        String key;
        for (TableKey tableKey : tableIndex) {
            if (tableKey.lineType == TableKey.PROTEIN) {
                Collections.sort(sequences);
                for (String sequence : sequences) {
                    tempIndex.addAll(tempMap.get(sequence));
                }
                sequences = new ArrayList<String>();
                tempMap = new HashMap<String, ArrayList<TableKey>>();
                tempIndex.add(tableKey);
            } else {
                key = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getPeptideMatch().getTheoreticPeptide().getSequence();
                if (!tempMap.containsKey(key)) {
                    tempMap.put(key, new ArrayList<TableKey>());
                    sequences.add(key);
                }
                tempMap.get(key).add(tableKey);
            }
        }
        tableIndex = tempIndex;
        repaintResultsTable();
    }

    /**
     * Method used to sort the table according to the spectra column
     */
    private void sortSpectra() {
        ArrayList<String> titles = new ArrayList<String>();
        HashMap<String, ArrayList<TableKey>> tempMap = new HashMap<String, ArrayList<TableKey>>();
        ArrayList<TableKey> tempIndex = new ArrayList<TableKey>();
        String key;
        for (TableKey tableKey : tableIndex) {
            if (tableKey.lineType != TableKey.SPECTRUM) {
                Collections.sort(titles);
                for (String title : titles) {
                    tempIndex.addAll(tempMap.get(title));
                }
                titles = new ArrayList<String>();
                tempMap = new HashMap<String, ArrayList<TableKey>>();
                tempIndex.add(tableKey);
            } else {
                key = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                        tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getSpectrum().getSpectrumTitle();
                if (!tempMap.containsKey(key)) {
                    tempMap.put(key, new ArrayList<TableKey>());
                    titles.add(key);
                }
                tempMap.get(key).add(tableKey);
            }
        }
        tableIndex = tempIndex;
        repaintResultsTable();
    }

    /**
     * Method used to sort the table according to the number of spectra per protein
     */
    private void sortProteinsNSpectra() {
        HashMap<Integer, ArrayList<TableKey>> tempMap = new HashMap<Integer, ArrayList<TableKey>>();
        ArrayList<Integer> nSpectra = new ArrayList<Integer>();
        int nSpectrum;
        for (TableKey key : tableIndex) {
            nSpectrum = quantification.getProteinQuantification().get(key.proteinIndex).getProteinMatch().getSpectrumCount();
            if (!tempMap.containsKey(nSpectrum)) {
                tempMap.put(nSpectrum, new ArrayList<TableKey>());
                nSpectra.add(nSpectrum);
            }
            tempMap.get(nSpectrum).add(key);
        }
        Collections.sort(nSpectra);

        tableIndex = new ArrayList<TableKey>();
        for (int i = nSpectra.size() - 1; i >= 0; i--) {
            tableIndex.addAll(tempMap.get(nSpectra.get(i)));
        }
        repaintResultsTable();
    }

    /**
     * Method used to sort the table according to the number of spectra per peptide
     */
    private void sortPeptidesNSpectra() {
        ArrayList<Integer> nSpectra = new ArrayList<Integer>();
        HashMap<Integer, ArrayList<TableKey>> tempMap = new HashMap<Integer, ArrayList<TableKey>>();
        ArrayList<TableKey> tempIndex = new ArrayList<TableKey>();
        int key;
        for (TableKey tableKey : tableIndex) {
            if (tableKey.lineType == TableKey.PROTEIN) {
                Collections.sort(nSpectra);
                for (int i = nSpectra.size() - 1; i >= 0; i--) {
                    tempIndex.addAll(tempMap.get(nSpectra.get(i)));
                }
                nSpectra = new ArrayList<Integer>();
                tempMap = new HashMap<Integer, ArrayList<TableKey>>();
                tempIndex.add(tableKey);
            } else {
                key = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().size();
                if (!tempMap.containsKey(key)) {
                    tempMap.put(key, new ArrayList<TableKey>());
                    nSpectra.add(key);
                }
                tempMap.get(key).add(tableKey);
            }
        }
        tableIndex = tempIndex;
        repaintResultsTable();
    }

    /**
     * Method used to sort the table according to a ratio indexed by the reporter ion index
     *
     * @param ion the reporter ion index
     */
    private void sortRatio(int ion) {
        HashMap<Double, ArrayList<TableKey>> tempMap = new HashMap<Double, ArrayList<TableKey>>();
        ArrayList<Double> ratios = new ArrayList<Double>();
        double ratio;
        ArrayList<TableKey> tempIndex = new ArrayList<TableKey>();
        for (TableKey tableKey : tableIndex) {
            if (tableKey.lineType != TableKey.SPECTRUM) {
                Collections.sort(ratios);
                for (double ratioKey : ratios) {
                    tempIndex.addAll(tempMap.get(ratioKey));
                }
                ratios = new ArrayList<Double>();
                tempMap = new HashMap<Double, ArrayList<TableKey>>();
                tempIndex.add(tableKey);
            } else {
                try {
                    ratio = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                            tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getRatios().get(
                            reporterIons.get(ion).getIndex()).getRatio();
                } catch (Exception e) {
                    ratio = Math.pow(9, 9);
                }
                if (!tempMap.containsKey(ratio)) {
                    tempMap.put(ratio, new ArrayList<TableKey>());
                    ratios.add(ratio);
                }
                tempMap.get(ratio).add(tableKey);
            }
        }
        tableIndex = tempIndex;

        tempIndex = new ArrayList<TableKey>();
        for (TableKey tableKey : tableIndex) {
            if (tableKey.lineType == TableKey.PROTEIN) {
                Collections.sort(ratios);
                for (double ratioKey : ratios) {
                    tempIndex.addAll(tempMap.get(ratioKey));
                }
                ratios = new ArrayList<Double>();
                tempMap = new HashMap<Double, ArrayList<TableKey>>();
                tempIndex.add(tableKey);
            } else {
                try {
                    ratio = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                            tableKey.peptideIndex).getRatios().get(reporterIons.get(ion).getIndex()).getRatio();
                } catch (Exception e) {
                    ratio = Math.pow(9, 9);
                }
                if (!tempMap.containsKey(ratio)) {
                    tempMap.put(ratio, new ArrayList<TableKey>());
                    ratios.add(ratio);
                }
                tempMap.get(ratio).add(tableKey);
            }
        }
        tableIndex = tempIndex;

        for (TableKey key : tableIndex) {
            try {
                ratio = quantification.getProteinQuantification().get(key.proteinIndex).getProteinRatios().get(reporterIons.get(ion).getIndex()).getRatio();
            } catch (Exception e) {
                ratio = Math.pow(9, 9);
            }
            if (!tempMap.containsKey(ratio)) {
                tempMap.put(ratio, new ArrayList<TableKey>());
                ratios.add(ratio);
            }
            tempMap.get(ratio).add(key);
        }
        Collections.sort(ratios);
        tableIndex = new ArrayList<TableKey>();
        for (double keyRatio : ratios) {
            tableIndex.addAll(tempMap.get(keyRatio));
        }

        repaintResultsTable();
    }

    /**
     * Method used to sort the table according to the quality index
     */
    private void sortQuality() {
        ItraqScore score;
        HashMap<Double, ArrayList<TableKey>> tempMap = new HashMap<Double, ArrayList<TableKey>>();
        ArrayList<Double> qualities = new ArrayList<Double>();
        double quality;
        ArrayList<TableKey> tempIndex = new ArrayList<TableKey>();
        for (TableKey tableKey : tableIndex) {
            if (tableKey.lineType != TableKey.SPECTRUM) {
                Collections.sort(qualities);
                for (int i = qualities.size() - 1; i >= 0; i--) {
                    tempIndex.addAll(tempMap.get(qualities.get(i)));
                }
                qualities = new ArrayList<Double>();
                tempMap = new HashMap<Double, ArrayList<TableKey>>();
                tempIndex.add(tableKey);
            } else {
                try {
                    quality = ((ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                            tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(new ItraqScore())).getMinScore();
                } catch (Exception e) {
                    quality = 0;
                }
                if (!tempMap.containsKey(quality)) {
                    tempMap.put(quality, new ArrayList<TableKey>());
                    qualities.add(quality);
                }
                tempMap.get(quality).add(tableKey);
            }
        }
        tableIndex = tempIndex;

        tempIndex = new ArrayList<TableKey>();
        for (TableKey tableKey : tableIndex) {
            if (tableKey.lineType == TableKey.PROTEIN) {
                Collections.sort(qualities);
                for (int i = qualities.size() - 1; i >= 0; i--) {
                    tempIndex.addAll(tempMap.get(qualities.get(i)));
                }
                qualities = new ArrayList<Double>();
                tempMap = new HashMap<Double, ArrayList<TableKey>>();
                tempIndex.add(tableKey);
            } else {
                try {
                    quality = ((ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                            tableKey.peptideIndex).getUrParam(new ItraqScore())).getMinScore();
                } catch (Exception e) {
                    quality = 0;
                }
                if (!tempMap.containsKey(quality)) {
                    tempMap.put(quality, new ArrayList<TableKey>());
                    qualities.add(quality);
                }
                tempMap.get(quality).add(tableKey);
            }
        }
        tableIndex = tempIndex;

        for (TableKey key : tableIndex) {
            try {
                quality = ((ItraqScore) quantification.getProteinQuantification().get(key.proteinIndex).getUrParam(new ItraqScore())).getMinScore();
            } catch (Exception e) {
                quality = 0;
            }
            if (!tempMap.containsKey(quality)) {
                tempMap.put(quality, new ArrayList<TableKey>());
                qualities.add(quality);
            }
            tempMap.get(quality).add(key);
        }
        Collections.sort(qualities);
        tableIndex = new ArrayList<TableKey>();
        for (int i = qualities.size() - 1; i >= 0; i--) {
            tableIndex.addAll(tempMap.get(qualities.get(i)));
        }

        repaintResultsTable();
    }

    /**
     * Method called to sort the identifications
     */
    private void sort() {
        int column = resultTable.getSelectedColumn();
        if (column == 2) {
            sortProteins();
        } else if (column == 3) {
            TableKey tableKey = tableIndex.get(resultTable.getSelectedRow());
            if (tableKey.lineType == TableKey.PEPTIDE) {
                sortPeptides();
            } else {
                sortProteinsNPeptides();
            }
        } else if (column == 4) {
            // @TODO: implement sorting on variable modifications...
        } else if (column == 5) {
            TableKey tableKey = tableIndex.get(resultTable.getSelectedRow());
            if (tableKey.lineType == TableKey.SPECTRUM) {
                sortSpectra();
            } else if (tableKey.lineType == TableKey.PEPTIDE) {
                sortPeptidesNSpectra();
            } else if (tableKey.lineType == TableKey.PROTEIN) {
                sortProteinsNSpectra();
            }
        } else if (column > 5 && column < 5 + reporterIons.size()) {
            sortRatio(column - 5);
        } else if (column == 5 + reporterIons.size()) {
            sortQuality();
        }
    }

    /**
     * Table model for the result table
     */
    private class ResultTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            return tableIndex.size();
        }

        @Override
        public int getColumnCount() {
            return 5 + reporterIons.size();
        }

        @Override
        public String getColumnName(int column) {
            if (column == 2) {
                return "Protein";
            } else if (column == 3) {
                return "Peptide";
            } else if (column == 4) {
                return "Variable Modification(s)";
            } else if (column == 5) {
                return "Spectrum";
            } else if (column > 5 && column < 5 + reporterIons.size()) {
                int pos = column - 5;
                return quantification.getSample(reporterIons.get(pos).getIndex()).getReference() + "/"
                        + quantification.getSample(quantification.getReferenceLabel()).getReference();
            } else if (column == 5 + reporterIons.size()) {
                return "Quality";
            } else {
                return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            try {
                TableKey tableKey = tableIndex.get(row);
                if (column == 0) {
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        if (row == tableIndex.size() - 1) {
                            return "+";
                        } else if (tableIndex.get(row + 1).lineType == TableKey.PROTEIN) {
                            return "+";
                        } else if (tableIndex.get(row + 1).lineType == TableKey.PEPTIDE) {
                            return "-";
                        }
                    }
                    return " ";
                } else if (column == 1) {
                    if (tableKey.lineType == TableKey.PEPTIDE) {
                        if (row < getRowCount() && tableIndex.get(row + 1).lineType == TableKey.SPECTRUM) {
                            return "-";
                        } else {
                            return "+";
                        }
                    }
                    return " ";
                } else if (column == 2) {
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getProteinMatch().getTheoreticProtein().getAccession();
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        Peptide peptide = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getPeptideMatch().getTheoreticPeptide();
                        String accession = quantification.getProteinQuantification().get(tableKey.proteinIndex).getProteinMatch().getTheoreticProtein().getAccession();
                        ArrayList<String> otherProteins = new ArrayList<String>();
                        for (Protein protein : peptide.getParentProteins()) {
                            if (!protein.getAccession().equals(accession)) {
                                otherProteins.add(protein.getAccession());
                            }
                        }
                        Collections.sort(otherProteins);
                        String result = "";
                        for (String name : otherProteins) {
                            result+= name + " ";
                        }
                        return result;
                    } else {
                        return " ";
                    }
                } else if (column == 3) {
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getProteinMatch().getPeptideMatches().size();
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                                tableKey.peptideIndex).getPeptideMatch().getTheoreticPeptide().getSequence();
                    } else {
                        return " ";
                    }
                } else if (column == 4) {
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        return "";
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        String varmods = "";
                        String name;
                        for (ModificationMatch mod : quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                                tableKey.peptideIndex).getPeptideMatch().getTheoreticPeptide().getModificationMatches()) {
                            if (mod == null) {
                                name = "undefined";
                                if (varmods.lastIndexOf(name) < 0) {
                                    varmods += name + " ";
                                }
                            } else if (mod.isVariable()) {
                                name = mod.getTheoreticPtm().getName();
                                if (varmods.lastIndexOf(name) < 0) {
                                    varmods += name + " ";
                                }
                            }
                        }
                        return varmods;
                    } else {
                        return "";
                    }
                } else if (column == 5) {
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getProteinMatch().getSpectrumCount();
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().size();
                    } else {
                        MSnSpectrum spectrum = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                                tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getSpectrum();
                        return spectrum.getSpectrumTitle() + " in " + spectrum.getFileName();
                    }
                } else if (column > 5 && column < 5 + reporterIons.size()) {
                    int pos = column - 5;
                    Double ratio;
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        ratio = quantification.getProteinQuantification().get(tableKey.proteinIndex).getProteinRatios().get(reporterIons.get(pos).getIndex()).getRatio();
                        if (ratio == null) {
                            ratio = Double.NaN;
                        }
                        return ratio;
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        ratio = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getRatios().get(
                                reporterIons.get(pos).getIndex()).getRatio();
                        if (ratio == null) {
                            ratio = Double.NaN;
                        }
                        return ratio;
                    } else {
                        ratio = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                                tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getRatios().get(reporterIons.get(pos).getIndex()).getRatio();
                        if (ratio == null) {
                            ratio = Double.NaN;
                        }
                        return ratio;
                    }
                } else if (column == 5 + reporterIons.size()) {
                    ItraqScore itraqScore;
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        itraqScore = (ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getUrParam(new ItraqScore());
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        itraqScore = (ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                                tableKey.peptideIndex).getUrParam(new ItraqScore());
                    } else {
                        itraqScore = (ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                                tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(new ItraqScore());
                    }
                    return itraqScore.getMinScore();
                } else {
                    return " ";
                }
            } catch (Exception e) {
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
            return getValueAt(0, columnIndex).getClass();
        }
    }

    /**
     * The table key will be used to index a line
     */
    private class TableKey {

        /**
         * index for a protein line
         */
        public static final int PROTEIN = 0;
        /**
         * index for a peptide line
         */
        public static final int PEPTIDE = 1;
        /**
         * index for a spectrum line
         */
        public static final int SPECTRUM = 2;
        /**
         * index of the considered protein
         */
        public int proteinIndex;
        /**
         * index of the considered peptide
         */
        public int peptideIndex;
        /**
         * index of the considered spectrum
         */
        public int spectrumIndex;
        /**
         * type of the line as indexed by the static field
         */
        public int lineType;

        /**
         * constructor for a table key
         * @param lineType      type of the considered line
         * @param proteinIndex  index of the corresponding protein
         * @param peptideIndex  index of the corresponding peptide
         * @param spectrumIndex index of the corresponding spectrum
         */
        public TableKey(int lineType, int proteinIndex, int peptideIndex, int spectrumIndex) {
            this.proteinIndex = proteinIndex;
            this.peptideIndex = peptideIndex;
            this.spectrumIndex = spectrumIndex;
            this.lineType = lineType;
        }
    }

    /**
     * Table cell renderer for the result table
     */
    private class ReporterCellRenderer implements TableCellRenderer {

        /**
         * constructor for the cell renderer
         * @param table         The table
         * @param value         value of the table
         * @param isSelected    boolean indicating whether the cell is selected
         * @param hasFocus      boolean indicating whether the cell has focus
         * @param row           row number
         * @param column        column number
         * @return the adapted JLabel
         */
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            
            JLabel label;
            if (value instanceof JLabel) {
                label = (JLabel) value;
                label.setOpaque(true);
                if (row < tableIndex.size()) {
                    setColor(tableIndex.get(row), label, isSelected);
                    if (column > 5 && column < 5 + reporterIons.size()) {
                        setFont(tableIndex.get(row), label, column);
                    }
                }
            } else if (value instanceof Double) {
                label = new JLabel();
                label.setOpaque(true);
                if (row < tableIndex.size()) {
                    setColor(tableIndex.get(row), label, isSelected);
                    if (column > 5 && column < 5 + reporterIons.size()) {
                        setFont(tableIndex.get(row), label, column);
                    }
                }
                if (value != null) {
                    Double doubleValue = (Double) value;
                    doubleValue = Math.floor(doubleValue * 1000) / 1000;
                    label.setText(doubleValue + "");
                } else {
                    label.setText(" ");
                }
            } else {
                label = new JLabel();
                label.setOpaque(true);
                if (row < tableIndex.size()) {
                    setColor(tableIndex.get(row), label, isSelected);
                    if (column > 5 && column < 5 + reporterIons.size()) {
                        setFont(tableIndex.get(row), label, column);
                    }
                }
                if (value != null) {
                    label.setText(value.toString());
                } else {
                    label.setText(" ");
                }
            }

            // add selected cell highlighting
            if (hasFocus) {
                label.setBorder(new LineBorder(Color.BLACK, 1));
            } else {
                label.setBorder(null);
            }

            return label;
        }

        /**
         * Sets the color of the cell
         * @param tableKey  the corresponding line index
         * @param label     the label
         * @param selected  boolean indicating whether the cell is selected
         */
        private void setColor(TableKey tableKey, JLabel label, boolean selected) {
            if (!selected) {
                if (tableKey.lineType == TableKey.PROTEIN) {
                    label.setBackground(new Color(250, 250, 255));
                } else if (tableKey.lineType == TableKey.PEPTIDE) {
                    label.setBackground(new Color(240, 240, 255));
                } else if (tableKey.lineType == TableKey.SPECTRUM) {
                    label.setBackground(new Color(230, 230, 255));
                }
            } else {
                label.setBackground(new Color(200, 200, 240));
            }
        }

        /**
         * Sets the font of the cell
         * @param tableKey  the corresponding line index
         * @param label     the label
         * @param selected  boolean indicating whether the cell is selected
         */
        private void setFont(TableKey tableKey, JLabel label, int column) {
            IgnoredRatios ignoredRatios = new IgnoredRatios();
            if (tableKey.lineType == TableKey.PROTEIN) {
                ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getUrParam(ignoredRatios);
            } else if (tableKey.lineType == TableKey.PEPTIDE) {
                ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getUrParam(ignoredRatios);
            } else {
                ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(
                        tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(ignoredRatios);
            }
            int pos = column - 5;
            if (ignoredRatios.isIgnored(reporterIons.get(pos).getIndex())) {
                label.setForeground(Color.lightGray);
            }
        }
    }
}
