package eu.isas.reporter.gui.tablemodels;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.biology.Protein;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.protein_sequences.SequenceFactory;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import java.awt.Color;
import java.io.IOException;
import java.sql.SQLNonTransientConnectionException;
import java.util.ArrayList;
import java.util.Collections;
import no.uib.jsparklines.data.ArrrayListDataPoints;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.data.StartIndexes;
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
     * The sequence factory.
     */
    private SequenceFactory sequenceFactory = SequenceFactory.getInstance();
    /**
     * A list of ordered peptide keys.
     */
    private ArrayList<String> peptideKeys = null;
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
    public PeptideTableModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            DisplayFeaturesGenerator displayFeaturesGenerator, DisplayPreferences displayPreferences, IdentificationParameters identificationParameters, String proteinAccession,
            ArrayList<String> peptideKeys, boolean displayScores, ExceptionHandler exceptionHandler) {
        this.identification = identification;
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
    public void updateDataModel(Identification identification, IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            DisplayFeaturesGenerator displayFeaturesGenerator, DisplayPreferences displayPreferences, IdentificationParameters identificationParameters,
            String proteinAccession, ArrayList<String> peptideKeys, boolean showScores) {
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
     *
     */
    public PeptideTableModel() {
    }

    @Override
    public int getRowCount() {
        if (peptideKeys != null) {
            return peptideKeys.size();
        } else {
            return 0;
        }
    }

    @Override
    public int getColumnCount() {
        return 9;
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
                return "Start";
            case 6:
                return "#Spectra";
            case 7:
                if (showScores) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 8:
                return "";
            default:
                return "";
        }
    }

    @Override
    public Object getValueAt(int row, int column) {

        try {
            boolean useDB = !isSelfUpdating();
            int viewIndex = getViewIndex(row);

            if (viewIndex >= peptideKeys.size()) {
                return null;
            }

            String peptideKey = peptideKeys.get(viewIndex);

            switch (column) {
                case 0:
                    return viewIndex + 1;
                case 1:
                    ArrayList<Double> data = new ArrayList<Double>();
                    PeptideMatch peptideMatch = identification.getPeptideMatch(peptideKey, useDB && !isScrolling);
                    PeptideQuantificationDetails quantificationDetails = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch, null);
                    ArrayList<String> reagentsOrder = displayPreferences.getReagents();
                    for (String tempReagent : reagentsOrder) {
                        int sampleIndex = sampleIndexes.indexOf(tempReagent);
                        Double ratio = quantificationDetails.getRatio(sampleIndexes.get(sampleIndex), reporterIonQuantification.getNormalizationFactors());
                        if (ratio != null) {
                            if (ratio != 0) {
                                ratio = BasicMathFunctions.log(ratio, 2);
                            }

                            data.add(ratio);
                        }
                    }
                    return new JSparklinesDataSeries(data, Color.BLACK, null);
                case 2:
                    PSParameter psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB && !isScrolling);
                    if (psParameter == null) {
                        if (isScrolling()) {
                            return null;
                        } else if (!useDB) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                    }
                    return psParameter.getProteinInferenceClass();
                case 3:
                    peptideMatch = identification.getPeptideMatch(peptideKey, useDB && !isScrolling);
                    if (peptideMatch == null) {
                        if (isScrolling()) {
                            return null;
                        } else if (!useDB) {
                            dataMissingAtRow(row);
                            return peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences()).get(0); // @TODO: find a better way!
                        }
                    }
                    return peptideMatch.getTheoreticPeptide().getParentProteins(identificationParameters.getSequenceMatchingPreferences()).get(0); // @TODO: find a better way!
                case 4:
                    peptideMatch = identification.getPeptideMatch(peptideKey, useDB && !isScrolling);
                    if (peptideMatch == null) {
                        if (isScrolling()) {
                            return null;
                        } else if (!useDB) {
                            dataMissingAtRow(row);
                            return Peptide.getSequence(peptideKey);
                        }
                    }
                    return displayFeaturesGenerator.getTaggedPeptideSequence(peptideMatch, true, true, true);
                case 5:
                    if (isScrolling) {
                        return null;
                    }
                    ArrayList<Integer> indexes;
                    if (sequenceFactory == null || proteinAccession == null) {
                        return null;
                    }
                    try {
                        Protein currentProtein = sequenceFactory.getProtein(proteinAccession);
                        String peptideSequence = Peptide.getSequence(peptideKey);
                        indexes = currentProtein.getPeptideStart(peptideSequence,
                                identificationParameters.getSequenceMatchingPreferences());
                    } catch (IOException e) {
                        exceptionHandler.catchException(e);
                        return "IO Exception";
                    }
                    Collections.sort(indexes);
                    return new StartIndexes(indexes); // note: have to be "packed" like this in order to be able to resetSorting on the first index if multiple indexes
                case 6:
                    if (isScrolling) {
                        return null;
                    }
                    peptideMatch = identification.getPeptideMatch(peptideKey, useDB);
                    if (!useDB
                            && (peptideMatch == null || !identificationFeaturesGenerator.nValidatedSpectraForPeptideInCache(peptideKey))
                            && (peptideMatch == null || !identification.peptideDetailsInCache(peptideKey))) {
                        dataMissingAtRow(row);
                        return DisplayPreferences.LOADING_MESSAGE;
                    }

                    double nConfidentSpectra = identificationFeaturesGenerator.getNConfidentSpectraForPeptide(peptideKey);
                    double nDoubtfulSpectra = identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideKey) - nConfidentSpectra;
                    int nSpectra = peptideMatch.getSpectrumMatchesKeys().size();

                    ArrayList<Double> doubleValues = new ArrayList<Double>();
                    doubleValues.add(nConfidentSpectra);
                    doubleValues.add(nDoubtfulSpectra);
                    doubleValues.add(nSpectra - nConfidentSpectra - nDoubtfulSpectra);
                    ArrrayListDataPoints arrrayListDataPoints = new ArrrayListDataPoints(doubleValues, JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers);
                    return arrrayListDataPoints;
                case 7:
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB && !isScrolling);
                    if (psParameter == null) {
                        if (isScrolling) {
                            return null;
                        } else if (!useDB) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                    }
                    if (psParameter != null) {
                        if (showScores) {
                            return psParameter.getPeptideScore();
                        } else {
                            return psParameter.getPeptideConfidence();
                        }
                    } else {
                        return null;
                    }
                case 8:
                    psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, new PSParameter(), useDB && !isScrolling);
                    if (psParameter == null) {
                        if (isScrolling) {
                            return null;
                        } else if (!useDB) {
                            dataMissingAtRow(row);
                            return DisplayPreferences.LOADING_MESSAGE;
                        }
                    }
                    if (psParameter != null) {
                        return psParameter.getMatchValidationLevel().getIndex();
                    } else {
                        return null;
                    }
                default:
                    return null;
            }
        } catch (SQLNonTransientConnectionException e) {
            // this one can be ignored i think?
            return null;
        } catch (Exception e) {
            if (exceptionHandler != null) {
                exceptionHandler.catchException(e);
            } else {
                throw new IllegalArgumentException("Table not instantiated.");
            }
            return null;
        }
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

        ArrayList<String> tempKeys = new ArrayList<String>();
        for (int i : rows) {
            if (i < peptideKeys.size()) {
                String peptideKey = peptideKeys.get(i);
                tempKeys.add(peptideKey);
            }
        }

        try {
            ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
            parameters.add(new PSParameter());
            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(tempKeys, parameters, true, parameters, waitingHandler);
            peptideMatchesIterator.setBatchSize(batchSize);
            PeptideMatch peptideMatch;

            int i = 0;
            
            while ((peptideMatch = peptideMatchesIterator.next()) != null) {
                if (waitingHandler.isRunCanceled()) {
                    return rows.get(i);
                }
                String peptideKey = peptideMatch.getKey();
                identificationFeaturesGenerator.getNValidatedSpectraForPeptide(peptideKey);
                i++;
            }
        } catch (SQLNonTransientConnectionException e) {
            // connection has been closed
            return rows.get(0);
        } catch (Exception e) {
            catchException(e);
            return rows.get(0);
        }

        return rows.get(rows.size() - 1);
    }

    @Override
    protected void loadDataForColumn(int column, WaitingHandler waitingHandler) {
        try {
            if (column == 1
                    || column == 2
                    || column == 6
                    || column == 7) {
                identification.loadPeptideMatchParameters(peptideKeys, new PSParameter(), waitingHandler, false);
            } else if (column == 3
                    || column == 4
                    || column == 5) {
                identification.loadPeptideMatches(peptideKeys, waitingHandler, false);
            }
        } catch (Exception e) {
            catchException(e);
        }
    }
}
