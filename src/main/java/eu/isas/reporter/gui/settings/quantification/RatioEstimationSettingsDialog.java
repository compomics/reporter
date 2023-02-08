package eu.isas.reporter.gui.settings.quantification;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.gui.renderers.AlignedListCellRenderer;
import com.compomics.util.parameters.identification.search.ModificationParameters;
import eu.isas.reporter.settings.RatioEstimationSettings;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingConstants;

/**
 * The preferences dialog.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class RatioEstimationSettingsDialog extends javax.swing.JDialog {

    /**
     * Boolean indicating whether the editing of the settings has been canceled.
     */
    private boolean canceled = false;
    /**
     * The modification profile of the search.
     */
    private ModificationParameters modificationParameters;

    /**
     * Creates a new RatioEstimationSettingsDialog.
     *
     * @param parentDialog the parent dialog
     * @param ratioEstimationSettings the settings to display
     * @param modificationParameters the modification profile of the search
     * @param editable boolean indicating whether the settings can be edited
     */
    public RatioEstimationSettingsDialog(
            JDialog parentDialog,
            RatioEstimationSettings ratioEstimationSettings,
            ModificationParameters modificationParameters,
            boolean editable
    ) {

        super(parentDialog, true);
        this.modificationParameters = modificationParameters;
        initComponents();
        setUpGui(editable);
        populateGUI(ratioEstimationSettings);
        setLocationRelativeTo(parentDialog);
        setVisible(true);

    }

    /**
     * Set up the GUI.
     *
     * @param editable boolean indicating whether the settings can be edited
     */
    private void setUpGui(boolean editable) {

        // centrally align the comboboxes
        proteinValidationCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        peptideValidationCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        psmValidationCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        excludeMissingIntensitiesCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        excludeSharedCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));
        excludeMiscleavedCmb.setRenderer(new AlignedListCellRenderer(SwingConstants.CENTER));

        //@TODO: Set editable or not
    }

    /**
     * Fills the gui with the given settings.
     *
     * @param ratioEstimationSettings the settings to display
     */
    private void populateGUI(RatioEstimationSettings ratioEstimationSettings) {

        proteinValidationCmb.setSelectedIndex(ratioEstimationSettings.getProteinValidationLevel().getIndex());
        peptideValidationCmb.setSelectedIndex(ratioEstimationSettings.getPeptideValidationLevel().getIndex());
        psmValidationCmb.setSelectedIndex(ratioEstimationSettings.getPsmValidationLevel().getIndex());

        if (ratioEstimationSettings.isIgnoreNullIntensities()) {
            excludeMissingIntensitiesCmb.setSelectedIndex(0);
        } else {
            excludeMissingIntensitiesCmb.setSelectedIndex(1);
        }

        widthTxt.setText(ratioEstimationSettings.getPercentile() + "");
        resolutionTxt.setText(ratioEstimationSettings.getRatioResolution() + "");

        ArrayList<String> selectedModificationsList = new ArrayList<>(ratioEstimationSettings.getExcludingPtms());
        Collections.sort(selectedModificationsList);
        String[] allModificationsAsArray = new String[selectedModificationsList.size()];

        for (int i = 0; i < selectedModificationsList.size(); i++) {
            allModificationsAsArray[i] = selectedModificationsList.get(i);
        }

        selectedPTMs.setListData(allModificationsAsArray);
        updateModificationList();

        int nUnique = ratioEstimationSettings.getMinUnique();
        boolean excludeShared = nUnique >= 0;

        if (excludeShared) {
            excludeSharedCmb.setSelectedIndex(0);
        } else {
            excludeSharedCmb.setSelectedIndex(1);
        }

        uniquePeptidesSpinner.setEnabled(excludeShared);

        if (nUnique >= 0) {
            uniquePeptidesSpinner.setValue(ratioEstimationSettings.getMinUnique());
        }

        if (ratioEstimationSettings.isIgnoreMissedCleavages()) {
            excludeMiscleavedCmb.setSelectedIndex(0);
        } else {
            excludeMiscleavedCmb.setSelectedIndex(1);
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

        backgroundPanel = new javax.swing.JPanel();
        idSelectionPanel = new javax.swing.JPanel();
        excludePeptidesLabel = new javax.swing.JLabel();
        selectedPtmsScrollPane = new javax.swing.JScrollPane();
        selectedPTMs = new javax.swing.JList();
        allPtmsScrollPane = new javax.swing.JScrollPane();
        allPTMs = new javax.swing.JList();
        availablePtmsLabel = new javax.swing.JLabel();
        addModifications = new javax.swing.JButton();
        removeModification = new javax.swing.JButton();
        ratioEstimationsPanel = new javax.swing.JPanel();
        resolutionLabel = new javax.swing.JLabel();
        windowWidthLabel = new javax.swing.JLabel();
        widthTxt = new javax.swing.JTextField();
        resolutionTxt = new javax.swing.JTextField();
        proteinsLabel = new javax.swing.JLabel();
        peptidesLabel = new javax.swing.JLabel();
        psmsLabel = new javax.swing.JLabel();
        peptideValidationCmb = new javax.swing.JComboBox();
        proteinValidationCmb = new javax.swing.JComboBox();
        psmValidationCmb = new javax.swing.JComboBox();
        jLabel1 = new javax.swing.JLabel();
        excludeMissingIntensitiesCmb = new javax.swing.JComboBox();
        helpLabel = new javax.swing.JLabel();
        helpLinkLabel = new javax.swing.JLabel();
        okButton = new javax.swing.JButton();
        cancelButton = new javax.swing.JButton();
        proteinGroupJPanel = new javax.swing.JPanel();
        uniquePeptidesSpinner = new javax.swing.JSpinner();
        uniquePeptidesLbl = new javax.swing.JLabel();
        jLabel2 = new javax.swing.JLabel();
        jLabel3 = new javax.swing.JLabel();
        excludeSharedCmb = new javax.swing.JComboBox();
        excludeMiscleavedCmb = new javax.swing.JComboBox();

        setDefaultCloseOperation(javax.swing.WindowConstants.DISPOSE_ON_CLOSE);
        setTitle("Ratio Estimation Settings");
        setResizable(false);

        backgroundPanel.setBackground(new java.awt.Color(230, 230, 230));

        idSelectionPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("PTM Selection"));
        idSelectionPanel.setOpaque(false);

        excludePeptidesLabel.setFont(excludePeptidesLabel.getFont().deriveFont((excludePeptidesLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        excludePeptidesLabel.setText("Exclude peptides with the following PTMs");

        selectedPtmsScrollPane.setViewportView(selectedPTMs);

        allPTMs.setModel(new javax.swing.AbstractListModel() {
            String[] strings = { "Item 1", "Item 2", "Item 3", "Item 4", "Item 5" };
            public int getSize() { return strings.length; }
            public Object getElementAt(int i) { return strings[i]; }
        });
        allPtmsScrollPane.setViewportView(allPTMs);

        availablePtmsLabel.setFont(availablePtmsLabel.getFont().deriveFont((availablePtmsLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        availablePtmsLabel.setText("Available PTMs");

        addModifications.setText("<<");
        addModifications.setToolTipText("Add to list of expected modifications");
        addModifications.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                addModificationsActionPerformed(evt);
            }
        });

        removeModification.setText(">>");
        removeModification.setToolTipText("Remove from list of selected modifications");
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
                .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(idSelectionPanelLayout.createSequentialGroup()
                        .addComponent(selectedPtmsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 408, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                            .addComponent(addModifications, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addComponent(removeModification))
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                    .addGroup(idSelectionPanelLayout.createSequentialGroup()
                        .addComponent(excludePeptidesLabel)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)))
                .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(allPtmsScrollPane, javax.swing.GroupLayout.PREFERRED_SIZE, 405, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(availablePtmsLabel)))
        );

        idSelectionPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {allPtmsScrollPane, selectedPtmsScrollPane});

        idSelectionPanelLayout.setVerticalGroup(
            idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(idSelectionPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addGroup(idSelectionPanelLayout.createSequentialGroup()
                        .addGap(31, 31, 31)
                        .addComponent(addModifications)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(removeModification)
                        .addGap(69, 117, Short.MAX_VALUE))
                    .addGroup(idSelectionPanelLayout.createSequentialGroup()
                        .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addGroup(idSelectionPanelLayout.createSequentialGroup()
                                .addGroup(idSelectionPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                                    .addComponent(availablePtmsLabel)
                                    .addComponent(excludePeptidesLabel))
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(allPtmsScrollPane))
                            .addGroup(idSelectionPanelLayout.createSequentialGroup()
                                .addGap(22, 22, 22)
                                .addComponent(selectedPtmsScrollPane)))
                        .addContainerGap())))
        );

        ratioEstimationsPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Ratio Estimation"));
        ratioEstimationsPanel.setOpaque(false);

        resolutionLabel.setText("Resolution");

        windowWidthLabel.setText("Window Width (%)");

        widthTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        resolutionTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);

        proteinsLabel.setText("Proteins");

        peptidesLabel.setText("Peptides");

        psmsLabel.setText("PSMs");

        peptideValidationCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All", "Validated", "Confident" }));

        proteinValidationCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All", "Validated", "Confident" }));

        psmValidationCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "All", "Validated", "Confident" }));

        jLabel1.setText("Exclude Missing Intensities");

        excludeMissingIntensitiesCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No" }));

        javax.swing.GroupLayout ratioEstimationsPanelLayout = new javax.swing.GroupLayout(ratioEstimationsPanel);
        ratioEstimationsPanel.setLayout(ratioEstimationsPanelLayout);
        ratioEstimationsPanelLayout.setHorizontalGroup(
            ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(peptidesLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(psmsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(proteinsLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(resolutionLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(windowWidthLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel1))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, 115, Short.MAX_VALUE)
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(excludeMissingIntensitiesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                        .addGap(1, 1, 1)
                        .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                            .addComponent(psmValidationCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(peptideValidationCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(proteinValidationCmb, 0, 150, Short.MAX_VALUE)
                            .addComponent(resolutionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                            .addComponent(widthTxt, javax.swing.GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))))
                .addContainerGap())
        );

        ratioEstimationsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {peptidesLabel, proteinsLabel, psmsLabel});

        ratioEstimationsPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {peptideValidationCmb, proteinValidationCmb, psmValidationCmb, resolutionTxt, widthTxt});

        ratioEstimationsPanelLayout.setVerticalGroup(
            ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(ratioEstimationsPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(proteinsLabel)
                    .addComponent(proteinValidationCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(peptidesLabel)
                    .addComponent(peptideValidationCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(psmsLabel)
                    .addComponent(psmValidationCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(0, 0, 0)
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(resolutionTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(resolutionLabel))
                .addGap(0, 0, 0)
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(widthTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(windowWidthLabel))
                .addGap(0, 0, 0)
                .addGroup(ratioEstimationsPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(jLabel1)
                    .addComponent(excludeMissingIntensitiesCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

        helpLabel.setFont(helpLabel.getFont().deriveFont((helpLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        helpLabel.setText("For ratio estimation help see:");

        helpLinkLabel.setText("<html> <a href=\\\"dummy_link\">Burkhart et al.  (2011) [PMID: 21328540]</a>.</html>");
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

        cancelButton.setText("Cancel");
        cancelButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                cancelButtonActionPerformed(evt);
            }
        });

        proteinGroupJPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Protein Group Ratio Estimation"));
        proteinGroupJPanel.setOpaque(false);

        uniquePeptidesSpinner.setModel(new javax.swing.SpinnerNumberModel(3, 1, null, 1));

        uniquePeptidesLbl.setText("Minimum Unique");

        jLabel2.setText("Exclude Shared Peptides If Enough Unique");

        jLabel3.setText("Exclude Miscleaved Peptides");

        excludeSharedCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No" }));

        excludeMiscleavedCmb.setModel(new javax.swing.DefaultComboBoxModel(new String[] { "Yes", "No" }));

        javax.swing.GroupLayout proteinGroupJPanelLayout = new javax.swing.GroupLayout(proteinGroupJPanel);
        proteinGroupJPanel.setLayout(proteinGroupJPanelLayout);
        proteinGroupJPanelLayout.setHorizontalGroup(
            proteinGroupJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinGroupJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinGroupJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(jLabel2)
                    .addComponent(jLabel3)
                    .addComponent(uniquePeptidesLbl, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addGap(30, 30, 30)
                .addGroup(proteinGroupJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                    .addComponent(uniquePeptidesSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(excludeMiscleavedCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(excludeSharedCmb, javax.swing.GroupLayout.PREFERRED_SIZE, 150, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap(13, Short.MAX_VALUE))
        );
        proteinGroupJPanelLayout.setVerticalGroup(
            proteinGroupJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinGroupJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(proteinGroupJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(excludeSharedCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel2))
                .addGap(0, 0, 0)
                .addGroup(proteinGroupJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(excludeMiscleavedCmb, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(jLabel3))
                .addGap(0, 0, 0)
                .addGroup(proteinGroupJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.BASELINE)
                    .addComponent(uniquePeptidesSpinner, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(uniquePeptidesLbl))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
        );

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
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addComponent(okButton, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE)
                        .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(cancelButton, javax.swing.GroupLayout.PREFERRED_SIZE, 69, javax.swing.GroupLayout.PREFERRED_SIZE))
                    .addGroup(backgroundPanelLayout.createSequentialGroup()
                        .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.TRAILING, false)
                            .addComponent(idSelectionPanel, javax.swing.GroupLayout.Alignment.LEADING, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                            .addGroup(backgroundPanelLayout.createSequentialGroup()
                                .addComponent(ratioEstimationsPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                                .addComponent(proteinGroupJPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)))
                        .addGap(0, 0, Short.MAX_VALUE)))
                .addContainerGap())
        );

        backgroundPanelLayout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {proteinGroupJPanel, ratioEstimationsPanel});

        backgroundPanelLayout.setVerticalGroup(
            backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(backgroundPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING, false)
                    .addComponent(ratioEstimationsPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(proteinGroupJPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(idSelectionPanel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(backgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(helpLabel)
                    .addComponent(helpLinkLabel, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(okButton)
                    .addComponent(cancelButton))
                .addContainerGap(javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
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
     * Close the dialog and cancel the changes.
     *
     * @param evt
     */
    private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_cancelButtonActionPerformed

        canceled = true;
        dispose();

    }//GEN-LAST:event_cancelButtonActionPerformed

    /**
     * Save the data and close the dialog.
     *
     * @param evt
     */
    private void okButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_okButtonActionPerformed

        if (validateInput()) {
            dispose();
        }

    }//GEN-LAST:event_okButtonActionPerformed

    /**
     * Changes the cursor back to the default cursor when leaving the help link.
     *
     * @param evt
     */
    private void helpLinkLabelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLinkLabelMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_helpLinkLabelMouseExited

    /**
     * Changes the cursor to the hand cursor when over the help link.
     *
     * @param evt
     */
    private void helpLinkLabelMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLinkLabelMouseEntered
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_helpLinkLabelMouseEntered

    /**
     * Opens the help link in the web browser.
     *
     * @param evt
     */
    private void helpLinkLabelMouseClicked(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_helpLinkLabelMouseClicked

        this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
        BareBonesBrowserLaunch.openURL("https://onlinelibrary.wiley.com/doi/10.1002/pmic.201000711/abstract");
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

    }//GEN-LAST:event_helpLinkLabelMouseClicked

    /**
     * Remove a modification from the list.
     *
     * @param evt
     */
    private void removeModificationActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_removeModificationActionPerformed

        int nSelected = selectedPTMs.getModel().getSize();
        int nToRemove = selectedPTMs.getSelectedIndices().length;
        String[] variableModifications = new String[nSelected - nToRemove];
        int cpt = 0;
        boolean found;

        for (int i = 0; i < selectedPTMs.getModel().getSize(); i++) {

            found = false;

            for (Object selection : selectedPTMs.getSelectedValuesList()) {

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

    /**
     * Add a modification to the list.
     *
     * @param evt
     */
    private void addModificationsActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_addModificationsActionPerformed

        int nSelected = selectedPTMs.getModel().getSize();
        int nNew = allPTMs.getSelectedIndices().length;
        String[] fixedModifications = new String[nSelected + nNew];
        int cpt = 0;

        for (int i = 0; i < nSelected; i++) {
            fixedModifications[cpt] = (String) selectedPTMs.getModel().getElementAt(i);
            cpt++;
        }

        boolean found;

        for (Object selection : allPTMs.getSelectedValuesList()) {

            String name = (String) selection;
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

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JButton addModifications;
    private javax.swing.JList allPTMs;
    private javax.swing.JScrollPane allPtmsScrollPane;
    private javax.swing.JLabel availablePtmsLabel;
    private javax.swing.JPanel backgroundPanel;
    private javax.swing.JButton cancelButton;
    private javax.swing.JComboBox excludeMiscleavedCmb;
    private javax.swing.JComboBox excludeMissingIntensitiesCmb;
    private javax.swing.JLabel excludePeptidesLabel;
    private javax.swing.JComboBox excludeSharedCmb;
    private javax.swing.JLabel helpLabel;
    private javax.swing.JLabel helpLinkLabel;
    private javax.swing.JPanel idSelectionPanel;
    private javax.swing.JLabel jLabel1;
    private javax.swing.JLabel jLabel2;
    private javax.swing.JLabel jLabel3;
    private javax.swing.JButton okButton;
    private javax.swing.JComboBox peptideValidationCmb;
    private javax.swing.JLabel peptidesLabel;
    private javax.swing.JPanel proteinGroupJPanel;
    private javax.swing.JComboBox proteinValidationCmb;
    private javax.swing.JLabel proteinsLabel;
    private javax.swing.JComboBox psmValidationCmb;
    private javax.swing.JLabel psmsLabel;
    private javax.swing.JPanel ratioEstimationsPanel;
    private javax.swing.JButton removeModification;
    private javax.swing.JLabel resolutionLabel;
    private javax.swing.JTextField resolutionTxt;
    private javax.swing.JList selectedPTMs;
    private javax.swing.JScrollPane selectedPtmsScrollPane;
    private javax.swing.JLabel uniquePeptidesLbl;
    private javax.swing.JSpinner uniquePeptidesSpinner;
    private javax.swing.JTextField widthTxt;
    private javax.swing.JLabel windowWidthLabel;
    // End of variables declaration//GEN-END:variables

    /**
     * Indicates whether the user canceled the editing.
     *
     * @return a boolean indicating whether the user canceled the editing
     */
    public boolean isCanceled() {
        return canceled;
    }

    /**
     * Methods which validates the user input (returns false in case of wrong
     * input).
     *
     * @return true if the input can be processed
     */
    private boolean validateInput() {

        // check the resolution and window width
        Double input;
        try {
            input = Double.valueOf(resolutionTxt.getText());
        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "please enter a correct ratio resolution.",
                    "Ratio Resolution Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;

        }
        if (input <= 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please input a positive number for the ratio resolution.",
                    "Ratio Resolution Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;

        }
        try {
            input = Double.valueOf(widthTxt.getText());
        } catch (Exception e) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please enter a correct window width.",
                    "Window Width Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;

        }
        if (input <= 0) {

            JOptionPane.showMessageDialog(
                    this,
                    "Please input a positive number for the window width.",
                    "Window Width Error",
                    JOptionPane.ERROR_MESSAGE
            );

            return false;

        }

        return true;
    }

    /**
     * Updates the modification list (right).
     */
    private void updateModificationList() {

        ArrayList<String> allModificationsList = new ArrayList<>(modificationParameters.getAllNotFixedModifications());
        int nSelected = selectedPTMs.getModel().getSize();
        ArrayList<String> allModifications = new ArrayList<>();

        for (String name : allModificationsList) {

            boolean found = false;

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

    /**
     * Returns the settings as set by the user.
     *
     * @return the settings as set by the user
     */
    public RatioEstimationSettings getRatioEstimationSettings() {

        RatioEstimationSettings ratioEstimationSettings = new RatioEstimationSettings();
        ratioEstimationSettings.setProteinValidationLevel(MatchValidationLevel.getMatchValidationLevel(proteinValidationCmb.getSelectedIndex()));
        ratioEstimationSettings.setPeptideValidationLevel(MatchValidationLevel.getMatchValidationLevel(peptideValidationCmb.getSelectedIndex()));
        ratioEstimationSettings.setPsmValidationLevel(MatchValidationLevel.getMatchValidationLevel(psmValidationCmb.getSelectedIndex()));
        Double resolution = Double.valueOf(resolutionTxt.getText());
        ratioEstimationSettings.setRatioResolution(resolution);
        Double windowWidth = Double.valueOf(widthTxt.getText());
        ratioEstimationSettings.setPercentile(windowWidth);
        ratioEstimationSettings.setIgnoreNullIntensities(excludeMissingIntensitiesCmb.getSelectedIndex() == 0);

        for (int i = 0; i < selectedPTMs.getModel().getSize(); i++) {
            ratioEstimationSettings.addExcludingPtm(selectedPTMs.getModel().getElementAt(i).toString());
        }

        if (excludeSharedCmb.getSelectedIndex() == 0) {
            ratioEstimationSettings.setMinUnique((Integer) uniquePeptidesSpinner.getValue());
        } else {
            ratioEstimationSettings.setMinUnique(-1);
        }

        ratioEstimationSettings.setIgnoreMissedCleavages(excludeMiscleavedCmb.getSelectedIndex() == 0);

        return ratioEstimationSettings;

    }
}
