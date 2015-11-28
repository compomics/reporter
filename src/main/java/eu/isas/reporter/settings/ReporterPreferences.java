package eu.isas.reporter.settings;

import com.compomics.util.io.SerializationUtils;
import java.io.File;
import java.io.Serializable;

/**
 * This class contains the quantification options set by the user.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterPreferences implements Serializable {

    /**
     * Location of the user preferences file.
     */
    private static String USER_PREFERENCES_FILE = System.getProperty("user.home") + "/.reporter/reporter_user_preferences.cup";
    
    /**
     * The default reporter settings.
     */
    private ReporterSettings defaultSettings = null;

    /**
     * Constructor. Creates new preferences set to default.
     */
    private ReporterPreferences() {
    }

    @Override
    public ReporterPreferences clone() throws CloneNotSupportedException {
        ReporterPreferences clone =  new ReporterPreferences();
        clone.setDefaultSettings(defaultSettings.clone());
        return clone;
    }
    
    /**
     * Indicates whether another setting is the same as this one.
     * 
     * @param anotherSetting another setting
     * 
     * @return a boolean indicating whether another setting is the same as this one
     */
    public boolean isSameAs(ReporterPreferences anotherSetting) {
        return defaultSettings.isSameAs(anotherSetting.getDefaultSettings());
    }

    /**
     * Returns the default settings.
     * 
     * @return the default settings
     */
    public ReporterSettings getDefaultSettings() {
        if (defaultSettings == null) {
            defaultSettings = new ReporterSettings();
        }
        return defaultSettings;
    }

    /**
     * Sets the default settings.
     * 
     * @param defaultSettings the default settings
     */
    public void setDefaultSettings(ReporterSettings defaultSettings) {
        this.defaultSettings = defaultSettings;
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
}
