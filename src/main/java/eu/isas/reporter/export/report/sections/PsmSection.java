package eu.isas.reporter.export.report.sections;

import com.compomics.util.experiment.biology.PTM;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.PeptideShaker;
import eu.isas.peptideshaker.export.sections.FragmentSection;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.myparameters.PSPtmScores;
import eu.isas.peptideshaker.scoring.PtmScoring;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.ReporterExportFeature;
import eu.isas.reporter.export.report.export_features.ProteinFeatures;
import eu.isas.reporter.export.report.export_features.PsmFeatures;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Set;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the PSM level quantification export features.
 *
 * @author Marc Vaudel
 */
public class PsmSection {

    /**
     * The identification features to export.
     */
    private ArrayList<ExportFeature> identificationFeatures = new ArrayList<ExportFeature>();

    /**
     * The quantification features to export.
     */
    private ArrayList<ReporterExportFeature> quantificationFeatures = new ArrayList<ReporterExportFeature>();
    /**
     * The fragment subsection if needed.
     */
    private FragmentSection fragmentSection = null;
    /**
     * The separator used to separate columns.
     */
    private String separator;
    /**
     * Boolean indicating whether the line shall be indexed.
     */
    private boolean indexes;
    /**
     * Boolean indicating whether column headers shall be included.
     */
    private boolean header;
    /**
     * The writer used to send the output to file.
     */
    private BufferedWriter writer;

    /**
     * Constructor.
     *
     * @param exportFeatures the features to export in this section
     * @param separator
     * @param indexes
     * @param header
     * @param writer
     */
    public PsmSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof PsmFeatures) {
                quantificationFeatures.add((ReporterExportFeature) exportFeature);
            } else if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.PsmFeatures) {
                identificationFeatures.add(exportFeature);
            } else if (exportFeature instanceof eu.isas.peptideshaker.export.exportfeatures.FragmentFeatures) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        if (!fragmentFeatures.isEmpty()) {
            fragmentSection = new FragmentSection(fragmentFeatures, separator, indexes, header, writer);
        }
        this.separator = separator;
        this.indexes = indexes;
        this.header = header;
        this.writer = writer;
    }

    /**
     * Writes the desired section.
     *
     * @param identification the identification of the project
     * @param identificationFeaturesGenerator the identification features
     * generator of the project
     * @param quantificationFeaturesGenerator the quantification features
     * generator containing the quantification information
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, ArrayList<String> keys, String linePrefix, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
            ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        if (waitingHandler != null) {
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        }

        if (header) {
            writeHeader(reporterIonQuantification);
        }

        HashMap<String, ArrayList<String>> psmMap = new HashMap<String, ArrayList<String>>();

        if (keys == null) {
            psmMap = identification.getSpectrumIdentificationMap();
        } else {
            for (String key : keys) {
                String fileName = Spectrum.getSpectrumFile(key);
                if (!psmMap.containsKey(fileName)) {
                    psmMap.put(fileName, new ArrayList<String>());
                }
                psmMap.get(fileName).add(key);
            }
        }

        PSParameter psParameter = new PSParameter();
        SpectrumMatch spectrumMatch = null;
        int line = 1;

        int totalSize = 0;

        for (String spectrumFile : psmMap.keySet()) {
            totalSize += psmMap.get(spectrumFile).size();
        }

        // get the spectrum keys
        ArrayList<String> spectrumKeys = new ArrayList<String>();

        for (String spectrumFile : psmMap.keySet()) {
            for (String spectrumKey : psmMap.get(spectrumFile)) {
                if (!spectrumKeys.contains(spectrumKey)) {
                    spectrumKeys.add(spectrumKey);
                }
            }
        }

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Spectra. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadSpectrumMatches(spectrumKeys, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Loading Spectrum Details. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
        }
        identification.loadSpectrumMatchParameters(spectrumKeys, psParameter, waitingHandler);

        if (waitingHandler != null) {
            waitingHandler.setWaitingText("Exporting. Please Wait...");
            waitingHandler.resetSecondaryProgressCounter();
            waitingHandler.setMaxSecondaryProgressCounter(totalSize);
        }

        for (String spectrumFile : psmMap.keySet()) {

            for (String spectrumKey : psmMap.get(spectrumFile)) {

                if (waitingHandler != null) {
                    if (waitingHandler.isRunCanceled()) {
                        return;
                    }
                    waitingHandler.increaseSecondaryProgressCounter();
                }

                if (indexes) {
                    if (linePrefix != null) {
                        writer.write(linePrefix);
                    }
                    writer.write(line + separator);
                }

                spectrumMatch = identification.getSpectrumMatch(spectrumKey);
                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                for (ExportFeature exportFeature : identificationFeatures) {
                    eu.isas.peptideshaker.export.exportfeatures.PsmFeatures psmFeature = (eu.isas.peptideshaker.export.exportfeatures.PsmFeatures) exportFeature;
                    writer.write(eu.isas.peptideshaker.export.sections.PsmSection.getFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, linePrefix, separator, spectrumMatch, psParameter, psmFeature, waitingHandler) + separator);
                }
            ArrayList<Integer> sampleIndexes = new ArrayList<Integer>(reporterIonQuantification.getSampleIndexes());
            Collections.sort(sampleIndexes);
                for (ExportFeature exportFeature : quantificationFeatures) {
                    PsmFeatures psmFeature = (PsmFeatures) exportFeature;
                if (psmFeature.hasChannels()) {
                    for (int sampleIndex : sampleIndexes) {
                        writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, spectrumKey, psmFeature, sampleIndex)+ separator);
                    }
                } else {
                    writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, spectrumKey, psmFeature, -1) + separator);
                }

                }
                writer.newLine();
                if (fragmentSection != null) {
                    String fractionPrefix = "";
                    if (linePrefix != null) {
                        fractionPrefix += linePrefix;
                    }
                    fractionPrefix += line + ".";
                    fragmentSection.writeSection(spectrumMatch, searchParameters, annotationPreferences, fractionPrefix, null);
                }
                line++;
            }
        }
    }

    /**
     * Returns the report component corresponding to a feature at a given
     * channel.
     *
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     * @param spectrumKey the spectrum key
     * @param psmFeatures the PSM feature to export
     * @param sampleIndex the index of the sample in case the feature is channel
     * dependent, ignored otherwise
     *
     * @return the report component corresponding to a feature at a given
     * channel
     *
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public static String getFeature(QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, String spectrumKey, PsmFeatures psmFeatures, int sampleIndex) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (psmFeatures) {
            case ratio:
                PsmQuantificationDetails psmDetails = quantificationFeaturesGenerator.getPSMQuantificationDetails(spectrumKey);
                return psmDetails.getRatio(sampleIndex).toString();
            default:
                return "Not implemented";
        }
    }

    /**
     * Writes the header of this section.
     *
     * @param reporterIonQuantification the reporter ion quantification object
     * containing the quantification configuration
     *
     * @throws IOException
     */
    public void writeHeader(ReporterIonQuantification reporterIonQuantification) throws IOException {

        boolean needSecondLine = false;
        ArrayList<Integer> sampleIndexes = new ArrayList<Integer>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

        boolean firstColumn = true;
        if (indexes) {
            writer.write(separator);
        }
        for (ExportFeature exportFeature : identificationFeatures) {
            for (String title : exportFeature.getTitles()) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(title);
            }
        }
        for (ReporterExportFeature exportFeature : quantificationFeatures) {
            if (firstColumn) {
                firstColumn = false;
            } else {
                writer.write(separator);
            }
            for (String title : exportFeature.getTitles()) {
                writer.write(title);
                if (exportFeature.hasChannels()) {
                    for (int i = 1; i < sampleIndexes.size(); i++) {
                        writer.write(separator);
                    }
                    needSecondLine = true;
                }
            }
        }
        if (needSecondLine) {
            writer.newLine();
            firstColumn = true;
        if (indexes) {
            writer.write(separator);
        }
            for (ExportFeature exportFeature : identificationFeatures) {
                for (String title : exportFeature.getTitles()) {
                    if (firstColumn) {
                        firstColumn = false;
                    } else {
                        writer.write(separator);
                    }
                }
            }
            for (ReporterExportFeature exportFeature : quantificationFeatures) {
                for (String title : exportFeature.getTitles()) {
                    if (exportFeature.hasChannels()) {
                        for (int sampleIndex : sampleIndexes) {
                            if (firstColumn) {
                                firstColumn = false;
                            } else {
                                writer.write(separator);
                            }
                            writer.write(reporterIonQuantification.getSample(sampleIndex).getReference());
                        }
                    } else {
                        if (firstColumn) {
                            firstColumn = false;
                        } else {
                            writer.write(separator);
                        }
                    }
                }
            }
        }
        writer.newLine();
    }
}
