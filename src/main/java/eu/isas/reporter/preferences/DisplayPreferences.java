package eu.isas.reporter.preferences;

import eu.isas.reporter.settings.ClusteringSettings;
import java.io.Serializable;
import java.util.ArrayList;

/**
 * Reporter display preferences.
 *
 * @author Marc Vaudel
 */
public class DisplayPreferences implements Serializable {

    /**
     * The clustering settings.
     */
    private ClusteringSettings clusteringSettings = new ClusteringSettings();
    /**
     * The type of ratio to display.
     */
    private int proteinRatioType = 0;
    /**
     * The list of the reagents on the user defined order. Null if not set.
     */
    private ArrayList<String> reagents = null;
    /**
     * The text displayed in the cell of a table in case the data is not loaded.
     */
    public static final String LOADING_MESSAGE = "Loading...";

    /**
     * Constructor.
     */
    public DisplayPreferences() {

    }

    /**
     * Returns the clustering settings.
     *
     * @return the clustering settings
     */
    public ClusteringSettings getClusteringSettings() {
        return clusteringSettings;
    }

    /**
     * Sets the clustering settings.
     *
     * @param clusteringSettings the cluster settings
     */
    public void setClusteringSettings(ClusteringSettings clusteringSettings) {
        this.clusteringSettings = clusteringSettings;
    }

    /**
     * Returns the index of the selected protein ratio type to display.
     *
     * @return the index of the selected protein ratio type to display
     */
    public int getProteinRatioType() {
        return proteinRatioType;
    }

    /**
     * Sets the index of the selected protein ratio type to display.
     *
     * @param proteinRatioType the index of the selected protein ratio type to
     * display
     */
    public void setProteinRatioType(int proteinRatioType) {
        this.proteinRatioType = proteinRatioType;
    }
    
    /**
     * Returns the reagents in the user defined order.
     * 
     * @return the reagents in the user defined order
     */
    public ArrayList<String> getReagents() {
        return reagents;
    }
    
    /**
     * Set the regents.
     * 
     * @param reagents the regents
     */
    public void setReagents(ArrayList<String> reagents) {
        this.reagents = reagents;
    }
}
