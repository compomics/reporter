package eu.isas.reporter.quantificationdetails;

/**
 * Enum for the protein ratio types.
 *
 * @author Marc Vaudel
 */
public enum ProteinRatioType {

    all(0, "All", "Ratio calculated using all peptides."),
    unique(1, "Unique", "Ratio calculated using peptides unique to the protein group."),
    shared(2, "Shared", "Ratio calculated using peptides shared with other protein groups.");

    /**
     * Index of the ratio type.
     */
    public final int index;
    /**
     * Name of the ratio type.
     */
    public final String name;
    /**
     * Description of the ratio type.
     */
    public final String description;

    /**
     * Constructor.
     *
     * @param index the index of the ratio type
     * @param name the name of the ratio type
     * @param description the description of the ratio type
     */
    private ProteinRatioType(
            int index,
            String name,
            String description
    ) {

        this.index = index;
        this.name = name;
        this.description = description;

    }

    /**
     * Returns an array of names of the different options.
     *
     * @return an array of names of the different options
     */
    public static String[] names() {

        ProteinRatioType[] values = values();
        String[] names = new String[values.length];
        int i = 0;

        for (ProteinRatioType proteinRatioType : values) {
            names[i] = proteinRatioType.name;
            i++;
        }

        return names;

    }

    /**
     * Returns the ratio type corresponding to the given index.
     *
     * @param index the index
     *
     * @return the corresponding ratio type.
     */
    public static ProteinRatioType getProteinRatioType(int index) {

        for (ProteinRatioType proteinRatioType : values()) {

            if (proteinRatioType.index == index) {
                return proteinRatioType;
            }

        }

        return null;

    }
}
