/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.reporter.export.report.export_features;

import com.compomics.util.io.export.ExportFeature;
import eu.isas.reporter.export.report.ReporterExportFeature;
import java.util.ArrayList;

/**
 * This enum lists all the peptide export features available from reporter complementarily to the ones available in PeptideShaker
 *
 * @author Marc
 */
public enum PeptideFeatures implements ReporterExportFeature {
    
    raw_ratio("Raw Ratios", "The ratios of this peptide.", true),
    spread("Spread", "The spread of the PSM ratios of this peptide.", true),
    normalized_ratio("Normalized Ratios", "The normalized ratios of this peptide.", true);

    /**
     * The title of the feature which will be used for column heading.
     */
    public String title;
    /**
     * The description of the feature.
     */
    public String description;
    /**
     * Indicates whether the feature is channel dependent
     */
    private boolean hasChannels;
    /**
     * The type of export feature.
     */
    public final static String type = "Peptide Reporter Quantification Summary";

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     * @param hasChannels indicates whether the feature is channel dependent
     */
    private PeptideFeatures(String title, String description, boolean hasChannels) {
        this.title = title;
        this.description = description;
        this.hasChannels = hasChannels;
    }

    @Override
    public String[] getTitles() {
        return new String[]{title};
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
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures.aaAfter.getExportFeatures();
        result.add(raw_ratio);
        result.add(spread);
        result.add(normalized_ratio);
        return result;
    }

    @Override
    public boolean hasChannels() {
        return hasChannels;
    }
}
