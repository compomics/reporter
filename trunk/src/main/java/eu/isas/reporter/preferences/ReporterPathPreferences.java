package eu.isas.reporter.preferences;

import com.compomics.util.preferences.UtilitiesPathPreferences;
import eu.isas.peptideshaker.preferences.PeptideShakerPathPreferences;
import eu.isas.reporter.export.report.ReporterExportFactory;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * This class sets the path preferences for the files to read/write.
 *
 * @author Marc Vaudel
 */
public class ReporterPathPreferences {

    /**
     * Enum of the paths which can be set in Reporter.
     */
    public enum ReporterPathKey {

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
        private ReporterPathKey(String id, String description, String defaultSubDirectory, boolean isDirectory) {
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

        /**
         * Returns the id of the path.
         *
         * @return the id of the path
         */
        public String getId() {
            return id;
        }

        /**
         * Returns the description of the path.
         *
         * @return the description of the path
         */
        public String getDescription() {
            return description;
        }

    }

    /**
     * Loads the path preferences from a text file.
     *
     * @param inputFile the file to load the path preferences from
     *
     * @throws FileNotFoundException
     * @throws IOException
     */
    public static void loadPathPreferencesFromFile(File inputFile) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        try {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (!line.equals("") && !line.startsWith("#")) {
                    loadPathPreferenceFromLine(line);
                }
            }
        } finally {
            br.close();
        }
    }

    /**
     * Loads a path to be set from a line.
     *
     * @param line the line where to read the path from
     * @throws java.io.FileNotFoundException
     */
    public static void loadPathPreferenceFromLine(String line) throws FileNotFoundException, IOException {
        String id = UtilitiesPathPreferences.getPathID(line);
        if (id.equals("")) {
            throw new IllegalArgumentException("Impossible to parse path in " + line + ".");
        }
        ReporterPathKey reporterPathKey = ReporterPathKey.getKeyFromId(id);
        if (reporterPathKey == null) {
            PeptideShakerPathPreferences.loadPathPreferenceFromLine(line);
        } else {
            String path = UtilitiesPathPreferences.getPath(line);
            File file = new File(path);
            if (!file.exists()) {
                throw new FileNotFoundException("File " + path + " not found.");
            }
            if (reporterPathKey.isDirectory && !file.isDirectory()) {
                throw new FileNotFoundException("Found a file when expecting a directory for " + reporterPathKey.id + ".");
            }
            setPathPreference(reporterPathKey, path);
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
                ReporterExportFactory.setSerializationFolder(path);
                return;
            default:
                throw new UnsupportedOperationException("Path " + reporterPathKey.id + " not implemented.");
        }
    }

    /**
     * Sets all the paths inside a given folder.
     *
     * @param path the path of the folder where to redirect all paths.
     *
     * @throws FileNotFoundException
     */
    public static void setAllPathsIn(String path) throws FileNotFoundException {
        for (ReporterPathKey reporterPathKey : ReporterPathKey.values()) {
            String subDirectory = reporterPathKey.defaultSubDirectory;
            File newFile = new File(path, subDirectory);
            if (!newFile.exists()) {
                newFile.mkdirs();
            }
            if (!newFile.exists()) {
                throw new FileNotFoundException(newFile.getAbsolutePath() + "could not be created.");
            }
            setPathPreference(reporterPathKey, newFile.getAbsolutePath());
        }
        UtilitiesPathPreferences.setAllPathsIn(path);
    }

    /**
     * Writes all path configurations to the given file.
     *
     * @param file the destination file
     *
     * @throws IOException
     */
    public static void writeConfigurationToFile(File file) throws IOException {
        BufferedWriter bw = new BufferedWriter(new FileWriter(file));
        try {
            writeConfigurationToFile(bw);
        } finally {
            bw.close();
        }
    }

    /**
     * Writes the configurations file using the provided buffered writer.
     * 
     * @param bw the writer to use for writing.
     *
     * @throws IOException
     */
    public static void writeConfigurationToFile(BufferedWriter bw) throws IOException {
        for (ReporterPathKey pathKey : ReporterPathKey.values()) {
            writePathToFile(bw, pathKey);
        }
        PeptideShakerPathPreferences.writeConfigurationToFile(bw);
    }

    /**
     * Writes the path of interest using the provided buffered writer.
     *
     * @param bw the writer to use for writing
     * @param pathKey the key of the path of interest
     *
     * @throws IOException
     */
    public static void writePathToFile(BufferedWriter bw, ReporterPathKey pathKey) throws IOException {
        bw.write(pathKey.id + UtilitiesPathPreferences.separator);
        switch (pathKey) {
            case reporterExports:
                bw.write(ReporterExportFactory.getSerializationFolder());
                break;
            default:
                throw new UnsupportedOperationException("Path " + pathKey.id + " not implemented.");
        }
        bw.newLine();
    }
}
