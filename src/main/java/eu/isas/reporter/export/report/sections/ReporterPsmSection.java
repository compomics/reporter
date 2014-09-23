package eu.isas.reporter.export.report.sections;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.PeptideAssumption;
import com.compomics.util.experiment.identification.SearchParameters;
import com.compomics.util.experiment.identification.TagAssumption;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.io.export.ExportFeature;
import com.compomics.util.preferences.AnnotationPreferences;
import com.compomics.util.preferences.SequenceMatchingPreferences;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.export.exportfeatures.PsFragmentFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsIdentificationAlgorithmMatchesFeature;
import eu.isas.peptideshaker.export.exportfeatures.PsPsmFeature;
import eu.isas.peptideshaker.export.sections.PsFragmentSection;
import eu.isas.peptideshaker.export.sections.PsIdentificationAlgorithmMatchesSection;
import eu.isas.peptideshaker.export.sections.PsPsmSection;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.export.report.ReporterExportFeature;
import eu.isas.reporter.export.report.export_features.ReporterPsmFeatures;
import eu.isas.reporter.myparameters.ReporterPreferences;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import eu.isas.reporter.quantificationdetails.SpectrumQuantificationDetails;
import java.io.BufferedWriter;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * This class outputs the PSM level quantification export features.
 *
 * @author Marc Vaudel
 */
public class ReporterPsmSection {

    /**
     * The features to export.
     */
    private ArrayList<PsPsmFeature> psmFeatures = new ArrayList<PsPsmFeature>();
    /**
     * The features to export.
     */
    private ArrayList<PsIdentificationAlgorithmMatchesFeature> identificationAlgorithmMatchesFeatures = new ArrayList<PsIdentificationAlgorithmMatchesFeature>();
    /**
     * The quantification features to export.
     */
    private ArrayList<ReporterExportFeature> quantificationFeatures = new ArrayList<ReporterExportFeature>();
    /**
     * The fragment subsection if needed.
     */
    private PsFragmentSection fragmentSection = null;
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
    public ReporterPsmSection(ArrayList<ExportFeature> exportFeatures, String separator, boolean indexes, boolean header, BufferedWriter writer) {
        ArrayList<ExportFeature> fragmentFeatures = new ArrayList<ExportFeature>();
        for (ExportFeature exportFeature : exportFeatures) {
            if (exportFeature instanceof ReporterPsmFeatures) {
                quantificationFeatures.add((ReporterExportFeature) exportFeature);
            } else if (exportFeature instanceof PsPsmFeature) {
                psmFeatures.add((PsPsmFeature) exportFeature);
            } else if (exportFeature instanceof PsIdentificationAlgorithmMatchesFeature) {
                identificationAlgorithmMatchesFeatures.add((PsIdentificationAlgorithmMatchesFeature) exportFeature);
            } else if (exportFeature instanceof PsFragmentFeature) {
                fragmentFeatures.add(exportFeature);
            } else {
                throw new IllegalArgumentException("Export feature of type " + exportFeature.getClass() + " not recognized.");
            }
        }
        if (!fragmentFeatures.isEmpty()) {
            fragmentSection = new PsFragmentSection(fragmentFeatures, separator, indexes, header, writer);
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
     * @param reporterPreferences the reporter preferences
     * @param searchParameters the search parameters of the project
     * @param annotationPreferences the annotation preferences
     * @param sequenceMatchingPreferences the sequence matching preferences
     * @param keys the keys of the PSM matches to output
     * @param linePrefix the line prefix
     * @param validatedOnly whether only validated matches should be exported
     * @param decoys whether decoy matches should be exported as well
     * @param waitingHandler the waiting handler
     * @throws IOException exception thrown whenever an error occurred while
     * writing the file.
     * @throws IllegalArgumentException
     * @throws SQLException
     * @throws ClassNotFoundException
     * @throws InterruptedException
     * @throws MzMLUnmarshallerException
     */
    public void writeSection(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator, QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, ReporterPreferences reporterPreferences,
            SearchParameters searchParameters, AnnotationPreferences annotationPreferences, SequenceMatchingPreferences sequenceMatchingPreferences, ArrayList<String> keys, String linePrefix, boolean validatedOnly, boolean decoys, WaitingHandler waitingHandler) throws IOException, IllegalArgumentException, SQLException,
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

                psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                if (!validatedOnly || psParameter.getMatchValidationLevel().isValidated()) {

                    spectrumMatch = identification.getSpectrumMatch(spectrumKey);

                    PeptideAssumption peptideAssumption = spectrumMatch.getBestPeptideAssumption();

                    if (decoys || peptideAssumption == null || !peptideAssumption.getPeptide().isDecoy(sequenceMatchingPreferences)) {

                        boolean first = true;

                        if (indexes) {
                            if (linePrefix != null) {
                                writer.write(linePrefix);
                            }
                            writer.write(line + "");
                            first = false;
                        }
                        for (PsIdentificationAlgorithmMatchesFeature identificationAlgorithmMatchesFeature : identificationAlgorithmMatchesFeatures) {
                            if (!first) {
                                writer.write(separator);
                            } else {
                                first = false;
                            }
                            String feature;
                            if (peptideAssumption != null) {
                                peptideAssumption = spectrumMatch.getBestPeptideAssumption();
                                feature = PsIdentificationAlgorithmMatchesSection.getPeptideAssumptionFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, keys, linePrefix, separator, peptideAssumption, spectrumMatch.getKey(), psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                            } else if (spectrumMatch.getBestTagAssumption() != null) {
                                TagAssumption tagAssumption = spectrumMatch.getBestTagAssumption();
                                feature = PsIdentificationAlgorithmMatchesSection.getTagAssumptionFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, keys, linePrefix, separator, tagAssumption, spectrumMatch.getKey(), psParameter, identificationAlgorithmMatchesFeature, waitingHandler);
                            } else {
                                throw new IllegalArgumentException("No best match found for spectrum " + spectrumMatch.getKey() + ".");
                            }
                            writer.write(feature);
                        }
                        for (PsPsmFeature psmFeature : psmFeatures) {
                            if (!first) {
                                writer.write(separator);
                            } else {
                                first = false;
                            }
                            writer.write(PsPsmSection.getFeature(identification, identificationFeaturesGenerator, searchParameters, annotationPreferences, sequenceMatchingPreferences, keys, linePrefix, separator, spectrumMatch, psParameter, psmFeature, validatedOnly, decoys, waitingHandler));
                        }
                        ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
                        Collections.sort(sampleIndexes);
                        for (ExportFeature exportFeature : quantificationFeatures) {
                            if (!first) {
                                writer.write(separator);
                            } else {
                                first = false;
                            }
                            ReporterPsmFeatures psmFeature = (ReporterPsmFeatures) exportFeature;
                            if (psmFeature.hasChannels()) {
                                for (String sampleIndex : sampleIndexes) {
                                    if (!first) {
                                        writer.write(separator);
                                    } else {
                                        first = false;
                                    }
                                    writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, reporterPreferences, spectrumKey, psmFeature, sampleIndex));
                                }
                            } else {
                                if (!first) {
                                    writer.write(separator);
                                } else {
                                    first = false;
                                }
                                writer.write(getFeature(quantificationFeaturesGenerator, reporterIonQuantification, reporterPreferences, spectrumKey, psmFeature, ""));
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
     * @param reporterPreferences the reporter preferences
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
    public static String getFeature(QuantificationFeaturesGenerator quantificationFeaturesGenerator, ReporterIonQuantification reporterIonQuantification, ReporterPreferences reporterPreferences, String spectrumKey, ReporterPsmFeatures psmFeatures, String sampleIndex) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {
        switch (psmFeatures) {
            case ratio:
                PsmQuantificationDetails psmDetails = quantificationFeaturesGenerator.getPSMQuantificationDetails(spectrumKey);
                return psmDetails.getRatio(sampleIndex).toString();
            case reporter_intensity:
                SpectrumQuantificationDetails spectrumDetails = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(reporterIonQuantification, reporterPreferences, spectrumKey);
                IonMatch ionMatch = spectrumDetails.getRepoterMatch(sampleIndex);
                if (ionMatch == null) {
                    return "";
                }
                return ionMatch.peak.intensity + "";
            case reporter_mz:
                spectrumDetails = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(reporterIonQuantification, reporterPreferences, spectrumKey);
                ionMatch = spectrumDetails.getRepoterMatch(sampleIndex);
                if (ionMatch == null) {
                    return "";
                }
                return ionMatch.peak.mz + "";
            case deisotoped_intensity:
                spectrumDetails = quantificationFeaturesGenerator.getSpectrumQuantificationDetails(reporterIonQuantification, reporterPreferences, spectrumKey);
                return spectrumDetails.getDeisotopedIntensity(sampleIndex).toString();
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
        ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

        boolean firstColumn = true;
        if (indexes) {
            writer.write(separator);
        }
        for (ExportFeature exportFeature : identificationAlgorithmMatchesFeatures) {
            for (String title : exportFeature.getTitles()) {
                if (firstColumn) {
                    firstColumn = false;
                } else {
                    writer.write(separator);
                }
                writer.write(title);
            }
        }
        for (ExportFeature exportFeature : psmFeatures) {
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
            for (ExportFeature exportFeature : identificationAlgorithmMatchesFeatures) {
                for (String title : exportFeature.getTitles()) {
                    if (firstColumn) {
                        firstColumn = false;
                    } else {
                        writer.write(separator);
                    }
                }
            }
            for (ExportFeature exportFeature : psmFeatures) {
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
                        for (String sampleIndex : sampleIndexes) {
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
