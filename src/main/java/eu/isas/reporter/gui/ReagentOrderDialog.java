package eu.isas.reporter.gui;

import com.compomics.util.experiment.biology.ions.impl.ReporterIon;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethodFactory;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import eu.isas.reporter.Reporter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.extra.TrueFalseIconRenderer;
import org.xmlpull.v1.XmlPullParserException;

/**
 * Dialog for altering the reagent order.
 *
 * @author Harald Barsnes
 */
public class ReagentOrderDialog extends javax.swing.JDialog {

    /**
     * List of reagents used in this reporter method.
     */
    private ArrayList<String> reagents;
    /**
     * The method selected.
     */
    private ReporterMethod selectedMethod;
    /**
     * The reporter ion quantifications.
     */
    private ReporterIonQuantification reporterIonQuantification;
    /**
     * List of control samples.
     */
    private ArrayList<String> controlSamples;
    /**
     * The sample assignment table column header tooltips.
     */
    private ArrayList<String> sampleAssignmentTableToolTips;
    /**
     * The current methods file.
     */
    private File methodsFile;
    /**
     * The compomics reporter methods factory.
     */
    private ReporterMethodFactory methodsFactory = ReporterMethodFactory.getInstance();
    /**
     * Boolean indicating whether the user canceled the project creation
     */
    private boolean cancelled = false;
    /**
     * The ReporterGUI parent.
     */
    private ReporterGUI reporterGUI;

    /**
     * Creates a new ReagetOrderDialog.
     *
     * @param reporterGUI the parent frame
     * @param modal if the dialog is to be modal or not
     * @param reagents the reagents order
     * @param aSelectedMethod the selected method
     * @param reporterIonQuantification the reporter ion quantifications
     * @param controlSamples the control sample names
     */
    public ReagentOrderDialog(
            ReporterGUI reporterGUI,
            boolean modal,
            ArrayList<String> reagents,
            ReporterMethod aSelectedMethod,
            ReporterIonQuantification reporterIonQuantification,
            ArrayList<String> controlSamples
    ) {

        super(reporterGUI, modal);

        this.reporterGUI = reporterGUI;

        // copy the regagents
        this.reagents = new ArrayList<String>();
        this.reagents.addAll(reagents);

        selectedMethod = aSelectedMethod;
        this.reporterIonQuantification = reporterIonQuantification;
        this.controlSamples = controlSamples;

        methodsFile = Reporter.getMethodsFile();
        importMethods();

        initComponents();

        setUpGui();

        reporterMethodComboBox.setSelectedItem(aSelectedMethod.getName());
        setTableProperties();

        setLocationRelativeTo(reporterGUI);
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

        sampleAssignmentTable.getColumnModel().getColumn(3).setCellRenderer(
                new TrueFalseIconRenderer(
                        new ImageIcon(this.getClass().getResource("/icons/selected_green-new.png")),
                        null,
                        "Yes",
                        "No"
                )
        );
    }

    /**
     * Returns the new reagents order.
     *
     * @return the reagents order
     */
    public ArrayList<String> getReagentOrder() {
        return reagents;
    }

    /**
     * Indicates whether the user canceled the dialog.
     *
     * @return a boolean indicating whether the user canceled the dialog
     */
    public boolean isCancelled() {
        return cancelled;
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
     * Method called whenever an error was encountered while loading the
     * methods.
     */
    private void importMethodsError() {

        JOptionPane.showMessageDialog(
                this,
                "Default reporter methods file could not be parsed, please select a method file.",
                "No Spectrum File Selected",
                JOptionPane.WARNING_MESSAGE
        );

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

                JOptionPane.showMessageDialog(
                        null,
                        "File " + newFile + " could not be parsed.",
                        "Methods file error",
                        JOptionPane.WARNING_MESSAGE
                );

                importMethodsError();

            } catch (XmlPullParserException e) {

                JOptionPane.showMessageDialog(
                        this,
                        "An error occurred while parsing " + newFile + " at line " + e.getLineNumber() + ".",
                        "Parsing error",
                        JOptionPane.WARNING_MESSAGE
                );

                importMethodsError();
            }
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
        orderSettingsPanel = new javax.swing.JPanel();
        moveUpButton = new javax.swing.JButton();
        moveTopButton = new javax.swing.JButton();
        moveDownButton = new javax.swing.JButton();
        moveBottomButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Reagents Order");
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

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
        reporterMethodComboBox.setEnabled(false);

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

        javax.swing.GroupLayout orderSettingsPanelLayout = new javax.swing.GroupLayout(orderSettingsPanel);
        orderSettingsPanel.setLayout(orderSettingsPanelLayout);
        orderSettingsPanelLayout.setHorizontalGroup(
            orderSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, orderSettingsPanelLayout.createSequentialGroup()
                .addGap(0, 0, Short.MAX_VALUE)
                .addGroup(orderSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(moveUpButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(moveDownButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(moveBottomButton, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(moveTopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 36, javax.swing.GroupLayout.PREFERRED_SIZE)))
        );
        orderSettingsPanelLayout.setVerticalGroup(
            orderSettingsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, orderSettingsPanelLayout.createSequentialGroup()
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addComponent(moveTopButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(moveUpButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(moveDownButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(moveBottomButton, javax.swing.GroupLayout.PREFERRED_SIZE, 20, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        javax.swing.GroupLayout samplePanelLayout = new javax.swing.GroupLayout(samplePanel);
        samplePanel.setLayout(samplePanelLayout);
        samplePanelLayout.setHorizontalGroup(
            samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, samplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(samplePanelLayout.createSequentialGroup()
                        .addComponent(reporterMethodLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 120, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(reporterMethodComboBox, 0, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addComponent(sampleAssignmentJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 657, Short.MAX_VALUE))
                .addGap(22, 22, 22)
                .addComponent(orderSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18))
        );
        samplePanelLayout.setVerticalGroup(
            samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(samplePanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(reporterMethodLabel)
                    .addComponent(reporterMethodComboBox, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(samplePanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(sampleAssignmentJScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 342, Short.MAX_VALUE)
                    .addGroup(samplePanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(orderSettingsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                .addContainerGap())
        );

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(samplePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, layout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton)))
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(samplePanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap())
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

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
     * Clear the data and close the dialog.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing

        cancelled = true;
        this.dispose();

    }//GEN-LAST:event_formWindowClosing

    /**
     * Cancel the dialog.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed

        cancelled = true;
        this.dispose();

    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        this.dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton moveBottomButton;
    private javax.swing.JButton moveDownButton;
    private javax.swing.JButton moveTopButton;
    private javax.swing.JButton moveUpButton;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel orderSettingsPanel;
    private javax.swing.JComboBox reporterMethodComboBox;
    private javax.swing.JLabel reporterMethodLabel;
    private javax.swing.JScrollPane sampleAssignmentJScrollPane;
    private javax.swing.JTable sampleAssignmentTable;
    private javax.swing.JPanel samplePanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Resets the table indexes.
     */
    private void resetTableIndexes() {
        for (int i = 0; i < sampleAssignmentTable.getRowCount(); i++) {
            sampleAssignmentTable.setValueAt((i + 1), i, 0);
        }
    }

    /**
     * Table model for the sample to reporter ion assignment.
     */
    private class AssignementTableModel extends DefaultTableModel {

        @Override
        public int getRowCount() {
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
                    return reporterIonQuantification.getSample(reagentName);
                case 3:
                    return controlSamples.contains(reagentName);
                default:
                    return "";
            }

        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return getValueAt(0, columnIndex).getClass();
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
