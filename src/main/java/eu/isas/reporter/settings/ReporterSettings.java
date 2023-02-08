package eu.isas.reporter.settings;

import com.compomics.util.experiment.identification.spectrum_annotation.AnnotationParameters;
import eu.isas.reporter.preferences.ProjectDetails;
import java.io.Serializable;

/**
 * This class contains the reporter settings for this project.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterSettings implements Serializable {

    /**
     * The annotation parameters.
     */
    private AnnotationParameters annotationParameters;
    /**
     * The project details.
     */
    private ProjectDetails projectDetails;
    /**
     * The settings for the selection of the reporter ions in spectra.
     */
    private ReporterIonSelectionSettings reporterIonSelectionSettings = new ReporterIonSelectionSettings();
    /**
     * The ratio estimation settings.
     */
    private RatioEstimationSettings ratioEstimationSettings = new RatioEstimationSettings();
    /**
     * The normalization settings.
     */
    private NormalizationSettings normalizationSettings = new NormalizationSettings();

    /**
     * Constructor.
     */
    public ReporterSettings() {
    }

    @Override
    public ReporterSettings clone() throws CloneNotSupportedException {
        
        ReporterSettings clone = new ReporterSettings();
        
        clone.setReporterIonSelectionSettings(reporterIonSelectionSettings.clone());
        clone.setRatioEstimationSettings(ratioEstimationSettings.clone());
        clone.setNormalizationSettings(normalizationSettings.clone());
        clone.setAnnotationParameters(annotationParameters.clone());
        clone.setProjectDetails(projectDetails); //@TODO add cloning of the project details after implementation of the project details
        
        return clone;
    }

    /**
     * Indicates whether another setting is the same as this one.
     *
     * @param anotherSetting another setting
     *
     * @return a boolean indicating whether another setting is the same as this
     * one
     */
    public boolean isSameAs(ReporterSettings anotherSetting) {
        
        return reporterIonSelectionSettings.isSameAs(anotherSetting.getReporterIonSelectionSettings())
                && ratioEstimationSettings.isSameAs(anotherSetting.getRatioEstimationSettings())
                && normalizationSettings.isSameAs(anotherSetting.getNormalizationSettings()); //@TODO: add annotation preferences and project details
        
    }

    /**
     * Returns the spectrum annotation parameters.
     *
     * @return the spectrum annotation parameters
     */
    public AnnotationParameters getAnnotationParameters() {
        return annotationParameters;
    }

    /**
     * Sets the spectrum annotation parameters.
     *
     * @param annotationParameters the spectrum annotation parameters
     */
    public void setAnnotationParameters(AnnotationParameters annotationParameters) {
        this.annotationParameters = annotationParameters;
    }

    /**
     * Returns the project details.
     *
     * @return the project details
     */
    public ProjectDetails getProjectDetails() {
        return projectDetails;
    }

    /**
     * Sets the project details.
     *
     * @param projectDetails the project details
     */
    public void setProjectDetails(ProjectDetails projectDetails) {
        this.projectDetails = projectDetails;
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
     * Returns the normalization settings.
     *
     * @return the normalization settings
     */
    public NormalizationSettings getNormalizationSettings() {
        return normalizationSettings;
    }

    /**
     * Sets the normalization settings.
     *
     * @param normalizationSettings the normalization settings
     */
    public void setNormalizationSettings(NormalizationSettings normalizationSettings) {
        this.normalizationSettings = normalizationSettings;
    }
}
