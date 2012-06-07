package eu.isas.reporter.preferences;

import java.awt.Color;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Set;

/**
 * This class contains the preferences related to the PTM display.
 *
 * @author Marc Vaudel
 */
public class ModificationPreferences implements Serializable {

    /**
     * Mapping of the utilities modification names to the PeptideShaker names.
     */
    private HashMap<String, String> modificationNames = new HashMap<String, String>();
    /**
     * Mapping of the PeptideShaker names to the short names.
     */
    private HashMap<String, String> shortNames = new HashMap<String, String>();
    /**
     * Mapping of the PeptideShaker names to the color used.
     */
    private HashMap<String, Color> colors = new HashMap<String, Color>();

    /**
     * Constructor.
     */
    public ModificationPreferences() {
    }

    /**
     * Returns the set of the utilities modification names included in this
     * profile.
     *
     * @return the set of the utilities modification names included in this
     * profile
     */
    public Set<String> getUtilitiesNames() {
        return modificationNames.keySet();
    }

    /**
     * Returns the peptide shaker names included in this profile.
     *
     * @return the peptide shaker names included in this profile
     */
    public Set<String> getPeptideShakerNames() {
        return shortNames.keySet();
    }

    /**
     * Returns the PeptideShaker name corresponding to the given utilities name.
     *
     * @param utilitiesName the given utilities name
     * @return the corresponding PeptideShaker name
     */
    public String getPeptideShakerName(String utilitiesName) {
        return modificationNames.get(utilitiesName);
    }

    /**
     * Returns the short name of the given modification.
     *
     * @param peptideShakerName the PeptideShaker name of the modification
     * @return the corresponding short name
     */
    public String getShortName(String peptideShakerName) {
        return shortNames.get(peptideShakerName);
    }

    /**
     * Returns the color used to code the given modification.
     *
     * @param peptideShakerName the PeptideShaker name of the given modification
     * @return the corresponding color
     */
    public Color getColor(String peptideShakerName) {
        return colors.get(peptideShakerName);
    }

    /**
     * Sets a new PeptideShaker name for the given modification.
     *
     * @param utilitiesName the utilities name of the modification
     * @param peptideShakerName the new PeptideShaker name
     */
    public void setPeptideShakerName(String utilitiesName, String peptideShakerName) {
        
        String oldKey = modificationNames.get(utilitiesName);
        
        if (modificationNames.containsKey(utilitiesName)) {
            if (!oldKey.equals(peptideShakerName) && !shortNames.containsKey(peptideShakerName)) {
                shortNames.put(peptideShakerName, shortNames.get(oldKey));
                colors.put(peptideShakerName, colors.get(oldKey));
            }

        }
        
        modificationNames.put(utilitiesName, peptideShakerName);
        
        if (!modificationNames.containsValue(oldKey)) {
            shortNames.remove(oldKey);
            colors.remove(oldKey);
        }
    }

    /**
     * Sets a new short name for the given modification.
     *
     * @param peptideShakerName the PeptideShaker name of the modification
     * @param shortName the new short name
     */
    public void setShortName(String peptideShakerName, String shortName) {
        shortNames.put(peptideShakerName, shortName);
    }

    /**
     * Sets a new color for the modification.
     *
     * @param peptideShakerName the Peptide-Shaker name of the modification
     * @param color the new color
     */
    public void setColor(String peptideShakerName, Color color) {
        colors.put(peptideShakerName, color);
    }

    /**
     * Removes a modification from the profile.
     *
     * @param utilitiesName the utilities name of the modification
     */
    public void remove(String utilitiesName) {
        
        String psName = modificationNames.get(utilitiesName);
        modificationNames.remove(utilitiesName);
        
        if (!modificationNames.values().contains(psName)) {
            shortNames.remove(psName);
            colors.remove(psName);
        }
    }

    /**
     * Returns a mapping of the PeptideShaker names to the colors used.
     * 
     * @return a mapping of the PeptideShaker names to the colors used
     */
    public HashMap<String, Color> getPtmColors() {
        return colors;
    }
}
