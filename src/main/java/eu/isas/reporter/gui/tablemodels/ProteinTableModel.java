package eu.isas.reporter.gui.tablemodels;

import com.compomics.util.exceptions.ExceptionHandler;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.utils.ProteinUtils;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.experiment.io.biology.protein.ProteinDetailsProvider;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.mass_spectrometry.SpectrumProvider;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.utils.DisplayFeaturesGenerator;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import java.awt.Color;
import java.awt.Component;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import javax.swing.JTable;
import javax.swing.table.DefaultTableColumnModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import no.uib.jsparklines.data.ArrrayListDataPoints;
import no.uib.jsparklines.data.Chromosome;
import no.uib.jsparklines.data.JSparklinesDataSeries;
import no.uib.jsparklines.extra.ChromosomeTableCellRenderer;
import no.uib.jsparklines.extra.HtmlLinksRenderer;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesHeatMapTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import no.uib.jsparklines.renderers.util.GradientColorCoding;
import org.jfree.chart.plot.PlotOrientation;

/**
 * Model for the protein table.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ProteinTableModel extends SelfUpdatingTableModel {

    /**
     * The identification of this project.
     */
    private Identification identification;
    /**
     * The identification features generator provides identification information
     * on the matches.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * The protein details provider.
     */
    private ProteinDetailsProvider proteinDetailsProvider;
    /**
     * The spectrum provider.
     */
    private SpectrumProvider spectrumProvider;
    /**
     * The protein sequences provider.
     */
    private SequenceProvider sequenceProvider;
    /**
     * The gene maps.
     */
    protected GeneMaps geneMaps;
    /**
     * The display features generator provides display features.
     */
    private DisplayFeaturesGenerator displayFeaturesGenerator;
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
     * The exception handler catches exceptions.
     */
    private ExceptionHandler exceptionHandler;
    /**
     * The list of the keys of the protein matches being displayed.
     */
    private long[] proteinKeys = null;
    /**
     * If true the scores will be shown.
     */
    private boolean showScores = false;
    /**
     * The batch size.
     */
    private int batchSize = 20;
    /**
     * The display preferences.
     */
    private DisplayPreferences displayPreferences;

    /**
     * Constructor for an empty table.
     */
    public ProteinTableModel() {
    }

    /**
     * Constructor.
     *
     * @param identification the identification containing the protein
     * information
     * @param identificationFeaturesGenerator the identification features
     * generator generating the features of the identification
     * @param proteinDetailsProvider the protein details provider
     * @param sequenceProvider the protein sequences provider
     * @param geneMaps the gene maps
     * @param reporterIonQuantification the reporter quantification information
     * @param quantificationFeaturesGenerator the quantification feature
     * generator
     * @param displayFeaturesGenerator the display features generator generating
     * the display elements
     * @param displayPreferences the display preferences
     * @param exceptionHandler an exception handler catching exceptions
     * @param proteinKeys the keys of the protein matches to display
     */
    public ProteinTableModel(
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ProteinDetailsProvider proteinDetailsProvider,
            SequenceProvider sequenceProvider,
            GeneMaps geneMaps,
            ReporterIonQuantification reporterIonQuantification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            DisplayFeaturesGenerator displayFeaturesGenerator,
            DisplayPreferences displayPreferences,
            ExceptionHandler exceptionHandler,
            long[] proteinKeys
    ) {

        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.proteinDetailsProvider = proteinDetailsProvider;
        this.sequenceProvider = sequenceProvider;
        this.geneMaps = geneMaps;
        this.reporterIonQuantification = reporterIonQuantification;
        this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.displayPreferences = displayPreferences;
        this.exceptionHandler = exceptionHandler;
        this.proteinKeys = proteinKeys;

        sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

    }

    /**
     * Update the data in the table model without having to reset the whole
     * table model.This keeps the sorting order of the table.
     *
     * @param identification the identification containing the protein
     * information
     * @param identificationFeaturesGenerator the identification features
     * generator generating the features of the identification
     * @param geneMaps the gene maps
     * @param proteinDetailsProvider the protein details provider
     * @param spectrumProvider the spectrum provider
     * @param sequenceProvider the protein sequences provider
     * @param reporterIonQuantification the reporter quantification information
     * @param quantificationFeaturesGenerator the quantification feature
     * generator
     * @param displayFeaturesGenerator the display features generator generating
     * the display elements
     * @param displayPreferences the display preferences
     * @param proteinKeys the keys of the protein matches to display
     */
    public void updateDataModel(
            Identification identification,
            IdentificationFeaturesGenerator identificationFeaturesGenerator,
            ProteinDetailsProvider proteinDetailsProvider,
            SequenceProvider sequenceProvider,
            SpectrumProvider spectrumProvider,
            GeneMaps geneMaps,
            ReporterIonQuantification reporterIonQuantification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            DisplayFeaturesGenerator displayFeaturesGenerator,
            DisplayPreferences displayPreferences,
            long[] proteinKeys
    ) {

        this.identification = identification;
        this.identificationFeaturesGenerator = identificationFeaturesGenerator;
        this.proteinDetailsProvider = proteinDetailsProvider;
        this.sequenceProvider = sequenceProvider;
        this.spectrumProvider = spectrumProvider;
        this.geneMaps = geneMaps;
        this.reporterIonQuantification = reporterIonQuantification;
        this.quantificationFeaturesGenerator = quantificationFeaturesGenerator;
        this.displayFeaturesGenerator = displayFeaturesGenerator;
        this.displayPreferences = displayPreferences;
        this.proteinKeys = proteinKeys;

        sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

    }

    /**
     * Sets whether the scores should be displayed.
     *
     * @param showScores a boolean indicating whether the scores should be
     * displayed
     */
    public void showScores(boolean showScores) {
        this.showScores = showScores;
    }

    /**
     * Reset the protein keys.
     */
    public void reset() {
        proteinKeys = null;
    }

    @Override
    public int getRowCount() {
        return proteinKeys == null ? 0 : proteinKeys.length;
    }

    @Override
    public int getColumnCount() {
        return 12;
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
                return "Description";
            case 5:
                return "Chr";
            case 6:
                return "Coverage";
            case 7:
                return "#Peptides";
            case 8:
                return "#Spectra";
            case 9:
                return "MW";
            case 10:
                if (showScores) {
                    return "Score";
                } else {
                    return "Confidence";
                }
            case 11:
                return "";
            default:
                return "";
        }

    }

    @Override
    public Object getValueAt(int row, int column) {

        if (proteinKeys != null) {

            int viewIndex = getViewIndex(row);

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
            long proteinKey = proteinKeys[viewIndex];

            ProteinMatch proteinMatch = identification.getProteinMatch(proteinKey);

            switch (column) {

                case 1:

                    ArrayList<Double> data = new ArrayList<>();

                    ProteinQuantificationDetails quantificationDetails
                            = quantificationFeaturesGenerator.getProteinMatchQuantificationDetails(
                                    spectrumProvider,
                                    proteinKey,
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

                    PSParameter psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                    return psParameter.getProteinInferenceGroupClass();

                case 3:

                    return isScrolling ? proteinMatch.getLeadingAccession()
                            : displayFeaturesGenerator.getDatabaseLink(proteinMatch.getLeadingAccession());

                case 4:

                    return proteinDetailsProvider.getSimpleDescription(proteinMatch.getLeadingAccession());

                case 5:

                    String geneName = proteinDetailsProvider.getGeneName(proteinMatch.getLeadingAccession());
                    String chromosomeName = geneMaps.getChromosome(geneName);

                    return (chromosomeName == null || chromosomeName.length() == 0) ? new Chromosome(null) : new Chromosome(chromosomeName);

                case 6:

                    HashMap<Integer, Double> sequenceCoverage = identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                    Double sequenceCoverageConfident = 100 * sequenceCoverage.get(MatchValidationLevel.confident.getIndex());
                    Double sequenceCoverageDoubtful = 100 * sequenceCoverage.get(MatchValidationLevel.doubtful.getIndex());
                    Double sequenceCoverageNotValidated = 100 * sequenceCoverage.get(MatchValidationLevel.not_validated.getIndex());
                    double possibleCoverage = 100 * identificationFeaturesGenerator.getObservableCoverage(proteinKey);

                    ArrayList<Double> doubleValues = new ArrayList<>(4);
                    doubleValues.add(sequenceCoverageConfident);
                    doubleValues.add(sequenceCoverageDoubtful);
                    doubleValues.add(sequenceCoverageNotValidated);
                    doubleValues.add(possibleCoverage - sequenceCoverageConfident - sequenceCoverageDoubtful - sequenceCoverageNotValidated);

                    return new ArrrayListDataPoints(
                            doubleValues,
                            JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumExceptLastNumber
                    );

                case 7:

                    int nPeptides = proteinMatch.getPeptideCount();
                    int nConfidentPeptides = identificationFeaturesGenerator.getNConfidentPeptides(proteinKey);
                    int nValidatedPeptides = identificationFeaturesGenerator.getNValidatedPeptides(proteinKey);
                    int nDoubtfulPeptides = nValidatedPeptides - nConfidentPeptides;

                    doubleValues = new ArrayList<>(3);
                    doubleValues.add((double) nConfidentPeptides);
                    doubleValues.add((double) nDoubtfulPeptides);
                    doubleValues.add((double) (nPeptides - nConfidentPeptides - nDoubtfulPeptides));

                    return new ArrrayListDataPoints(
                            doubleValues,
                            JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers
                    );

                case 8:

                    int nPsms = identificationFeaturesGenerator.getNSpectra(proteinKey);
                    int nConfidentPsms = identificationFeaturesGenerator.getNConfidentSpectra(proteinKey);
                    int nValidatedPsms = identificationFeaturesGenerator.getNValidatedSpectra(proteinKey);
                    int nDoubtfulPsms = nValidatedPsms - nConfidentPsms;

                    doubleValues = new ArrayList<>(3);
                    doubleValues.add((double) nConfidentPsms);
                    doubleValues.add((double) nDoubtfulPsms);
                    doubleValues.add((double) (nPsms - nConfidentPsms - nDoubtfulPsms));

                    return new ArrrayListDataPoints(
                            doubleValues,
                            JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers
                    );

                case 9:

                    return ProteinUtils.computeMolecularWeight(sequenceProvider.getSequence(proteinMatch.getLeadingAccession()));

                case 10:

                    psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                    return showScores ? psParameter.getTransformedScore() : psParameter.getConfidence();

                case 11:

                    psParameter = (PSParameter) proteinMatch.getUrParam(PSParameter.dummy);
                    return psParameter.getMatchValidationLevel().getIndex();

                default:

                    return null;

            }

        } else {
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

        boolean canceled = rows.parallelStream()
                .map(i -> ((ProteinMatch) identification.retrieveObject(proteinKeys[i])))
                .anyMatch(proteinMatch -> {
                    long proteinKey = proteinMatch.getKey();
                    identificationFeaturesGenerator.getSequenceCoverage(proteinKey);
                    identificationFeaturesGenerator.getObservableCoverage(proteinKey);
                    identificationFeaturesGenerator.getNValidatedPeptides(proteinKey);
                    identificationFeaturesGenerator.getNValidatedSpectra(proteinKey);
                    identificationFeaturesGenerator.getNSpectra(proteinKey);
                    identificationFeaturesGenerator.getSpectrumCounting(proteinKey);
                    return waitingHandler.isRunCanceled();
                });

        return canceled ? rows.get(0) : rows.get(rows.size() - 1);
    }

    /**
     * Set up the properties of the protein table.
     *
     * @param proteinTable the protein table
     * @param sparklineColor the sparkline color to use
     * @param sparklineColorNotValidated the sparkline color for not validated
     * stuffs
     * @param parentClass the parent class used to get icons
     * @param sparklineColorNotFound the sparkline color for not found stuff
     * @param sparklineColorDoubtful the sparkline color for doubtful
     * @param scoreAndConfidenceDecimalFormat the decimal format for score and
     * confidence
     * @param maxProteinKeyLength the longest protein key to display
     * @param maxAbsProteinValue the maximum absolute protein value
     */
    public static void setProteinTableProperties(
            JTable proteinTable,
            Color sparklineColor,
            Color sparklineColorNotValidated,
            Color sparklineColorNotFound,
            Color sparklineColorDoubtful,
            DecimalFormat scoreAndConfidenceDecimalFormat,
            Class parentClass,
            Integer maxProteinKeyLength,
            Double maxAbsProteinValue
    ) {

        // @TODO: find a better location for this method?
        // the index column
        proteinTable.getColumn(" ").setMaxWidth(50);
        proteinTable.getColumn(" ").setMinWidth(50);

//        proteinTable.getColumn("Quant").setMaxWidth(90);
//        proteinTable.getColumn("Quant").setMinWidth(90);
        proteinTable.getColumn("Chr").setMaxWidth(50);
        proteinTable.getColumn("Chr").setMinWidth(50);

        try {
            proteinTable.getColumn("Confidence").setMaxWidth(90);
            proteinTable.getColumn("Confidence").setMinWidth(90);
        } catch (IllegalArgumentException w) {
            proteinTable.getColumn("Score").setMaxWidth(90);
            proteinTable.getColumn("Score").setMinWidth(90);
        }

        // the validated column
        proteinTable.getColumn("").setMaxWidth(30);
        proteinTable.getColumn("").setMinWidth(30);

        // the protein inference column
        proteinTable.getColumn("PI").setMaxWidth(37);
        proteinTable.getColumn("PI").setMinWidth(37);

        // the quant plot column
        proteinTable.getColumn("Quant").setCellRenderer(
                new JSparklinesHeatMapTableCellRenderer(
                        GradientColorCoding.ColorGradient.GreenWhiteRed,
                        maxAbsProteinValue
                )
        );

        // set up the protein inference color map
        HashMap<Integer, Color> proteinInferenceColorMap = new HashMap<Integer, Color>();
        proteinInferenceColorMap.put(PSParameter.NOT_GROUP, sparklineColor);
        proteinInferenceColorMap.put(PSParameter.RELATED, Color.YELLOW);
        proteinInferenceColorMap.put(PSParameter.RELATED_AND_UNRELATED, Color.ORANGE);
        proteinInferenceColorMap.put(PSParameter.UNRELATED, Color.RED);

        // set up the protein inference tooltip map
        HashMap<Integer, String> proteinInferenceTooltipMap = new HashMap<Integer, String>();
        proteinInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Single Protein");
        proteinInferenceTooltipMap.put(PSParameter.RELATED, "Related Proteins");
        proteinInferenceTooltipMap.put(PSParameter.RELATED_AND_UNRELATED, "Related and Unrelated Proteins");
        proteinInferenceTooltipMap.put(PSParameter.UNRELATED, "Unrelated Proteins");

        proteinTable.getColumn("Accession").setCellRenderer(
                new HtmlLinksRenderer(
                        TableProperties.getSelectedRowHtmlTagFontColor(),
                        TableProperties.getNotSelectedRowHtmlTagFontColor()
                )
        );

        proteinTable.getColumn("PI").setCellRenderer(
                new JSparklinesIntegerColorTableCellRenderer(
                        sparklineColor,
                        proteinInferenceColorMap,
                        proteinInferenceTooltipMap
                )
        );

        // use a gray color for no decoy searches
        Color nonValidatedColor = sparklineColorNotValidated;
        ArrayList<Color> sparklineColors = new ArrayList<Color>();
        sparklineColors.add(sparklineColor);
        sparklineColors.add(sparklineColorDoubtful);
        sparklineColors.add(nonValidatedColor);
        sparklineColors.add(sparklineColorNotFound);

        JSparklinesArrayListBarChartTableCellRenderer coverageCellRendered
                = new JSparklinesArrayListBarChartTableCellRenderer(
                        PlotOrientation.HORIZONTAL,
                        100.0,
                        sparklineColors,
                        JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumExceptLastNumber
                );

        coverageCellRendered.showNumberAndChart(
                true,
                TableProperties.getLabelWidth(),
                new DecimalFormat("0.00")
        );

        proteinTable.getColumn("Coverage").setCellRenderer(coverageCellRendered);

        JSparklinesArrayListBarChartTableCellRenderer peptidesCellRenderer
                = new JSparklinesArrayListBarChartTableCellRenderer(
                        PlotOrientation.HORIZONTAL,
                        100.0,
                        sparklineColors,
                        JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers
                );

        peptidesCellRenderer.showNumberAndChart(
                true,
                TableProperties.getLabelWidth(),
                new DecimalFormat("0")
        );

        proteinTable.getColumn("#Peptides").setCellRenderer(peptidesCellRenderer);

        JSparklinesArrayListBarChartTableCellRenderer spectraCellRenderer
                = new JSparklinesArrayListBarChartTableCellRenderer(
                        PlotOrientation.HORIZONTAL,
                        100.0,
                        sparklineColors,
                        JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers
                );

        spectraCellRenderer.showNumberAndChart(
                true,
                TableProperties.getLabelWidth(),
                new DecimalFormat("0")
        );

        proteinTable.getColumn("#Spectra").setCellRenderer(spectraCellRenderer);

        JSparklinesBarChartTableCellRenderer mwCellRenderer
                = new JSparklinesBarChartTableCellRenderer(
                        PlotOrientation.HORIZONTAL,
                        10.0,
                        sparklineColor
                );

        mwCellRenderer.showNumberAndChart(true, TableProperties.getLabelWidth());
        proteinTable.getColumn("MW").setCellRenderer(mwCellRenderer);

        proteinTable.getColumn("Chr").setCellRenderer(
                new ChromosomeTableCellRenderer(
                        Color.WHITE,
                        Color.BLACK
                )
        );

        try {

            proteinTable.getColumn("Confidence").setCellRenderer(
                    new JSparklinesBarChartTableCellRenderer(
                            PlotOrientation.HORIZONTAL,
                            100.0,
                            sparklineColor
                    )
            );

            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                    true,
                    TableProperties.getLabelWidth() - 20,
                    scoreAndConfidenceDecimalFormat
            );

        } catch (IllegalArgumentException e) {

            proteinTable.getColumn("Score").setCellRenderer(
                    new JSparklinesBarChartTableCellRenderer(
                            PlotOrientation.HORIZONTAL,
                            100.0,
                            sparklineColor
                    )
            );

            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                    true,
                    TableProperties.getLabelWidth() - 20,
                    scoreAndConfidenceDecimalFormat
            );
        }

        proteinTable.getColumn("").setCellRenderer(
                new JSparklinesIntegerIconTableCellRenderer(
                        MatchValidationLevel.getIconMap(parentClass),
                        MatchValidationLevel.getTooltipMap()
                )
        );

        // set the preferred size of the accession column
        if (maxProteinKeyLength != null) {

            Integer width = getPreferredAccessionColumnWidth(
                    proteinTable,
                    proteinTable.getColumn("Accession").getModelIndex(),
                    6,
                    maxProteinKeyLength
            );

            if (width != null) {
                proteinTable.getColumn("Accession").setMinWidth(width);
                proteinTable.getColumn("Accession").setMaxWidth(width);
            } else {
                proteinTable.getColumn("Accession").setMinWidth(15);
                proteinTable.getColumn("Accession").setMaxWidth(Integer.MAX_VALUE);
            }

        }

    }

    /**
     * Gets the preferred width of the column specified by colIndex. The column
     * will be just wide enough to show the column head and the widest cell in
     * the column. Margin pixels are added to the left and right (resulting in
     * an additional width of 2*margin pixels. Returns null if the max width
     * cannot be set.
     *
     * @param table the table
     * @param colIndex the column index
     * @param margin the margin to add
     * @param maxProteinKeyLength the maximal protein key length
     *
     * @return the preferred width of the column
     */
    public static Integer getPreferredAccessionColumnWidth(
            JTable table,
            int colIndex,
            int margin,
            Integer maxProteinKeyLength
    ) {

        DefaultTableColumnModel colModel = (DefaultTableColumnModel) table.getColumnModel();
        TableColumn col = colModel.getColumn(colIndex);

        // get width of column header
        TableCellRenderer renderer = col.getHeaderRenderer();

        if (renderer == null) {
            renderer = table.getTableHeader().getDefaultRenderer();
        }

        Component comp = renderer.getTableCellRendererComponent(table, col.getHeaderValue(), false, false, 0, 0);
        int width = comp.getPreferredSize().width;

        // get maximum width of column data
        if (maxProteinKeyLength == null || (maxProteinKeyLength + 5) > (table.getColumnName(colIndex).length() + margin)) {
            return null;
        }

        // add margin
        width += 2 * margin;

        return width;

    }
}
