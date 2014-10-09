package eu.isas.reporter.export.report.export_features;

import com.compomics.util.io.export.ExportFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPeptideFeature;
import eu.isas.reporter.export.report.ReporterExportFeature;
import java.util.ArrayList;

/**
 * This enum lists all the peptide export features available from reporter
 * complementarily to the ones available in PeptideShaker.
 *
 * @author Marc Vaudel
 */
public enum ReporterPeptideFeature implements ReporterExportFeature {

    raw_ratio("Raw Ratios", "The ratios of this peptide.", true, true),
    spread("Spread", "The spread of the PSM ratios of this peptide.", true, false),
    normalized_ratio("Normalized Ratios", "The normalized ratios of this peptide.", true, false);

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
    public final static String type = "Peptide Reporter Quantification Summary";
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
    private ReporterPeptideFeature(String title, String description, boolean hasChannels, boolean advanced) {
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
        ArrayList<ExportFeature> result = PsPeptideFeature.values()[0].getExportFeatures(includeSubFeatures);
        result.add(raw_ratio);
        result.add(spread);
        result.add(normalized_ratio);
        if (includeSubFeatures) {
            result.addAll(ReporterPsmFeatures.values()[0].getExportFeatures(includeSubFeatures));
        }
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
