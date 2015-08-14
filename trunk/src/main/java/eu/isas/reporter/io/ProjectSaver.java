package eu.isas.reporter.io;

import com.compomics.util.db.ObjectsDB;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.reporter.settings.ReporterSettings;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * This class can be used to save the Reporter projets in a cps file.
 *
 * @author Marc Vaudel
 */
public class ProjectSaver {

    /**
     * Name for the reporter settings table
     */
    public static final String reporterSettingsTableName = "reporter_settings";

    /**
     * Saves the Reporter project information in the given database.
     *
     * @param reporterSettings the reporter settings
     * @param reporterIonQuantification the reporter ion quantification
     * @param cpsParent the cps parent
     * @param waitingHandler waiting handler displaying feedback to the user.
     * can be null.
     *
     *
     * @throws IOException thrown of IOException occurs exception thrown
     * whenever an error occurred while reading or writing a file
     * @throws SQLException thrown of SQLException occurs exception thrown
     * whenever an error occurred while interacting with the database
     * @throws java.lang.ClassNotFoundException exception thrown whenever an
     * error occurred while deserializing an object
     * @throws java.lang.InterruptedException exception thrown whenever a
     * threading error occurred while saving the project
     * @throws ArchiveException thrown of ArchiveException occurs exception
     * thrown whenever an error occurred while taring the project
     */
    public static void saveProject(ReporterSettings reporterSettings, ReporterIonQuantification reporterIonQuantification, CpsParent cpsParent, WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, ArchiveException {

        ObjectsDB objectsDB = cpsParent.getIdentification().getIdentificationDB().getObjectsDB();
        if (!objectsDB.hasTable(reporterSettingsTableName)) {
            objectsDB.addTable(reporterSettingsTableName);
        }
        if (objectsDB.inDB(reporterSettingsTableName, ReporterSettings.class.getName(), false)) {
            objectsDB.updateObject(reporterSettingsTableName, ReporterSettings.class.getName(), reporterSettings, false);
        } else {
            objectsDB.insertObject(reporterSettingsTableName, ReporterSettings.class.getName(), reporterSettings, false);
        }
        if (objectsDB.inDB(reporterSettingsTableName, ReporterIonQuantification.class.getName(), false)) {
            objectsDB.updateObject(reporterSettingsTableName, ReporterIonQuantification.class.getName(), reporterIonQuantification, false);
        } else {
            objectsDB.insertObject(reporterSettingsTableName, ReporterIonQuantification.class.getName(), reporterIonQuantification, false);
        }

        cpsParent.saveProject(waitingHandler, false);
    }
}
