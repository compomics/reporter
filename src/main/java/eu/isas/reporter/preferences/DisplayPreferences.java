package eu.isas.reporter.preferences;

import eu.isas.reporter.settings.ClusteringSettings;
import java.io.Serializable;

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
}
