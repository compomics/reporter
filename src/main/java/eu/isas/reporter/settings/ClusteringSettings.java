package eu.isas.reporter.settings;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Settings for the clustering.
 *
 * @author Marc Vaudel
 */
public class ClusteringSettings {

    /**
     * The selected protein classes
     */
    private ArrayList<String> selectedProteinClasses = new ArrayList<String>();

    /**
     * The selected peptide classes
     */
    private ArrayList<String> selectedPeptideClasses = new ArrayList<String>();

    /**
     * The selected PSM classes
     */
    private ArrayList<String> selectedPsmClasses = new ArrayList<String>();
    
    /**
     * The classes color coding in a map where the key is the toString() representation of the key.
     */
    private HashMap<String, Color> classesColors = new HashMap<String, Color>();
    
    /**
     * Constructor.
     */
    public ClusteringSettings() {
        
    }

    /**
     * Returns the selected protein cluster classes represented by their toString() method.
     * 
     * @return the selected protein cluster classes
     */
    public ArrayList<String> getSelectedProteinClasses() {
        return selectedProteinClasses;
    }

    /**
     * Sets the selected protein cluster classes represented by their toString() method.
     * 
     * @param selectedProteinClasses the selected protein cluster classes
     */
    public void setSelectedProteinClasses(ArrayList<String> selectedProteinClasses) {
        this.selectedProteinClasses = selectedProteinClasses;
    }

    /**
     * Returns the selected peptide cluster classes represented by their toString() method.
     * 
     * @return the selected peptide cluster classes
     */
    public ArrayList<String> getSelectedPeptideClasses() {
        return selectedPeptideClasses;
    }

    /**
     * Sets the selected peptide cluster classes represented by their toString() method.
     * 
     * @param selectedPeptideClasses the selected peptide cluster classes
     */
    public void setSelectedPeptideClasses(ArrayList<String> selectedPeptideClasses) {
        this.selectedPeptideClasses = selectedPeptideClasses;
    }

    /**
     * Returns the selected PSM cluster classes represented by their toString() method.
     * 
     * @return the selected PSM cluster classes
     */
    public ArrayList<String> getSelectedPsmClasses() {
        return selectedPsmClasses;
    }

    /**
     * Sets the selected PSM cluster classes represented by their toString() method.
     * 
     * @param selectedPsmClasses the selected PSM cluster classes
     */
    public void setSelectedPsmClasses(ArrayList<String> selectedPsmClasses) {
        this.selectedPsmClasses = selectedPsmClasses;
    }
    
    /**
     * Returns the color corresponding to a cluster class.
     * 
     * @param clusterClass the cluster class
     * 
     * @return the corresponding color
     */
    public Color getColor(String clusterClass) {
        Color result = classesColors.get(clusterClass);
        if (result == null) {
            result = Color.BLACK;
            classesColors.put(clusterClass, result);
        }
        return result;
    }
    
    /**
     * Sets the cluster class color.
     * 
     * @param clusterClass the cluster class representation as String
     * @param color the cluster class color
     */
    public void setColor(String clusterClass, Color color) {
        classesColors.put(clusterClass, color);
    }
}
