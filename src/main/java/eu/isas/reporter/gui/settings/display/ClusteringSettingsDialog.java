package eu.isas.reporter.gui.settings.display;

import eu.isas.reporter.calculation.clustering.ClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.PeptideClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.ProteinClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.PsmClusterClassKey;
import eu.isas.reporter.settings.ClusteringSettings;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import javax.swing.JFrame;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;
import no.uib.jsparklines.extra.NimbusCheckBoxRenderer;
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;

/**
 * Clustering settings dialog.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ClusteringSettingsDialog extends javax.swing.JDialog {

    /**
     * Boolean indicating whether the editing of the settings has been canceled.
     */
    private boolean canceled = false;
    /**
     * The available protein classes.
     */
    private ArrayList<String> proteinClasses;
    /**
     * The available peptide classes.
     */
    private ArrayList<String> peptideClasses;
    /**
     * The available PSM classes.
     */
    private ArrayList<String> psmClasses;
    /**
     * The protein classes map.
     */
    private HashMap<String, ProteinClusterClassKey> proteinClassesMap;
    /**
     * The peptide classes map.
     */
    private HashMap<String, PeptideClusterClassKey> peptideClassesMap;
    /**
     * The PSM classes map.
     */
    private HashMap<String, PsmClusterClassKey> psmClassesMap;
    /**
     * The selected protein classes.
     */
    private ArrayList<String> selectedProteinClasses;
    /**
     * The selected peptide classes.
     */
    private ArrayList<String> selectedPeptideClasses;
    /**
     * The selected PSM classes.
     */
    private ArrayList<String> selectedPsmClasses;
    /**
     * The classes color coding.
     */
    private HashMap<String, Color> classesColors;
    /**
     * Boolean indicating whether the settings can be edited.
     */
    private boolean editable;

    /**
     * Constructor.
     *
     * @param parentFrame the parent frame
     * @param clusteringSettings the clustering settings
     * @param editable if the content is editable or not
     */
    public ClusteringSettingsDialog(
            JFrame parentFrame,
            ClusteringSettings clusteringSettings,
            boolean editable
    ) {

        super(parentFrame, true);
        initComponents();
        this.editable = editable;
        populateGUI(clusteringSettings);
        setUpGui();
        setLocationRelativeTo(parentFrame);
        setVisible(true);

    }

    /**
     * Set up the GUI.
     *
     * @param editable boolean indicating whether the settings can be edited
     */
    private void setUpGui() {

        // set the column width and cell renderes
        TableColumn colorColumn = proteinClassesTable.getColumnModel().getColumn(0);
        colorColumn.setCellRenderer(new JSparklinesColorTableCellRenderer());
        colorColumn.setMaxWidth(35);
        colorColumn.setMinWidth(35);

        colorColumn = peptideClassesTable.getColumnModel().getColumn(0);
        colorColumn.setCellRenderer(new JSparklinesColorTableCellRenderer());
        colorColumn.setMaxWidth(35);
        colorColumn.setMinWidth(35);

        colorColumn = psmClassesTable.getColumnModel().getColumn(0);
        colorColumn.setCellRenderer(new JSparklinesColorTableCellRenderer());
        colorColumn.setMaxWidth(35);
        colorColumn.setMinWidth(35);

        colorColumn = proteinClassesTable.getColumnModel().getColumn(2);
        colorColumn.setCellRenderer(new NimbusCheckBoxRenderer());
        colorColumn.setMaxWidth(35);
        colorColumn.setMinWidth(35);

        colorColumn = peptideClassesTable.getColumnModel().getColumn(2);
        colorColumn.setCellRenderer(new NimbusCheckBoxRenderer());
        colorColumn.setMaxWidth(35);
        colorColumn.setMinWidth(35);

        colorColumn = psmClassesTable.getColumnModel().getColumn(2);
        colorColumn.setCellRenderer(new NimbusCheckBoxRenderer());
        colorColumn.setMaxWidth(35);
        colorColumn.setMinWidth(35);

        // set main table properties
        proteinClassesTable.getTableHeader().setReorderingAllowed(false);
        peptideClassesTable.getTableHeader().setReorderingAllowed(false);
        psmClassesTable.getTableHeader().setReorderingAllowed(false);

        // make sure that the scroll panes are see-through
        proteinClassesScrollPane.getViewport().setOpaque(false);
        peptideClassesScrollPane.getViewport().setOpaque(false);
        psmClassesScrollPane.getViewport().setOpaque(false);
    }

    /**
     * Fills the GUI with the given settings.
     *
     * @param clusteringSettings the clustering settings to display
     */
    private void populateGUI(ClusteringSettings clusteringSettings) {

        proteinClasses = new ArrayList<String>(clusteringSettings.getPossibleProteinClasses());
        peptideClasses = new ArrayList<String>(clusteringSettings.getPossiblePeptideClasses());
        psmClasses = new ArrayList<String>(clusteringSettings.getPossiblePsmClasses());
        selectedProteinClasses = new ArrayList<String>(clusteringSettings.getSelectedProteinClasses());
        selectedPeptideClasses = new ArrayList<String>(clusteringSettings.getSelectedPeptideClasses());
        selectedPsmClasses = new ArrayList<String>(clusteringSettings.getSelectedPsmClasses());

        classesColors = clusteringSettings.getClassesColors();

        proteinClassesMap = clusteringSettings.getProteinKeysMap();
        HashMap<String, ClusterClassKey> proteinKeysMap = new HashMap<>(proteinClassesMap);
        proteinClassesTable.setModel(new ClassListTableModel(proteinClasses, selectedProteinClasses, proteinKeysMap));

        peptideClassesMap = clusteringSettings.getPeptideKeysMap();
        HashMap<String, ClusterClassKey> peptideKeysMap = new HashMap<>(peptideClassesMap);
        peptideClassesTable.setModel(new ClassListTableModel(peptideClasses, selectedPeptideClasses, peptideKeysMap));

        psmClassesMap = clusteringSettings.getPsmKeysMap();
        HashMap<String, ClusterClassKey> psmKeysMap = new HashMap<>(psmClassesMap);
        psmClassesTable.setModel(new ClassListTableModel(psmClasses, selectedPsmClasses, psmKeysMap));

        updateGUI();
    }

    /**
     * Updates the GUI according to the dialog attributes.
     */
    private void updateGUI() {

        ((DefaultTableModel) proteinClassesTable.getModel()).fireTableDataChanged();
        ((DefaultTableModel) peptideClassesTable.getModel()).fireTableDataChanged();
        ((DefaultTableModel) psmClassesTable.getModel()).fireTableDataChanged();
    }

    /**
     * Indicates whether the user clicked the cancel button.
     *
     * @return a boolean indicating whether the user clicked the cancel button
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Returns the clustering settings as set by the user.
     *
     * @return the clustering settings as set by the user
     */
    public ClusteringSettings getClusteringSettings() {

        ArrayList<ProteinClusterClassKey> proteinClassesKeys = new ArrayList<>(proteinClasses.size());

        for (String classKey : proteinClasses) {
            proteinClassesKeys.add(proteinClassesMap.get(classKey));
        }

        ArrayList<PeptideClusterClassKey> peptideClassesKeys = new ArrayList<>(peptideClasses.size());

        for (String classKey : peptideClasses) {
            peptideClassesKeys.add(peptideClassesMap.get(classKey));
        }

        ArrayList<PsmClusterClassKey> psmClassesKeys = new ArrayList<>(psmClasses.size());

        for (String classKey : psmClasses) {
            psmClassesKeys.add(psmClassesMap.get(classKey));
        }

        ClusteringSettings clusteringSettings = new ClusteringSettings();
        clusteringSettings.setProteinClassKeys(proteinClassesKeys);
        clusteringSettings.setPeptideClassKeys(peptideClassesKeys);
        clusteringSettings.setPsmClassKeys(psmClassesKeys);

        for (int i = 0; i < proteinClasses.size(); i++) {

            Boolean selected = (Boolean) proteinClassesTable.getValueAt(i, 2);

            if (selected) {
                String selection = proteinClasses.get(i);
                clusteringSettings.addProteinClass(selection);
            }

        }

        for (int i = 0; i < peptideClasses.size(); i++) {

            Boolean selected = (Boolean) peptideClassesTable.getValueAt(i, 2);

            if (selected) {
                String selection = peptideClasses.get(i);
                clusteringSettings.addPeptideClass(selection);
            }

        }

        for (int i = 0; i < psmClasses.size(); i++) {

            Boolean selected = (Boolean) psmClassesTable.getValueAt(i, 2);

            if (selected) {
                String selection = psmClasses.get(i);
                clusteringSettings.addPsmClass(selection);
            }

        }

        clusteringSettings.setClassesColors(classesColors);
        return clusteringSettings;
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
        cancelButton = new javax.swing.JButton();
        okButton = new javax.swing.JButton();
        selectionPanel = new javax.swing.JPanel();
        proteinsClassesLbl = new javax.swing.JLabel();
        peptidesClassesLbl = new javax.swing.JLabel();
        psmsClassesLbl = new javax.swing.JLabel();
        proteinClassesScrollPane = new javax.swing.JScrollPane();
        proteinClassesTable = new javax.swing.JTable();
        peptideClassesScrollPane = new javax.swing.JScrollPane();
        peptideClassesTable = new javax.swing.JTable();
        psmClassesScrollPane = new javax.swing.JScrollPane();
        psmClassesTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Cluster Settings");
        setBackground(new java.awt.Color(230, 230, 230));
        addWindowListener(new java.awt.event.WindowAdapter() {
            public void windowClosing(java.awt.event.WindowEvent evt) {
                formWindowClosing(evt);
            }
        });

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        selectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Cluster Categories"));
        selectionPanel.setOpaque(false);

        proteinsClassesLbl.setText("Proteins");

        peptidesClassesLbl.setText("Peptides");

        psmsClassesLbl.setText("PSMs");

        proteinClassesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        proteinClassesScrollPane.setViewportView(proteinClassesTable);

        peptideClassesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        peptideClassesScrollPane.setViewportView(peptideClassesTable);

        psmClassesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        psmClassesScrollPane.setViewportView(psmClassesTable);

        javax.swing.GroupLayout selectionPanelLayout = new javax.swing.GroupLayout(selectionPanel);
        selectionPanel.setLayout(selectionPanelLayout);
        selectionPanelLayout.setHorizontalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinsClassesLbl)
                    .addComponent(psmsClassesLbl)
                    .addComponent(peptidesClassesLbl)
                    .addComponent(psmClassesScrollPane)
                    .addComponent(peptideClassesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 496, Short.MAX_VALUE)
                    .addComponent(proteinClassesScrollPane, javax.swing.GroupLayout.Alignment.TRAILING))
                .addContainerGap())
        );
        selectionPanelLayout.setVerticalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(proteinsClassesLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(proteinClassesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(peptidesClassesLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(peptideClassesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 121, Short.MAX_VALUE)
                .addGap(18, 18, 18)
                .addComponent(psmsClassesLbl)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(psmClassesScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 119, Short.MAX_VALUE)
                .addContainerGap())
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(selectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(cancelButton)
                    .addComponent(okButton))
                .addContainerGap())
        );

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(backgroundPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );

        pack();
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Close the dialog and keep the changes.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Close the dialog and cancel the changes.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed

        canceled = true;
        dispose();

    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Close the dialog and cancel the changes.
     *
     * @param evt
     */
    private void formWindowClosing(java.awt.event.WindowEvent evt) {//GEN-FIRST:event_formWindowClosing

        canceled = true;
        dispose();

    }//GEN-LAST:event_formWindowClosing


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JButton okButton;
    private javax.swing.JScrollPane peptideClassesScrollPane;
    private javax.swing.JTable peptideClassesTable;
    private javax.swing.JLabel peptidesClassesLbl;
    private javax.swing.JScrollPane proteinClassesScrollPane;
    private javax.swing.JTable proteinClassesTable;
    private javax.swing.JLabel proteinsClassesLbl;
    private javax.swing.JScrollPane psmClassesScrollPane;
    private javax.swing.JTable psmClassesTable;
    private javax.swing.JLabel psmsClassesLbl;
    private javax.swing.JPanel selectionPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Model for the cluster classes tables.
     */
    private class ClassListTableModel extends DefaultTableModel {

        /**
         * The classes to display.
         */
        private ArrayList<String> classes;
        /**
         * The classes selected.
         */
        private HashMap<String, Boolean> selectedClasses;
        /**
         * The keys map.
         */
        private HashMap<String, ClusterClassKey> keysMap;

        /**
         * Constructor.
         *
         * @param possibleClasses list of the keys of possible classes
         * @param selectedClasses list of the keys of selected classes
         * @param keysMap map of the key to class key object
         */
        public ClassListTableModel(
                ArrayList<String> possibleClasses,
                ArrayList<String> selectedClasses,
                HashMap<String, ClusterClassKey> keysMap
        ) {

            classes = possibleClasses;
            this.selectedClasses = new HashMap<String, Boolean>(classes.size());

            for (String category : classes) {

                if (selectedClasses.contains(category)) {
                    this.selectedClasses.put(category, true);
                } else {
                    this.selectedClasses.put(category, false);
                }

            }

            this.keysMap = keysMap;
        }

        @Override
        public int getRowCount() {

            if (classes == null) {
                return 0;
            }

            return classes.size();

        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {

            if (column == 2) {
                String key = classes.get(row);
                selectedClasses.put(key, !selectedClasses.get(key));
            }

        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {

            switch (column) {
                case 1:
                    return "Category";
                default:
                    return "";
            }

        }

        @Override
        public Object getValueAt(int row, int column) {

            switch (column) {

                case 0:

                    String key = classes.get(row);
                    Color color = classesColors.get(key);

                    if (color == null) {
                        color = Color.GRAY;
                    }

                    return color;

                case 1:

                    key = classes.get(row);
                    ClusterClassKey clusterClassKey = keysMap.get(key);
                    return clusterClassKey.getName();

                case 2:

                    key = classes.get(row);
                    return selectedClasses.get(key);

                default:

                    return "";

            }
        }

        @Override
        public Class getColumnClass(int columnIndex) {

            for (int i = 0; i < getRowCount(); i++) {

                if (getValueAt(i, columnIndex) != null) {
                    return getValueAt(i, columnIndex).getClass();
                }

            }

            return String.class;

        }

        @Override
        public boolean isCellEditable(int rowIndex, int columnIndex) {
            return columnIndex == 2;
        }
    }
}
