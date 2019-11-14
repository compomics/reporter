package eu.isas.reporter.io;

import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.CpsParent;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.settings.ReporterSettings;
import java.io.IOException;
import java.sql.SQLException;
import org.apache.commons.compress.archivers.ArchiveException;

/**
 * This class can be used to save the Reporter projects in a cps file.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProjectSaver {

    /**
     * Name for the reporter settings table.
     */
    public static final String REPORTER_SETTINGS_TABLE_NAME = "reporter_settings";

    /**
     * Saves the Reporter project information in the given database.
     *
     * @param reporterSettings the reporter settings
     * @param reporterIonQuantification the reporter ion quantification
     * @param displayPreferences the display preferences
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
    public static void saveProject(ReporterSettings reporterSettings, ReporterIonQuantification reporterIonQuantification, DisplayPreferences displayPreferences, CpsParent cpsParent,
            WaitingHandler waitingHandler) throws IOException, SQLException, ClassNotFoundException, InterruptedException, ArchiveException {

//        ObjectsDB objectsDB = cpsParent.getIdentification().getIdentificationDB().getObjectsDB();
//        if (!objectsDB.hasTable(REPORTER_SETTINGS_TABLE_NAME)) {
//            objectsDB.addTable(REPORTER_SETTINGS_TABLE_NAME);
//        }
//        if (objectsDB.inDB(REPORTER_SETTINGS_TABLE_NAME, ReporterSettings.class.getName(), false)) {
//            objectsDB.updateObject(REPORTER_SETTINGS_TABLE_NAME, ReporterSettings.class.getName(), reporterSettings, false);
//        } else {
//            objectsDB.insertObject(REPORTER_SETTINGS_TABLE_NAME, ReporterSettings.class.getName(), reporterSettings, false);
//        }
//        if (objectsDB.inDB(REPORTER_SETTINGS_TABLE_NAME, ReporterIonQuantification.class.getName(), false)) {
//            objectsDB.updateObject(REPORTER_SETTINGS_TABLE_NAME, ReporterIonQuantification.class.getName(), reporterIonQuantification, false);
//        } else {
//            objectsDB.insertObject(REPORTER_SETTINGS_TABLE_NAME, ReporterIonQuantification.class.getName(), reporterIonQuantification, false);
//        }
//        if (objectsDB.inDB(REPORTER_SETTINGS_TABLE_NAME, DisplayPreferences.class.getName(), false)) {
//            objectsDB.updateObject(REPORTER_SETTINGS_TABLE_NAME, DisplayPreferences.class.getName(), displayPreferences, false);
//        } else {
//            objectsDB.insertObject(REPORTER_SETTINGS_TABLE_NAME, DisplayPreferences.class.getName(), displayPreferences, false);
//        }

        cpsParent.saveProject(waitingHandler, false);
    }
}
