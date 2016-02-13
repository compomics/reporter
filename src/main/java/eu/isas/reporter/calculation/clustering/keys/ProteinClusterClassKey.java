package eu.isas.reporter.calculation.clustering.keys;

import eu.isas.reporter.calculation.clustering.ClusterClassKey;

/**
 * Key for the class of a protein cluster.
 *
 * @author Marc Vaudel
 */
public class ProteinClusterClassKey implements ClusterClassKey {

    /**
     * Indicates whether the psms must be starred.
     */
    private Boolean starred = false;

    /**
     * Constructor.
     */
    public ProteinClusterClassKey() {
        
    }

    /**
     * Indicates whether the psms must be starred.
     *
     * @return a boolean indicating whether the psms must be starred
     */
    public Boolean isStarred() {
        return starred;
    }

    /**
     * Sets whether the psms must be starre.
     *
     * @param starred a boolean indicating whether the psms must be starred
     */
    public void setStarred(Boolean starred) {
        this.starred = starred;
    }
    
    @Override
    public String getName() {
        if (starred) {
            return "Starred";
        }
        return "All";
    }

    @Override
    public String getDescription() {
        StringBuilder description = new StringBuilder();
        if (starred) {
        description.append("Starred ");
                } else {
            description.append("All ");
        }
        description.append("proteins");
        return description.toString();
    }

}
