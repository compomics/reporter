package eu.isas.reporter.preferences;

import eu.isas.reporter.quantificationdetails.ProteinRatioType;
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
     * The type of ratio to display.
     */
    private int proteinRatioType = 0;
    
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
     * @param proteinRatioType the index of the selected protein ratio type to display
     */
    public void setProteinRatioType(int proteinRatioType) {
        this.proteinRatioType = proteinRatioType;
    }
    
    

}
