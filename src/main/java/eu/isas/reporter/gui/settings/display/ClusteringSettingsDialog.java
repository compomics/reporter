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
import no.uib.jsparklines.renderers.JSparklinesColorTableCellRenderer;

/**
 *
 * @author Marc Vaudel
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
     * Constructor.
     *
     * @param parentFrame the parent frame
     * @param clusteringSettings the clustering settings
     * @param editable
     */
    public ClusteringSettingsDialog(JFrame parentFrame, ClusteringSettings clusteringSettings, boolean editable) {
        super(parentFrame, true);
        initComponents();
        populateGUI(clusteringSettings);
        setUpGui(editable);
        setLocationRelativeTo(parentFrame);
        setVisible(true);
    }

    /**
     * Set up the GUI.
     *
     * @param editable boolean indicating whether the settings can be edited
     */
    private void setUpGui(boolean editable) {
        addProteinClass.setEnabled(editable);
        removeProteinClass.setEnabled(editable);
        addPeptideClass.setEnabled(editable);
        removePeptideClass.setEnabled(editable);
        addPsmClass.setEnabled(editable);
        removePsmClass.setEnabled(editable);

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
        HashMap<String, ClusterClassKey> proteinKeysMap = new HashMap<String, ClusterClassKey>(proteinClassesMap);
        proteinClassesTable.setModel(new ClassListTableModel(proteinClasses, selectedProteinClasses, proteinKeysMap));

        peptideClassesMap = clusteringSettings.getPeptideKeysMap();
        HashMap<String, ClusterClassKey> peptideKeysMap = new HashMap<String, ClusterClassKey>(peptideClassesMap);
        peptideClassesTable.setModel(new ClassListTableModel(peptideClasses, selectedPeptideClasses, peptideKeysMap));

        psmClassesMap = clusteringSettings.getPsmKeysMap();
        HashMap<String, ClusterClassKey> psmKeysMap = new HashMap<String, ClusterClassKey>(psmClassesMap);
        psmClassesTable.setModel(new ClassListTableModel(psmClasses, selectedPsmClasses, psmKeysMap));

        HashMap<String, ClusterClassKey> keysMap = new HashMap<String, ClusterClassKey>(proteinKeysMap.size() + peptideKeysMap.size() + psmKeysMap.size());
        keysMap.putAll(proteinKeysMap);
        keysMap.putAll(peptideKeysMap);
        keysMap.putAll(psmKeysMap);
        selectionTable.setModel(new SelectionTableModel(selectedProteinClasses, selectedPeptideClasses, selectedPsmClasses, keysMap));

        updateGUI();
    }

    /**
     * Updates the GUI according to the dialog attributes.
     */
    private void updateGUI() {

        ((DefaultTableModel) proteinClassesTable.getModel()).fireTableDataChanged();
        ((DefaultTableModel) peptideClassesTable.getModel()).fireTableDataChanged();
        ((DefaultTableModel) psmClassesTable.getModel()).fireTableDataChanged();
        ((DefaultTableModel) selectionTable.getModel()).fireTableDataChanged();
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
        ArrayList<ProteinClusterClassKey> proteinClassesKeys = new ArrayList<ProteinClusterClassKey>(proteinClasses.size());
        for (String classKey : proteinClasses) {
            proteinClassesKeys.add(proteinClassesMap.get(classKey));
        }

        ArrayList<PeptideClusterClassKey> peptideClassesKeys = new ArrayList<PeptideClusterClassKey>(peptideClasses.size());
        for (String classKey : peptideClasses) {
            peptideClassesKeys.add(peptideClassesMap.get(classKey));
        }

        ArrayList<PsmClusterClassKey> psmClassesKeys = new ArrayList<PsmClusterClassKey>(psmClasses.size());
        for (String classKey : psmClasses) {
            psmClassesKeys.add(psmClassesMap.get(classKey));
        }

        ClusteringSettings clusteringSettings = new ClusteringSettings();
        clusteringSettings.setProteinClassKeys(proteinClassesKeys);
        clusteringSettings.setPeptideClassKeys(peptideClassesKeys);
        clusteringSettings.setPsmClassKeys(psmClassesKeys);
        for (String selection : selectedProteinClasses) {
            clusteringSettings.addProteinClass(selection);
        }
        for (String selection : selectedPeptideClasses) {
            clusteringSettings.addPeptideClass(selection);
        }
        for (String selection : selectedPsmClasses) {
            clusteringSettings.addPsmClass(selection);
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
        jScrollPane5 = new javax.swing.JScrollPane();
        selectionTable = new javax.swing.JTable();
        proteinsLbl1 = new javax.swing.JLabel();
        addProteinClass = new javax.swing.JButton();
        removeProteinClass = new javax.swing.JButton();
        removePeptideClass = new javax.swing.JButton();
        addPeptideClass = new javax.swing.JButton();
        removePsmClass = new javax.swing.JButton();
        addPsmClass = new javax.swing.JButton();
        jScrollPane1 = new javax.swing.JScrollPane();
        proteinClassesTable = new javax.swing.JTable();
        jScrollPane2 = new javax.swing.JScrollPane();
        peptideClassesTable = new javax.swing.JTable();
        jScrollPane6 = new javax.swing.JScrollPane();
        psmClassesTable = new javax.swing.JTable();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setBackground(new java.awt.Color(230, 230, 230));

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

        selectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Cluster Classes Selection"));
        selectionPanel.setOpaque(false);

        proteinsClassesLbl.setText("Proteins:");

        peptidesClassesLbl.setText("Peptides:");

        psmsClassesLbl.setText("PSMs:");

        jScrollPane5.setViewportView(selectionTable);

        proteinsLbl1.setText("Selection:");

        addProteinClass.setText(">>");

        removeProteinClass.setText("<<");

        removePeptideClass.setText("<<");

        addPeptideClass.setText(">>");

        removePsmClass.setText("<<");

        addPsmClass.setText(">>");

        proteinClassesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane1.setViewportView(proteinClassesTable);

        peptideClassesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane2.setViewportView(peptideClassesTable);

        psmClassesTable.setModel(new javax.swing.table.DefaultTableModel(
            new Object [][] {

            },
            new String [] {

            }
        ));
        jScrollPane6.setViewportView(psmClassesTable);

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
                    .addComponent(jScrollPane1, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE)
                    .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                    .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                .addGap(45, 45, 45)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(addProteinClass)
                    .addComponent(removeProteinClass)
                    .addComponent(addPeptideClass)
                    .addComponent(removePeptideClass)
                    .addComponent(addPsmClass)
                    .addComponent(removePsmClass))
                .addGap(46, 46, 46)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(proteinsLbl1)
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 400, Short.MAX_VALUE))
                .addContainerGap())
        );
        selectionPanelLayout.setVerticalGroup(
            selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(selectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proteinsClassesLbl)
                    .addComponent(proteinsLbl1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(selectionPanelLayout.createSequentialGroup()
                        .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(selectionPanelLayout.createSequentialGroup()
                                .addComponent(addProteinClass)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removeProteinClass))
                            .addComponent(jScrollPane1, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE))
                        .addGap(18, 18, 18)
                        .addComponent(peptidesClassesLbl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane2, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGroup(selectionPanelLayout.createSequentialGroup()
                                .addComponent(addPeptideClass)
                                .addGap(8, 8, 8)
                                .addComponent(removePeptideClass)))
                        .addGap(18, 18, 18)
                        .addComponent(psmsClassesLbl)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(selectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jScrollPane6, javax.swing.GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
                            .addGroup(selectionPanelLayout.createSequentialGroup()
                                .addComponent(addPsmClass)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removePsmClass))))
                    .addComponent(jScrollPane5, javax.swing.GroupLayout.DEFAULT_SIZE, 426, Short.MAX_VALUE))
                .addContainerGap())
        );

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                        .addGap(0, 0, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 65, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addComponent(cancelButton))
                    .addComponent(selectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );
        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(selectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addGap(18, 18, 18)
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

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        dispose();
    }//GEN-LAST:event_okButtonActionPerformed

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        canceled = true;
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed


    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addPeptideClass;
    private javax.swing.JButton addProteinClass;
    private javax.swing.JButton addPsmClass;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JScrollPane jScrollPane1;
    private javax.swing.JScrollPane jScrollPane2;
    private javax.swing.JScrollPane jScrollPane5;
    private javax.swing.JScrollPane jScrollPane6;
    private javax.swing.JButton okButton;
    private javax.swing.JTable peptideClassesTable;
    private javax.swing.JLabel peptidesClassesLbl;
    private javax.swing.JTable proteinClassesTable;
    private javax.swing.JLabel proteinsClassesLbl;
    private javax.swing.JLabel proteinsLbl1;
    private javax.swing.JTable psmClassesTable;
    private javax.swing.JLabel psmsClassesLbl;
    private javax.swing.JButton removePeptideClass;
    private javax.swing.JButton removeProteinClass;
    private javax.swing.JButton removePsmClass;
    private javax.swing.JPanel selectionPanel;
    private javax.swing.JTable selectionTable;
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
        public ClassListTableModel(ArrayList<String> possibleClasses, ArrayList<String> selectedClasses, HashMap<String, ClusterClassKey> keysMap) {
            classes = new ArrayList<String>(possibleClasses.size());
            for (String possibleClass : possibleClasses) {
                if (!selectedClasses.contains(possibleClass)) {
                    classes.add(possibleClass);
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
        public int getColumnCount() {
            return 2;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Name";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String key = classes.get(row);
            switch (column) {
                case 0:
                    Color color = classesColors.get(key);
                    return color;
                case 1:
                    ClusterClassKey clusterClassKey = keysMap.get(key);
                    return clusterClassKey.getName();
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

    /**
     * Model for the selection table.
     */
    private class SelectionTableModel extends DefaultTableModel {

        /**
         * The classes to display.
         */
        private ArrayList<String> classes;
        /**
         * The number of protein classes.
         */
        private int nProteins;
        /**
         * The number of peptide classes.
         */
        private int nPeptides;
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
        public SelectionTableModel(ArrayList<String> selectedProteins, ArrayList<String> selectedPeptides, ArrayList<String> selectedPsms, HashMap<String, ClusterClassKey> keysMap) {
            classes = new ArrayList<String>(selectedProteins.size() + selectedPeptides.size() + selectedPsms.size());
            classes.addAll(selectedProteins);
            nProteins = selectedProteins.size();
            classes.addAll(selectedPeptides);
            nProteins = selectedPeptides.size();
            classes.addAll(selectedPsms);
            this.keysMap = new HashMap<String, ClusterClassKey>(keysMap);
        }

        @Override
        public int getRowCount() {
            if (classes == null) {
                return 0;
            }
            return classes.size();
        }

        @Override
        public int getColumnCount() {
            return 3;
        }

        @Override
        public String getColumnName(int column) {
            switch (column) {
                case 0:
                    return " ";
                case 1:
                    return "Category";
                case 2:
                    return "Name";
                default:
                    return "";
            }
        }

        @Override
        public Object getValueAt(int row, int column) {
            String key = classes.get(row);
            switch (column) {
                case 0:
                    Color color = classesColors.get(key);
                    return color;
                case 1:
                    if (row < nProteins) {
                        return "Proteins";
                    } else if (row - nProteins < nPeptides) {
                        return "Peptides";
                    } else {
                        return "PSMs";
                    }
                case 2:
                    ClusterClassKey clusterClassKey = keysMap.get(key);
                    return clusterClassKey.getName();
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
