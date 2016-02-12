package eu.isas.reporter.calculation.clustering;

/**
 * Key for a cluster class.
 *
 * @author Marc Vaudel
 */
public interface ClusterClassKey {

    /**
     * Returns the name of the cluster class.
     * 
     * @return the name of the cluster class
     */
    public String getName();
    
    /**
     * Returns the description of the cluster class.
     * 
     * @return the description of the cluster class
     */
    public String getDescription();
    
}
