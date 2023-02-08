package eu.isas.reporter.gui.tablemodels;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.mass_spectrometry.spectra.Precursor;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.parameters.identification.search.SearchParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.gui.tabpanels.SpectrumIdentificationPanel;
import eu.isas.peptideshaker.scoring.PSMaps;
import eu.isas.peptideshaker.scoring.maps.InputMap;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Collections;
import no.uib.jsparklines.data.JSparklinesDataSeries;

/**
 * Model for the PSM table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class PsmTableModel extends SelfUpdatingTableModel {

    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * The spectrum provider.
     */
    private SpectrumProvider spectrumProvider;
    /**
     * The display features generator.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
    /**
     * The ID input map.
     */
    private InputMap inputMap;
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
     * A list of ordered PSM keys.
     */
    private long[] psmKeys = null;
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
     * Constructor which sets a new empty table.
     */
    public PsmTableModel() {
    }

    /**
     * Constructor which sets a new table.
     *
     * @param identification the identification object containing the matches
     * @param spectrumProvider the spectrum provider
     * @param displayFeaturesGenerator the display features generator
     * @param displayPreferences the display preferences
     * @param reporterIonQuantification the reporter quantification information
     * @param quantificationFeaturesGenerator the quantification feature
     * @param identificationParameters the identification parameters
     * @param psmKeys the PSM keys
     * @param displayScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     * @param exceptionHandler handler for the exceptions
     */
    public PsmTableModel(
            Identification identification,
            SpectrumProvider spectrumProvider,
            DisplayFeaturesGenerator displayFeaturesGenerator,
            DisplayPreferences displayPreferences,
            IdentificationParameters identificationParameters,
            ReporterIonQuantification reporterIonQuantification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            long[] psmKeys,
            boolean displayScores,
            ExceptionHandler exceptionHandler
    ) {

        this.identification = identification;
        this.spectrumProvider = spectrumProvider;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.displayPreferences = displayPreferences;
        this.reporterIonQuantification = reporterIonQuantification;
        this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
        this.identificationParameters = identificationParameters;
        this.psmKeys = psmKeys;
        this.showScores = displayScores;
        this.exceptionHandler = exceptionHandler;

        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) identification.getUrParam(pSMaps);
        inputMap = pSMaps.getInputMap();

        sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model. This keeps the sorting order of the table.
     *
     * @param identification the identification object containing the matches
     * @param spectrumProvider the spectrum provider
     * @param displayFeaturesGenerator the display features generator
     * @param displayPreferences the display preferences
     * @param reporterIonQuantification the reporter quantification information
     * @param quantificationFeaturesGenerator the quantification feature
     * @param identificationParameters the identification parameters
     * @param psmKeys the PSM keys
     * @param displayScores boolean indicating whether the scores should be
     * displayed instead of the confidence
     */
    public void updateDataModel(
            Identification identification,
            SpectrumProvider spectrumProvider,
            DisplayFeaturesGenerator displayFeaturesGenerator,
            DisplayPreferences displayPreferences,
            IdentificationParameters identificationParameters,
            ReporterIonQuantification reporterIonQuantification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            long[] psmKeys,
            boolean displayScores
    ) {

        this.identification = identification;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.displayPreferences = displayPreferences;
        this.reporterIonQuantification = reporterIonQuantification;
        this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
        this.identificationParameters = identificationParameters;
        this.psmKeys = psmKeys;
        this.showScores = displayScores;

        PSMaps pSMaps = new PSMaps();
        pSMaps = (PSMaps) identification.getUrParam(pSMaps);
        inputMap = pSMaps.getInputMap();

        sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

    }

    /**
     * Resets the peptide keys.
     */
    public void reset() {
        psmKeys = null;
    }

    @Override
    public int getRowCount() {
        return psmKeys == null ? 0 : psmKeys.length;
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
                return "ID";
            case 3:
                return "Sequence";
            case 4:
                return "Charge";
            case 5:
                return "m/z Error";
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

        if (viewIndex < psmKeys.length) {

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
            long psmKey = psmKeys[viewIndex];
            SpectrumMatch spectrumMatch = identification.getSpectrumMatch(psmKey);

            switch (column) {

                case 1:

                    ArrayList<Double> data = new ArrayList<>();
                    PsmQuantificationDetails quantificationDetails
                            = quantificationFeaturesGenerator.getPSMQuantificationDetails(
                                    spectrumProvider,
                                    spectrumMatch.getKey()
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

                    return SpectrumIdentificationPanel.isBestPsmEqualForAllIdSoftware(
                            spectrumMatch,
                            identificationParameters.getSequenceMatchingParameters(),
                            inputMap.getInputAlgorithmsSorted().size()
                    );

                case 3:

                    return displayFeaturesGenerator.getTaggedPeptideSequence(
                            spectrumMatch,
                            true,
                            true,
                            true
                    );

                case 4:

                    if (spectrumMatch.getBestPeptideAssumption() != null) {
                        return spectrumMatch.getBestPeptideAssumption().getIdentificationCharge();
                    } else if (spectrumMatch.getBestTagAssumption() != null) {
                        return spectrumMatch.getBestTagAssumption().getIdentificationCharge();
                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + psmKey + ".");
                    }

                case 5:

                    Precursor precursor = spectrumProvider.getPrecursor(
                            spectrumMatch.getSpectrumFile(),
                            spectrumMatch.getSpectrumTitle()
                    );

                    SearchParameters searchParameters = identificationParameters.getSearchParameters();

                    if (spectrumMatch.getBestPeptideAssumption() != null) {

                        return Math.abs(
                                spectrumMatch.getBestPeptideAssumption().getDeltaMz(
                                        precursor.mz,
                                        searchParameters.isPrecursorAccuracyTypePpm(),
                                        searchParameters.getMinIsotopicCorrection(),
                                        searchParameters.getMaxIsotopicCorrection()
                                )
                        );

                    } else if (spectrumMatch.getBestTagAssumption() != null) {

                        return Math.abs(
                                spectrumMatch.getBestTagAssumption().getDeltaMz(
                                        precursor.mz,
                                        searchParameters.isPrecursorAccuracyTypePpm(),
                                        searchParameters.getMinIsotopicCorrection(),
                                        searchParameters.getMaxIsotopicCorrection()
                                )
                        );

                    } else {
                        throw new IllegalArgumentException("No best assumption found for spectrum " + psmKey + ".");
                    }

                case 6:

                    PSParameter psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
                    return showScores ? psParameter.getTransformedScore() : psParameter.getConfidence();

                case 7:

                    psParameter = (PSParameter) spectrumMatch.getUrParam(PSParameter.dummy);
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
    public boolean isCellEditable(
            int rowIndex, 
            int columnIndex
    ) {
        return false;
    }

    @Override
    protected void catchException(Exception e) {

        setSelfUpdating(false);
        exceptionHandler.catchException(e);

    }

    @Override
    protected int loadDataForRows(
            ArrayList<Integer> rows, 
            WaitingHandler waitingHandler
    ) {

        boolean canceled = rows.stream()
                .map(i -> identification.getSpectrumMatch(psmKeys[i]))
                .anyMatch(dummy -> waitingHandler.isRunCanceled());

        return canceled ? rows.get(0) : rows.get(rows.size() - 1);

    }
}
