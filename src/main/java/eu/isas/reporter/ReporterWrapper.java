package eu.isas.reporter;

import com.compomics.software.CompomicsWrapper;
import com.compomics.software.settings.PathKey;
import eu.isas.reporter.preferences.ReporterPathPreferences;
import eu.isas.reporter.utils.Properties;
import java.io.*;
import java.util.ArrayList;

/**
 * A wrapper class used to start the jar file with parameters. The parameters
 * are read from the JavaOptions file in the Properties folder.
 *
 * @author Harald Barsnes
 */
public class ReporterWrapper extends CompomicsWrapper {

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the arguments to pass to the tool
     */
    public ReporterWrapper(String[] args) {

        // get the version number set in the pom file
        String jarFileName = "Reporter-" + new Properties().getVersion() + ".jar";
        String jarFilePath = Reporter.getJarFilePath();
        File jarFile = new File(jarFilePath, jarFileName);

        // get the splash 
        String splash = "reporter-splash.png";
        String mainClass = "eu.isas.reporter.gui.ReporterGUI";

        // set path for utilities preferences
        try {
            Reporter.setPathConfiguration();
        } catch (Exception e) {
            System.out.println("Impossible to load path configuration, default will be used.");
        }
        try {
            ArrayList<PathKey> errorKeys = ReporterPathPreferences.getErrorKeys(jarFilePath);
            if (!errorKeys.isEmpty()) {
                System.out.println("Unable to write in the following configuration folders. Please edit the configuration paths.");
                for (PathKey pathKey : errorKeys) {
                    System.out.println(pathKey.getId() + ": " + pathKey.getDescription());
                }
            }
        } catch (Exception e) {
            System.out.println("Unable to load the path configurations. Default paths will be used.");
        }

        launchTool("Reporter", jarFile, splash, mainClass, args);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        new ReporterWrapper(args);
    }
}
