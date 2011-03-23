package eu.isas.reporter.gui.qcpanels;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import eu.isas.reporter.myparameters.IgnoredRatios;
import eu.isas.reporter.myparameters.ItraqScore;
import java.awt.Color;
import java.util.ArrayList;
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
 * This chart will display the peptide score (quality) distribution
 *
 * @author Marc Vaudel
 */
public class PeptideScoreCharts {

    /**
     * Removes the default 3D look and feel of the bars.
     */
    static {
        XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter());
    }

    /**
     * The method resolution
     */
    private final double resolution = 0.1;
    /**
     * The conducted quantification
     */
    private ReporterIonQuantification quantification;
    /**
     * The jFree chart
     */
    private RatioChart ratioCharts;

    /**
     * The background color
     */
    private Color allPeptidesColor = Color.lightGray;

    /**
     * constructor
     *
     * @param quantification the quantification
     */
    public PeptideScoreCharts(ReporterIonQuantification quantification) {
        this.quantification = quantification;
        ArrayList<ReporterIon> reporterIons = quantification.getReporterMethod().getReporterIons();

        createRatioCharts();
    }

    /**
     * Returns the chart panel.
     *
     * @param showLegend if true the chart legend is shown
     * @return the chart panel.
     */
    public ChartPanel getChart(boolean showLegend) {
        ChartPanel chartPanel = new ChartPanel(new JFreeChart("Peptide Quantification Quality", ratioCharts.getPlot()));

        chartPanel.getChart().getLegend().setVisible(showLegend);

        return chartPanel;
    }

    /**
     * Method which creates the ratio charts
     */
    private void createRatioCharts() {

        ArrayList<ReporterIon> ions = quantification.getReporterMethod().getReporterIons();

        double minimum = -1, maximum = -1;

        Double score;
        ItraqScore itraqScore = new ItraqScore();
        IgnoredRatios ignoredRatios = new IgnoredRatios();
        for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification()) {
            for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
                itraqScore = (ItraqScore) peptideQuantification.getUrParam(itraqScore);
                ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
                for (int ion : peptideQuantification.getRatios().keySet()) {
                    if (!ignoredRatios.isIgnored(ion)) {
                        score = itraqScore.getScore(ion);
                        if (score != null) {
                            if (score < minimum || minimum == -1) {
                                minimum = score;
                            }
                            if (score > maximum || maximum == -1) {
                                maximum = score;
                            }
                        }
                    }
                }
            }
        }

        ArrayList<Double> xValuesList = new ArrayList<Double>();
        ArrayList<Integer> allCounts = new ArrayList<Integer>();

        int count;
        double binScore = minimum;
        while (binScore <= maximum) {
            count = 0;
            for (ProteinQuantification proteinQuantification : quantification.getProteinQuantification()) {
                for (PeptideQuantification peptideQuantification : proteinQuantification.getPeptideQuantification()) {
                    itraqScore = (ItraqScore) peptideQuantification.getUrParam(itraqScore);
                    ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(ignoredRatios);
                    for (int ion : peptideQuantification.getRatios().keySet()) {
                        if (!ignoredRatios.isIgnored(ion)) {
                            score = itraqScore.getScore(ion);
                            if (score != null && score >= binScore && score < binScore + resolution) {
                                count++;
                            }
                        }
                    }
                }
            }
            xValuesList.add(binScore);
            allCounts.add(count);
            binScore += resolution;
        }

        double[] xValues, xValuesBegin, xValuesEnd;
        double[] counts;
        int nBins = xValuesList.size();
        xValues = new double[nBins];
        xValuesBegin = new double[nBins];
        xValuesEnd = new double[nBins];
        counts = new double[nBins];
        for (int i = 0; i < nBins; i++) {
            xValuesBegin[i] = xValuesList.get(i);
            xValuesEnd[i] = xValuesList.get(i) + resolution;
            xValues[i] = (xValuesBegin[i] + xValuesEnd[i]) / 2;
            counts[i] = allCounts.get(i);
        }
        double[][] dataset = new double[6][nBins];
        dataset[0] = xValues;
        dataset[1] = xValuesBegin;
        dataset[2] = xValuesEnd;
        dataset[3] = counts;
        dataset[4] = counts;
        dataset[5] = counts;
        ratioCharts = new RatioChart(dataset);
    }

    /**
     * Sets the peptide to display
     *
     * @param peptideQuantification the selected peptide quantification
     */
    public void setPeptide(PeptideQuantification peptideQuantification) {
        ratioCharts.setPeptide(peptideQuantification);
    }

    /**
     * private class for the ratio chart
     */
    public class RatioChart {

        /**
         * The jFree plot
         */
        private XYPlot currentPlot = new XYPlot();

        /**
         * Constructor
         * @param backGroundValues  the background values
         */
        public RatioChart(double[][] backGroundValues) {

            NumberAxis xAxis = new NumberAxis("Quality");
            NumberAxis nProtAxis = new NumberAxis("#Ratios");
            NumberAxis protAxis = new NumberAxis("Selected Peptide");
            nProtAxis.setAutoRangeIncludesZero(true);
            protAxis.setAutoRangeIncludesZero(true);
            currentPlot.setDomainAxis(xAxis);
            currentPlot.setRangeAxis(0, nProtAxis);
            currentPlot.setRangeAxis(1, protAxis);
            currentPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);
            currentPlot.setRangeAxisLocation(1, AxisLocation.BOTTOM_OR_RIGHT);

            DefaultIntervalXYDataset backGround = new DefaultIntervalXYDataset();
            backGround.addSeries("All Peptides", backGroundValues);
            XYBarRenderer backgroundRenderer = new XYBarRenderer();
            backgroundRenderer.setShadowVisible(false);
            backgroundRenderer.setSeriesPaint(0, allPeptidesColor);
            backgroundRenderer.setMargin(0.2);
            backgroundRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

            currentPlot.setDataset(1000, backGround);
            currentPlot.setRenderer(1000, backgroundRenderer);
            currentPlot.mapDatasetToRangeAxis(1000, 0);
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
         * sets the peptide to display
         *
         * @param peptideQuantification the corresponding peptide quantification
         */
        public void setPeptide(PeptideQuantification peptideQuantification) {
            Double currentScore;
            IgnoredRatios ignoredRatios = (IgnoredRatios) peptideQuantification.getUrParam(new IgnoredRatios());
            ItraqScore itraqScore = (ItraqScore) peptideQuantification.getUrParam(new ItraqScore());
            for (int ion : peptideQuantification.getRatios().keySet()) {
                if (!ignoredRatios.isIgnored(ion)) {
                    currentScore = itraqScore.getScore(ion);
                    if (currentScore != null) {
                        double[] score = {currentScore};
                        double[] scoreHeight = {1};
                        double[][] scoreValues = {score, score, score, scoreHeight, scoreHeight, scoreHeight};
                        DefaultIntervalXYDataset scoreDataset = new DefaultIntervalXYDataset();
                        scoreDataset.addSeries(peptideQuantification.getRatios().get(ion).getMeasureSample() + " / "
                                + peptideQuantification.getRatios().get(ion).getControlSample(), scoreValues);
                        XYBarRenderer scoreRenderer = new XYBarRenderer();
                        scoreRenderer.setShadowVisible(false);
                        scoreRenderer.setBaseToolTipGenerator(new StandardXYToolTipGenerator());

                        currentPlot.setDataset(ion, scoreDataset);
                        currentPlot.setRenderer(ion, scoreRenderer);
                        currentPlot.mapDatasetToRangeAxis(ion, 1);
                    } else {
                        currentPlot.setDataset(ion, null);
                    }
                }
            }
        }
    }
}
