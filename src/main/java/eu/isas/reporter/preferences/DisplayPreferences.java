package eu.isas.reporter.preferences;

import eu.isas.reporter.settings.ClusteringSettings;

/**
 * Reporter display preferences.
 *
 * @author Marc Vaudel
 */
public class DisplayPreferences {

    /**
     * The clustering settings.
     */
    private ClusteringSettings clusteringSettings;
    
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
     * @param clusteringSettings 
     */
    public void setClusteringSettings(ClusteringSettings clusteringSettings) {
        this.clusteringSettings = clusteringSettings;
    }

}
