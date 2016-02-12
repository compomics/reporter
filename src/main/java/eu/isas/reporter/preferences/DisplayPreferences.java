package eu.isas.reporter.preferences;

import java.util.ArrayList;

/**
 * Reporter display preferences.
 *
 * @author Marc Vaudel
 */
public class DisplayPreferences {

    /**
     * The protein classes selected.
     */
    private ArrayList<String> selectedProteinClasses = new ArrayList<String>();

    /**
     * The peptide classes selected.
     */
    private ArrayList<String> selectedPeptideClasses = new ArrayList<String>();

    /**
     * The PSM classes selected.
     */
    private ArrayList<String> selectedPsmClasses = new ArrayList<String>();
    
    /**
     * Constructor.
     */
    public DisplayPreferences() {
        
    }

    /**
     * Returns the selected protein classes.
     * 
     * @return the selected protein classes
     */
    public ArrayList<String> getSelectedProteinClasses() {
        return selectedProteinClasses;
    }

    /**
     * Sets the selected protein classes.
     * 
     * @param selectedProteinClasses the selected protein classes
     */
    public void setSelectedProteinClasses(ArrayList<String> selectedProteinClasses) {
        this.selectedProteinClasses = selectedProteinClasses;
    }

    /**
     * Returns the selected peptide classes.
     * 
     * @return the selected peptide classes
     */
    public ArrayList<String> getSelectedPeptideClasses() {
        return selectedPeptideClasses;
    }

    /**
     * Sets the selected peptide classes.
     * 
     * @param selectedPeptideClasses the selected peptide classes
     */
    public void setSelectedPeptideClasses(ArrayList<String> selectedPeptideClasses) {
        this.selectedPeptideClasses = selectedPeptideClasses;
    }

    /**
     * Returns the selected PSM classes.
     * 
     * @return the selected PSM classes
     */
    public ArrayList<String> getSelectedPsmClasses() {
        return selectedPsmClasses;
    }

    /**
     * Sets the selected PSM classes.
     * 
     * @param selectedPsmClasses the selected PSM classes
     */
    public void setSelectedPsmClasses(ArrayList<String> selectedPsmClasses) {
        this.selectedPsmClasses = selectedPsmClasses;
    }
    
}
