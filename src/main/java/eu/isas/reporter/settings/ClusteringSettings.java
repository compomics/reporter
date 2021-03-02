package eu.isas.reporter.settings;

import com.compomics.util.math.clustering.settings.KMeansClusteringSettings;
import eu.isas.reporter.calculation.clustering.keys.PeptideClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.ProteinClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.PsmClusterClassKey;
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
     * The possible protein classes.
     */
    private ArrayList<String> possibleProteinClassesNames;
    /**
     * Map of the classes keys indexed by name.
     */
    private HashMap<String, ProteinClusterClassKey> possibleProteinClassesKeys;
    /**
     * The possible peptide classes.
     */
    private ArrayList<String> possiblePeptideClassesNames;
    /**
     * Map of the classes keys indexed by name.
     */
    private HashMap<String, PeptideClusterClassKey> possiblePeptideClassesKeys;
    /**
     * The possible PSM classes.
     */
    private ArrayList<String> possiblePsmClassesNames;
    /**
     * Map of the classes keys indexed by name.
     */
    private HashMap<String, PsmClusterClassKey> possiblePsmClassesKeys;
    /**
     * The selected protein classes.
     */
    private ArrayList<String> selectedProteinClasses = new ArrayList<>();
    /**
     * The selected peptide classes.
     */
    private ArrayList<String> selectedPeptideClasses = new ArrayList<>();
    /**
     * The selected PSM classes.
     */
    private ArrayList<String> selectedPsmClasses = new ArrayList<>();
    /**
     * The classes color coding in a map where the key is the toString()
     * representation of the key.
     */
    private HashMap<String, Color> classesColors = new HashMap<>();
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
            result = Color.GRAY;
            classesColors.put(clusterClass, result);
        }
        return result;
    }
    
    /**
     * Returns the color for a non-selected line of this class.
     * 
     * @param clusterClass the cluster class
     * 
     * @return the color for a non-selected line of this class
     */
    public Color getNonSelectedColor(String clusterClass) {
        Color refColor = getColor(clusterClass);
        refColor = refColor.brighter();
        Color unselectedColor = new Color(refColor.getRed(), refColor.getGreen(), refColor.getBlue(), refColor.getAlpha()/10);
        return unselectedColor;
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

    /**
     * Returns the possible protein classes names.
     *
     * @return the possible protein classes names
     */
    public ArrayList<String> getPossibleProteinClasses() {
        return possibleProteinClassesNames;
    }

    /**
     * Returns the class key corresponding to the given name.
     *
     * @param name the name
     *
     * @return the class key corresponding to the given name
     */
    public ProteinClusterClassKey getProteinClassKey(String name) {
        if (possibleProteinClassesKeys == null) {
            return null;
        }
        return possibleProteinClassesKeys.get(name);
    }

    /**
     * Sets the cluster class keys.
     *
     * @param clusterClassKeys the cluster class keys
     */
    public void setProteinClassKeys(ArrayList<ProteinClusterClassKey> clusterClassKeys) {

        possibleProteinClassesNames = new ArrayList<String>(clusterClassKeys.size());
        possibleProteinClassesKeys = new HashMap<String, ProteinClusterClassKey>(clusterClassKeys.size());
        for (ProteinClusterClassKey clusterClassKey : clusterClassKeys) {
            String key = clusterClassKey.toString();
            possibleProteinClassesNames.add(key);
            possibleProteinClassesKeys.put(key, clusterClassKey);
        }
    }

    /**
     * Returns a map of the possible keys indexed by their toString()
     * representation.
     *
     * @return a map of the possible keys
     */
    public HashMap<String, ProteinClusterClassKey> getProteinKeysMap() {
        return possibleProteinClassesKeys;
    }

    /**
     * Returns the possible peptide classes names.
     *
     * @return the possible peptide classes names
     */
    public ArrayList<String> getPossiblePeptideClasses() {
        return possiblePeptideClassesNames;
    }

    /**
     * Returns the class key corresponding to the given name.
     *
     * @param name the name
     *
     * @return the class key corresponding to the given name
     */
    public PeptideClusterClassKey getPeptideClassKey(String name) {
        if (possiblePeptideClassesKeys == null) {
            return null;
        }
        return possiblePeptideClassesKeys.get(name);
    }

    /**
     * Sets the cluster class keys.
     *
     * @param clusterClassKeys the cluster class keys
     */
    public void setPeptideClassKeys(ArrayList<PeptideClusterClassKey> clusterClassKeys) {

        possiblePeptideClassesNames = new ArrayList<String>(clusterClassKeys.size());
        possiblePeptideClassesKeys = new HashMap<String, PeptideClusterClassKey>(clusterClassKeys.size());
        for (PeptideClusterClassKey clusterClassKey : clusterClassKeys) {
            String key = clusterClassKey.toString();
            possiblePeptideClassesNames.add(key);
            possiblePeptideClassesKeys.put(key, clusterClassKey);
        }
    }

    /**
     * Returns a map of the possible keys indexed by their toString()
     * representation.
     *
     * @return a map of the possible keys
     */
    public HashMap<String, PeptideClusterClassKey> getPeptideKeysMap() {
        return possiblePeptideClassesKeys;
    }

    /**
     * Returns the possible PSM classes names.
     *
     * @return the possible PSM classes names
     */
    public ArrayList<String> getPossiblePsmClasses() {
        return possiblePsmClassesNames;
    }

    /**
     * Returns the class key corresponding to the given name.
     *
     * @param name the name
     *
     * @return the class key corresponding to the given name
     */
    public PsmClusterClassKey getPsmClassKey(String name) {
        if (possiblePsmClassesKeys == null) {
            return null;
        }
        return possiblePsmClassesKeys.get(name);
    }

    /**
     * Sets the cluster class keys.
     *
     * @param clusterClassKeys the cluster class keys
     */
    public void setPsmClassKeys(ArrayList<PsmClusterClassKey> clusterClassKeys) {

        possiblePsmClassesNames = new ArrayList<String>(clusterClassKeys.size());
        possiblePsmClassesKeys = new HashMap<String, PsmClusterClassKey>(clusterClassKeys.size());
        for (PsmClusterClassKey clusterClassKey : clusterClassKeys) {
            String key = clusterClassKey.toString();
            possiblePsmClassesNames.add(key);
            possiblePsmClassesKeys.put(key, clusterClassKey);
        }
    }

    /**
     * Returns a map of the possible keys indexed by their toString()
     * representation.
     *
     * @return a map of the possible keys
     */
    public HashMap<String, PsmClusterClassKey> getPsmKeysMap() {
        return possiblePsmClassesKeys;
    }

    /**
     * Returns the classes color coding.
     *
     * @return the classes color coding
     */
    public HashMap<String, Color> getClassesColors() {
        return classesColors;
    }

    /**
     * Sets the classes color coding.
     *
     * @param classesColors the classes color coding
     */
    public void setClassesColors(HashMap<String, Color> classesColors) {
        this.classesColors = classesColors;
    }
}
