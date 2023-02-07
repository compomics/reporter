package eu.isas.reporter.calculation.clustering.keys;

import eu.isas.reporter.calculation.clustering.ClusterClassKey;
import java.io.Serializable;

/**
 * Key for the class of a PSM cluster.
 *
 * @author Marc Vaudel
 */
public class PsmClusterClassKey implements ClusterClassKey, Serializable {

    /**
     * Indicates whether the PSMs must be starred.
     */
    private Boolean starred = false;
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
     * Indicates whether the PSMs must be starred.
     *
     * @return a boolean indicating whether the PSMs must be starred
     */
    public Boolean isStarred() {
        return starred;
    }

    /**
     * Sets whether the PSMs must be starred.
     *
     * @param starred a boolean indicating whether the PSMs must be starred
     */
    public void setStarred(Boolean starred) {
        this.starred = starred;
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

        StringBuilder name = new StringBuilder();

        if (starred) {
            name.append("Starred");
        }

        if (file != null) {

            if (name.length() > 0) {
                name.append(" ");
            }

            name.append(file);

        }

        if (name.length() > 0) {
            return name.toString();
        }

        return "All";
    }

    @Override
    public String getDescription() {

        StringBuilder desciption = new StringBuilder();

        if (starred) {
            desciption.append("Starred ");
        } else if (file == null) {
            desciption.append("All ");
        }

        desciption.append("PSMs");

        if (file != null) {
            desciption.append(" from ").append(file);
        }

        return desciption.toString();

    }

    @Override
    public String toString() {
        return "PSM_" + getName();
    }
}
