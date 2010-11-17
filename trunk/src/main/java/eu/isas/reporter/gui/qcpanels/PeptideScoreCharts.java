package eu.isas.reporter.gui.qcpanels;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.ProteinQuantification;
import eu.isas.reporter.compomicsutilitiessettings.IgnoredRatios;
import eu.isas.reporter.compomicsutilitiessettings.ItraqScore;
import java.awt.Color;
import java.util.ArrayList;
import javax.swing.JPanel;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.AxisLocation;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.plot.XYPlot;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.xy.DefaultIntervalXYDataset;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class PeptideScoreCharts {

    private final double resolution = 0.1;
    private ReporterIonQuantification quantification;
    private RatioChart ratioCharts;

    /**
     * @TODO: JavaDoc missing
     *
     * @param quantification
     */
    public PeptideScoreCharts(ReporterIonQuantification quantification) {
        this.quantification = quantification;
        ArrayList<ReporterIon> reporterIons = quantification.getReporterMethod().getReporterIons();

        createRatioCharts();
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @return
     */
    public JPanel getChart() {
        return new ChartPanel(new JFreeChart("Peptide Quantification Quality", ratioCharts.getPlot()));
    }

    /**
     * @TODO: JavaDoc missing
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
     * @TODO: JavaDoc missing
     *
     * @param peptideQuantification
     */
    public void setPeptide(PeptideQuantification peptideQuantification) {
        ratioCharts.setPeptide(peptideQuantification);
    }

    /**
     * @TODO: JavaDoc missing
     */
    public class RatioChart {

        private XYPlot currentPlot = new XYPlot();

        public RatioChart(double[][] backGroundValues) {

            NumberAxis xAxis = new NumberAxis("Quality");
            NumberAxis nProtAxis = new NumberAxis("Number of Ratios");
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
            backgroundRenderer.setSeriesPaint(0, Color.gray);
            backgroundRenderer.setMargin(0.2);

            currentPlot.setDataset(1000, backGround);
            currentPlot.setRenderer(1000, backgroundRenderer);
            currentPlot.mapDatasetToRangeAxis(1000, 0);
        }

        /**
         * @TODO: JavaDoc missing
         *
         * @return
         */
        public XYPlot getPlot() {
            return currentPlot;
        }

        /**
         * @TODO: JavaDoc missing
         *
         * @param peptideQuantification
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
                        scoreDataset.addSeries(peptideQuantification.getRatios().get(ion).getMeasureSample() + "/"
                                + peptideQuantification.getRatios().get(ion).getControlSample(), scoreValues);
                        XYBarRenderer scoreRenderer = new XYBarRenderer();
                        scoreRenderer.setShadowVisible(false);

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
