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
     *
     * @param args the arguments to pass to the tool
     */
    public ReporterWrapper(String[] args) {

        // get the version number set in the pom file
        String jarFileName = "Reporter-" + new Properties().getVersion() + ".jar";
        String path = getJarFilePath();
        File jarFile = new File(path, jarFileName);

        // get the splash 
        String splash = "reporter-splash.png";
        String mainClass = "eu.isas.reporter.gui.ReporterGUI";

        // set path for utilities preferences
        try {
            //setPathConfiguration(); // @TODO: implement..?
        } catch (Exception e) {
            System.out.println("Impossible to load path configuration, default will be used.");
        }

        launchTool("Reporter", jarFile, splash, mainClass, args);
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {
        return CompomicsWrapper.getJarFilePath(this.getClass().getResource("ReporterWrapper.class").getPath(), "Reporter");
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
