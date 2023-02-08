package eu.isas.reporter.gui.tablemodels;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import java.util.TreeMap;
import java.util.stream.Collectors;
import no.uib.jsparklines.data.ArrrayListDataPoints;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;

/**
 * Model for the peptide table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PeptideTableModel extends SelfUpdatingTableModel {

    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The spectrum provider.
     */
    private SpectrumProvider spectrumProvider;
    /**
     * The identification features generator.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The display features generator.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The identification parameters.
     */
    private IdentificationParameters identificationParameters;
    /**
     * The quantification feature generator.
     */
    private QuantificationFeaturesGenerator quantificationFeaturesGenerator;
    /**
     * The reporter ion quantification.
     */
    private ReporterIonQuantification reporterIonQuantification;
    /**
     * The sample indexes.
     */
    private ArrayList<String> sampleIndexes;
    /**
     * A list of ordered peptide keys.
     */
    private long[] peptideKeys = null;
    /**
     * The main accession of the protein match to which the list of peptides is
     * attached.
     */
    private String proteinAccession;
    /**
     * Indicates whether the scores should be displayed instead of the
     * confidence
     */
    private boolean showScores = false;
    /**
     * The batch size.
     */
    private int batchSize = 20;
    /**
     * The exception handler catches exceptions.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The display preferences.
     */
    private DisplayPreferences displayPreferences;

    /**
     * Constructor which sets a new table.
     *
     * @param identification the identification object containing the matches
     * @param spectrumProvider the spectrum provider
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param reporterIonQuantification the reporter quantification information
     * @param quantificationFeaturesGenerator the quantification feature
     * generator
     * @param displayFeaturesGenerator the display features generator
     * @param displayPreferences the display preferences
     * @param identificationParameters the identification parameters
     * @param proteinAccession the protein accession
     * @param peptideKeys the peptide keys
     * @param displayScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     * @param exceptionHandler handler for the exceptions
     */
    public PeptideTableModel(
            Identification identification,
            SpectrumProvider spectrumProvider,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ReporterIonQuantification reporterIonQuantification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            DisplayFeaturesGenerator displayFeaturesGenerator,
            DisplayPreferences displayPreferences,
            IdentificationParameters identificationParameters,
            String proteinAccession,
            long[] peptideKeys,
            boolean displayScores,
            ExceptionHandler exceptionHandler
    ) {

        this.identification = identification;
        this.spectrumProvider = spectrumProvider;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.reporterIonQuantification = reporterIonQuantification;
        this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.displayPreferences = displayPreferences;
        this.identificationParameters = identificationParameters;
        this.peptideKeys = peptideKeys;
        this.proteinAccession = proteinAccession;
        this.showScores = displayScores;
        this.exceptionHandler = exceptionHandler;

        sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param identification the identification object containing the matches
     * @param identificationFeaturesGenerator the identification features
     * generator
     * @param reporterIonQuantification the reporter quantification information
     * @param quantificationFeaturesGenerator the quantification feature
     * generator
     * @param displayFeaturesGenerator the display features generator
     * @param displayPreferences the display preferences
     * @param identificationParameters the identification parameters
     * @param proteinAccession the protein accession
     * @param peptideKeys the peptide keys
     * @param showScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     */
    public void updateDataModel(
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ReporterIonQuantification reporterIonQuantification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            DisplayFeaturesGenerator displayFeaturesGenerator,
            DisplayPreferences displayPreferences,
            IdentificationParameters identificationParameters,
            String proteinAccession,
            long[] peptideKeys,
            boolean showScores
    ) {

        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.reporterIonQuantification = reporterIonQuantification;
        this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.displayPreferences = displayPreferences;
        this.identificationParameters = identificationParameters;
        this.proteinAccession = proteinAccession;
        this.peptideKeys = peptideKeys;
        this.showScores = showScores;

        sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

    }

    /**
     * Resets the peptide keys.
     */
    public void reset() {
        peptideKeys = null;
    }

    /**
     * Constructor which sets a new empty table.
     */
    public PeptideTableModel() {
    }

    @Override
    public int getRowCount() {
        return peptideKeys == null ? 0 : peptideKeys.length;
    }

    @Override
    public int getColumnCount() {
        return 8;
    }

    @Override
    public String getColumnName(int column) {

        switch (column) {
            case 0:
                return " ";
            case 1:
                return "Quant";
            case 2:
                return "PI";
            case 3:
                return "Accession";
            case 4:
                return "Sequence";
            case 5:
                return "#Spectra";
            case 6:
                if (showScores) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 7:
                return "";
            default:
                return "";
        }

    }

    @Override
    public Object getValueAt(int row, int column) {

        int viewIndex = getViewIndex(row);

        if (viewIndex < peptideKeys.length) {

            if (column == 0) {
                return viewIndex + 1;
            }

//            if (isScrolling) {
//                return null;
//            }
//
//            if (!isSelfUpdating()) {
//                dataMissingAtRow(row);
//                return DisplayParameters.LOADING_MESSAGE;
//            }
            long peptideKey = peptideKeys[viewIndex];
            PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey);

            switch (column) {

                case 1:

                    ArrayList<Double> data = new ArrayList<>();
                    PeptideQuantificationDetails quantificationDetails
                            = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(
                                    spectrumProvider,
                                    peptideMatch,
                                    null
                            );

                    ArrayList<String> reagentsOrder = displayPreferences.getReagents();

                    for (String tempReagent : reagentsOrder) {

                        int sampleIndex = sampleIndexes.indexOf(tempReagent);
                        Double ratio = quantificationDetails.getRatio(
                                sampleIndexes.get(sampleIndex),
                                reporterIonQuantification.getNormalizationFactors()
                        );

                        if (ratio != null) {

                            if (ratio != 0) {
                                ratio = BasicMathFunctions.log(ratio, 2);
                            }

                            data.add(ratio);
                        }

                    }

                    return new JSparklinesDataSeries(data, Color.BLACK, null);

                case 2:

                    PSParameter psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    return psParameter.getProteinInferenceGroupClass();

                case 3:

                    TreeMap<String, int[]> proteinMapping = peptideMatch.getPeptide().getProteinMapping();
                    return proteinMapping.navigableKeySet().stream().collect(Collectors.joining(","));

                case 4:

                    return displayFeaturesGenerator.getTaggedPeptideSequence(peptideMatch, true, true, true);

                case 5:

                    double nConfidentSpectra = identificationFeaturesGenerator.getNConfidentSpectraForPeptide(peptideKey);
                    double nDoubtfulSpectra = identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideKey) - nConfidentSpectra;
                    int nSpectra = peptideMatch.getSpectrumMatchesKeys().length;

                    ArrayList<Double> doubleValues = new ArrayList<>(3);
                    doubleValues.add(nConfidentSpectra);
                    doubleValues.add(nDoubtfulSpectra);
                    doubleValues.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                    ArrrayListDataPoints arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);

                    return arrrayListDataPoints;

                case 6:

                    psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    return showScores ? psParameter.getTransformedScore() : psParameter.getConfidence();

                case 7:

                    psParameter = (PSParameter) peptideMatch.getUrParam(PSParameter.dummy);
                    return psParameter.getMatchValidationLevel().getIndex();

                default:

                    return null;

            }
        }

        return null;
    }

    /**
     * Indicates whether the table content was instantiated.
     *
     * @return a boolean indicating whether the table content was instantiated.
     */
    public boolean isInstantiated() {
        return identification != null;
    }

    @Override
    public Class getColumnClass(int columnIndex) {

        for (int i = 0; i < getRowCount(); i++) {

            if (getValueAt(i, columnIndex) != null) {
                return getValueAt(i, columnIndex).getClass();

            }
        }

        return String.class;

    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return false;
    }

    @Override
    protected void catchException(Exception e) {

        setSelfUpdating(false);
        exceptionHandler.catchException(e);

    }

    @Override
    protected int loadDataForRows(ArrayList<Integer> rows, WaitingHandler waitingHandler) {

        boolean canceled = rows.parallelStream()
                .map(i -> identification.getPeptideMatch(peptideKeys[i]))
                .map(peptideMatch -> identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideMatch.getKey()))
                .anyMatch(dummy -> waitingHandler.isRunCanceled());

        return canceled ? rows.get(0) : rows.get(rows.size() - 1);

    }
}
