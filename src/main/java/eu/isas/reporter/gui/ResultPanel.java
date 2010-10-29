
/*
 * ResultPanel.java
 *
 * Created on Oct 1, 2010, 4:57:40 PM
 */
package eu.isas.reporter.gui;

import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.reporter.Reporter;
import eu.isas.reporter.calculation.RatioEstimator;
import eu.isas.reporter.compomicsutilitiessettings.CompomicsKeysFactory;
import eu.isas.reporter.compomicsutilitiessettings.IgnoredRatios;
import eu.isas.reporter.compomicsutilitiessettings.ItraqScore;
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
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;

/**
 *
 * @author vaudel
 */
public class ResultPanel extends javax.swing.JPanel {

    private static final int DISP_OBSOLETE = -1;
    private static final int DISP_SPECTRUM = 0;
    private static final int DISP_PEPTIDE = 1;
    private static final int DISP_PROTEIN = 2;
    private static final int DISP_SPECTRUM_SCORE = 3;
    private static final int DISP_PEPTIDE_SCORE = 4;
    private static final int DISP_PROTEIN_SCORE = 5;
    private int displayed;
    private ReporterIonQuantification quantification;
    private ArrayList<ReporterIon> reporterIons;
    private Reporter parent;
    private MsExperiment experiment;
    private RatioEstimator ratioEstimator;
    private ArrayList<TableKey> tableIndex = new ArrayList<TableKey>();
    private CompomicsKeysFactory compomicsKeysFactory = CompomicsKeysFactory.getInstance();
    // QC Panels
    private JPanel horizontalPanel;
    private ProteinCharts proteinCharts;
    private PeptideCharts peptideCharts;
    private SpectrumCharts spectrumCharts;
    private ProteinScoreCharts proteinScoreCharts;
    private PeptideScoreCharts peptideScoreCharts;
    private SpectrumScoreCharts spectrumScoreCharts;
    private JPopupMenu tablePopUp;

    /** Creates new form ResultPanel */
    public ResultPanel(Reporter parent, ReporterIonQuantification quantification, MsExperiment experiment) {
        this.experiment = experiment;
        this.parent = parent;
        this.quantification = quantification;
        this.reporterIons = quantification.getMethod().getReporterIons();
        createTableIndex();

        initComponents();
        projectTxt.setText(experiment.getReference());

        resultTable.setAutoCreateRowSorter(false);
        resultTable.getColumnModel().getColumn(0).setMaxWidth(10);
        resultTable.getColumnModel().getColumn(1).setMaxWidth(10);
        resultTable.setDefaultRenderer(String.class, new ReporterCellRenderer());
        resultTable.setDefaultRenderer(Integer.class, new ReporterCellRenderer());
        resultTable.setDefaultRenderer(Double.class, new ReporterCellRenderer());

        // disables the user to drag column headers to reorder columns
        resultTable.getTableHeader().setReorderingAllowed(false);

        // centrally align the combobox
        separatorCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        if (validateInput()) {
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), getRatioMin(), getRatioMax());
            estimateAllProteinRatios();
        }

        proteinCharts = new ProteinCharts(quantification, getResolution());
        horizontalPanel = new JPanel();
        horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
        for (ReporterIon ion : reporterIons) {
            if (ion.getIndex() != quantification.getReferenceLabel()) {
                horizontalPanel.add(Box.createHorizontalStrut(10));
                horizontalPanel.add(proteinCharts.getChart(ion.getIndex()));
            }
            horizontalPanel.add(Box.createHorizontalStrut(10));
        }
        chartPanel.setLayout(new BoxLayout(chartPanel, BoxLayout.Y_AXIS));
        chartPanel.add(Box.createVerticalStrut(10));
        chartPanel.add(new JScrollPane(horizontalPanel));
        chartPanel.add(Box.createVerticalStrut(10));
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
                    }
                });
        tablePopUp = new JPopupMenu();
        tablePopUp.add(ignoreItem);
        tablePopUp.add(accountItem);
        tablePopUp.add(sortItem);
        tablePopUp.add(detailsItem);
    }

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
        jScrollPane1 = new javax.swing.JScrollPane();
        resultTable = new javax.swing.JTable();
        exitButton = new javax.swing.JButton();
        saveAsButton = new javax.swing.JButton();
        projectTxt = new javax.swing.JTextField();
        jLabel1 = new javax.swing.JLabel();
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

        resultTable.setModel(new ResultTable());
        resultTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                resultTableMouseClicked(evt);
            }
        });
        jScrollPane1.setViewportView(resultTable);

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

        projectTxt.setEditable(false);
        projectTxt.setText("projectId");
        projectTxt.setBorder(null);

        jLabel1.setText("Quantification Results");

        javax.swing.GroupLayout chartPanelLayout = new javax.swing.GroupLayout(chartPanel);
        chartPanel.setLayout(chartPanelLayout);
        chartPanelLayout.setHorizontalGroup(
            chartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 930, Short.MAX_VALUE)
        );
        chartPanelLayout.setVerticalGroup(
            chartPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 472, Short.MAX_VALUE)
        );

        javax.swing.GroupLayout jPanel1Layout = new javax.swing.GroupLayout(jPanel1);
        jPanel1.setLayout(jPanel1Layout);
        jPanel1Layout.setHorizontalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap(399, Short.MAX_VALUE)
                .addComponent(projectTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 85, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel1)
                .addGap(367, 367, 367))
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addGap(20, 20, 20)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                    .addComponent(chartPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, 930, Short.MAX_VALUE)
                    .addGroup(jPanel1Layout.createSequentialGroup()
                        .addComponent(saveAsButton, javax.swing.GroupLayout.PREFERRED_SIZE, 80, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(exitButton, javax.swing.GroupLayout.PREFERRED_SIZE, 84, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        jPanel1Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {exitButton, saveAsButton});

        jPanel1Layout.setVerticalGroup(
            jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, jPanel1Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(jLabel1)
                    .addComponent(projectTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(chartPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 386, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(jPanel1Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(exitButton)
                    .addComponent(saveAsButton))
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

        javax.swing.GroupLayout jPanel3Layout = new javax.swing.GroupLayout(jPanel3);
        jPanel3.setLayout(jPanel3Layout);
        jPanel3Layout.setHorizontalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                    .addComponent(ratioResolutionTxt)
                    .addComponent(kTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 76, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jLabel4)
                .addGap(130, 130, 130)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel5)
                    .addComponent(jLabel6))
                .addGap(18, 18, 18)
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(ratioMinTxt)
                    .addComponent(ratioMaxTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(423, Short.MAX_VALUE))
        );

        jPanel3Layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {kTxt, ratioMaxTxt, ratioMinTxt, ratioResolutionTxt});

        jPanel3Layout.setVerticalGroup(
            jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel3Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING)
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addComponent(ratioMaxTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addComponent(ratioMinTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addGroup(jPanel3Layout.createSequentialGroup()
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(ratioResolutionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel5))
                            .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                            .addGroup(jPanel3Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                .addComponent(kTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addComponent(jLabel4)
                                .addComponent(jLabel6)
                                .addComponent(jLabel3)))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        jPanel4.setBorder(javax.swing.BorderFactory.createTitledBorder("File Export"));

        jLabel7.setText("Exported Column Separator:");

        separatorCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Tab", "Comma", "Point", "§" }));

        javax.swing.GroupLayout jPanel4Layout = new javax.swing.GroupLayout(jPanel4);
        jPanel4.setLayout(jPanel4Layout);
        jPanel4Layout.setHorizontalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jLabel7)
                .addGap(18, 18, 18)
                .addComponent(separatorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 86, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(677, Short.MAX_VALUE))
        );
        jPanel4Layout.setVerticalGroup(
            jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel4Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel4Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel7)
                    .addComponent(separatorCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        javax.swing.GroupLayout jPanel2Layout = new javax.swing.GroupLayout(jPanel2);
        jPanel2.setLayout(jPanel2Layout);
        jPanel2Layout.setHorizontalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jPanel3, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(jPanel4, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        jPanel2Layout.setVerticalGroup(
            jPanel2Layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(jPanel2Layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(jPanel3, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(jPanel4, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap(754, Short.MAX_VALUE))
        );

        jTabbedPane1.addTab("Quantification Settings", jPanel2);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 965, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(jTabbedPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 963, Short.MAX_VALUE)
        );
    }// </editor-fold>//GEN-END:initComponents

    private void exitButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exitButtonActionPerformed
        parent.close(0);
    }//GEN-LAST:event_exitButtonActionPerformed

    private void ratioResolutionTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ratioResolutionTxtActionPerformed
        if (validateInput()) {
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), getRatioMin(), getRatioMax());
            estimateAllProteinRatios();
        }
    }//GEN-LAST:event_ratioResolutionTxtActionPerformed

    private void kTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_kTxtActionPerformed
        if (validateInput()) {
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), getRatioMin(), getRatioMax());
            estimateAllProteinRatios();
        }
    }//GEN-LAST:event_kTxtActionPerformed

    private void resultTableMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_resultTableMouseClicked
        if (evt.getButton() == MouseEvent.BUTTON1) {
            tableLeftClicked();
        } else if (evt.getButton() == MouseEvent.BUTTON3) {
            tablePopUp.show(resultTable, evt.getX(), evt.getY());
        }
    }//GEN-LAST:event_resultTableMouseClicked

    private void ratioMaxTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ratioMaxTxtActionPerformed
        if (validateInput()) {
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), getRatioMin(), getRatioMax());
            estimateAllProteinRatios();
        }
    }//GEN-LAST:event_ratioMaxTxtActionPerformed

    private void ratioMinTxtActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ratioMinTxtActionPerformed
        if (validateInput()) {
            ratioEstimator = new RatioEstimator(quantification, getResolution(), getK(), getRatioMin(), getRatioMax());
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
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel chartPanel;
    private javax.swing.JButton exitButton;
    private javax.swing.JLabel jLabel1;
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
    private javax.swing.JTabbedPane jTabbedPane1;
    private javax.swing.JTextField kTxt;
    private javax.swing.JTextField projectTxt;
    private javax.swing.JTextField ratioMaxTxt;
    private javax.swing.JTextField ratioMinTxt;
    private javax.swing.JTextField ratioResolutionTxt;
    private javax.swing.JTable resultTable;
    private javax.swing.JButton saveAsButton;
    private javax.swing.JComboBox separatorCmb;
    // End of variables declaration//GEN-END:variables

    private void estimateAllProteinRatios() {
        for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification()) {
            ratioEstimator.estimateRatios(proteinQuantification);
        }
    }

    private double getK() {
        return new Double(kTxt.getText().trim());
    }

    private double getResolution() {
        return new Double(ratioResolutionTxt.getText().trim());
    }

    private double getRatioMin() {
        return new Double(ratioMinTxt.getText().trim());
    }

    private double getRatioMax() {
        return new Double(ratioMaxTxt.getText().trim());
    }

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

    private int getNLines() {
        int nLines = 0;
        for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification()) {
            nLines++;
            for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
                nLines++;
                nLines += peptideQuantification.getSpectrumQuantification().size();
            }
        }
        return nLines;
    }

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

    private void ignore() {
        int row = resultTable.getSelectedRow();
        int column = resultTable.getSelectedColumn();
        TableKey tableKey = tableIndex.get(row);
        IgnoredRatios ignoredRatios;
        if (tableKey.lineType == TableKey.PROTEIN) {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
        } else if (tableKey.lineType == TableKey.PEPTIDE) {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
        } else {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
        }
        if (column > 4 && column < 4 + reporterIons.size()) {
            int pos = column - 4;
            ignoredRatios.ignore(reporterIons.get(pos).getIndex());
        } else {
            for (ReporterIon reporterIon : reporterIons) {
                ignoredRatios.ignore(reporterIon.getIndex());
            }
        }
        ratioEstimator.estimateRatios(quantification.getProteinQuantification().get(tableKey.proteinIndex));
        resultTable.repaint();
        proteinCharts = new ProteinCharts(quantification, getResolution());
        displayed = DISP_OBSOLETE;
        tableLeftClicked();
    }

    private void account() {
        int row = resultTable.getSelectedRow();
        int column = resultTable.getSelectedColumn();
        TableKey tableKey = tableIndex.get(row);
        IgnoredRatios ignoredRatios;
        if (tableKey.lineType == TableKey.PROTEIN) {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
        } else if (tableKey.lineType == TableKey.PEPTIDE) {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
        } else {
            ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
        }
        if (column > 4 && column < 4 + reporterIons.size()) {
            int pos = column - 4;
            ignoredRatios.account(reporterIons.get(pos).getIndex());
        } else {
            for (ReporterIon reporterIon : reporterIons) {
                ignoredRatios.account(reporterIon.getIndex());
            }
        }
        ratioEstimator.estimateRatios(quantification.getProteinQuantification().get(tableKey.proteinIndex));
        resultTable.repaint();
        proteinCharts = new ProteinCharts(quantification, getResolution());
        displayed = DISP_OBSOLETE;
        tableLeftClicked();
    }

    private void tableLeftClicked() {
        int row = resultTable.getSelectedRow();
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
            } else {
                horizontalPanel.removeAll();
                horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
                for (ReporterIon ion : reporterIons) {
                    if (ion.getIndex() != quantification.getReferenceLabel()) {
                        horizontalPanel.add(Box.createHorizontalStrut(10));
                        horizontalPanel.add(proteinCharts.getChart(ion.getIndex()));
                    }
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                }
                horizontalPanel.validate();
            }
            displayed = DISP_PROTEIN;
        } else if (column == 3) {
            if (peptideCharts == null) {
                peptideCharts = new PeptideCharts(tableKey.proteinIndex, quantification, getResolution());
            }
            ProteinQuantification proteinQuantification = quantification.getProteinQuantification().get(tableKey.proteinIndex);
            if (tableKey.lineType == TableKey.PROTEIN) {
                peptideCharts = new PeptideCharts(tableKey.proteinIndex, quantification, getResolution());
                horizontalPanel.removeAll();
                horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
                for (ReporterIon ion : reporterIons) {
                    if (ion.getIndex() != quantification.getReferenceLabel()) {
                        horizontalPanel.add(Box.createHorizontalStrut(10));
                        horizontalPanel.add(peptideCharts.getChart(ion.getIndex()));
                    }
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                }
                horizontalPanel.validate();
            } else if (tableKey.lineType == TableKey.PEPTIDE) {
                if (displayed == DISP_PEPTIDE && peptideCharts.getProteinIndex() == tableKey.proteinIndex) {
                    peptideCharts.setPeptide(proteinQuantification.getPeptideQuantification().get(tableKey.peptideIndex));
                } else {
                    peptideCharts = new PeptideCharts(tableKey.proteinIndex, quantification, getResolution());
                    horizontalPanel.removeAll();
                    horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
                    for (ReporterIon ion : reporterIons) {
                        if (ion.getIndex() != quantification.getReferenceLabel()) {
                            horizontalPanel.add(Box.createHorizontalStrut(10));
                            horizontalPanel.add(peptideCharts.getChart(ion.getIndex()));
                        }
                        horizontalPanel.add(Box.createHorizontalStrut(10));
                    }
                    horizontalPanel.validate();
                }
            }
            displayed = DISP_PEPTIDE;
        } else if (column == 4) {
            PeptideQuantification peptideQuantification = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex);
            if (tableKey.lineType == TableKey.PEPTIDE) {
                spectrumCharts = new SpectrumCharts(tableKey.proteinIndex, tableKey.peptideIndex, quantification, getResolution());
                horizontalPanel.removeAll();
                horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
                for (ReporterIon ion : reporterIons) {
                    if (ion.getIndex() != quantification.getReferenceLabel()) {
                        horizontalPanel.add(Box.createHorizontalStrut(10));
                        horizontalPanel.add(spectrumCharts.getChart(ion.getIndex()));
                    }
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                }
                horizontalPanel.validate();
            } else if (tableKey.lineType == TableKey.SPECTRUM) {
                if (displayed == DISP_SPECTRUM && spectrumCharts.getProteinIndex() == tableKey.proteinIndex && spectrumCharts.getPeptideIndex() == tableKey.peptideIndex) {
                    spectrumCharts.setSpectrum(peptideQuantification.getSpectrumQuantification().get(tableKey.spectrumIndex));
                } else {
                    spectrumCharts = new SpectrumCharts(tableKey.proteinIndex, tableKey.peptideIndex, quantification, getResolution());
                    horizontalPanel.removeAll();
                    horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
                    for (ReporterIon ion : reporterIons) {
                        if (ion.getIndex() != quantification.getReferenceLabel()) {
                            horizontalPanel.add(Box.createHorizontalStrut(10));
                            horizontalPanel.add(spectrumCharts.getChart(ion.getIndex()));
                        }
                        horizontalPanel.add(Box.createHorizontalStrut(10));
                    }
                    horizontalPanel.validate();
                }
                displayed = DISP_SPECTRUM;
            }
        } else if (column == 4 + reporterIons.size()) {
            if (tableKey.lineType == TableKey.PROTEIN) {
                if (displayed != DISP_PROTEIN_SCORE) {
                    proteinScoreCharts = new ProteinScoreCharts(quantification);
                    horizontalPanel.removeAll();
                    horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                    horizontalPanel.add(proteinScoreCharts.getChart());
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                    horizontalPanel.validate();
                }
                proteinScoreCharts.setProtein(quantification.getProteinQuantification().get(tableKey.proteinIndex));
                displayed = DISP_PROTEIN_SCORE;
            } else if (tableKey.lineType == TableKey.PEPTIDE) {
                if (displayed != DISP_PEPTIDE_SCORE) {
                    peptideScoreCharts = new PeptideScoreCharts(quantification);
                    horizontalPanel.removeAll();
                    horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                    horizontalPanel.add(peptideScoreCharts.getChart());
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                    horizontalPanel.validate();
                }
                peptideScoreCharts.setPeptide(quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex));
                displayed = DISP_PEPTIDE_SCORE;
            } else if (tableKey.lineType == TableKey.SPECTRUM) {
                if (displayed != DISP_SPECTRUM_SCORE) {
                    spectrumScoreCharts = new SpectrumScoreCharts(quantification);
                    horizontalPanel.removeAll();
                    horizontalPanel.setLayout(new BoxLayout(horizontalPanel, BoxLayout.X_AXIS));
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                    horizontalPanel.add(spectrumScoreCharts.getChart());
                    horizontalPanel.add(Box.createHorizontalStrut(10));
                    horizontalPanel.validate();
                }
                spectrumScoreCharts.setSpectrum(quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex));
                displayed = DISP_SPECTRUM_SCORE;
            }
        }
        this.repaint();
    }

    private void expandProtein(int row) {
        TableKey tableKey = tableIndex.get(row);
        int nPeptides = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().size();
        for (int i = nPeptides - 1; i >= 0; i--) {
            tableIndex.add(row + 1, new TableKey(TableKey.PEPTIDE, tableKey.proteinIndex, i, 0));
        }
    }

    private void expandPeptide(int row) {
        TableKey tableKey = tableIndex.get(row);
        int nSpectra = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().size();
        for (int i = nSpectra - 1; i >= 0; i--) {
            tableIndex.add(row + 1, new TableKey(TableKey.SPECTRUM, tableKey.proteinIndex, tableKey.peptideIndex, i));
        }
    }

    private void concatProtein(int row) {
        while (tableIndex.get(row + 1).lineType != TableKey.PROTEIN) {
            tableIndex.remove(row + 1);
            if (row == tableIndex.size() - 1) {
                break;
            }
        }
    }

    private void concatPeptide(int row) {
        while (tableIndex.get(row + 1).lineType != TableKey.PEPTIDE
                && tableIndex.get(row + 1).lineType != TableKey.PROTEIN) {
            tableIndex.remove(row + 1);
            if (row == tableIndex.size() - 1) {
                break;
            }
        }
    }

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
        resultTable.repaint();
    }

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
        resultTable.repaint();
    }

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
        resultTable.repaint();
    }

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
                key = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getSpectrum().getSpectrumTitle();
                if (!tempMap.containsKey(key)) {
                    tempMap.put(key, new ArrayList<TableKey>());
                    titles.add(key);
                }
                tempMap.get(key).add(tableKey);
            }
        }
        tableIndex = tempIndex;
        resultTable.repaint();
    }

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
        resultTable.repaint();
    }

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
        resultTable.repaint();
    }

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
                    ratio = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getRatios().get(reporterIons.get(ion).getIndex()).getRatio();
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
                    ratio = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getRatios().get(reporterIons.get(ion).getIndex()).getRatio();
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

        resultTable.repaint();
    }

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
                    quality = ((ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.SCORE))).getMinScore();
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
                    quality = ((ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.SCORE))).getMinScore();
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
                quality = ((ItraqScore) quantification.getProteinQuantification().get(key.proteinIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.SCORE))).getMinScore();
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

        resultTable.repaint();
    }

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
            TableKey tableKey = tableIndex.get(resultTable.getSelectedRow());
            if (tableKey.lineType == TableKey.SPECTRUM) {
                sortSpectra();
            } else if (tableKey.lineType == TableKey.PEPTIDE) {
                sortPeptidesNSpectra();
            } else if (tableKey.lineType == TableKey.PROTEIN) {
                sortProteinsNSpectra();
            }
        } else if (column > 4 && column < 4 + reporterIons.size()) {
            sortRatio(column - 4);
        } else if (column == 4 + reporterIons.size()) {
            sortQuality();
        }
    }

    // Private class
    private class ResultTable extends DefaultTableModel {

        @Override
        public int getRowCount() {
            tableIndex.size();
            return getNLines();
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
                return "Spectrum";
            } else if (column > 4 && column < 4 + reporterIons.size()) {
                int pos = column - 4;
                return quantification.getSample(reporterIons.get(pos).getIndex()).getReference() + "/"
                        + quantification.getSample(quantification.getReferenceLabel()).getReference();

            } else if (column == 4 + reporterIons.size()) {
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
                    } else {
                        return " ";
                    }
                } else if (column == 3) {
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getProteinMatch().getPeptideMatches().size();
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getPeptideMatch().getTheoreticPeptide().getSequence();
                    } else {
                        return " ";
                    }
                } else if (column == 4) {
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getProteinMatch().getSpectrumCount();
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().size();
                    } else {
                        MSnSpectrum spectrum = quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getSpectrum();
                        return spectrum.getSpectrumTitle() + " in " + spectrum.getFileName();
                    }
                } else if (column > 4 && column < 4 + reporterIons.size()) {
                    int pos = column - 4;
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getProteinRatios().get(reporterIons.get(pos).getIndex()).getRatio();
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getRatios().get(reporterIons.get(pos).getIndex()).getRatio();
                    } else {
                        return quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getRatios().get(reporterIons.get(pos).getIndex()).getRatio();
                    }
                } else if (column == 4 + reporterIons.size()) {
                    ItraqScore itraqScore;
                    if (tableKey.lineType == TableKey.PROTEIN) {
                        itraqScore = (ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.SCORE));
                    } else if (tableKey.lineType == TableKey.PEPTIDE) {
                        itraqScore = (ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.SCORE));
                    } else {
                        itraqScore = (ItraqScore) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.SCORE));
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
            for (int i = 0 ; i < getRowCount() ; i++) {
                if (getValueAt(i, columnIndex)!=null) {
                    return getValueAt(i, columnIndex).getClass();
                }
            }
            return getValueAt(0, columnIndex).getClass();
        }
    }

    private class TableKey {

        public static final int PROTEIN = 0;
        public static final int PEPTIDE = 1;
        public static final int SPECTRUM = 2;
        public int proteinIndex;
        public int peptideIndex;
        public int spectrumIndex;
        public int lineType;

        public TableKey(int lineType, int proteinIndex, int peptideIndex, int spectrumIndex) {
            this.proteinIndex = proteinIndex;
            this.peptideIndex = peptideIndex;
            this.spectrumIndex = spectrumIndex;
            this.lineType = lineType;
        }
    }

    private class ReporterCellRenderer implements TableCellRenderer {

        private JLabel label;

        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {

            if (value instanceof JLabel) {
                label = (JLabel) value;
                label.setOpaque(true);
                if (row < tableIndex.size()) {
                    setColor(tableIndex.get(row), label, isSelected);
                    if (column > 4 && column < 4 + reporterIons.size()) {
                        setFont(tableIndex.get(row), label, column);
                    }
                }
            } else if (value instanceof Double) {
                label = new JLabel();
                label.setOpaque(true);
                if (row < tableIndex.size()) {
                    setColor(tableIndex.get(row), label, isSelected);
                    if (column > 4 && column < 4 + reporterIons.size()) {
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
                    if (column > 4 && column < 4 + reporterIons.size()) {
                        setFont(tableIndex.get(row), label, column);
                    }
                }
                if (value != null) {
                    label.setText(value.toString());
                } else {
                    label.setText(" ");
                }
            }

            return label;
        }

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

        private void setFont(TableKey tableKey, JLabel label, int column) {
            IgnoredRatios ignoredRatios;
            if (tableKey.lineType == TableKey.PROTEIN) {
                ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
            } else if (tableKey.lineType == TableKey.PEPTIDE) {
                ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
            } else {
                ignoredRatios = (IgnoredRatios) quantification.getProteinQuantification().get(tableKey.proteinIndex).getPeptideQuantification().get(tableKey.peptideIndex).getSpectrumQuantification().get(tableKey.spectrumIndex).getUrParam(compomicsKeysFactory.getKey(CompomicsKeysFactory.IGNORED_RATIOS));
            }
            int pos = column - 4;
            if (ignoredRatios.isIgnored(reporterIons.get(pos).getIndex())) {
                label.setForeground(Color.lightGray);
            }
        }
    }
}
