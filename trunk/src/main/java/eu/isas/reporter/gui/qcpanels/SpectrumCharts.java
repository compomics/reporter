package eu.isas.reporter.gui.qcpanels;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.PeptideQuantification;
import com.compomics.util.experiment.quantification.reporterion.quantification.SpectrumQuantification;
import eu.isas.reporter.compomicsutilitiessettings.IgnoredRatios;
import eu.isas.reporter.compomicsutilitiessettings.RatioLimits;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
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
public class SpectrumCharts {

    private PeptideQuantification peptideQuantification;
    private int proteinIndex;
    private int peptideIndex;
    private double resolution;
    private ReporterIonQuantification quantification;
    private HashMap<Integer, RatioChart> ratioCharts = new HashMap<Integer, RatioChart>();

    /**
     * @TODO: JavaDoc missing
     *
     * @param proteinIndex
     * @param peptideIndex
     * @param quantification
     * @param resolution
     */
    public SpectrumCharts(int proteinIndex, int peptideIndex, ReporterIonQuantification quantification, double resolution) {
        this.proteinIndex = proteinIndex;
        this.peptideIndex = peptideIndex;
        this.quantification = quantification;
        ArrayList<ReporterIon> reporterIons = quantification.getReporterMethod().getReporterIons();
        this.peptideQuantification = quantification.getProteinQuantification().get(proteinIndex).getPeptideQuantification().get(peptideIndex);
        this.resolution = resolution;

        createReporterPanels(peptideQuantification);
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param ion
     * @return
     */
    public JPanel getChart(int ion) {
        return new ChartPanel(new JFreeChart(quantification.getSample(ion).getReference() + "/"
                + quantification.getSample(quantification.getReferenceLabel()).getReference(), ratioCharts.get(ion).getPlot()));
    }

    /**
     * @TODO: JavaDoc missing
     * 
     * @param peptideQuantification
     */
    private void createReporterPanels(PeptideQuantification peptideQuantification) {

        IgnoredRatios ignoredRatios = new IgnoredRatios();

        ArrayList<ReporterIon> ions = quantification.getReporterMethod().getReporterIons();

        HashMap<Integer, Double> minima = new HashMap<Integer, Double>();
        HashMap<Integer, Double> maxima = new HashMap<Integer, Double>();

        double ratio;
        for (SpectrumQuantification spectrumQuantification : peptideQuantification.getSpectrumQuantification()) {
            ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
            for (int ion : spectrumQuantification.getRatios().keySet()) {
                if (!ignoredRatios.isIgnored(ion)) {
                    ratio = spectrumQuantification.getRatios().get(ion).getRatio();
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
            for (SpectrumQuantification spectrumQuantification : peptideQuantification.getSpectrumQuantification()) {
                ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
                for (int ion : spectrumQuantification.getRatios().keySet()) {
                    if (!ignoredRatios.isIgnored(ion)) {
                        ratio = spectrumQuantification.getRatios().get(ion).getRatio();
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
            newChart.addPeptideRatio(peptideQuantification, ion.getIndex());
            ratioCharts.put(ion.getIndex(), newChart);
        }
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param spectrumQuantification
     */
    public void setSpectrum(SpectrumQuantification spectrumQuantification) {
        for (int ion : ratioCharts.keySet()) {
            ratioCharts.get(ion).setSpectrum(spectrumQuantification, ion);
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
     * Returns the peptide index.
     *
     * @return the peptide index
     */
    public int getPeptideIndex() {
        return peptideIndex;
    }

    /**
     * @TODO: JavaDoc missing
     */
    private class RatioChart {

        private XYPlot currentPlot = new XYPlot();

        public RatioChart(double[][] backGroundValues) {

            NumberAxis xAxis = new NumberAxis("Ratio");
            NumberAxis nSpectraAxis = new NumberAxis("#Spectra");
            nSpectraAxis.setAutoRangeIncludesZero(true);
            currentPlot.setDomainAxis(xAxis);
            currentPlot.setRangeAxis(0, nSpectraAxis);
            currentPlot.setRangeAxisLocation(0, AxisLocation.TOP_OR_LEFT);

            DefaultIntervalXYDataset backGround = new DefaultIntervalXYDataset();
            backGround.addSeries("#Peptides", backGroundValues);
            XYBarRenderer backgroundRenderer = new XYBarRenderer();
            backgroundRenderer.setShadowVisible(false);
            backgroundRenderer.setSeriesPaint(0, Color.gray);
            backgroundRenderer.setMargin(0.2);

            currentPlot.setDataset(3, backGround);
            currentPlot.setRenderer(3, backgroundRenderer);
            currentPlot.mapDatasetToRangeAxis(3, 0);
        }

        /*
         * @TODO: JavaDoc missing
         */
        public XYPlot getPlot() {
            return currentPlot;
        }

        /**
         * @TODO: JavaDoc missing
         *
         * @param peptideQuantification
         * @param ion
         */
        public void addPeptideRatio(PeptideQuantification peptideQuantification, int ion) {
            RatioLimits ratioLimits = (RatioLimits) peptideQuantification.getUrParam(new RatioLimits());
            int nPeptides;
            IgnoredRatios ignoredRatios = new IgnoredRatios();
            nPeptides = 0;
            for (SpectrumQuantification spectrumQuantification : peptideQuantification.getSpectrumQuantification()) {
                ignoredRatios = (IgnoredRatios) spectrumQuantification.getUrParam(ignoredRatios);
                if (!ignoredRatios.isIgnored(ion)) {
                    if (spectrumQuantification.getRatios().get(ion).getRatio() > ratioLimits.getLimits(ion)[0]
                            && spectrumQuantification.getRatios().get(ion).getRatio() < ratioLimits.getLimits(ion)[1]) {
                        nPeptides++;
                    }
                }
            }

            double[] spreadBegin = {ratioLimits.getLimits(ion)[0]};
            double[] spreadEnd = {ratioLimits.getLimits(ion)[1]};
            double[] ratio = {peptideQuantification.getRatios().get(ion).getRatio()};
            double[] ratioBegin = {ratio[0] - resolution / 2};
            double[] ratioEnd = {ratio[0] + resolution / 2};
            double[] spreadHeight = {(double) nPeptides / 10};
            double[] peptides = {nPeptides};

            double[][] spreadData = new double[][]{ratio, spreadBegin, spreadEnd, spreadHeight, spreadHeight, spreadHeight};
            double[][] ratioData = new double[][]{ratio, ratioBegin, ratioEnd, peptides, peptides, peptides};

            DefaultIntervalXYDataset spread = new DefaultIntervalXYDataset();
            spread.addSeries("Spectra Spread", spreadData);
            XYBarRenderer spreadRenderer = new XYBarRenderer();
            spreadRenderer.setShadowVisible(false);
            Color spreadColor = new Color(0, 0, 150, 75);
            spreadRenderer.setSeriesPaint(0, spreadColor);

            currentPlot.setDataset(2, spread);
            currentPlot.setRenderer(2, spreadRenderer);
            currentPlot.mapDatasetToRangeAxis(2, 0);

            DefaultIntervalXYDataset ratioDataset = new DefaultIntervalXYDataset();
            ratioDataset.addSeries("Peptide Ratio", ratioData);
            XYBarRenderer ratioRenderer = new XYBarRenderer();
            ratioRenderer.setShadowVisible(false);
            Color ratioColor = new Color(0, 0, 255, 255);
            ratioRenderer.setSeriesPaint(0, ratioColor);

            currentPlot.setDataset(1, ratioDataset);
            currentPlot.setRenderer(1, ratioRenderer);
            currentPlot.mapDatasetToRangeAxis(1, 0);
        }

        /**
         * @TODO: JavaDoc missing
         * 
         * @param spectrumQuantification
         * @param ion
         */
        public void setSpectrum(SpectrumQuantification spectrumQuantification, int ion) {

            double[] ratio = {spectrumQuantification.getRatios().get(ion).getRatio()};
            double[] ratioBegin = {ratio[0] - resolution / 2};
            double[] ratioEnd = {ratio[0] + resolution / 2};
            double[] height = {1.0};

            double[][] ratioData = new double[][]{ratio, ratioBegin, ratioEnd, height, height, height};

            DefaultIntervalXYDataset ratioDataset = new DefaultIntervalXYDataset();
            ratioDataset.addSeries("Spectrum Ratio", ratioData);
            XYBarRenderer ratioRenderer = new XYBarRenderer();
            ratioRenderer.setShadowVisible(false);
            Color ratioColor = new Color(255, 0, 0, 255);
            ratioRenderer.setSeriesPaint(0, ratioColor);

            currentPlot.setDataset(0, ratioDataset);
            currentPlot.setRenderer(0, ratioRenderer);
            currentPlot.mapDatasetToRangeAxis(0, 0);
        }
    }
}
