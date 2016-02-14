package eu.isas.reporter.settings;

import com.compomics.util.math.clustering.settings.KMeansClusteringSettings;
import java.awt.Color;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Settings for the clustering.
 *
 * @author Marc Vaudel
 */
public class ClusteringSettings implements Serializable {

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
     * The classes color coding in a map where the key is the toString()
     * representation of the key.
     */
    private HashMap<String, Color> classesColors = new HashMap<String, Color>();

    /**
     * The k-means clustering settings.
     */
    private KMeansClusteringSettings kMeansClusteringSettings;

    /**
     * Constructor.
     */
    public ClusteringSettings() {

    }

    /**
     * Returns the selected protein cluster classes represented by their
     * toString() method.
     *
     * @return the selected protein cluster classes
     */
    public ArrayList<String> getSelectedProteinClasses() {
        return selectedProteinClasses;
    }

    /**
     * Sets the selected protein cluster classes represented by their toString()
     * method.
     *
     * @param selectedProteinClasses the selected protein cluster classes
     */
    public void setSelectedProteinClasses(ArrayList<String> selectedProteinClasses) {
        this.selectedProteinClasses = selectedProteinClasses;
    }

    /**
     * Adds a class to the selected classes.
     *
     * @param className the class name
     */
    public void addProteinClass(String className) {
        selectedProteinClasses.add(className);
    }

    /**
     * Clears the protein classes.
     */
    public void clearProteinClasses() {
        selectedProteinClasses.clear();
    }

    /**
     * Returns the selected peptide cluster classes represented by their
     * toString() method.
     *
     * @return the selected peptide cluster classes
     */
    public ArrayList<String> getSelectedPeptideClasses() {
        return selectedPeptideClasses;
    }

    /**
     * Sets the selected peptide cluster classes represented by their toString()
     * method.
     *
     * @param selectedPeptideClasses the selected peptide cluster classes
     */
    public void setSelectedPeptideClasses(ArrayList<String> selectedPeptideClasses) {
        this.selectedPeptideClasses = selectedPeptideClasses;
    }

    /**
     * Adds a class to the selected classes.
     *
     * @param className the class name
     */
    public void addPeptideClass(String className) {
        selectedPeptideClasses.add(className);
    }

    /**
     * Clears the peptide classes.
     */
    public void clearPeptideClasses() {
        selectedPeptideClasses.clear();
    }

    /**
     * Returns the selected PSM cluster classes represented by their toString()
     * method.
     *
     * @return the selected PSM cluster classes
     */
    public ArrayList<String> getSelectedPsmClasses() {
        return selectedPsmClasses;
    }

    /**
     * Sets the selected PSM cluster classes represented by their toString()
     * method.
     *
     * @param selectedPsmClasses the selected PSM cluster classes
     */
    public void setSelectedPsmClasses(ArrayList<String> selectedPsmClasses) {
        this.selectedPsmClasses = selectedPsmClasses;
    }

    /**
     * Adds a class to the selected classes.
     *
     * @param className the class name
     */
    public void addPsmClass(String className) {
        selectedPsmClasses.add(className);
    }

    /**
     * Clears the peptide classes.
     */
    public void clearPsmClasses() {
        selectedPsmClasses.clear();
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

    /**
     * Returns the k-means clustering settings.
     *
     * @return the k-means clustering settings
     */
    public KMeansClusteringSettings getKMeansClusteringSettings() {
        return kMeansClusteringSettings;
    }

    /**
     * Sets the k-means clustering settings.
     *
     * @param kMeansClusteringSettings the k-means clustering settings
     */
    public void setKMeansClusteringSettings(KMeansClusteringSettings kMeansClusteringSettings) {
        this.kMeansClusteringSettings = kMeansClusteringSettings;
    }

}
