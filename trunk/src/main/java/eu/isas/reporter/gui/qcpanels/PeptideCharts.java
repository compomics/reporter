package eu.isas.reporter.gui.qcpanels;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PsmQuantification;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.RatioLimits;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.StandardXYToolTipGenerator;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.DefaultIntervalXYDataset;

/**
 * This panel will display the distribution of the ratios of peptides ascribed to the selected protein (also displayed), the selected peptide ratio and its distribution spread.
 *
 * @author Marc Vaudel
 */
public class PeptideCharts {

    /**
     * Removes the default 3D look and feel of the bars.
     */
    static {
        XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter());
    }

    /**
     * The protein quantification of interest
     */
    private ProteinQuantification proteinQuantification;
    /**
     * The protein index
     */
    private int proteinIndex;
    /**
     * The method resolution
     */
    private double resolution;
    /**
     * The reporter ion quantification
     */
    private ReporterIonQuantification quantification;
    /**
     * The various ratios charts indexed by the reporter ion index.
     */
    private HashMap<Integer, RatioChart> ratioCharts = new HashMap<Integer, RatioChart>();

    /**
     * color for the peptide distribution
     */
    private Color peptideColor = Color.lightGray;
    /**
     * color for the spread of peptides
     */
    private Color peptideSpreadColor = new Color(0, 0, 150, 75);
    /**
     * Color for the spread of spectra
     */
    private Color spectrumSpreadColor = new Color(150, 0, 0, 75);
    /**
     * Color for the protein ratio
     */
    private Color proteinRatioColor = new Color(0, 0, 255, 255);
    /**
     * Color for the peptide ratio
     */
    private Color peptideRatioColor = new Color(255, 0, 0, 255);

    /**
     * constructor
     *
     * @param proteinIndex      The protein index of the selected protein
     * @param quantification    The quantification
     * @param resolution        The resolution to be used
     */
    public PeptideCharts(int proteinIndex, ReporterIonQuantification quantification, double resolution) {
        this.proteinIndex = proteinIndex;
        this.quantification = quantification;
        ArrayList<ReporterIon> reporterIons = quantification.getReporterMethod().getReporterIons();
        this.proteinQuantification = quantification.getProteinQuantification().get(proteinIndex);
        this.resolution = resolution;

        createReporterPanels(proteinQuantification);
    }

    /**
     * Returns the chart panel for the given ion.
     *
     * @param ion the index of the ion to get the chart panel for
     * @param showLegend if true the chart legend is shown
     * @return the chart panel for the given ion
     */
    public ChartPanel getChart(int ion, boolean showLegend) {
        ChartPanel chartPanel= new ChartPanel(new JFreeChart(quantification.getSample(ion).getReference() + " / "
                + quantification.getSample(quantification.getReferenceLabel()).getReference(), ratioCharts.get(ion).getPlot()));

        chartPanel.getChart().getLegend().setVisible(showLegend);

        return chartPanel;
    }

    /**
     * creates the display panels
     * 
     * @param proteinQuantification The protein quantification of interest
     */
    private void createReporterPanels(ProteinQuantification proteinQuantification) {

        ArrayList<ReporterIon> ions = quantification.getReporterMethod().getReporterIons();

        HashMap<Integer, Double> minima = new HashMap<Integer, Double>();
        HashMap<Integer, Double> maxima = new HashMap<Integer, Double>();

        IgnoredRatios ignoredRatios = new IgnoredRatios();

        double ratio;
        for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification().values()) {
            ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
            for (int ion : peptideQuantification.getRatios().keySet()) {
                if (!ignoredRatios.isIgnored(ion)) {
                    ratio = peptideQuantification.getRatios().get(ion).getRatio();
                    if (!minima.containsKey(ion)) {
                        minima.put(ion, ratio);
                    } else if (ratio < minima.get(ion)) {
                        minima.put(ion, ratio);
                    }
                    if (!maxima.containsKey(ion)) {
                        maxima.put(ion, ratio);
                    } else if (ratio > maxima.get(ion)) {
                        maxima.put(ion, ratio);
                    }
                }
            }
        }

        HashMap<Integer, ArrayList<Double>> xValuesMap = new HashMap<Integer, ArrayList<Double>>();
        HashMap<Integer, ArrayList<Integer>> allCounts = new HashMap<Integer, ArrayList<Integer>>();

        double minimum = -1;
        double maximum = -1;
        for (ReporterIon ion : ions) {
            if (minimum == -1 || minima.get(ion.getIndex()) < minimum) {
                minimum = minima.get(ion.getIndex());
            }
            if (maximum == -1 || maxima.get(ion.getIndex()) > maximum) {
                maximum = maxima.get(ion.getIndex());
            }
            xValuesMap.put(ion.getIndex(), new ArrayList<Double>());
            allCounts.put(ion.getIndex(), new ArrayList<Integer>());
        }

        HashMap<Integer, Integer> count = new HashMap<Integer, Integer>();
        double binRatio = minimum;
        while (binRatio <= maximum) {
            for (ReporterIon ion : ions) {
                count.put(ion.getIndex(), 0);
            }
            for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification().values()) {
                ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
                for (int ion : peptideQuantification.getRatios().keySet()) {
                    if (!ignoredRatios.isIgnored(ion)) {
                        ratio = peptideQuantification.getRatios().get(ion).getRatio();
                        if (ratio >= binRatio && ratio < binRatio + resolution) {
                            count.put(ion, count.get(ion) + 1);
                        }
                    }
                }
            }
            for (int ion : minima.keySet()) {
                if (binRatio > minima.get(ion) - resolution
                        && binRatio <= maxima.get(ion)) {
                    xValuesMap.get(ion).add(binRatio);
                    allCounts.get(ion).add(count.get(ion));
                }
            }
            binRatio += resolution;
        }

        double[] xValues, xValuesBegin, xValuesEnd;
        double[] counts;
        for (ReporterIon ion : ions) {
            int nBins = xValuesMap.get(ion.getIndex()).size();
            xValues = new double[nBins];
            xValuesBegin = new double[nBins];
            xValuesEnd = new double[nBins];
            counts = new double[nBins];
            for (int i = 0; i < nBins; i++) {
                xValuesBegin[i] = xValuesMap.get(ion.getIndex()).get(i);
                xValuesEnd[i] = xValuesMap.get(ion.getIndex()).get(i) + resolution;
                xValues[i] = (xValuesBegin[i] + xValuesEnd[i]) / 2;
                counts[i] = allCounts.get(ion.getIndex()).get(i);
            }
            double[][] dataset = new double[6][nBins];
            dataset[0] = xValues;
            dataset[1] = xValuesBegin;
            dataset[2] = xValuesEnd;
            dataset[3] = counts;
            dataset[4] = counts;
            dataset[5] = counts;
            RatioChart newChart = new RatioChart(dataset);
            newChart.addProteinRatio(proteinQuantification, ion.getIndex());
            ratioCharts.put(ion.getIndex(), newChart);
        }
    }

    /**
     * Method used to change the peptide to display
     *
     * @param peptideQuantification the peptide quantification of interest
     */
    public void setPeptide(PeptideQuantification peptideQuantification) {
        IgnoredRatios ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(new IgnoredRatios());
        for (int ion : ratioCharts.keySet()) {
            if (!ignoredRatios.isIgnored(ion)) {
                ratioCharts.get(ion).setPeptide(peptideQuantification, ion);
            }
        }
    }

    /**
     * Returns the protein index.
     *
     * @return the protein index
     */
    public int getProteinIndex() {
        return proteinIndex;
    }

    /**
     * Private class to construct a chart
     */
    private class RatioChart {

        /**
         * The JFree plot
         */
        private XYPlot currentPlot = new XYPlot();

        /**
         * Constructs the chart with only the background
         *
         * @param backGroundValues ratios of the background
         */
        public RatioChart(double[][] backGroundValues) {

            NumberAxis xAxis = new NumberAxis("Ratio");
            NumberAxis nProtAxis = new NumberAxis("#Peptides");
            NumberAxis nPepAxis = new NumberAxis("#Spectra");
            nProtAxis.setAutoRangeIncludesZero(true);
            nPepAxis.setAutoRangeIncludesZero(true);
            currentPlot.setDomainAxis(xAxis);
            currentPlot.setRangeAxis(0, nProtAxis);
            currentPlot.setRangeAxis(1, nPepAxis);
            currentPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
            currentPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);

            DefaultIntervalXYDataset backGround = new DefaultIntervalXYDataset();
            backGround.addSeries("#Peptides", backGroundValues);
            XYBarRenderer backgroundRenderer = new XYBarRenderer();
            backgroundRenderer.setShadowVisible(false);
            backgroundRenderer.setSeriesPaint(0, peptideColor);
            backgroundRenderer.setMargin(0.2);
            backgroundRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(4, backGround);
            currentPlot.setRenderer(4, backgroundRenderer);
            currentPlot.mapDatasetToRangeAxis(4, 0);
        }

        /**
         * getter for the plot
         *
         * @return  the jFree plot
         */
        public XYPlot getPlot() {
            return currentPlot;
        }

        /**
         * display a protein ratio of interest
         *
         * @param proteinQuantification the protein quantification of interest
         * @param ion                   the reporter ion considered
         */
        public void addProteinRatio(ProteinQuantification proteinQuantification, int ion) {
            RatioLimits ratioLimits = (RatioLimits) proteinQuantification.getUrParam(new RatioLimits());
            int nPeptides;
            IgnoredRatios ignoredRatios = new IgnoredRatios();
            nPeptides = 0;
            for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification().values()) {
                ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
                if (!ignoredRatios.isIgnored(ion)) {
                    if (peptideQuantification.getRatios().get(ion).getRatio() > ratioLimits.getLimits(ion)[0]
                            && peptideQuantification.getRatios().get(ion).getRatio() < ratioLimits.getLimits(ion)[1]) {
                        nPeptides++;
                    }
                }
            }

            double[] spreadBegin = {ratioLimits.getLimits(ion)[0]};
            double[] spreadEnd = {ratioLimits.getLimits(ion)[1]};
            double[] ratio = {proteinQuantification.getProteinRatios().get(ion).getRatio()};
            double[] ratioBegin = {ratio[0] - resolution / 2};
            double[] ratioEnd = {ratio[0] + resolution / 2};
            double[] spreadHeight = {(double) nPeptides / 10};
            double[] peptides = {nPeptides};

            double[][] spreadData = new double[][]{ratio, spreadBegin, spreadEnd, spreadHeight, spreadHeight, spreadHeight};
            double[][] ratioData = new double[][]{ratio, ratioBegin, ratioEnd, peptides, peptides, peptides};


            DefaultIntervalXYDataset spread = new DefaultIntervalXYDataset();
            spread.addSeries("Peptides Spread", spreadData);
            XYBarRenderer spreadRenderer = new XYBarRenderer();
            spreadRenderer.setShadowVisible(false);
            spreadRenderer.setSeriesPaint(0, peptideSpreadColor);
            spreadRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(3, spread);
            currentPlot.setRenderer(3, spreadRenderer);
            currentPlot.mapDatasetToRangeAxis(3, 1);

            DefaultIntervalXYDataset ratioDataset = new DefaultIntervalXYDataset();
            ratioDataset.addSeries("Protein Ratio", ratioData);
            XYBarRenderer ratioRenderer = new XYBarRenderer();
            ratioRenderer.setShadowVisible(false);
            ratioRenderer.setSeriesPaint(0, proteinRatioColor);
            ratioRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(2, ratioDataset);
            currentPlot.setRenderer(2, ratioRenderer);
            currentPlot.mapDatasetToRangeAxis(2, 1);
        }

        /**
         * Displays a peptide quantification of interest
         *
         * @param peptideQuantification the peptide quantification
         * @param ion                   the considered reporter ion
         */
        public void setPeptide(PeptideQuantification peptideQuantification, int ion) {
            RatioLimits ratioLimits = (RatioLimits) peptideQuantification.getUrParam(new RatioLimits());
            IgnoredRatios ignoredRatios = new IgnoredRatios();
            int nSpectra = 0;
            for (PsmQuantification spectrumQuantification : peptideQuantification.getPsmQuantification().values()) {
                ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
                if (!ignoredRatios.isIgnored(ion)) {
                    if (spectrumQuantification.getRatios().get(ion).getRatio() > ratioLimits.getLimits(ion)[0]
                            && spectrumQuantification.getRatios().get(ion).getRatio() < ratioLimits.getLimits(ion)[1]) {
                        nSpectra++;
                    }
                }
            }

            double[] spreadBegin = {ratioLimits.getLimits(ion)[0]};
            double[] spreadEnd = {ratioLimits.getLimits(ion)[1]};
            double[] ratio = {peptideQuantification.getRatios().get(ion).getRatio()};
            double[] ratioBegin = {ratio[0] - resolution / 2};
            double[] ratioEnd = {ratio[0] + resolution / 2};
            double[] spreadHeight = {(double) nSpectra / 10};
            double[] spectra = {nSpectra};

            double[][] spreadData = new double[][]{ratio, spreadBegin, spreadEnd, spreadHeight, spreadHeight, spreadHeight};
            double[][] ratioData = new double[][]{ratio, ratioBegin, ratioEnd, spectra, spectra, spectra};

            DefaultIntervalXYDataset spread = new DefaultIntervalXYDataset();
            spread.addSeries("Spectra Spread", spreadData);
            XYBarRenderer spreadRenderer = new XYBarRenderer();
            spreadRenderer.setShadowVisible(false);
            spreadRenderer.setSeriesPaint(0, spectrumSpreadColor);
            spreadRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(1, spread);
            currentPlot.setRenderer(1, spreadRenderer);
            currentPlot.mapDatasetToRangeAxis(1, 1);

            DefaultIntervalXYDataset ratioDataset = new DefaultIntervalXYDataset();
            ratioDataset.addSeries("Peptide Ratio", ratioData);
            XYBarRenderer ratioRenderer = new XYBarRenderer();
            ratioRenderer.setShadowVisible(false);
            ratioRenderer.setSeriesPaint(0, peptideRatioColor);
            ratioRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(0, ratioDataset);
            currentPlot.setRenderer(0, ratioRenderer);
            currentPlot.mapDatasetToRangeAxis(0, 1);
        }
    }
}
