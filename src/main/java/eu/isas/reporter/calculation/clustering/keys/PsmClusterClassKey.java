package eu.isas.reporter.calculation.clustering.keys;

import eu.isas.reporter.calculation.clustering.ClusterClassKey;

/**
 * Key for the class of a PSM cluster.
 *
 * @author Marc Vaudel
 */
public class PsmClusterClassKey implements ClusterClassKey {

    /**
     * The name of the spectrum file.
     */
    private String file = null;

    /**
     * Constructor.
     */
    public PsmClusterClassKey() {

    }

    /**
     * Returns the spectrum file.
     *
     * @return the spectrum file
     */
    public String getFile() {
        return file;
    }

    /**
     * Sets the spectrum file.
     *
     * @param file the spectrum file
     */
    public void setFile(String file) {
        this.file = file;
    }

    @Override
    public String getName() {
        return file;
    }

    @Override
    public String getDescription() {
        return "PSMs from" + file;
    }
}
