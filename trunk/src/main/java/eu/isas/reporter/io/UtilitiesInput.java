package eu.isas.reporter.io;

import com.compomics.util.experiment.MsExperiment;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.Date;
import javax.swing.JOptionPane;

/**
 * This class will be used to import an experiment from utilities
 *
 * @author Marc
 */
public class UtilitiesInput {

    /**
     * Constructor
     */
    public UtilitiesInput() {
    }

    /**
     * Returns the experiment contained in the selected file
     * @param utilitiesFile the selected file
     * @return the loaded experiment
     */
    public MsExperiment importExperiment(File utilitiesFile) {

        try {
            FileInputStream fis = new FileInputStream(utilitiesFile);
            ObjectInputStream in = new ObjectInputStream(fis);
            Date date = (Date) in.readObject();
            MsExperiment experiment = (MsExperiment) in.readObject();
            in.close();

            JOptionPane.showMessageDialog(null, "Experiment " + experiment.getReference() + " created on " + date.toString() + " imported.", "Identifications Imported.", JOptionPane.INFORMATION_MESSAGE);
            return experiment;
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(null, "An error occured while reading" + utilitiesFile + ". Please verif that the compomics utilities version used to create the file was the same as the one used by your version of Reporter.", "File Input Error.", JOptionPane.ERROR_MESSAGE);
            ex.printStackTrace();
        }

        return null;

    }
}
