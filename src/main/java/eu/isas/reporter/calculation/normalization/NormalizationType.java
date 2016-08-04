package eu.isas.reporter.calculation.normalization;

/**
 * Enum for the different normalization types supported.
 *
 * @author Marc Vaudel
 */
public enum NormalizationType {

    none(0, "No normalization"),
    median(1, "Median"),
    mean(2, "Mean"),
    sum(3, "Sum"),
    mode(4, "Mode");

    /**
     * The index of the normalization type.
     */
    public final int index;
    /**
     * The name of the normalization type.
     */
    public final String name;

    /**
     * Constructor.
     *
     * @param index the index of the normalization type
     * @param name the name of the normalization type
     */
    private NormalizationType(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /**
     * Returns the normalization type corresponding to the given index.
     *
     * @param index the index of the normalization type
     *
     * @return the normalization type of interest.
     */
    public static NormalizationType getNormalizationType(int index) {
        for (NormalizationType normalizationType : NormalizationType.values()) {
            if (normalizationType.index == index) {
                return normalizationType;
            }
        }
        throw new IllegalArgumentException("No normalization type found for index " + index + ".");
    }

    /**
     * Returns the normalization type corresponding to the given name.
     *
     * @param name the name of the normalization type
     *
     * @return the normalization type of interest.
     */
    public static NormalizationType getNormalizationType(String name) {
        for (NormalizationType normalizationType : NormalizationType.values()) {
            if (normalizationType.name.equals(name)) {
                return normalizationType;
            }
        }
        throw new IllegalArgumentException("No normalization type found for name " + name + ".");
    }
    
    /**
     * Returns the different options as command line description.
     * 
     * @return the different options as command line description
     */
    public static String getCommandLineDescription() {
        StringBuilder sb = new StringBuilder();
        for (NormalizationType normalizationType : values()) {
            if (sb.length() > 0) {
                sb.append(", ");
            }
            sb.append(normalizationType.index).append(": ").append(normalizationType.name);
        }
        return sb.toString();
    }
}
