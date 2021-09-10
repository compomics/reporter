package eu.isas.reporter.preferences;

import com.compomics.software.settings.PathKey;
import com.compomics.software.settings.UtilitiesPathParameters;
import com.compomics.util.io.flat.SimpleFileReader;
import com.compomics.util.io.flat.SimpleFileWriter;
import eu.isas.peptideshaker.preferences.PeptideShakerPathParameters;
import eu.isas.reporter.export.report.ReporterExportFactory;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

/**
 * This class sets the path preferences for the files to read/write.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterPathPreferences {

    /**
     * Enum of the paths which can be set in Reporter.
     */
    public enum ReporterPathKey implements PathKey {

        /**
         * Folder containing the user custom exports file.
         */
        reporterExports("reporter_exports", "Folder containing the user custom exports file.", "", true);
        /**
         * The key used to refer to this path.
         */
        private String id;
        /**
         * The description of the path usage.
         */
        private String description;
        /**
         * The default sub directory or file to use in case all paths should be
         * included in a single directory.
         */
        private String defaultSubDirectory;
        /**
         * Indicates whether the path should be a folder.
         */
        private boolean isDirectory;

        /**
         * Constructor.
         *
         * @param id the id used to refer to this path key
         * @param description the description of the path usage
         * @param defaultSubDirectory the sub directory to use in case all paths
         * should be included in a single directory
         * @param isDirectory boolean indicating whether a folder is expected
         */
        private ReporterPathKey(
                String id, 
                String description, 
                String defaultSubDirectory, 
                boolean isDirectory
        ) {

            this.id = id;
            this.description = description;
            this.defaultSubDirectory = defaultSubDirectory;
            this.isDirectory = isDirectory;

        }

        /**
         * Returns the key from its id. Null if not found.
         *
         * @param id the id of the key of interest
         *
         * @return the key of interest
         */
        public static ReporterPathKey getKeyFromId(String id) {

            for (ReporterPathKey pathKey : values()) {

                if (pathKey.id.equals(id)) {
                    return pathKey;
                }

            }

            return null;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public String getDescription() {
            return description;
        }

    }

    /**
     * Loads the path preferences from a text file.
     *
     * @param inputFile the file to load the path preferences from
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     * @throws IOException thrown if an IOException occurs
     */
    public static void loadPathParametersFromFile(
            File inputFile
    ) throws FileNotFoundException, IOException {

        try ( SimpleFileReader reader = SimpleFileReader.getFileReader(inputFile)) {

            String line;

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (!line.equals("") && !line.startsWith("#")) {

                    loadPathParametersFromLine(line);

                }
            }
        }
    }

    /**
     * Loads a path to be set from a line.
     *
     * @param line the line where to read the path from
     * @throws FileNotFoundException thrown of the file cannot be found
     */
    public static void loadPathParametersFromLine(
            String line
    ) throws FileNotFoundException, IOException {

        String id = UtilitiesPathParameters.getPathID(line);

        if (id.equals("")) {
            throw new IllegalArgumentException("Impossible to parse path in " + line + ".");
        }

        ReporterPathKey reporterPathKey = ReporterPathKey.getKeyFromId(id);

        if (reporterPathKey == null) {

            PeptideShakerPathParameters.loadPathParametersFromLine(line);

        } else {

            String path = UtilitiesPathParameters.getPath(line);

            if (!path.equals(UtilitiesPathParameters.defaultPath)) {

                File file = new File(path);

                if (!file.exists()) {
                    throw new FileNotFoundException(
                            "File "
                            + path
                            + " not found."
                    );
                }

                if (reporterPathKey.isDirectory && !file.isDirectory()) {
                    throw new FileNotFoundException(
                            "Found a file when expecting a directory for "
                            + reporterPathKey.id
                            + "."
                    );
                }

                setPathPreference(reporterPathKey, path);
            }
        }
    }

    /**
     * Sets the path according to the given key and path.
     *
     * @param reporterPathKey the key of the path
     * @param path the path to be set
     */
    public static void setPathPreference(ReporterPathKey reporterPathKey, String path) {

        switch (reporterPathKey) {

            case reporterExports:
                ReporterExportFactory.setJsonFolder(path);
                return;

            default:
                throw new UnsupportedOperationException(
                        "Path "
                        + reporterPathKey.id
                        + " not implemented."
                );
        }

    }

    /**
     * Sets all the paths inside a given folder.
     *
     * @param path the path of the folder where to redirect all paths.
     *
     * @throws FileNotFoundException thrown if a file cannot be found
     */
    public static void setAllPathsIn(String path) throws FileNotFoundException {

        for (ReporterPathKey reporterPathKey : ReporterPathKey.values()) {

            String subDirectory = reporterPathKey.defaultSubDirectory;
            File newFile = new File(path, subDirectory);

            if (!newFile.exists()) {
                newFile.mkdirs();
            }

            if (!newFile.exists()) {
                throw new FileNotFoundException(
                        newFile.getAbsolutePath()
                        + "could not be created."
                );
            }

            setPathPreference(reporterPathKey, newFile.getAbsolutePath());
        }

        UtilitiesPathParameters.setAllPathsIn(path);
    }

    /**
     * Writes all path configurations to the given file.
     *
     * @param file the destination file
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void writeConfigurationToFile(
            File file
    ) throws IOException {

        try ( SimpleFileWriter writer = new SimpleFileWriter(file, false)) {

            writeConfigurationToFile(writer);

        }
    }
    
    /**
     * Writes the configuration file using the provided buffered writer.
     *
     * @param writer the writer to use for writing.
     *
     * @throws FileNotFoundException thrown if an FileNotFoundException occurs
     */
    public static void writeConfigurationToFile(
            SimpleFileWriter writer
    ) throws IOException {

        for (ReporterPathKey pathKey : ReporterPathKey.values()) {

            writePathToFile(writer, pathKey);

        }

        PeptideShakerPathParameters.writeConfigurationToFile(writer);

    }

    /**
     * Writes the path of interest using the provided buffered writer.
     *
     * @param writer the writer to use for writing
     * @param pathKey the key of the path of interest
     *
     * @throws IOException thrown if an IOException occurs
     */
    public static void writePathToFile(
            SimpleFileWriter writer, 
            ReporterPathKey pathKey
    ) throws IOException {

        writer.write(pathKey.id + UtilitiesPathParameters.separator);

        switch (pathKey) {

            case reporterExports:

                String toWrite = ReporterExportFactory.getJsonFolder();

                if (toWrite == null) {
                    toWrite = UtilitiesPathParameters.defaultPath;
                }

                writer.write(toWrite);

                break;

            default:
                throw new UnsupportedOperationException(
                        "Path "
                        + pathKey.id
                        + " not implemented."
                );
        }

        writer.newLine();
    }

    /**
     * Returns the path according to the given key and path.
     *
     * @param reporterPathKey the key of the path
     * @param jarFilePath path to the jar file
     *
     * @return the path
     */
    public static String getPathPreference(ReporterPathKey reporterPathKey, String jarFilePath) {

        switch (reporterPathKey) {

            case reporterExports:
                return ReporterExportFactory.getJsonFolder();

            default:
                throw new UnsupportedOperationException(
                        "Path "
                        + reporterPathKey.id
                        + " not implemented."
                );
        }
    }

    /**
     * Returns a list containing the keys of the paths where the tool is not
     * able to write.
     *
     * @param jarFilePath the path to the jar file
     *
     * @return a list containing the keys of the paths where the tool is not
     * able to write
     *
     * @throws IOException exception thrown whenever an error occurred while
     * loading the path configuration
     */
    public static ArrayList<PathKey> getErrorKeys(String jarFilePath) throws IOException {

        ArrayList<PathKey> result = new ArrayList<>();

        for (ReporterPathKey pathKey : ReporterPathKey.values()) {

            String folder = ReporterPathPreferences.getPathPreference(pathKey, jarFilePath);

            if (folder != null && !UtilitiesPathParameters.testPath(folder)) {
                result.add(pathKey);
            }
        }

        result.addAll(UtilitiesPathParameters.getErrorKeys());

        return result;
    }
}
