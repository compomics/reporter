package eu.isas.reporter.cli;

import com.compomics.cli.identification_parameters.AbstractIdentificationParametersCli;
import com.compomics.software.CompomicsWrapper;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingHandlerCLIImpl;
import com.compomics.util.waiting.WaitingHandler;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

/**
 * The SearchParametersCLI allows creating search parameters files using command
 * line arguments.
 *
 * @author Marc Vaudel
 */
public class IdentificationParametersCLI extends AbstractIdentificationParametersCli {

    /**
     * The waiting handler.
     */
    private WaitingHandler waitingHandler;

    /**
     * Construct a new SearchParametersCLI runnable from a list of arguments.
     * When initialization is successful, calling "run" will write the created
     * parameters file.
     *
     * @param args the command line arguments
     */
    public IdentificationParametersCLI(String[] args) {

        try {

            waitingHandler = new WaitingHandlerCLIImpl();
            // check if there are updates to the paths
            String[] nonPathSettingArgsAsList = PathSettingsCLI.extractAndUpdatePathOptions(args);
            initiate(nonPathSettingArgsAsList);

        } catch (ParseException ex) {

            waitingHandler.appendReport(
                    "An error occurred while running the command line.",
                    true,
                    true
            );

            ex.printStackTrace();

        }

    }

    /**
     * Starts the launcher by calling the launch method. Use this as the main
     * class in the jar file.
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {

        try {
            new IdentificationParametersCLI(args);
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    @Override
    protected void createOptionsCLI(Options options) {
        ReporterCLIParameters.createOptionsCLI(options);
    }

    @Override
    protected String getOptionsAsString() {
        return ReporterCLIParameters.getOptionsAsString();
    }

    /**
     * Returns the path to the jar file.
     *
     * @return the path to the jar file
     */
    public String getJarFilePath() {

        return CompomicsWrapper.getJarFilePath(
                this.getClass().getResource("IdentificationParametersCLI.class").getPath(),
                "Reporter"
        );

    }
}
