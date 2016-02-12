package eu.isas.reporter.calculation.clustering.keys;

import eu.isas.reporter.calculation.clustering.ClusterClassKey;

/**
 * Key for the class of a protein cluster.
 *
 * @author Marc Vaudel
 */
public class ProteinClusterClassKey implements ClusterClassKey {

    /**
     * Constructor.
     */
    public ProteinClusterClassKey() {
        
    }
    
    @Override
    public String getName() {
        return "Proteins";
    }

    @Override
    public String getDescription() {
        return "Proteins";
    }

}
