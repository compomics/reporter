package eu.isas.reporter.gui;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.PTMFactory;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import javax.swing.JOptionPane;

/**
 * The preferences dialog.
 * 
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PreferencesDialog extends javax.swing.JDialog {

    /**
     * The Reproter GUI parent.
     */
    private ReporterGUI reporterGui;
    /**
     * The quantification preferences.
     */
    private QuantificationPreferences quantificationPreferences;
    /**
     * The ptms.
     */
    private HashMap<String, PTM> ptms = new HashMap<String, PTM>();
    /**
     * Modification file.
     */
    private final String MODIFICATIONS_FILE = "conf/reporter_mods.xml";
    /**
     * User modification file.
     */
    private final String USER_MODIFICATIONS_FILE = "conf/reporter_usermods.xml";
    /**
     * The compomics PTM factory.
     */
    private PTMFactory ptmFactory = PTMFactory.getInstance();

    /**
     * Creates new form PreferencesDialog
     * 
     * @param reporterGui reference to the ReporterGUI
     * @param quantificationPreferences the quantification preferences
     */
    public PreferencesDialog(ReporterGUI reporterGui, QuantificationPreferences quantificationPreferences) {
        super(reporterGui, true);
        this.reporterGui = reporterGui;
        this.quantificationPreferences = quantificationPreferences;
        initComponents();
        loadModifications();
        loadValues();
        updateModificationList();
        setLocationRelativeTo(reporterGui);
        setVisible(true);
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
        idSelectionPanel = new javax.swing.JPanel();
        miscleavageCheck = new javax.swing.JCheckBox();
        jLabel1 = new javax.swing.JLabel();
        selectedPtmsScrollPane = new javax.swing.JScrollPane();
        selectedPTMs = new javax.swing.JList();
        allPtmsScrollPane = new javax.swing.JScrollPane();
        allPTMs = new javax.swing.JList();
        jLabel8 = new javax.swing.JLabel();
        addModifications = new javax.swing.JButton();
        removeModification = new javax.swing.JButton();
        ratioEstimationsPanel = new javax.swing.JPanel();
        nullIntensitiesCheck = new javax.swing.JCheckBox();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        ratioMaxTxt = new javax.swing.JTextField();
        ratioMinTxt = new javax.swing.JTextField();
        jLabel4 = new javax.swing.JLabel();
        jLabel5 = new javax.swing.JLabel();
        jLabel6 = new javax.swing.JLabel();
        kTxt = new javax.swing.JTextField();
        resolutionTxt = new javax.swing.JTextField();
        cancelButton = new javax.swing.JButton();
        helpLabel = new javax.swing.JLabel();
        helpLinkLabel = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Quantification Preferences");

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        idSelectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Identifications Selection"));
        idSelectionPanel.setOpaque(false);

        miscleavageCheck.setText("Ignore miscleaved peptides");
        miscleavageCheck.setIconTextGap(10);
        miscleavageCheck.setOpaque(false);

        jLabel1.setFont(jLabel1.getFont().deriveFont((jLabel1.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel1.setText("Ignore peptides presenting the following PTMS:");

        selectedPtmsScrollPane.setViewportView(selectedPTMs);

        allPTMs.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        allPtmsScrollPane.setViewportView(allPTMs);

        jLabel8.setFont(jLabel8.getFont().deriveFont((jLabel8.getFont().getStyle() | java.awt.Font.ITALIC)));
        jLabel8.setText("Available PTMs:");

        addModifications.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowUp_grey.png"))); // NOI18N
        addModifications.setText("Add");
        addModifications.setToolTipText("Add to list of expected modifications");
        addModifications.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        addModifications.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowUp.png"))); // NOI18N
        addModifications.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addModificationsActionPerformed(evt);
            }
        });

        removeModification.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowDown_grey.png"))); // NOI18N
        removeModification.setText("Remove");
        removeModification.setToolTipText("Remove from list of selected modifications");
        removeModification.setHorizontalAlignment(javax.swing.SwingConstants.LEFT);
        removeModification.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/arrowDown.png"))); // NOI18N
        removeModification.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                removeModificationActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout idSelectionPanelLayout = new javax.swing.GroupLayout(idSelectionPanel);
        idSelectionPanel.setLayout(idSelectionPanelLayout);
        idSelectionPanelLayout.setHorizontalGroup(
            idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSelectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(idSelectionPanelLayout.createSequentialGroup()
                        .addComponent(jLabel8)
                        .addGap(0, 0, Short.MAX_VALUE))
                    .addGroup(idSelectionPanelLayout.createSequentialGroup()
                        .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(javax.swing.GroupLayout.Alignment.TRAILING, idSelectionPanelLayout.createSequentialGroup()
                                .addGap(0, 0, Short.MAX_VALUE)
                                .addComponent(addModifications, javax.swing.GroupLayout.PREFERRED_SIZE, 99, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(removeModification, javax.swing.GroupLayout.PREFERRED_SIZE, 119, javax.swing.GroupLayout.PREFERRED_SIZE))
                            .addComponent(miscleavageCheck)
                            .addComponent(allPtmsScrollPane)
                            .addComponent(selectedPtmsScrollPane)
                            .addComponent(jLabel1))
                        .addContainerGap())))
        );

        idSelectionPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {addModifications, removeModification});

        idSelectionPanelLayout.setVerticalGroup(
            idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSelectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(miscleavageCheck)
                .addGap(23, 23, 23)
                .addComponent(jLabel1)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectedPtmsScrollPane)
                .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(idSelectionPanelLayout.createSequentialGroup()
                        .addGap(36, 36, 36)
                        .addComponent(jLabel8))
                    .addGroup(idSelectionPanelLayout.createSequentialGroup()
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(removeModification, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(addModifications, javax.swing.GroupLayout.PREFERRED_SIZE, 28, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(allPtmsScrollPane)
                .addContainerGap())
        );

        ratioEstimationsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Ratio Estimation"));
        ratioEstimationsPanel.setOpaque(false);

        nullIntensitiesCheck.setText("Ignore null intensities");
        nullIntensitiesCheck.setIconTextGap(10);
        nullIntensitiesCheck.setOpaque(false);

        jLabel2.setText("Minimum Ratio:");

        jLabel3.setText("Maximum Ratio:");

        ratioMaxTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        ratioMinTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        jLabel4.setText("Resolution:");

        jLabel5.setText("Window Width:");

        jLabel6.setText("* MAD");

        kTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        resolutionTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        javax.swing.GroupLayout ratioEstimationsPanelLayout = new javax.swing.GroupLayout(ratioEstimationsPanel);
        ratioEstimationsPanel.setLayout(ratioEstimationsPanelLayout);
        ratioEstimationsPanelLayout.setHorizontalGroup(
            ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(nullIntensitiesCheck)
                    .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel2)
                            .addComponent(jLabel3))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(ratioMaxTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 167, Short.MAX_VALUE)
                            .addComponent(ratioMinTxt))
                        .addGap(18, 18, 18)
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(jLabel4, javax.swing.GroupLayout.Alignment.TRAILING)
                            .addComponent(jLabel5, javax.swing.GroupLayout.Alignment.TRAILING))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.UNRELATED)
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                                .addComponent(kTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 123, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(jLabel6))
                            .addComponent(resolutionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 167, javax.swing.GroupLayout.PREFERRED_SIZE))))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        ratioEstimationsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {jLabel2, jLabel3, jLabel4, jLabel5});

        ratioEstimationsPanelLayout.setVerticalGroup(
            ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel2)
                            .addComponent(ratioMinTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(ratioMaxTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel3)))
                    .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(resolutionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel4))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                            .addComponent(jLabel6)
                            .addComponent(kTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(jLabel5))))
                .addGap(18, 18, 18)
                .addComponent(nullIntensitiesCheck)
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        helpLabel.setFont(helpLabel.getFont().deriveFont((helpLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        helpLabel.setText("For ratio estimation help see:");

        helpLinkLabel.setText("<html> <a href=\\\"dummy_link\">Burkhart et al.  (2011) [ PMID: 21328540]</a></html>");
        helpLinkLabel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseClicked(java.awt.event.MouseEvent evt) {
                helpLinkLabelMouseClicked(evt);
            }
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                helpLinkLabelMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                helpLinkLabelMouseExited(evt);
            }
        });

        okButton.setText("OK");
        okButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                okButtonActionPerformed(evt);
            }
        });

        javax.swing.GroupLayout backgroundPanelLayout = new javax.swing.GroupLayout(backgroundPanel);
        backgroundPanel.setLayout(backgroundPanelLayout);
        backgroundPanelLayout.setHorizontalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGap(10, 10, 10)
                        .addComponent(helpLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(helpLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 66, Short.MAX_VALUE)
                        .addComponent(okButton)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton))
                    .addComponent(idSelectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(ratioEstimationsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {cancelButton, okButton});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(idSelectionPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(ratioEstimationsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(helpLabel)
                    .addComponent(helpLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
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

    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed
        dispose();
    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Changes the cursor to the hand cursor when over the help link.
     *
     * @param evt
     */
    private void helpLinkLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLinkLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_helpLinkLabelMouseEntered

    /**
     * Changes the cursor back to the default cursor when leaving the help link.
     *
     * @param evt
     */
    private void helpLinkLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLinkLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpLinkLabelMouseExited

    /**
     * Opens the help link in the web browser.
     *
     * @param evt
     */
    private void helpLinkLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLinkLabelMouseClicked
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("http://onlinelibrary.wiley.com/doi/10.1002/pmic.201000711/abstract");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpLinkLabelMouseClicked

    private void addModificationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addModificationsActionPerformed

        int nSelected = selectedPTMs.getModel().getSize();
        int nNew = allPTMs.getSelectedIndices().length;
        String[] fixedModifications = new String[nSelected + nNew];
        int cpt = 0;
        String name;
        for (int i = 0; i < nSelected; i++) {
            fixedModifications[cpt] = (String) selectedPTMs.getModel().getElementAt(i);
            cpt++;
        }
        boolean found;
        for (Object selection : allPTMs.getSelectedValues()) {
            name = (String) selection;
            found = false;
            for (int i = 0; i < selectedPTMs.getModel().getSize(); i++) {
                if (((String) selectedPTMs.getModel().getElementAt(i)).equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                fixedModifications[cpt] = name;
                cpt++;
            }
        }
        selectedPTMs.setListData(fixedModifications);
        updateModificationList();
    }//GEN-LAST:event_addModificationsActionPerformed

    private void removeModificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeModificationActionPerformed

        int nSelected = selectedPTMs.getModel().getSize();
        int nToRemove = selectedPTMs.getSelectedIndices().length;
        String[] variableModifications = new String[nSelected - nToRemove];
        int cpt = 0;
        boolean found;
        for (int i = 0; i < selectedPTMs.getModel().getSize(); i++) {
            found = false;
            for (Object selection : selectedPTMs.getSelectedValues()) {
                if (((String) selectedPTMs.getModel().getElementAt(i)).equals((String) selection)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                variableModifications[cpt] = (String) selectedPTMs.getModel().getElementAt(i);
                cpt++;
            }
        }
        selectedPTMs.setListData(variableModifications);
        updateModificationList();
    }//GEN-LAST:event_removeModificationActionPerformed

    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed
        if (validateInput()) {
            saveValues();
            dispose();
        }
    }//GEN-LAST:event_okButtonActionPerformed

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addModifications;
    private javax.swing.JList allPTMs;
    private javax.swing.JScrollPane allPtmsScrollPane;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JLabel helpLabel;
    private javax.swing.JLabel helpLinkLabel;
    private javax.swing.JPanel idSelectionPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JLabel jLabel4;
    private javax.swing.JLabel jLabel5;
    private javax.swing.JLabel jLabel6;
    private javax.swing.JLabel jLabel8;
    private javax.swing.JTextField kTxt;
    private javax.swing.JCheckBox miscleavageCheck;
    private javax.swing.JCheckBox nullIntensitiesCheck;
    private javax.swing.JButton okButton;
    private javax.swing.JPanel ratioEstimationsPanel;
    private javax.swing.JTextField ratioMaxTxt;
    private javax.swing.JTextField ratioMinTxt;
    private javax.swing.JButton removeModification;
    private javax.swing.JTextField resolutionTxt;
    private javax.swing.JList selectedPTMs;
    private javax.swing.JScrollPane selectedPtmsScrollPane;
    // End of variables declaration//GEN-END:variables

    /**
     * Loads values from the quantificationPreferences.
     */
    private void loadValues() {
        miscleavageCheck.setSelected(quantificationPreferences.isIgnoreMissedCleavages());
        nullIntensitiesCheck.setSelected(quantificationPreferences.isIgnoreNullIntensities());
        kTxt.setText(quantificationPreferences.getK() + "");
        ratioMinTxt.setText(quantificationPreferences.getRatioMin() + "");
        ratioMaxTxt.setText(quantificationPreferences.getRatioMax() + "");
        resolutionTxt.setText(quantificationPreferences.getRatioResolution() + "");
        ArrayList<String> selectedModificationsList = quantificationPreferences.getIgnoredPTM();
        String[] allModificationsAsArray = new String[selectedModificationsList.size()];
        for (int i = 0; i < selectedModificationsList.size(); i++) {
            allModificationsAsArray[i] = selectedModificationsList.get(i);
        }
        selectedPTMs.setListData(allModificationsAsArray);
        updateModificationList();
    }

    /**
     * Saves the values in the quantificationPreferences.
     */
    private void saveValues() {
        quantificationPreferences.setIgnoreMissedCleavages(miscleavageCheck.isSelected());
        quantificationPreferences.setIgnoreNullIntensities(nullIntensitiesCheck.isSelected());
        quantificationPreferences.setK(new Double(kTxt.getText()));
        quantificationPreferences.setRatioMin(new Double(ratioMinTxt.getText()));
        quantificationPreferences.setRatioMax(new Double(ratioMaxTxt.getText()));
        quantificationPreferences.setRatioResolution(new Double(resolutionTxt.getText()));
        quantificationPreferences.emptyPTMList();
        String name;
        for (int j = 0; j < selectedPTMs.getModel().getSize(); j++) {
            name = (String) selectedPTMs.getModel().getElementAt(j);
            quantificationPreferences.addIgnoredPTM(ptms.get(name));
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
            test = new Double(kTxt.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "please enter a correct window width.", "Window width error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(ratioMinTxt.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "please enter a correct minimal ratio.", "Minimal ratio error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        try {
            test = new Double(resolutionTxt.getText());
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "please enter a correct ratio resolution.", "Ratio resolution error", JOptionPane.ERROR_MESSAGE);
            return false;
        }
        return true;
    }

    /**
     * Loads the modifications from the modification file.
     */
    private void loadModifications() {
        try {
            ptmFactory.importModifications(new File(MODIFICATIONS_FILE), false);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
        try {
            ptmFactory.importModifications(new File(USER_MODIFICATIONS_FILE), true);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(null, "An error (" + e.getMessage() + ") occured when trying to load the modifications from " + USER_MODIFICATIONS_FILE + ".",
                    "Configuration import Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    /**
     * Updates the modification list (right).
     */
    private void updateModificationList() {
        ArrayList<String> allModificationsList = new ArrayList<String>(ptms.keySet());
        int nSelected = selectedPTMs.getModel().getSize();
        ArrayList<String> allModifications = new ArrayList<String>();

        boolean found = false;

        for (String name : allModificationsList) {
            found = false;
            for (int j = 0; j < nSelected; j++) {
                if (((String) selectedPTMs.getModel().getElementAt(j)).equals(name)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                allModifications.add(name);
            }
        }

        String[] allModificationsAsArray = new String[allModifications.size()];

        for (int i = 0; i < allModifications.size(); i++) {
            allModificationsAsArray[i] = allModifications.get(i);
        }

        Arrays.sort(allModificationsAsArray);
        allPTMs.setListData(allModificationsAsArray);
        allPTMs.setSelectedIndex(0);
    }
}
