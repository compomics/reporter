package eu.isas.reporter.export.report.export_features;

import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.reporter.export.report.ReporterExportFeature;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * This enum lists all the PSM export features available from reporter
 * complementarily to the ones available in PeptideShaker.
 *
 * @author Marc Vaudel
 */
public enum ReporterPsmFeatures implements ReporterExportFeature {

    reporter_mz("Reporter m/z", "The reporter ions m/z as extracted from the spectrum.", true, true),
    reporter_intensity("Reporter Intensity", "The reporter ions instensities as extracted from the spectrum.", true, true),
    deisotoped_intensity("Deisotoped Intensity", "The instensities after deisotoping.", true, true),
    raw_ratio("Raw Ratios", "The ratios of this PSM prior to normalization.", true, false),
    ratio("Ratios", "The normalized ratios of this PSM.", true, false);

    /**
     * The title of the feature which will be used for column heading.
     */
    public String title;
    /**
     * The description of the feature.
     */
    public String description;
    /**
     * Indicates whether the feature is channel dependent.
     */
    private boolean hasChannels;
    /**
     * The type of export feature.
     */
    public final static String type = "PSM Reporter Quantification Summary";
    /**
     * Indicates whether a feature is for advanced user only.
     */
    private final boolean advanced;

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     * @param hasChannels indicates whether the feature is channel dependent
     * @param advanced indicates whether a feature is for advanced user only
     */
    private ReporterPsmFeatures(String title, String description, boolean hasChannels, boolean advanced) {
        this.title = title;
        this.description = description;
        this.hasChannels = hasChannels;
        this.advanced = advanced;
    }

    @Override
    public String getTitle() {
        return title;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public String getFeatureFamily() {
        return type;
    }

    @Override
    public ArrayList<ExportFeature> getExportFeatures(boolean includeSubFeatures) {
        ArrayList<ExportFeature> result = PsPsmFeature.values()[0].getExportFeatures(includeSubFeatures);
        result.addAll(Arrays.asList(values()));
        return result;
    }

    @Override
    public boolean hasChannels() {
        return hasChannels;
    }

    @Override
    public boolean isAdvanced() {
        return advanced;
    }
}
