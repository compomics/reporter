package eu.isas.reporter;

import eu.isas.reporter.calculation.ItraqCalculator;
import com.compomics.util.experiment.MsExperiment;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.jgoodies.looks.plastic.PlasticLookAndFeel;
import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;
import com.jgoodies.looks.plastic.theme.SkyKrupp;
import eu.isas.reporter.calculation.Ignorer;
import eu.isas.reporter.gui.ResultPanel;
import eu.isas.reporter.gui.StartPanel;
import eu.isas.reporter.utils.Properties;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;


/**
 * This class.
 *
 * @author Marc
 */
public class Reporter {
    /**
     * If set to true all messages will be sent to a log file.
     */
    private static boolean useLogFile = true;
    /**
     * The main frame.
     */
    private JFrame mainFrame;
    /**
     * The last folder opened by the user. Defaults to user.home.
     */
    private String lastSelectedFolder = "user.home";
    private Ignorer ignorer;
    /**
     * Main method.
     *
     * @param args String[] with the start-up arguments.
     */
    public static void main(String[] args) {
        new Reporter();
    }
    /**
     * main constructor.
     */
    public Reporter() {
        // check if a newer version of Reporter is available
        checkForNewVersion(new Properties().getVersion());

        // set up the ErrorLog
        setUpLogFile();

        // Start the GUI
        createandshowGUI();
    }

    /**
     * Creates the GUI and adds the tabs to the frame. Then sets the size and
     * location of the frame and makes it visible.
     */
    private void createandshowGUI() {

        mainFrame = new JFrame("Reporter " + new Properties().getVersion());

        mainFrame.addWindowListener(new WindowAdapter() {

            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             */
            @Override
            public void windowClosing(WindowEvent e) {
                close(0);
            }
        });

        // sets the icon of the frame
        mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().
                getResource("/icons/reporter.gif")));

        // update the look and feel after adding the panels
        setLookAndFeel();

        // display the start panel
        mainFrame.add(new StartPanel(this));

        // set size and location
        mainFrame.pack();
        mainFrame.setResizable(false);
        mainFrame.setLocationRelativeTo(null);
        // Pack is the minimal size, so add 20 pixels in each dimension.
        mainFrame.setSize(new Dimension(mainFrame.getSize().width + 20, mainFrame.getSize().height));
        mainFrame.setVisible(true);
    }

    public void startProcessing(ItraqCalculator calculator, Ignorer ignorer) {
        this.ignorer = ignorer;
        calculator.computeRatios();
    }

    public void restart() {
        mainFrame.dispose();
        createandshowGUI();
    }

    public void displayResults(ReporterIonQuantification quantification, MsExperiment experiment) {
        mainFrame.dispose();
        mainFrame= new JFrame("Reporter " + new Properties().getVersion());

        mainFrame.addWindowListener(new WindowAdapter() {

            /**
             * Invoked when a window is in the process of being closed.
             * The close operation can be overridden at this point.
             */
            @Override
            public void windowClosing(WindowEvent e) {
                close(0);
            }
        });

        // sets the icon of the frame
        mainFrame.setIconImage(Toolkit.getDefaultToolkit().getImage(getClass().
                getResource("/icons/reporter.gif")));

        // update the look and feel after adding the panels
        setLookAndFeel();

        // display the start panel
        mainFrame.add(new ResultPanel(this, quantification, experiment, ignorer));

        // set size and location
        mainFrame.pack();
        mainFrame.setResizable(false);
        mainFrame.setLocationRelativeTo(null);
        // Pack is the minimal size, so add 20 pixels in each dimension.
        mainFrame.setSize(new Dimension(mainFrame.getSize().width + 20, mainFrame.getSize().height));
        mainFrame.setVisible(true);
    }

    /**
     * Check if a newer version of reporter is available.
     *
     * @param currentVersion the version number of the currently running reporter
     */
    private static void checkForNewVersion(String currentVersion) {
/*
        try {
            boolean deprecatedOrDeleted = false;
            URL downloadPage = new URL(
                    "http://code.google.com/p/reporter/downloads/detail?name=reporter-" +
                    currentVersion + ".zip");
            int respons = ((java.net.HttpURLConnection) downloadPage.openConnection()).getResponseCode();

            // 404 means that the file no longer exists, which means that
            // the running version is no longer available for download,
            // which again means that a never version is available.
            if (respons == 404) {
                deprecatedOrDeleted = true;
            } else {

                // also need to check if the available running version has been
                // deprecated (but not deleted)
                BufferedReader in = new BufferedReader(
                        new InputStreamReader(downloadPage.openStream()));

                String inputLine;

                while ((inputLine = in.readLine()) != null && !deprecatedOrDeleted) {
                    if (inputLine.lastIndexOf("Deprecated") != -1 &&
                            inputLine.lastIndexOf("Deprecated Downloads") == -1 &&
                            inputLine.lastIndexOf("Deprecated downloads") == -1) {
                        deprecatedOrDeleted = true;
                    }
                }

                in.close();
            }

            // informs the user about an updated version of the converter, unless the user
            // is running a beta version
            if (deprecatedOrDeleted && currentVersion.lastIndexOf("beta") == -1) {
                int option = JOptionPane.showConfirmDialog(null,
                        "A newer version of reporter is available.\n" +
                        "Do you want to upgrade?",
                        "Upgrade Available",
                        JOptionPane.YES_NO_CANCEL_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    BareBonesBrowserLaunch.openURL("http://reporter.googlecode.com/");
                    System.exit(0);
                } else if (option == JOptionPane.CANCEL_OPTION) {
                    System.exit(0);
                }
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }*/
    }

    /**
     * Set up the log file.
     */
    private void setUpLogFile() {
        if (useLogFile && !getJarFilePath().equalsIgnoreCase(".")) {
            try {
                String path = getJarFilePath() + "/conf/reporterLog.txt";

                File file = new File(path);
                System.setOut(new java.io.PrintStream(new FileOutputStream(file, true)));
                System.setErr(new java.io.PrintStream(new FileOutputStream(file, true)));

                // creates a new log file if it does not exist
                if (!file.exists()) {
                    file.createNewFile();

                    FileWriter w = new FileWriter(file);
                    BufferedWriter bw = new BufferedWriter(w);

                    bw.close();
                    w.close();
                }
            } catch (Exception e) {
                JOptionPane.showMessageDialog(
                        null, "An error occured when trying to create the SearchGUILog.",
                        "Error Creating Log File", JOptionPane.ERROR_MESSAGE);
                e.printStackTrace();
            }
        }
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    private String getJarFilePath() {
        String path = this.getClass().getResource("Reporter.class").getPath();

        if (path.lastIndexOf("/reporter-") != -1) {
            path = path.substring(5, path.lastIndexOf("/reporter-"));
            path = path.replace("%20", " ");
        } else {
            path = ".";
        }

        return path;
    }

    /**
     * This method terminates the program.
     *
     * @param aStatus int with the completion status.
     */
    public void close(int aStatus) {
        mainFrame.setVisible(false);
        mainFrame.dispose();
        System.exit(aStatus);
    }


    /**
     * Sets the look and feel of the SearchGUI.
     * <p/>
     * Note that the GUI has been created with the following look and feel
     * in mind. If using a different look and feel you might need to tweak the GUI
     * to get the best appearance.
     */
    private void setLookAndFeel() {

        try {
            PlasticLookAndFeel.setPlasticTheme(new SkyKrupp());
            UIManager.setLookAndFeel(new PlasticXPLookAndFeel());
            SwingUtilities.updateComponentTreeUI(mainFrame);
        } catch (UnsupportedLookAndFeelException e) {
            // ignore exception, i.e. use default look and feel
        }
    }

    public JFrame getMainFrame() {
        return mainFrame;
    }

    /**
     * @return the lastSelectedFolder
     */
    public String getLastSelectedFolder() {
        return lastSelectedFolder;
    }

    /**
     * @param lastSelectedFolder the lastSelectedFolder to set
     */
    public void setLastSelectedFolder(String lastSelectedFolder) {
        this.lastSelectedFolder = lastSelectedFolder;
    }
}
