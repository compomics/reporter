package eu.isas.reporter;

import com.compomics.software.CompomicsWrapper;
import eu.isas.reporter.utils.Properties;
import java.io.*;

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
     */
    public ReporterWrapper() {

        // get the version number set in the pom file
        String jarFileName = "Reporter-" + new Properties().getVersion() + ".jar";
        String path = this.getClass().getResource("ReporterWrapper.class").getPath();
        // remove starting 'file:' tag if there
        if (path.startsWith("file:")) {
            path = path.substring("file:".length(), path.indexOf(jarFileName));
        } else {
            path = path.substring(0, path.indexOf(jarFileName));
        }
        path = path.replace("%20", " ");
        path = path.replace("%5b", "[");
        path = path.replace("%5d", "]");
        File jarFile = new File(path, jarFileName);
        // get the splash 
        String splash = "reporter-splash.png";
        String mainClass = "eu.isas.reporter.gui.ReporterGUI";

        launchTool("Reporter", jarFile, splash, mainClass);
    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args
     */
    public static void main(String[] args) {
        new ReporterWrapper();
    }
}
