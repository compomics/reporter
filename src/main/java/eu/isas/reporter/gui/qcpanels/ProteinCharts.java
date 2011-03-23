package eu.isas.reporter.gui.qcpanels;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
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
 * displays a protein ratio and spread with the protein background distribution
 *
 * @author Marc Vaudel
 */
public class ProteinCharts {

    /**
     * Removes the default 3D look and feel of the bars.
     */
    static {
        XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter());
    }

    /**
     * The quantification
     */
    private ReporterIonQuantification quantification;
    /**
     * The resolution used
     */
    private double resolution;
    /**
     * The ratio charts indexed by their reporter ion index
     */
    private HashMap<Integer, RatioChart> ratioCharts = new HashMap<Integer, RatioChart>();

    /**
     * color for the peptide spread
     */
    private Color peptideSpreadColor = new Color(150, 0, 0, 75);
    /**
     * Color for the protein ratio
     */
    private Color proteinRatioColor = new Color(255, 0, 0, 255);
    /**
     * Color for the background
     */
    private Color numberOfProteinsColor = Color.lightGray;


    /**
     * constructor
     *
     * @param quantification    the quantification
     * @param resolution        The resolution
     */
    public ProteinCharts(ReporterIonQuantification quantification, double resolution) {
        this.quantification = quantification;
        ArrayList<ReporterIon> reporterIons = quantification.getReporterMethod().getReporterIons();
        this.resolution = resolution;

        createRatioCharts();
    }

    /**
     * Returns the chart panel for the given ion.
     *
     * @param ion the index of the ion to get the chart panel for
     * @param showLegend if true the chart legend is shown
     * @return the chart panel for the given ion
     */
    public ChartPanel getChart(int ion, boolean showLegend) {
        ChartPanel chartPanel = new ChartPanel(new JFreeChart(quantification.getSample(ion).getReference() + " / "
                + quantification.getSample(quantification.getReferenceLabel()).getReference(), ratioCharts.get(ion).getPlot()));

        chartPanel.getChart().getLegend().setVisible(showLegend);

        return chartPanel;
    }

    /**
     * Creates the ratios charts
     */
    private void createRatioCharts() {

        ArrayList<ReporterIon> ions = quantification.getReporterMethod().getReporterIons();

        HashMap<Integer, Double> minima = new HashMap<Integer, Double>();
        HashMap<Integer, Double> maxima = new HashMap<Integer, Double>();

        double ratio;
        for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification()) {
            for (int ion : proteinQuantification.getProteinRatios().keySet()) {
                ratio = proteinQuantification.getProteinRatios().get(ion).getRatio();
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

        IgnoredRatios ignoredRatios = new IgnoredRatios();
        HashMap<Integer, Integer> count = new HashMap<Integer, Integer>();
        double binRatio = minimum;
        while (binRatio <= maximum) {
            for (ReporterIon ion : ions) {
                count.put(ion.getIndex(), 0);
            }
            for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification()) {
                ignoredRatios = (IgnoredRatios) proteinQuantification.getUrParam(ignoredRatios);
                for (int ion : proteinQuantification.getProteinRatios().keySet()) {
                    if (!ignoredRatios.isIgnored(ion)) {
                        ratio = proteinQuantification.getProteinRatios().get(ion).getRatio();
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
            ratioCharts.put(ion.getIndex(), new RatioChart(dataset));
        }
    }

    /**
     * Displays a new protein quantification
     *
     * @param proteinQuantification the corresponding protein quantification
     */
    public void setProtein(ProteinQuantification proteinQuantification) {
        for (int ion : ratioCharts.keySet()) {
            ratioCharts.get(ion).setProtein(proteinQuantification, ion);
        }
    }

    /**
     * private class to construct a ratio chart
     */
    public class RatioChart {

        /**
         * The jFree plot
         */
        private XYPlot currentPlot = new XYPlot();

        /**
         * constructor
         * @param backGroundValues the protein background
         */
        public RatioChart(double[][] backGroundValues) {

            NumberAxis xAxis = new NumberAxis("Ratio");
            NumberAxis nProtAxis = new NumberAxis("#Proteins");
            NumberAxis nPepAxis = new NumberAxis("#Peptides");
            nProtAxis.setAutoRangeIncludesZero(true);
            nPepAxis.setAutoRangeIncludesZero(true);
            currentPlot.setDomainAxis(xAxis);
            currentPlot.setRangeAxis(0, nProtAxis);
            currentPlot.setRangeAxis(1, nPepAxis);
            currentPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
            currentPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);

            DefaultIntervalXYDataset backGround = new DefaultIntervalXYDataset();
            backGround.addSeries("#Proteins", backGroundValues);
            XYBarRenderer backgroundRenderer = new XYBarRenderer();
            backgroundRenderer.setShadowVisible(false);
            backgroundRenderer.setSeriesPaint(0, numberOfProteinsColor);
            backgroundRenderer.setMargin(0.2);
            backgroundRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(2, backGround);
            currentPlot.setRenderer(2, backgroundRenderer);
            currentPlot.mapDatasetToRangeAxis(2, 0);
        }

        /**
         * returns the jFree plot
         *
         * @return the jFree plot
         */
        public XYPlot getPlot() {
            return currentPlot;
        }

        /**
         * sets a new protein quantification to display
         * 
         * @param proteinQuantification the corresponding protein quantification
         * @param ion   the selected reporter ion
         */
        public void setProtein(ProteinQuantification proteinQuantification, int ion) {
            RatioLimits ratioLimits = (RatioLimits) proteinQuantification.getUrParam(new RatioLimits());
            int nPeptides;
            IgnoredRatios ignoredRatios = new IgnoredRatios();
            nPeptides = 0;

            for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
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
            spread.addSeries("Peptide Spread", spreadData);
            XYBarRenderer spreadRenderer = new XYBarRenderer();
            spreadRenderer.setShadowVisible(false);
            spreadRenderer.setSeriesPaint(0, peptideSpreadColor);
            spreadRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(1, spread);
            currentPlot.setRenderer(1, spreadRenderer);
            currentPlot.mapDatasetToRangeAxis(1, 1);

            DefaultIntervalXYDataset ratioDataset = new DefaultIntervalXYDataset();
            ratioDataset.addSeries("Protein Ratio", ratioData);
            XYBarRenderer ratioRenderer = new XYBarRenderer();
            ratioRenderer.setShadowVisible(false);
            ratioRenderer.setSeriesPaint(0, proteinRatioColor);
            ratioRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(0, ratioDataset);
            currentPlot.setRenderer(0, ratioRenderer);
            currentPlot.mapDatasetToRangeAxis(0, 1);
        }
    }
}
