package eu.isas.reporter.project.attributes;

import eu.isas.reporter.calculation.clustering.keys.PeptideClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.ProteinClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.PsmClusterClassKey;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Metrics used to design the cluster classes.
 *
 * @author Marc Vaudel
 */
public class ClusterMetrics implements Serializable {

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
     * Constructor.
     */
    public ClusterMetrics() {

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
            String name = clusterClassKey.getName();
            possibleProteinClassesNames.add(name);
            possibleProteinClassesKeys.put(name, clusterClassKey);
        }
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
            String name = clusterClassKey.getName();
            possiblePeptideClassesNames.add(name);
            possiblePeptideClassesKeys.put(name, clusterClassKey);
        }
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
            String name = clusterClassKey.getName();
            possiblePsmClassesNames.add(name);
            possiblePsmClassesKeys.put(name, clusterClassKey);
        }
    }

}
