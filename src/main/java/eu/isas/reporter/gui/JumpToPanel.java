package eu.isas.reporter.gui;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * A Jump To panel for use in the menu bar in the main frame.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class JumpToPanel extends javax.swing.JPanel { // @TODO: should be merged with the same panel in PeptideShaker..?

    /**
     * Enum of the types of data to jump to.
     */
    public enum JumpType {

        proteinAndPeptides
    }
    /**
     * The type of data to jump to in that panel.
     */
    private JumpType jumpType = JumpType.proteinAndPeptides;
    /**
     * Instance of the main GUI class.
     */
    private ReporterGUI reporterGUI;
    /**
     * Items matching the criterion for each type.
     */
    private HashMap<JumpType, ArrayList<String>> possibilities = new HashMap<JumpType, ArrayList<String>>();
    /**
     * Currently selected item.
     */
    private HashMap<JumpType, Integer> currentSelection = new HashMap<JumpType, Integer>();
    /**
     * The text to display by default.
     */
    private HashMap<JumpType, String> lastInput = new HashMap<JumpType, String>();
    /**
     * The text to display by default.
     */
    private HashMap<JumpType, String> lastLabel = new HashMap<JumpType, String>();
    /**
     * Instance of the sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * The text to display by default.
     */
    private HashMap<JumpType, String> welcomeText;
    /**
     * Counts the number of times the users has pressed a key on the keyboard in
     * the search field.
     */
    private int keyPressedCounter = 0;
    /**
     * The time to wait between keys typed before updating the search.
     */
    private int waitingTime = 1000;
    /**
     * The current list of protein keys. Can be different from the complete
     * list, e.g., only include validated proteins.
     */
    private ArrayList<String> currentProteinKeys;

    /**
     * Type of item selected.
     */
    private enum Type {

        PROTEIN
    }
    /**
     * Type of each possible item.
     */
    private HashMap<JumpType, ArrayList<Type>> types = new HashMap<JumpType, ArrayList<Type>>();

    /**
     * Creates a new JumpToPanel.
     *
     * @param reporterGUI the parent
     */
    public JumpToPanel(ReporterGUI reporterGUI) {
        initComponents();

        this.reporterGUI = reporterGUI;
        currentProteinKeys = new ArrayList<String>();

        welcomeText = new HashMap<JumpType, String>();
        welcomeText.put(JumpType.proteinAndPeptides, "(protein)");
        inputTxt.setText(welcomeText.get(jumpType));
        indexLabel.setText("");
        previousButton.setEnabled(false);
        nextButton.setEnabled(false);
    }
    
    /**
     * Set the current protein keys to search in.
     * 
     * @param currentProteinKeys the current protein keys
     */
    public void setProteinKeys(ArrayList<String> currentProteinKeys) {
        this.currentProteinKeys = currentProteinKeys;
    }

    /**
     * Move the focus to the Jump To text field and select all the content.
     */
    public void selectTextField() {
        inputTxt.requestFocus();
        inputTxt.selectAll();
    }

    /**
     * Set the color for the hits.
     *
     * @param color the color
     */
    public void setColor(Color color) {
        indexLabel.setForeground(color);
    }

    /**
     * Updates the item selection in the selected tab.
     */
    public void updateSelectionInTab() {

        indexLabel.setForeground(Color.BLACK);

        if (types.get(jumpType).get(currentSelection.get(jumpType)) == Type.PROTEIN) {
            String selectedProtein = possibilities.get(jumpType).get(currentSelection.get(jumpType));
            ArrayList<String> selectedProteins = new ArrayList<String>();
            selectedProteins.add(selectedProtein);
            reporterGUI.minimizeChart();
            reporterGUI.setSelectedProteins(selectedProteins, true, true);
        }

        String label = "(" + (currentSelection.get(jumpType) + 1) + " of " + possibilities.get(jumpType).size() + ")";
        indexLabel.setText(label);
        lastLabel.put(jumpType, label);
    }

    /**
     * Returns a list of descriptions corresponding to every item matching the
     * search.
     *
     * @return a list of descriptions
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     */
    public ArrayList<String> getPossibilitiesDescriptions() throws SQLException, ClassNotFoundException, IOException, InterruptedException {

        Identification identification = reporterGUI.getIdentification();

        // some necessary pre-caching
        ArrayList<Type> typeList = types.get(jumpType);
        ArrayList<String> keys = possibilities.get(jumpType);
        ArrayList<String> proteinKeys = new ArrayList<String>();

        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            if (typeList.get(i) == Type.PROTEIN) {
                proteinKeys.add(key);
            }
        }
        if (!proteinKeys.isEmpty()) {
            identification.loadProteinMatches(proteinKeys, null, false);
        }

        ArrayList<String> descriptions = new ArrayList<String>();
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            Type type = typeList.get(i);
            String description = getItemDescription(key, type);
            descriptions.add(description);
        }

        return descriptions;
    }

    /**
     * Returns the description of an item.
     *
     * @param key the key of the item
     * @param itemType the type of the item
     * @return the description of an item
     *
     * @throws SQLException thrown if an SQLException occurs
     * @throws ClassNotFoundException thrown if a ClassNotFoundException occurs
     * @throws IOException thrown if an IOException occurs
     * @throws InterruptedException thrown if an InterruptedException occurs
     */
    private String getItemDescription(String key, Type itemType) throws IllegalArgumentException, SQLException, IOException, ClassNotFoundException, InterruptedException {
        Identification identification = reporterGUI.getIdentification();
        switch (itemType) {
            case PROTEIN:
                ProteinMatch proteinMatch = identification.getProteinMatch(key);
                String mainMatch = proteinMatch.getMainMatch();
                String description = sequenceFactory.getHeader(mainMatch).getSimpleProteinDescription();
                String result = mainMatch;
                for (String accession : ProteinMatch.getAccessions(key)) {
                    if (!accession.equals(mainMatch)) {
                        if (!result.equals(mainMatch)) {
                            result += ", ";
                        }
                        result += accession;
                    }
                }
                result += " - " + description;
                return result;
            default:
                return "Unknown";
        }
    }

    /**
     * Returns the index of the selected item.
     *
     * @return the index of the selected item
     */
    public int getIndexOfSelectedItem() {
        return currentSelection.get(jumpType);
    }

    /**
     * Sets the index of the selected item. Note: this does not update the
     * selection in tab and the GUI (see updateSelectionInTab()).
     *
     * @param itemIndex the item index
     */
    public void setSelectedItem(int itemIndex) {
        currentSelection.put(jumpType, itemIndex);
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        findJLabel = new javax.swing.JLabel();
        inputTxt = new javax.swing.JTextField();
        previousButton = new javax.swing.JButton();
        nextButton = new javax.swing.JButton();
        indexLabel = new javax.swing.JLabel();

        setOpaque(false);

        findJLabel.setText("Find");

        inputTxt.setForeground(new java.awt.Color(204, 204, 204));
        inputTxt.setHorizontalAlignment(javax.swing.JTextField.CENTER);
        inputTxt.setText("(peptide or protein)");
        inputTxt.setBorder(javax.swing.BorderFactory.createLineBorder(new java.awt.Color(204, 204, 204)));
        inputTxt.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                inputTxtMouseReleased(evt);
            }
        });
        inputTxt.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                inputTxtKeyReleased(evt);
            }
        });

        previousButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/previous_grey.png"))); // NOI18N
        previousButton.setToolTipText("Previous");
        previousButton.setBorder(null);
        previousButton.setBorderPainted(false);
        previousButton.setContentAreaFilled(false);
        previousButton.setIconTextGap(0);
        previousButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/previous.png"))); // NOI18N
        previousButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                previousButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                previousButtonMouseExited(evt);
            }
        });
        previousButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                previousButtonActionPerformed(evt);
            }
        });

        nextButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/next_grey.png"))); // NOI18N
        nextButton.setToolTipText("Next");
        nextButton.setBorderPainted(false);
        nextButton.setContentAreaFilled(false);
        nextButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/next.png"))); // NOI18N
        nextButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                nextButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                nextButtonMouseExited(evt);
            }
        });
        nextButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                nextButtonActionPerformed(evt);
            }
        });

        indexLabel.setFont(indexLabel.getFont().deriveFont((indexLabel.getFont().getStyle() | java.awt.Font.ITALIC)));
        indexLabel.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        indexLabel.setText(" ");

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addComponent(findJLabel)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(inputTxt, javax.swing.GroupLayout.PREFERRED_SIZE, 205, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(previousButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addGap(0, 0, 0)
                .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(javax.swing.LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(indexLabel, javax.swing.GroupLayout.PREFERRED_SIZE, 97, javax.swing.GroupLayout.PREFERRED_SIZE)
                .addContainerGap())
        );

        layout.linkSize(javax.swing.SwingConstants.HORIZONTAL, new java.awt.Component[] {nextButton, previousButton});

        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(layout.createSequentialGroup()
                .addContainerGap()
                .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.CENTER)
                    .addComponent(findJLabel)
                    .addComponent(inputTxt, javax.swing.GroupLayout.PREFERRED_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(indexLabel)
                    .addComponent(previousButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE)
                    .addComponent(nextButton, javax.swing.GroupLayout.PREFERRED_SIZE, 15, javax.swing.GroupLayout.PREFERRED_SIZE))
                .addContainerGap())
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Update the jump to filter.
     *
     * @param evt the key event
     */
    private void inputTxtKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_inputTxtKeyReleased

        final KeyEvent event = evt;
        keyPressedCounter++;

        new Thread("FindThread") {
            @Override
            public synchronized void run() {

                try {
                    wait(waitingTime);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                try {
                    // see if the gui is to be updated or not
                    Identification identification = reporterGUI.getIdentification();
                    if (identification != null && keyPressedCounter == 1) {

                        if (!inputTxt.getText().equalsIgnoreCase(welcomeText.get(jumpType))) {
                            inputTxt.setForeground(Color.black);
                        } else {
                            inputTxt.setForeground(new Color(204, 204, 204));
                        }

                        if (event.getKeyCode() == KeyEvent.VK_UP && previousButton.isEnabled()) {
                            previousButtonActionPerformed(null);
                        } else if (event.getKeyCode() == KeyEvent.VK_DOWN && nextButton.isEnabled()) {
                            nextButtonActionPerformed(null);
                        } else {
                            if (!possibilities.containsKey(jumpType)) {
                                possibilities.put(jumpType, new ArrayList<String>());
                                types.put(jumpType, new ArrayList<Type>());
                            } else {
                                possibilities.get(jumpType).clear();
                                types.get(jumpType).clear();
                            }
                            currentSelection.put(jumpType, 0);
                            String input = inputTxt.getText().trim().toLowerCase();
                            lastInput.put(jumpType, input);

                            if (!input.equals("")) {

                                reporterGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                                inputTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

                                if (jumpType == JumpType.proteinAndPeptides) {

                                    for (String proteinKey : currentProteinKeys) {
                                        if (!ProteinMatch.isDecoy(proteinKey)) {
                                            if (proteinKey.toLowerCase().contains(input)) {
                                                possibilities.get(jumpType).add(proteinKey);
                                                types.get(jumpType).add(Type.PROTEIN);
                                            } else {
                                                try {
                                                    for (String accession : ProteinMatch.getAccessions(proteinKey)) {
                                                        if (sequenceFactory.getHeader(accession).getSimpleProteinDescription().toLowerCase().contains(input)) {
                                                            possibilities.get(jumpType).add(proteinKey);
                                                            types.get(jumpType).add(Type.PROTEIN);
                                                            break;
                                                        }
                                                    }
                                                } catch (Exception e) {
                                                    // cannot get description, ignore
                                                }
                                            }
                                        }
                                    }
                                }

                                if (possibilities.get(jumpType).size() > 0) {

                                    if (possibilities.get(jumpType).size() > 1) {
                                        previousButton.setEnabled(true);
                                        nextButton.setEnabled(true);
                                    } else { // possibilities.size() == 1
                                        previousButton.setEnabled(false);
                                        nextButton.setEnabled(false);
                                    }

                                    updateSelectionInTab();
                                } else {
                                    previousButton.setEnabled(false);
                                    nextButton.setEnabled(false);

                                    if (!input.equalsIgnoreCase(welcomeText.get(jumpType))) {
                                        indexLabel.setText("(no matches)");
                                    } else {
                                        indexLabel.setText("");
                                    }
                                }

                                reporterGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                inputTxt.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                                inputTxt.requestFocus();

                            } else {
                                indexLabel.setText("");
                                previousButton.setEnabled(false);
                                nextButton.setEnabled(false);
                                inputTxt.setText(welcomeText.get(jumpType));
                                inputTxt.selectAll();
                                inputTxt.requestFocus();
                            }
                        }

                        lastLabel.put(jumpType, indexLabel.getText());

                        // gui updated, reset the counter
                        keyPressedCounter = 0;
                    } else {
                        // gui not updated, decrease the counter
                        keyPressedCounter--;
                    }
                } catch (Exception e) {
                    reporterGUI.catchException(e);
                }
            }
        }.start();
    }//GEN-LAST:event_inputTxtKeyReleased

    /**
     * Display the previous match in the list.
     *
     * @param evt the action event
     */
    private void previousButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_previousButtonActionPerformed
        if (currentSelection.get(jumpType) == 0) {
            currentSelection.put(jumpType, possibilities.get(jumpType).size() - 1);
        } else {
            currentSelection.put(jumpType, currentSelection.get(jumpType) - 1);
        }
        updateSelectionInTab();
    }//GEN-LAST:event_previousButtonActionPerformed

    /**
     * Display the next match in the list.
     *
     * @param evt the action event
     */
    private void nextButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_nextButtonActionPerformed
        if (currentSelection.get(jumpType) == possibilities.get(jumpType).size() - 1) {
            currentSelection.put(jumpType, 0);
        } else {
            currentSelection.put(jumpType, currentSelection.get(jumpType) + 1);
        }
        updateSelectionInTab();
    }//GEN-LAST:event_nextButtonActionPerformed

    /**
     * Select all text in the search field.
     *
     * @param evt the mouse event
     */
    private void inputTxtMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_inputTxtMouseReleased
        if (inputTxt.getText().equals(welcomeText.get(jumpType))) {
            inputTxt.selectAll();
        }
    }//GEN-LAST:event_inputTxtMouseReleased

    /**
     * Change the icon to a hand icon.
     *
     * @param evt the mouse event
     */
    private void previousButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_previousButtonMouseEntered
        if (previousButton.isEnabled()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }//GEN-LAST:event_previousButtonMouseEntered

    /**
     * Change the icon back to the default icon.
     *
     * @param evt the mouse event
     */
    private void previousButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_previousButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_previousButtonMouseExited

    /**
     * Change the icon back to the default icon.
     *
     * @param evt the mouse event
     */
    private void nextButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nextButtonMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_nextButtonMouseExited

    /**
     * Change the icon to a hand icon.
     *
     * @param evt the mouse event
     */
    private void nextButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_nextButtonMouseEntered
        if (nextButton.isEnabled()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }
    }//GEN-LAST:event_nextButtonMouseEntered
    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLabel findJLabel;
    private javax.swing.JLabel indexLabel;
    private javax.swing.JTextField inputTxt;
    private javax.swing.JButton nextButton;
    private javax.swing.JButton previousButton;
    // End of variables declaration//GEN-END:variables

    @Override
    public void setEnabled(boolean enabled) {

        inputTxt.setEnabled(enabled);
        indexLabel.setEnabled(enabled);

        if (possibilities.size() > 0 && enabled) {
            previousButton.setEnabled(true);
            nextButton.setEnabled(true);
        } else {
            previousButton.setEnabled(false);
            nextButton.setEnabled(false);
        }
    }

    /**
     * Changes the type of jumpToPanel.
     *
     * @param jumpType the new type of jump to panel
     */
    public void setType(JumpType jumpType) {
        this.jumpType = jumpType;
        if (lastInput.get(jumpType) != null && !lastInput.get(jumpType).equals("")) {
            inputTxt.setText(lastInput.get(jumpType));
            indexLabel.setText(lastLabel.get(jumpType));
        } else {
            inputTxt.setText(welcomeText.get(jumpType));
            indexLabel.setText("");
        }
    }
}
