/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.reporter.export.report.export_features;

import com.compomics.util.io.export.ExportFeature;
import java.util.ArrayList;

/**
 * This enum lists all the peptide export features available from reporter complementarily to the ones available in PeptideShaker
 *
 * @author Marc
 */
public enum PeptideFeatures implements ExportFeature {
    
    raw_ratio("Raw Ratios", "The ratios of this peptide."),
    spread("Spread", "The spread of the PSM ratios of this peptide."),
    normalized_ratio("Normalized Ratios", "The normalized ratios of this peptide.");

    /**
     * The title of the feature which will be used for column heading.
     */
    public String title;
    /**
     * The description of the feature.
     */
    public String description;
    /**
     * The type of export feature.
     */
    public final static String type = "Peptide Reporter Quantification Summary";

    /**
     * Constructor.
     *
     * @param title title of the feature
     * @param description description of the feature
     */
    private PeptideFeatures(String title, String description) {
        this.title = title;
        this.description = description;
    }

    @Override
    public String getTitle(String separator) {
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
    public ArrayList<ExportFeature> getExportFeatures() {
        ArrayList<ExportFeature> result = eu.isas.peptideshaker.export.exportfeatures.PeptideFeatures.aaAfter.getExportFeatures();
        result.add(raw_ratio);
        result.add(spread);
        result.add(normalized_ratio);
        return result;
    }
}
