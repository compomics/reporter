package eu.isas.reporter.settings;

/**
 * Enum of the possible spectrum reporter ions locations.
 *
 * @author Harald Barsnes
 */
public enum ReporterIonsLocationType {

    /**
     * The reporter ions are in the same MS2 spectrum.
     */
    ms2Spectra(0, "MS2 Spectra"),
    /**
     * The reporter ions are in the related MS3 spectrum.
     */
    ms3Spectra(1, "MS3 Spectra"),
    /**
     * The reporter ions are found by matching on mass and retention time.
     */
    precursorMatching(2, "Precursor Matching");

    /**
     * The index of the reporter ions location type.
     */
    public final int index;
    /**
     * The name of the reporter ions location type.
     */
    public final String name;

    /**
     * Constructor.
     *
     * @param index the index of the reporter ions location type
     * @param name the name of the reporter ions location type
     */
    private ReporterIonsLocationType(int index, String name) {
        this.index = index;
        this.name = name;
    }

    /**
     * Returns the reporter ions location type corresponding to the given index.
     *
     * @param index the index of the reporter ions location type
     *
     * @return the reporter ions location type of interest.
     */
    public static ReporterIonsLocationType getReporterIonsLocationType(int index) {

        for (ReporterIonsLocationType reporterIonsLocationType : ReporterIonsLocationType.values()) {

            if (reporterIonsLocationType.index == index) {
                return reporterIonsLocationType;
            }

        }

        throw new IllegalArgumentException(
                "No reporter ions location type found for index " + index + "."
        );
    }

    /**
     * Returns the reporter ions location type corresponding to the given name.
     *
     * @param name the name of the reporter ions location type
     *
     * @return the reporter ions location type of interest.
     */
    public static ReporterIonsLocationType getReporterIonsLocationType(String name) {

        for (ReporterIonsLocationType reporterIonsLocationType : ReporterIonsLocationType.values()) {

            if (reporterIonsLocationType.name.equals(name)) {
                return reporterIonsLocationType;
            }

        }

        throw new IllegalArgumentException(
                "No reporter ions location type found for name " + name + "."
        );
    }

    /**
     * Returns the different options as command line description.
     *
     * @return the different options as command line description
     */
    public static String getCommandLineDescription() {

        StringBuilder sb = new StringBuilder();

        for (ReporterIonsLocationType reporterIonsLocationType : values()) {

            if (sb.length() > 0) {
                sb.append(", ");
            }

            sb.append(reporterIonsLocationType.index).append(": ").append(reporterIonsLocationType.name);
        }

        return sb.toString();

    }
}
