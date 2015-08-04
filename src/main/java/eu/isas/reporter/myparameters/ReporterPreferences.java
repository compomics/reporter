package eu.isas.reporter.myparameters;

import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.io.SerializationUtils;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import java.io.File;
import java.util.ArrayList;

/**
 * This class contains the quantification options set by the user.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterPreferences implements UrParameter {

    
    /**
     * Location of the user preferences file.
     */
    private static String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.reporter/reporter_user_preferences.cup";
    /**
     * The settings for the selection fo the  reporter ions in spectra
     */
    private ReporterIonSelectionSettings reporterIonSelectionSettings;
    /**
     * The ratio estimation settings
     */
    private RatioEstimationSettings ratioEstimationSettings;

    /**
     * Constructor.
     */
    private ReporterPreferences() {
    }


    /**
     * Convenience method saving the user preferences.
     *
     * @param userPreferences the new user preferences
     */
    public static void saveUserPreferences(ReporterPreferences userPreferences) {

        try {
            File file = new File(USER_PREFERENCES_FILE);
            if (!file.getParentFile().exists()) {
                file.getParentFile().mkdir();
            }
            SerializationUtils.writeObject(userPreferences, file);
        } catch (Exception e) {
            System.err.println("An error occurred while saving " + USER_PREFERENCES_FILE + " (see below).");
            e.printStackTrace();
        }
    }

    /**
     * Loads the quantification user preferences. If an error is encountered,
     * preferences are set back to default.
     *
     * @return returns the utilities user preferences
     */
    public static ReporterPreferences getUserPreferences() {
        ReporterPreferences userPreferences;
        File file = new File(USER_PREFERENCES_FILE);

        if (!file.exists()) {
            userPreferences = new ReporterPreferences();
            ReporterPreferences.saveUserPreferences(userPreferences);
        } else {
            try {
                userPreferences = (ReporterPreferences) SerializationUtils.readObject(file);
            } catch (Exception e) {
                System.err.println("An error occurred while loading " + USER_PREFERENCES_FILE + " (see below). Preferences set back to default.");
                e.printStackTrace();
                userPreferences = new ReporterPreferences();
                ReporterPreferences.saveUserPreferences(userPreferences);
            }
        }

        return userPreferences;
    }

    /**
     * Returns the reporter ion selection settings.
     * 
     * @return the reporter ion selection settings
     */
    public ReporterIonSelectionSettings getReporterIonSelectionSettings() {
        return reporterIonSelectionSettings;
    }

    /**
     * Sets the reporter ion selection settings.
     * 
     * @param reporterIonSelectionSettings the reporter ion selection settings
     */
    public void setReporterIonSelectionSettings(ReporterIonSelectionSettings reporterIonSelectionSettings) {
        this.reporterIonSelectionSettings = reporterIonSelectionSettings;
    }

    /**
     * Returns the ratio estimation settings.
     * 
     * @return the ratio estimation settings
     */
    public RatioEstimationSettings getRatioEstimationSettings() {
        return ratioEstimationSettings;
    }

    /**
     * Sets the ratio estimation settings.
     * 
     * @param ratioEstimationSettings the ratio estimation settings
     */
    public void setRatioEstimationSettings(RatioEstimationSettings ratioEstimationSettings) {
        this.ratioEstimationSettings = ratioEstimationSettings;
    }

    /**
     * Returns the file used to save the user preferences.
     * 
     * @return the file used to save the user preferences
     */
    public static String getUserPreferencesFile() {
        return USER_PREFERENCES_FILE;
    }

    /**
     * Returns the folder used to save the user preferences.
     * 
     * @return the folder used to save the user preferences
     */
    public static String getUserPreferencesFolder() {
        File tempFile = new File(getUserPreferencesFile());
        return tempFile.getParent();
    }

    /**
     * Sets the folder used to save the user preferences.
     * 
     * @param userPreferencesFile the folder used to save the user preferences
     */
    public static void setUserPreferencesFolder(String userPreferencesFile) {
        ReporterPreferences.USER_PREFERENCES_FILE = userPreferencesFile + "/reporter_user_preferences.cup";
    }

    @Override
    public String getFamilyName() {
        return "Reporter";
    }

    @Override
    public int getIndex() {
        return 0;
    }
}
