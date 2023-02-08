package eu.isas.reporter.gui.resultpanels;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.biology.genes.GeneMaps;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.features.IdentificationFeaturesGenerator;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.validation.MatchValidationLevel;
import com.compomics.util.gui.genes.GeneDetailsDialog;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.TableProperties;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.math.clustering.KMeansClustering;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.reporter.calculation.clustering.ClusterBuilder;
import eu.isas.reporter.gui.ReporterGUI;
import eu.isas.reporter.gui.tablemodels.PeptideTableModel;
import eu.isas.reporter.gui.tablemodels.ProteinTableModel;
import eu.isas.reporter.gui.tablemodels.PsmTableModel;
import eu.isas.reporter.settings.ClusteringSettings;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.IntStream;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import no.uib.jsparklines.renderers.JSparklinesArrayListBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesBarChartTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesHeatMapTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerColorTableCellRenderer;
import no.uib.jsparklines.renderers.JSparklinesIntegerIconTableCellRenderer;
import no.uib.jsparklines.renderers.util.GradientColorCoding;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.CategoryItemRenderer;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.chart.renderer.xy.StandardXYBarPainter;
import org.jfree.chart.renderer.xy.XYBarRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

/**
 * The Overview tab.
 *
 * @author Harald Barsnes
 * @author Marc Vaudel
 */
public class OverviewPanel extends javax.swing.JPanel {

    /**
     * Turns of the gradient painting for the bar charts.
     */
    static {
        XYBarRenderer.setDefaultBarPainter(new StandardXYBarPainter());
    }

    /**
     * The protein table column header tooltips.
     */
    private ArrayList<String> proteinTableToolTips;
    /**
     * The main GUI class.
     */
    private ReporterGUI reporterGUI;
    /**
     * Utilities Identification containing the identification objects.
     */
    private Identification identification;
    /**
     * The gene maps.
     */
    private GeneMaps geneMaps;
    /**
     * PeptideShaker identification features generator.
     */
    private IdentificationFeaturesGenerator identificationFeaturesGenerator;
    /**
     * A simple progress dialog.
     */
    private ProgressDialogX progressDialog;
    /**
     * A list of proteins in the protein table.
     */
    private long[] proteinKeys;
    /**
     * A list of peptides in the peptide table.
     */
    private long[] peptideKeys;
    /**
     * A list of PSMs in the PSM table.
     */
    private long[] psmKeys;
    /**
     * The default line width for the line plots.
     */
    public static final float LINE_WIDTH = 4;
    /**
     * The maximum number of elements in a legend.
     */
    private int maxLegendSize = 20;
    /**
     * The current line chart chart panel.
     */
    private ChartPanel lineChartChartPanel;
    /**
     * The current chart panel where the maximize icon is showing.
     */
    private ChartPanel maximizeIconChartPanel;
    /**
     * The currently selected chart panel.
     */
    private ChartPanel selectedChartPanel = null;
    /**
     * The color used for the not selected protein profiles.
     */
    private Color notSelectedProteinProfileColor = new Color(200, 200, 200, 100);
    /**
     * The complete list of ordered protein keys.
     */
    private long[] allOrderedProteinKeys;
    /**
     * True of a single chart is maximized.
     */
    private boolean chartMaximized = false;
    /**
     * List with all the chart panels.
     */
    private ArrayList<ChartPanel> allChartPanels = new ArrayList<>();
    /**
     * Static index for the ID software agreement: no psm found.
     */
    public static final int NO_ID = 0; // @TODO: these should be moved to utilities..?
    /**
     * Static index for the ID software agreement: the ID software have
     * different top ranking peptides.
     */
    public static final int CONFLICT = 1;
    /**
     * Static index for the ID software agreement: one or more of the softwares
     * did not identify the spectrum, while one or more of the others did.
     */
    public static final int PARTIALLY_MISSING = 2;
    /**
     * Static index for the ID software agreement: the ID softwares all have the
     * same top ranking peptide without accounting for modification
     * localization.
     */
    public static final int AGREEMENT = 3;
    /**
     * Static index for the ID software agreement: the ID softwares all have the
     * same top ranking peptide.
     */
    public static final int AGREEMENT_WITH_MODS = 4;

    /**
     * Creates a new OverviewPanel.
     *
     * @param reporterGUI the ReporterGUI parent
     */
    public OverviewPanel(ReporterGUI reporterGUI) {

        initComponents();
        this.reporterGUI = reporterGUI;
        setUpGui();
        formComponentResized(null);

    }

    /**
     * Sets up the GUI components.
     */
    private void setUpGui() {

        maximizeChartJButton.setVisible(false);

        // set main table properties
        proteinTable.getTableHeader().setReorderingAllowed(false);
        peptideTable.getTableHeader().setReorderingAllowed(false);
        psmTable.getTableHeader().setReorderingAllowed(false);

        // correct the color for the upper right corner
        JPanel proteinCorner = new JPanel();
        proteinCorner.setBackground(proteinTable.getTableHeader().getBackground());
        proteinScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, proteinCorner);
        JPanel peptideCorner = new JPanel();
        peptideCorner.setBackground(proteinTable.getTableHeader().getBackground());
        peptideScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, peptideCorner);
        JPanel psmCorner = new JPanel();
        psmCorner.setBackground(proteinTable.getTableHeader().getBackground());
        psmScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, psmCorner);

        // add table sorting listeners
        SelfUpdatingTableModel.addSortListener(proteinTable, new ProgressDialogX(reporterGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true));

        SelfUpdatingTableModel.addSortListener(peptideTable, new ProgressDialogX(reporterGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true));

        SelfUpdatingTableModel.addSortListener(psmTable, new ProgressDialogX(reporterGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true));

        // add table scrolling listeners
        SelfUpdatingTableModel.addScrollListeners(proteinTable, proteinScrollPane, proteinScrollPane.getVerticalScrollBar());
        SelfUpdatingTableModel.addScrollListeners(peptideTable, peptideScrollPane, peptideScrollPane.getVerticalScrollBar());
        SelfUpdatingTableModel.addScrollListeners(psmTable, psmScrollPane, psmScrollPane.getVerticalScrollBar());

        // make sure that the scroll panes are see-through
        proteinScrollPane.getViewport().setOpaque(false);
        peptideScrollPane.getViewport().setOpaque(false);
        psmScrollPane.getViewport().setOpaque(false);

        setUpTableHeaderToolTips();
    }

    /**
     * Updates the display with the underlying data.
     */
    public void updateDisplay() {

        minimizeChart();

        identification = reporterGUI.getIdentification();
        geneMaps = reporterGUI.getGeneMaps();
        identificationFeaturesGenerator = reporterGUI.getIdentificationFeaturesGenerator();
        allChartPanels.clear();
        selectedChartPanel = null;

        numberOfClustersMenuItem.setEnabled(reporterGUI.getkMeansClutering() != null);

        progressDialog = new ProgressDialogX(
                reporterGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true
        );

        progressDialog.setPrimaryProgressCounterIndeterminate(true);
        progressDialog.setTitle("Loading Overview. Please Wait...");

        new Thread(new Runnable() {
            public void run() {

                try {
                    progressDialog.setVisible(true);
                } catch (IndexOutOfBoundsException e) {
                    // ignore
                }

            }
        }, "ProgressDialog").start();

        new Thread("DisplayThread") {
            @Override
            public void run() {

                try {
                    progressDialog.setPrimaryProgressCounterIndeterminate(true);
                    progressDialog.setTitle("Preparing Overview. Please Wait...");

                    reporterGUI.getIdentificationFeaturesGenerator().setProteinKeys(reporterGUI.getMetrics().getProteinKeys());
                    proteinKeys = reporterGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(progressDialog, reporterGUI.getFilterParameters());
                    allOrderedProteinKeys = reporterGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(progressDialog, reporterGUI.getFilterParameters());

                    // display the clusters
                    displayClusters(progressDialog);

                    // clear the tables
                    ArrayList<Long> emptySelection = new ArrayList<>();
                    reporterGUI.setSelectedProteins(emptySelection, false, false);
                    reporterGUI.setSelectedPeptides(emptySelection, false, false);
                    reporterGUI.setSelectedPsms(emptySelection, false, false);
                    proteinTable.clearSelection();
                    peptideTable.clearSelection();
                    psmTable.clearSelection();
                    proteinKeys = null;
                    peptideKeys = null;
                    psmKeys = null;

                    ((ProteinTableModel) proteinTable.getModel()).reset();
                    ((PeptideTableModel) peptideTable.getModel()).reset();
                    ((PsmTableModel) psmTable.getModel()).reset();

                    setProteinTableProperties();
                    setPeptideTableProperties();
                    setPsmTableProperties();

                    ((DefaultTableModel) proteinTable.getModel()).fireTableDataChanged();
                    updateProteinTableCellRenderers();
                    ((DefaultTableModel) peptideTable.getModel()).fireTableDataChanged();
                    updatePeptideTableCellRenderers();
                    ((DefaultTableModel) psmTable.getModel()).fireTableDataChanged();
                    updatePsmTableCellRenderers();

                    // update the border title
                    String title = ReporterGUI.TITLED_BORDER_HORIZONTAL_PADDING
                            + "Proteins (" + proteinTable.getRowCount() + "), "
                            + "Peptides (" + peptideTable.getRowCount() + "), "
                            + "PSMs (" + psmTable.getRowCount() + ")";

                    ((TitledBorder) proteinPeptidePsmLayeredPanel.getBorder()).setTitle(title);
                    proteinPeptidePsmLayeredPanel.repaint();

                    if (reporterGUI.getIdentificationDisplayPreferences().showScores()) {
                        proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Score");
                    } else {
                        proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Confidence");
                    }

                    // enable the contextual export options
                    exportProteinsJButton.setEnabled(true);
                    ratioPlotOptionsJButton.setEnabled(true);

                    reporterGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    progressDialog.setRunFinished();

                } catch (Exception e) {

                    reporterGUI.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
                    reporterGUI.catchException(e);
                    progressDialog.setRunFinished();

                }
            }
        }.start();
    }

    /**
     * Display the clusters.
     *
     * @param waitingHandler the waiting handler
     */
    private void displayClusters(WaitingHandler waitingHandler) {

        plotPanel.removeAll();

        ArrayList<String> sampleIndexes = new ArrayList<>(reporterGUI.getReporterIonQuantification().getSampleIndexes());
        Collections.sort(sampleIndexes);
        ArrayList<String> reagentsOrder = reporterGUI.getDisplayParameters().getReagents();
        KMeansClustering kMeansClustering = reporterGUI.getkMeansClutering();

        if (kMeansClustering != null) {

            for (int clusterIndex = 0; clusterIndex < kMeansClustering.getNumberOfClusters() && !waitingHandler.isRunCanceled(); clusterIndex++) {

                DefaultCategoryDataset chartDataset = new DefaultCategoryDataset();
                ArrayList<String> clusterMembers = kMeansClustering.getClusterMembers(clusterIndex);
                HashMap<String, ArrayList<Double>> allValues = kMeansClustering.getClusterMembersData(clusterIndex);

                for (String tempKey : clusterMembers) {

                    if (waitingHandler.isRunCanceled()) {
                        break;
                    }

                    ArrayList<Double> tempValues = allValues.get(tempKey);

                    for (String tempReagent : reagentsOrder) {

                        int sampleIndex = sampleIndexes.indexOf(tempReagent);

                        chartDataset.addValue(
                                tempValues.get(sampleIndex),
                                tempKey,
                                reporterGUI.getReporterIonQuantification().getSample(sampleIndexes.get(sampleIndex))
                        );

                    }
                }

                addClusterChart(chartDataset);
            }
        }
    }

    /**
     * Add a cluster line chart.
     *
     * @param dataset the dataset
     */
    private void addClusterChart(DefaultCategoryDataset dataset) {

        JFreeChart chart = ChartFactory.createLineChart(
                null, // chart title
                null, // domain axis label
                null, // range axis label
                dataset, // data
                PlotOrientation.VERTICAL, // the plot orientation
                false, // include legend
                true, // tooltips
                false // urls
        );

        // set the background and gridline colors
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // set the renderer
        ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();
        ClusteringSettings clusteringSettings = reporterGUI.getDisplayParameters().getClusteringSettings();
        CategoryItemRenderer renderer = new LineAndShapeRenderer(true, false);

        for (int i = 0; i < dataset.getRowCount(); i++) {

            Long key = Long.valueOf((String) dataset.getRowKey(i));
            boolean isProtein = clusterBuilder.getProteinIndex(key) != null;
            boolean isPeptide = clusterBuilder.getPeptideIndex(key) != null;
            boolean isPsm = clusterBuilder.getPsmIndex(key) != null;

            if (isProtein) {

                ArrayList<String> classes = clusterBuilder.getProteinClasses(key);
                String clusterClass = classes.get(0);  // @TODO: what if present in different classes?
                Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                renderer.setSeriesPaint(i, nonSelectedColor);

            } else if (isPeptide) {

                ArrayList<String> classes = clusterBuilder.getPeptideClasses(key);
                String clusterClass = classes.get(0);  // @TODO: what if present in different classes?
                Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                renderer.setSeriesPaint(i, nonSelectedColor);

            } else if (isPsm) {

                ArrayList<String> classes = clusterBuilder.getPsmClasses(key);
                String clusterClass = classes.get(0);  // @TODO: what if present in different classes?
                Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                renderer.setSeriesPaint(i, nonSelectedColor);

            } else {

                throw new IllegalArgumentException("No match found for key " + key + ".");

            }

            renderer.setSeriesStroke(
                    i,
                    new BasicStroke(
                            LINE_WIDTH,
                            BasicStroke.CAP_ROUND,
                            BasicStroke.JOIN_ROUND
                    )
            );
        }

        renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
        plot.setRenderer(renderer);

        // change the margin at the top and bottom of the range axis
        final ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLowerMargin(0.15);
        rangeAxis.setUpperMargin(0.15);

        // make sure that the chart has a symmetrical y-axis
        Double amplitude = clusterBuilder.getRatioAmplitude();
        rangeAxis.setUpperBound(amplitude);
        rangeAxis.setLowerBound(-amplitude);

        // create the chart panel
        lineChartChartPanel = new ChartPanel(chart, false);

        // set component name
        lineChartChartPanel.setName("" + plotPanel.getComponentCount());

        // remove the zoom support and pop up menu
        lineChartChartPanel.setRangeZoomable(false);
        lineChartChartPanel.setPopupMenu(null);

        // hide unwanted chart details
        plot.setOutlineVisible(false);
        plot.getDomainAxis().setVisible(false);
        plot.setDomainGridlinesVisible(false);

        // make the background see-through
        chart.setBackgroundPaint(Color.WHITE);
        plot.setBackgroundPaint(Color.WHITE);
        lineChartChartPanel.setBackground(Color.WHITE);

        lineChartChartPanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent cme) {

                if (cme.getTrigger().getButton() == MouseEvent.BUTTON1
                        && cme.getTrigger().getClickCount() == 2) {
                    maximizeChartJButtonActionPerformed(null);
                }

            }

            @Override
            public void chartMouseMoved(ChartMouseEvent cme) {

                ChartPanel chartPanelParent = (ChartPanel) cme.getTrigger().getComponent();
                int x = chartPanelParent.getX() + chartPanelParent.getWidth() - 5;
                int y = chartPanelParent.getY() + 38;

                maximizeChartJButton.setLocation(x, y);
                maximizeChartJButton.setVisible(true);

                maximizeIconChartPanel = chartPanelParent;

            }
        });

        lineChartChartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {

                Component c = e.getComponent();

                if (c instanceof ChartPanel) {

                    boolean chartAlreadySelected = false;

                    if (selectedChartPanel != null) {
                        chartAlreadySelected = selectedChartPanel.getName().equalsIgnoreCase(c.getName());
                    }

                    if (!chartAlreadySelected) {
                        reporterGUI.setSelectedProteins(new ArrayList<Long>(), true, true);
                        selectCluster((ChartPanel) c, true);
                    }

                }

            }
        });

        allChartPanels.add(lineChartChartPanel);

        plotPanel.add(lineChartChartPanel);
        plotPanel.validate();
    }

    /**
     * Selected the given cluster.
     *
     * @param chartPanel the chart panel containing the cluster
     * @param resetSelection reset the selection
     */
    private void selectCluster(ChartPanel chartPanel, boolean resetSelection) {

        if (selectedChartPanel != null) {
            selectedChartPanel.setBorder(null);

            // reset the selection
            if (resetSelection) {

                CategoryItemRenderer renderer = selectedChartPanel.getChart().getCategoryPlot().getRenderer();
                DefaultCategoryDataset dataset = (DefaultCategoryDataset) selectedChartPanel.getChart().getCategoryPlot().getDataset();
                ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();
                ClusteringSettings clusteringSettings = reporterGUI.getDisplayParameters().getClusteringSettings();

                for (int i = 0; i < dataset.getRowCount(); i++) {

                    Long key = Long.valueOf((String) dataset.getRowKey(i));
                    boolean isProtein = clusterBuilder.getProteinIndex(key) != null;
                    boolean isPeptide = clusterBuilder.getPeptideIndex(key) != null;
                    boolean isPsm = clusterBuilder.getPsmIndex(key) != null;

                    if (isProtein) {

                        ArrayList<String> classes = clusterBuilder.getProteinClasses(key);
                        String clusterClass = classes.get(0);  // @TODO: what if present in different classes?
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);

                    } else if (isPeptide) {

                        ArrayList<String> classes = clusterBuilder.getPeptideClasses(key);
                        String clusterClass = classes.get(0);  // @TODO: what if present in different classes?
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);

                    } else if (isPsm) {

                        ArrayList<String> classes = clusterBuilder.getPsmClasses(key);
                        String clusterClass = classes.get(0);  // @TODO: what if present in different classes?
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);

                    } else {
                        throw new IllegalArgumentException("No match found for key " + key + ".");
                    }
                }
            }

            selectedChartPanel.getChart().fireChartChanged();
        }

        chartPanel.setBorder(new LineBorder(Color.DARK_GRAY));
        chartPanel.getChart().fireChartChanged();

        selectedChartPanel = chartPanel;

        if (resetSelection) {

            // get the keys of the matches in the selected cluster
            DefaultCategoryDataset dataset = (DefaultCategoryDataset) chartPanel.getChart().getCategoryPlot().getDataset();
            List<String> rowKeys = dataset.getRowKeys();
            int size = Math.max(32, rowKeys.size() / 3);
            HashMap<Integer, Long> psmKeysMap = new HashMap<>(size);
            HashMap<Integer, Long> peptideKeysMap = new HashMap<>(size);
            HashMap<Integer, Long> proteinKeysMap = new HashMap<>(size);
            ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();

            for (String rowKey : rowKeys) {

                long rowKeyAsLong = Long.parseLong(rowKey);

                Integer proteinIndex = clusterBuilder.getProteinIndex(rowKeyAsLong);

                if (proteinIndex != null) {
                    proteinKeysMap.put(proteinIndex, rowKeyAsLong);
                }

                Integer peptideIndex = clusterBuilder.getPeptideIndex(rowKeyAsLong);

                if (peptideIndex != null) {
                    peptideKeysMap.put(peptideIndex, rowKeyAsLong);
                }

                Integer psmIndex = clusterBuilder.getPsmIndex(rowKeyAsLong);

                if (psmIndex != null) {
                    psmKeysMap.put(psmIndex, rowKeyAsLong);
                }

                if (proteinIndex == null && peptideIndex == null && psmIndex == null) {
                    throw new IllegalArgumentException("Key " + rowKey + " not found.");
                }

            }

            // get tables contents
            ArrayList<Integer> indexes = new ArrayList<>(proteinKeysMap.keySet());
            Collections.sort(indexes);
            proteinKeys = new long[proteinKeysMap.size()];

            for (int i = 0; i < indexes.size(); i++) {
                long accession = proteinKeysMap.get(indexes.get(i));
                proteinKeys[i] = accession;
            }

            indexes = new ArrayList<Integer>(peptideKeysMap.keySet());
            Collections.sort(indexes);
            peptideKeys = new long[peptideKeysMap.size()];

            for (int i = 0; i < indexes.size(); i++) {
                long accession = peptideKeysMap.get(indexes.get(i));
                peptideKeys[i] = accession;
            }

            indexes = new ArrayList<Integer>(psmKeysMap.keySet());
            Collections.sort(indexes);
            psmKeys = new long[psmKeysMap.size()];

            for (int i = 0; i < indexes.size(); i++) {
                long accession = psmKeysMap.get(indexes.get(i));
                psmKeys[i] = accession;
            }

            // update the tables and panels
            updateProteinTable();
            updatePeptideTable();
            updatePsmTable();

            // update the border title
            String title = ReporterGUI.TITLED_BORDER_HORIZONTAL_PADDING
                    + "Proteins (" + proteinTable.getRowCount() + "), "
                    + "Peptides (" + peptideTable.getRowCount() + "), "
                    + "PSMs (" + psmTable.getRowCount() + ")";

            ((TitledBorder) proteinPeptidePsmLayeredPanel.getBorder()).setTitle(title);
            proteinPeptidePsmLayeredPanel.repaint();

        }
    }

    /**
     * Updates the protein table.
     */
    private void updateProteinTable() {

        if (proteinTable.getModel() instanceof ProteinTableModel
                && ((ProteinTableModel) proteinTable.getModel()).isInstantiated()) {

            ((ProteinTableModel) proteinTable.getModel()).updateDataModel(
                    identification,
                    identificationFeaturesGenerator,
                    reporterGUI.getProteinDetailsProvider(),
                    reporterGUI.getSequenceProvider(),
                    reporterGUI.getSpectrumProvider(),
                    geneMaps,
                    reporterGUI.getReporterIonQuantification(),
                    reporterGUI.getQuantificationFeaturesGenerator(),
                    reporterGUI.getDisplayFeaturesGenerator(),
                    reporterGUI.getDisplayParameters(), proteinKeys
            );

        } else {

            ProteinTableModel proteinTableModel = new ProteinTableModel(
                    identification,
                    identificationFeaturesGenerator,
                    reporterGUI.getProteinDetailsProvider(),
                    reporterGUI.getSequenceProvider(),
                    geneMaps,
                    reporterGUI.getReporterIonQuantification(),
                    reporterGUI.getQuantificationFeaturesGenerator(),
                    reporterGUI.getDisplayFeaturesGenerator(),
                    reporterGUI.getDisplayParameters(),
                    reporterGUI.getExceptionHandler(),
                    proteinKeys
            );

            proteinTable.setModel(proteinTableModel);

        }

        setProteinTableProperties();

        ((DefaultTableModel) proteinTable.getModel()).fireTableDataChanged();
        updateProteinTableCellRenderers();
    }

    /**
     * Updates the peptide table.
     */
    private void updatePeptideTable() {

        if (peptideTable.getModel() instanceof PeptideTableModel && ((PeptideTableModel) peptideTable.getModel()).isInstantiated()) {

            ((PeptideTableModel) peptideTable.getModel()).updateDataModel(
                    identification,
                    identificationFeaturesGenerator,
                    reporterGUI.getReporterIonQuantification(),
                    reporterGUI.getQuantificationFeaturesGenerator(),
                    reporterGUI.getDisplayFeaturesGenerator(),
                    reporterGUI.getDisplayParameters(),
                    reporterGUI.getIdentificationParameters(),
                    null, peptideKeys,
                    false
            );

        } else {

            PeptideTableModel peptideTableModel = new PeptideTableModel(
                    identification,
                    reporterGUI.getSpectrumProvider(),
                    identificationFeaturesGenerator,
                    reporterGUI.getReporterIonQuantification(),
                    reporterGUI.getQuantificationFeaturesGenerator(),
                    reporterGUI.getDisplayFeaturesGenerator(),
                    reporterGUI.getDisplayParameters(),
                    reporterGUI.getIdentificationParameters(),
                    null,
                    peptideKeys,
                    false,
                    reporterGUI.getExceptionHandler()
            );

            peptideTable.setModel(peptideTableModel);
        }

        setPeptideTableProperties();

        ((DefaultTableModel) peptideTable.getModel()).fireTableDataChanged();
        updatePeptideTableCellRenderers();
    }

    /**
     * Updates the PSM table.
     */
    private void updatePsmTable() {

        if (psmTable.getModel() instanceof PsmTableModel && ((PsmTableModel) psmTable.getModel()).isInstantiated()) {

            ((PsmTableModel) psmTable.getModel()).updateDataModel(
                    identification,
                    reporterGUI.getSpectrumProvider(),
                    reporterGUI.getDisplayFeaturesGenerator(),
                    reporterGUI.getDisplayParameters(),
                    reporterGUI.getIdentificationParameters(),
                    reporterGUI.getReporterIonQuantification(),
                    reporterGUI.getQuantificationFeaturesGenerator(),
                    psmKeys,
                    false
            );

        } else {

            PsmTableModel psmTableModel = new PsmTableModel(
                    identification,
                    reporterGUI.getSpectrumProvider(),
                    reporterGUI.getDisplayFeaturesGenerator(),
                    reporterGUI.getDisplayParameters(),
                    reporterGUI.getIdentificationParameters(),
                    reporterGUI.getReporterIonQuantification(),
                    reporterGUI.getQuantificationFeaturesGenerator(),
                    psmKeys,
                    false,
                    reporterGUI.getExceptionHandler()
            );

            psmTable.setModel(psmTableModel);
        }

        setPsmTableProperties();

        ((DefaultTableModel) psmTable.getModel()).fireTableDataChanged();
        updatePsmTableCellRenderers();
    }

    /**
     * Sets up the table header tooltips.
     */
    private void setUpTableHeaderToolTips() {

        proteinTableToolTips = new ArrayList<String>();
        proteinTableToolTips.add(null);
        proteinTableToolTips.add("Protein Quantification");
        proteinTableToolTips.add("Protein Inference Class");
        proteinTableToolTips.add("Protein Accession Number");
        proteinTableToolTips.add("Protein Description");
        proteinTableToolTips.add("Chromosome Number");
        proteinTableToolTips.add("Protein Sequence Coverage (%) (Confident / Doubtful / Not Validated / Possible)");
        proteinTableToolTips.add("Number of Peptides (Confident / Doubtful / Not Validated)");
        proteinTableToolTips.add("Number of Spectra (Confident / Doubtful / Not Validated)");
        proteinTableToolTips.add("Protein Molecular Weight (kDa)");

        if (reporterGUI.getIdentificationDisplayPreferences() != null && reporterGUI.getIdentificationDisplayPreferences().showScores()) {
            proteinTableToolTips.add("Protein Score");
        } else {
            proteinTableToolTips.add("Protein Confidence");
        }

        proteinTableToolTips.add("Validated");
    }

    /**
     * Set up the properties of the protein table.
     */
    private void setProteinTableProperties() {

        ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();
        Double amplitude = clusterBuilder.getRatioAmplitude();

        ProteinTableModel.setProteinTableProperties(
                proteinTable,
                reporterGUI.getSparklineColor(),
                reporterGUI.getSparklineColorNonValidated(),
                reporterGUI.getSparklineColorNotFound(),
                reporterGUI.getSparklineColorDoubtful(),
                reporterGUI.getScoreAndConfidenceDecimalFormat(),
                this.getClass(),
                reporterGUI.getMetrics().getMaxProteinAccessionLength(),
                amplitude
        );

        proteinTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        resetSelection();
                    }
                });

            }
        });
    }

    /**
     * Set up the properties of the peptide table.
     */
    private void setPeptideTableProperties() {

        // the index column
        peptideTable.getColumn(" ").setMaxWidth(50);
        peptideTable.getColumn(" ").setMinWidth(50);

        try {
            peptideTable.getColumn("Confidence").setMaxWidth(90);
            peptideTable.getColumn("Confidence").setMinWidth(90);
        } catch (IllegalArgumentException w) {
            peptideTable.getColumn("Score").setMaxWidth(90);
            peptideTable.getColumn("Score").setMinWidth(90);
        }

        // the validated column
        peptideTable.getColumn("").setMaxWidth(30);

        // the protein inference column
        peptideTable.getColumn("PI").setMaxWidth(37);
        peptideTable.getColumn("PI").setMinWidth(37);

        peptideTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        resetSelection();
                    }
                });
            }
        });
    }

    /**
     * Set up the properties of the PSM table.
     */
    private void setPsmTableProperties() {

        // the index column
        psmTable.getColumn(" ").setMaxWidth(50);
        psmTable.getColumn(" ").setMinWidth(50);

        try {
            psmTable.getColumn("Confidence").setMaxWidth(90);
            psmTable.getColumn("Confidence").setMinWidth(90);
        } catch (IllegalArgumentException w) {
            psmTable.getColumn("Score").setMaxWidth(90);
            psmTable.getColumn("Score").setMinWidth(90);
        }

        // the validated column
        psmTable.getColumn("").setMaxWidth(30);
        psmTable.getColumn("").setMinWidth(30);

        // the protein inference column
        psmTable.getColumn("ID").setMaxWidth(37);
        psmTable.getColumn("ID").setMinWidth(37);

//        if (reporterGUI.getIdentificationParameters().getSearchParameters().isPrecursorAccuracyTypePpm()) { // @TODO: implement tooltips!
//            psmTableToolTips.set(psmTable.getColumn("m/z Error").getModelIndex(), "m/z Error (ppm)");
//        } else {
//            psmTableToolTips.set(psmTable.getColumn("m/z Error").getModelIndex(), "m/z Error (Da)");
//        }
        psmTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {

                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        resetSelection();
                    }
                });

            }
        });
    }

    /**
     * Reselect the proteins, peptides and PSMs.
     */
    public void resetSelection() {

        ArrayList<Long> selectedProteins = reporterGUI.getSelectedProteins();
        proteinTable.clearSelection();

        for (long tempProteinKey : selectedProteins) {

            int proteinRow = getProteinRow(tempProteinKey);

            if (proteinRow != -1) {
                proteinTable.addRowSelectionInterval(proteinRow, proteinRow);
            }

        }

        ArrayList<Long> selectedPeptides = reporterGUI.getSelectedPeptides();
        peptideTable.clearSelection();

        for (long tempPeptideKey : selectedPeptides) {

            int peptideRow = getPeptideRow(tempPeptideKey);

            if (peptideRow != -1) {
                peptideTable.addRowSelectionInterval(peptideRow, peptideRow);
            }

        }

        ArrayList<Long> selectedPsms = reporterGUI.getSelectedPsms();
        psmTable.clearSelection();

        for (long tempPsmKey : selectedPsms) {

            int psmRow = getPsmRow(tempPsmKey);

            if (psmRow != -1) {
                psmTable.addRowSelectionInterval(psmRow, psmRow);
            }

        }
    }

    /**
     * If a chart is maximized, then minimize it. If not maximized, do nothing.
     */
    public void minimizeChart() {

        if (chartMaximized) {
            maximizeChartJButtonActionPerformed(null);
        }

    }

    /**
     * Update the selection.
     *
     * @param clearSelection if true, the current selection will be removed
     */
    public void updateSelection(boolean clearSelection) {

        ArrayList<Long> selectedProteins = reporterGUI.getSelectedProteins();
        ArrayList<Long> selectedPeptides = reporterGUI.getSelectedPeptides();
        ArrayList<Long> selectedPsms = reporterGUI.getSelectedPsms();

        if (clearSelection) {
            proteinTable.clearSelection();
            peptideTable.clearSelection();
            psmTable.clearSelection();
        }

        // select the correct cluster
        if (selectedProteins.size() == 1) { // @TODO: how to handle combinations of protein, peptide and psm selections 

            int currentCluster = -1;

            for (int i = 0; i < reporterGUI.getkMeansClutering().getNumberOfClusters() && currentCluster == -1; i++) {
                if (reporterGUI.getkMeansClutering().getClusterMembers(i).contains(selectedProteins.get(0).toString())) {
                    currentCluster = i;
                }
            }

            if (currentCluster != -1) {
                ChartPanel currentChartPanel = (ChartPanel) plotPanel.getComponent(currentCluster);
                selectCluster(currentChartPanel, clearSelection); // @TODO: check!
            }
        }

        for (long tempProteinKey : selectedProteins) {

            int proteinRow = getProteinRow(tempProteinKey);

            if (proteinRow != -1) {
                proteinTable.addRowSelectionInterval(proteinRow, proteinRow);
            }

        }

        if (!selectedProteins.isEmpty()) {

            int proteinRow = getProteinRow(selectedProteins.get(0));

            if (proteinRow != -1) {
                proteinTable.scrollRectToVisible(proteinTable.getCellRect(proteinRow, 0, false));
            }

            proteinTableMouseReleased(null);
        }
    }

    /**
     * Returns the row of a desired protein.
     *
     * @param proteinKey the key of the protein
     * @return the row of the desired protein
     */
    private int getProteinRow(long proteinKey) {

        int modelIndex = IntStream.range(0, proteinKeys.length)
                .filter(i -> proteinKeys[i] == proteinKey)
                .findAny()
                .orElse(-1);

        return modelIndex == -1 ? -1 : ((SelfUpdatingTableModel) proteinTable.getModel()).getRowNumber(modelIndex);
    }

    /**
     * Returns the row of a desired peptide.
     *
     * @param peptideKey the key of the peptide
     * @return the row of the desired peptide
     */
    private int getPeptideRow(long peptideKey) {

        int modelIndex = IntStream.range(0, peptideKeys.length)
                .filter(i -> peptideKeys[i] == peptideKey)
                .findAny()
                .orElse(-1);

        return modelIndex == -1 ? -1 : ((SelfUpdatingTableModel) peptideTable.getModel()).getRowNumber(modelIndex);

    }

    /**
     * Returns the row of a desired PSM.
     *
     * @param psmKey the key of the PSM
     * @return the row of the desired PSM
     */
    private int getPsmRow(long psmKey) {

        int modelIndex = IntStream.range(0, psmKeys.length)
                .filter(i -> psmKeys[i] == psmKey)
                .findAny()
                .orElse(-1);

        return modelIndex == -1 ? -1 : ((SelfUpdatingTableModel) psmTable.getModel()).getRowNumber(modelIndex);

    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        clusterPopupMenu = new javax.swing.JPopupMenu();
        numberOfClustersMenuItem = new javax.swing.JMenuItem();
        backgroundLayeredPane = new javax.swing.JLayeredPane();
        overviewJPanel = new javax.swing.JPanel();
        overviewJSplitPane = new javax.swing.JSplitPane();
        ratioPlotsJPanel = new javax.swing.JPanel();
        ratioPlotsMainLayeredPane = new javax.swing.JLayeredPane();
        ratioPlotHelpJButton = new javax.swing.JButton();
        exportRatioPlotContextJButton = new javax.swing.JButton();
        ratioPlotOptionsJButton = new javax.swing.JButton();
        contextMenuRatioPlotBackgroundPanel = new javax.swing.JPanel();
        maximizeChartJButton = new javax.swing.JButton();
        ratioPlotsTitledPanel = new javax.swing.JPanel();
        plotPanel = new javax.swing.JPanel();
        proteinsJPanel = new javax.swing.JPanel();
        proteinsLayeredPane = new javax.swing.JLayeredPane();
        proteinsHelpJButton = new javax.swing.JButton();
        exportProteinsJButton = new javax.swing.JButton();
        contextMenuProteinsBackgroundPanel = new javax.swing.JPanel();
        proteinPeptidePsmLayeredPanel = new javax.swing.JPanel();
        matchesJTabbedPane = new javax.swing.JTabbedPane();
        proteinScrollPane = new javax.swing.JScrollPane();
        proteinTable = new JTable() {
            protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                    public String getToolTipText(MouseEvent e) {
                        java.awt.Point p = e.getPoint();
                        int index = columnModel.getColumnIndexAtX(p.x);
                        int realIndex = columnModel.getColumn(index).getModelIndex();
                        String tip = (String) proteinTableToolTips.get(realIndex);
                        return tip;
                    }
                };
            }
        };
        peptideScrollPane = new javax.swing.JScrollPane();
        peptideTable = new javax.swing.JTable();
        psmScrollPane = new javax.swing.JScrollPane();
        psmTable = new javax.swing.JTable();

        numberOfClustersMenuItem.setText("Number of Clusters");
        numberOfClustersMenuItem.setToolTipText("Set the number of clusters");
        numberOfClustersMenuItem.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                numberOfClustersMenuItemActionPerformed(evt);
            }
        });
        clusterPopupMenu.add(numberOfClustersMenuItem);

        setBackground(new java.awt.Color(255, 255, 255));
        addComponentListener(new java.awt.event.ComponentAdapter() {
            public void componentResized(java.awt.event.ComponentEvent evt) {
                formComponentResized(evt);
            }
        });

        overviewJPanel.setBackground(new java.awt.Color(255, 255, 255));
        overviewJPanel.setOpaque(false);
        overviewJPanel.setPreferredSize(new java.awt.Dimension(900, 800));

        overviewJSplitPane.setBorder(null);
        overviewJSplitPane.setDividerLocation(300);
        overviewJSplitPane.setDividerSize(0);
        overviewJSplitPane.setOrientation(javax.swing.JSplitPane.VERTICAL_SPLIT);
        overviewJSplitPane.setResizeWeight(0.5);

        ratioPlotsJPanel.setOpaque(false);

        ratioPlotsMainLayeredPane.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratioPlotsMainLayeredPaneMouseExited(evt);
            }
        });

        ratioPlotHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        ratioPlotHelpJButton.setToolTipText("Help");
        ratioPlotHelpJButton.setBorder(null);
        ratioPlotHelpJButton.setBorderPainted(false);
        ratioPlotHelpJButton.setContentAreaFilled(false);
        ratioPlotHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        ratioPlotHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratioPlotHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratioPlotHelpJButtonMouseExited(evt);
            }
        });
        ratioPlotHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                ratioPlotHelpJButtonActionPerformed(evt);
            }
        });
        ratioPlotsMainLayeredPane.setLayer(ratioPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        ratioPlotsMainLayeredPane.add(ratioPlotHelpJButton);
        ratioPlotHelpJButton.setBounds(930, 0, 10, 19);

        exportRatioPlotContextJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportRatioPlotContextJButton.setToolTipText("Copy to Clipboard");
        exportRatioPlotContextJButton.setBorder(null);
        exportRatioPlotContextJButton.setBorderPainted(false);
        exportRatioPlotContextJButton.setContentAreaFilled(false);
        exportRatioPlotContextJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportRatioPlotContextJButton.setEnabled(false);
        exportRatioPlotContextJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportRatioPlotContextJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportRatioPlotContextJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportRatioPlotContextJButtonMouseExited(evt);
            }
        });
        exportRatioPlotContextJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportRatioPlotContextJButtonActionPerformed(evt);
            }
        });
        ratioPlotsMainLayeredPane.setLayer(exportRatioPlotContextJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        ratioPlotsMainLayeredPane.add(exportRatioPlotContextJButton);
        exportRatioPlotContextJButton.setBounds(920, 0, 10, 19);

        ratioPlotOptionsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_gray.png"))); // NOI18N
        ratioPlotOptionsJButton.setToolTipText("Plot Options");
        ratioPlotOptionsJButton.setBorder(null);
        ratioPlotOptionsJButton.setBorderPainted(false);
        ratioPlotOptionsJButton.setContentAreaFilled(false);
        ratioPlotOptionsJButton.setEnabled(false);
        ratioPlotOptionsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_black.png"))); // NOI18N
        ratioPlotOptionsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                ratioPlotOptionsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                ratioPlotOptionsJButtonMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                ratioPlotOptionsJButtonMouseReleased(evt);
            }
        });
        ratioPlotsMainLayeredPane.setLayer(ratioPlotOptionsJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        ratioPlotsMainLayeredPane.add(ratioPlotOptionsJButton);
        ratioPlotOptionsJButton.setBounds(905, 5, 10, 19);

        contextMenuRatioPlotBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuRatioPlotBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuRatioPlotBackgroundPanel);
        contextMenuRatioPlotBackgroundPanel.setLayout(contextMenuRatioPlotBackgroundPanelLayout);
        contextMenuRatioPlotBackgroundPanelLayout.setHorizontalGroup(
            contextMenuRatioPlotBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 50, Short.MAX_VALUE)
        );
        contextMenuRatioPlotBackgroundPanelLayout.setVerticalGroup(
            contextMenuRatioPlotBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        ratioPlotsMainLayeredPane.setLayer(contextMenuRatioPlotBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);
        ratioPlotsMainLayeredPane.add(contextMenuRatioPlotBackgroundPanel);
        contextMenuRatioPlotBackgroundPanel.setBounds(890, 0, 50, 19);

        maximizeChartJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/maximize-grey.png"))); // NOI18N
        maximizeChartJButton.setToolTipText("Maximize");
        maximizeChartJButton.setBorder(null);
        maximizeChartJButton.setBorderPainted(false);
        maximizeChartJButton.setContentAreaFilled(false);
        maximizeChartJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/maximize.png"))); // NOI18N
        maximizeChartJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                maximizeChartJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                maximizeChartJButtonMouseExited(evt);
            }
        });
        maximizeChartJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                maximizeChartJButtonActionPerformed(evt);
            }
        });
        ratioPlotsMainLayeredPane.setLayer(maximizeChartJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        ratioPlotsMainLayeredPane.add(maximizeChartJButton);
        maximizeChartJButton.setBounds(50, 50, 20, 19);

        ratioPlotsTitledPanel.setBackground(new java.awt.Color(255, 255, 255));
        ratioPlotsTitledPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Profile Clusters"));
        ratioPlotsTitledPanel.setOpaque(false);

        plotPanel.setOpaque(false);
        plotPanel.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                plotPanelMouseMoved(evt);
            }
        });
        plotPanel.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                plotPanelMouseExited(evt);
            }
        });
        plotPanel.setLayout(new java.awt.GridLayout(3, 4, 15, 15));

        javax.swing.GroupLayout ratioPlotsTitledPanelLayout = new javax.swing.GroupLayout(ratioPlotsTitledPanel);
        ratioPlotsTitledPanel.setLayout(ratioPlotsTitledPanelLayout);
        ratioPlotsTitledPanelLayout.setHorizontalGroup(
            ratioPlotsTitledPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 948, Short.MAX_VALUE)
            .addGroup(ratioPlotsTitledPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ratioPlotsTitledPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(plotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        ratioPlotsTitledPanelLayout.setVerticalGroup(
            ratioPlotsTitledPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 267, Short.MAX_VALUE)
            .addGroup(ratioPlotsTitledPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(ratioPlotsTitledPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(plotPanel, javax.swing.GroupLayout.DEFAULT_SIZE, javax.swing.GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        ratioPlotsMainLayeredPane.add(ratioPlotsTitledPanel);
        ratioPlotsTitledPanel.setBounds(0, 0, 960, 290);

        javax.swing.GroupLayout ratioPlotsJPanelLayout = new javax.swing.GroupLayout(ratioPlotsJPanel);
        ratioPlotsJPanel.setLayout(ratioPlotsJPanelLayout);
        ratioPlotsJPanelLayout.setHorizontalGroup(
            ratioPlotsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ratioPlotsMainLayeredPane)
        );
        ratioPlotsJPanelLayout.setVerticalGroup(
            ratioPlotsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(ratioPlotsMainLayeredPane)
        );

        overviewJSplitPane.setLeftComponent(ratioPlotsJPanel);

        proteinsJPanel.setOpaque(false);

        proteinsHelpJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame_grey.png"))); // NOI18N
        proteinsHelpJButton.setToolTipText("Help");
        proteinsHelpJButton.setBorder(null);
        proteinsHelpJButton.setBorderPainted(false);
        proteinsHelpJButton.setContentAreaFilled(false);
        proteinsHelpJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/help_no_frame.png"))); // NOI18N
        proteinsHelpJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                proteinsHelpJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinsHelpJButtonMouseExited(evt);
            }
        });
        proteinsHelpJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                proteinsHelpJButtonActionPerformed(evt);
            }
        });
        proteinsLayeredPane.setLayer(proteinsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        proteinsLayeredPane.add(proteinsHelpJButton);
        proteinsHelpJButton.setBounds(930, 0, 10, 19);

        exportProteinsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setToolTipText("Copy to File");
        exportProteinsJButton.setBorder(null);
        exportProteinsJButton.setBorderPainted(false);
        exportProteinsJButton.setContentAreaFilled(false);
        exportProteinsJButton.setDisabledIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame_grey.png"))); // NOI18N
        exportProteinsJButton.setEnabled(false);
        exportProteinsJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/export_no_frame.png"))); // NOI18N
        exportProteinsJButton.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                exportProteinsJButtonMouseEntered(evt);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                exportProteinsJButtonMouseExited(evt);
            }
        });
        exportProteinsJButton.addActionListener(new java.awt.event.ActionListener() {
            public void actionPerformed(java.awt.event.ActionEvent evt) {
                exportProteinsJButtonActionPerformed(evt);
            }
        });
        proteinsLayeredPane.setLayer(exportProteinsJButton, javax.swing.JLayeredPane.POPUP_LAYER);
        proteinsLayeredPane.add(exportProteinsJButton);
        exportProteinsJButton.setBounds(920, 0, 10, 19);

        contextMenuProteinsBackgroundPanel.setBackground(new java.awt.Color(255, 255, 255));

        javax.swing.GroupLayout contextMenuProteinsBackgroundPanelLayout = new javax.swing.GroupLayout(contextMenuProteinsBackgroundPanel);
        contextMenuProteinsBackgroundPanel.setLayout(contextMenuProteinsBackgroundPanelLayout);
        contextMenuProteinsBackgroundPanelLayout.setHorizontalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 40, Short.MAX_VALUE)
        );
        contextMenuProteinsBackgroundPanelLayout.setVerticalGroup(
            contextMenuProteinsBackgroundPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 19, Short.MAX_VALUE)
        );

        proteinsLayeredPane.setLayer(contextMenuProteinsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);
        proteinsLayeredPane.add(contextMenuProteinsBackgroundPanel);
        contextMenuProteinsBackgroundPanel.setBounds(910, 0, 40, 19);

        proteinPeptidePsmLayeredPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins, Peptides & PSMs"));
        proteinPeptidePsmLayeredPanel.setOpaque(false);

        matchesJTabbedPane.setTabPlacement(javax.swing.JTabbedPane.BOTTOM);

        proteinTable.setModel(new ProteinTableModel());
        proteinTable.setOpaque(false);
        proteinTable.setSelectionMode(javax.swing.ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        proteinTable.addMouseMotionListener(new java.awt.event.MouseMotionAdapter() {
            public void mouseMoved(java.awt.event.MouseEvent evt) {
                proteinTableMouseMoved(evt);
            }
        });
        proteinTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseExited(java.awt.event.MouseEvent evt) {
                proteinTableMouseExited(evt);
            }
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                proteinTableMouseReleased(evt);
            }
        });
        proteinTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                proteinTableKeyReleased(evt);
            }
        });
        proteinScrollPane.setViewportView(proteinTable);

        matchesJTabbedPane.addTab("Proteins", proteinScrollPane);

        peptideTable.setModel(new PeptideTableModel());
        peptideTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                peptideTableMouseReleased(evt);
            }
        });
        peptideTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                peptideTableKeyReleased(evt);
            }
        });
        peptideScrollPane.setViewportView(peptideTable);

        matchesJTabbedPane.addTab("Peptides", peptideScrollPane);

        psmTable.setModel(new PsmTableModel());
        psmTable.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseReleased(java.awt.event.MouseEvent evt) {
                psmTableMouseReleased(evt);
            }
        });
        psmTable.addKeyListener(new java.awt.event.KeyAdapter() {
            public void keyReleased(java.awt.event.KeyEvent evt) {
                psmTableKeyReleased(evt);
            }
        });
        psmScrollPane.setViewportView(psmTable);

        matchesJTabbedPane.addTab("PSMs", psmScrollPane);

        javax.swing.GroupLayout proteinPeptidePsmLayeredPanelLayout = new javax.swing.GroupLayout(proteinPeptidePsmLayeredPanel);
        proteinPeptidePsmLayeredPanel.setLayout(proteinPeptidePsmLayeredPanelLayout);
        proteinPeptidePsmLayeredPanelLayout.setHorizontalGroup(
            proteinPeptidePsmLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinPeptidePsmLayeredPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(matchesJTabbedPane)
                .addContainerGap())
        );
        proteinPeptidePsmLayeredPanelLayout.setVerticalGroup(
            proteinPeptidePsmLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(proteinPeptidePsmLayeredPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(matchesJTabbedPane, javax.swing.GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                .addContainerGap())
        );

        proteinsLayeredPane.add(proteinPeptidePsmLayeredPanel);
        proteinPeptidePsmLayeredPanel.setBounds(0, 0, 950, 350);

        javax.swing.GroupLayout proteinsJPanelLayout = new javax.swing.GroupLayout(proteinsJPanel);
        proteinsJPanel.setLayout(proteinsJPanelLayout);
        proteinsJPanelLayout.setHorizontalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 970, Short.MAX_VALUE)
        );
        proteinsJPanelLayout.setVerticalGroup(
            proteinsJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addComponent(proteinsLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 458, Short.MAX_VALUE)
        );

        overviewJSplitPane.setRightComponent(proteinsJPanel);

        javax.swing.GroupLayout overviewJPanelLayout = new javax.swing.GroupLayout(overviewJPanel);
        overviewJPanel.setLayout(overviewJPanelLayout);
        overviewJPanelLayout.setHorizontalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane)
                .addContainerGap())
        );
        overviewJPanelLayout.setVerticalGroup(
            overviewJPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGroup(overviewJPanelLayout.createSequentialGroup()
                .addContainerGap()
                .addComponent(overviewJSplitPane)
                .addContainerGap())
        );

        backgroundLayeredPane.add(overviewJPanel);
        overviewJPanel.setBounds(0, 0, 990, 780);

        javax.swing.GroupLayout layout = new javax.swing.GroupLayout(this);
        this.setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 993, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(backgroundLayeredPane, javax.swing.GroupLayout.DEFAULT_SIZE, 993, Short.MAX_VALUE))
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 688, Short.MAX_VALUE)
            .addGroup(layout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addComponent(backgroundLayeredPane, javax.swing.GroupLayout.Alignment.TRAILING, javax.swing.GroupLayout.DEFAULT_SIZE, 688, Short.MAX_VALUE))
        );
    }// </editor-fold>//GEN-END:initComponents

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void proteinTableMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseExited
        this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinTableMouseExited

    /**
     * Update the protein selection.
     *
     * @param evt
     */
    private void proteinTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseReleased

        if (selectedChartPanel != null) {

            DefaultCategoryDataset oldDataset = (DefaultCategoryDataset) selectedChartPanel.getChart().getCategoryPlot().getDataset();

            // get the list of selected proteins
            ArrayList<Long> selectedProteins = new ArrayList<>();
            int[] selectedRowIndexes = proteinTable.getSelectedRows();

            for (int i = 0; i < selectedRowIndexes.length; i++) {
                Long proteinKey = proteinKeys[((SelfUpdatingTableModel) proteinTable.getModel()).getViewIndex(selectedRowIndexes[i])];
                selectedProteins.add(proteinKey);
            }

            reporterGUI.setSelectedProteins(selectedProteins, false, false); // @TODO: should be possible to select proteins, peptides and PSMs at the same time
            reporterGUI.setSelectedPeptides(new ArrayList<Long>(), false, false);
            reporterGUI.setSelectedPsms(new ArrayList<Long>(), false, false);

            // create the new dataset with the selected proteins in the front
            DefaultCategoryDataset newDataset = new DefaultCategoryDataset();

            for (int i = 0; i < oldDataset.getRowCount(); i++) {

                String proteinKey = (String) oldDataset.getRowKey(i);
                Long proteinKeyAsLong = Long.valueOf(proteinKey);

                if (!selectedProteins.contains(proteinKeyAsLong)) {

                    for (int j = 0; j < oldDataset.getColumnKeys().size(); j++) {

                        newDataset.addValue(
                                oldDataset.getValue(i, j),
                                proteinKey,
                                (String) oldDataset.getColumnKeys().get(j)
                        );

                    }

                }

            }

            for (int i = 0; i < oldDataset.getRowCount(); i++) {

                String proteinKey = (String) oldDataset.getRowKey(i);
                Long proteinKeyAsLong = Long.valueOf(proteinKey);

                if (selectedProteins.contains(proteinKeyAsLong)) {

                    for (int j = 0; j < oldDataset.getColumnKeys().size(); j++) {

                        newDataset.addValue(
                                oldDataset.getValue(i, j),
                                proteinKey,
                                (String) oldDataset.getColumnKeys().get(j)
                        );

                    }

                }

            }

            selectedChartPanel.getChart().getCategoryPlot().setDataset(newDataset);

            // update the renderer
            CategoryItemRenderer renderer = selectedChartPanel.getChart().getCategoryPlot().getRenderer();

            ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();
            ClusteringSettings clusteringSettings = reporterGUI.getDisplayParameters().getClusteringSettings();

            for (int i = 0; i < newDataset.getRowCount(); i++) {

                Long key = Long.valueOf((String) newDataset.getRowKey(i));
                boolean isProtein = clusterBuilder.getProteinIndex(key) != null;
                boolean isPeptide = clusterBuilder.getPeptideIndex(key) != null;
                boolean isPsm = clusterBuilder.getPsmIndex(key) != null;

                if (isProtein) {

                    ArrayList<String> classes = clusterBuilder.getProteinClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedProteins.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else if (isPeptide) {

                    ArrayList<String> classes = clusterBuilder.getPeptideClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedProteins.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else if (isPsm) {

                    ArrayList<String> classes = clusterBuilder.getPsmClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedProteins.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else {
                    throw new IllegalArgumentException("No match found for key " + key + ".");
                }
            }

            selectedChartPanel.getChart().fireChartChanged();
        }

        int row = proteinTable.getSelectedRow();
        int column = proteinTable.getSelectedColumn();

        int proteinIndex = -1;

        if (row != -1) {
            proteinIndex = proteinTable.convertRowIndexToModel(row);
        }

        if (evt == null || (evt.getButton() == MouseEvent.BUTTON1 && (proteinIndex != -1 && column != -1))) {

            if (proteinIndex != -1) {

                long proteinKey = proteinKeys[proteinIndex];

                // open the gene details dialog
                if (column == proteinTable.getColumn("Chr").getModelIndex() && evt != null
                        && evt.getButton() == MouseEvent.BUTTON1) {
                    try {

                        new GeneDetailsDialog(
                                reporterGUI,
                                identification.getProteinMatch(proteinKey),
                                geneMaps,
                                reporterGUI.getProteinDetailsProvider()
                        );

                    } catch (Exception ex) {
                        reporterGUI.catchException(ex);
                    }
                }

                // open protein link in web browser
                if (column == proteinTable.getColumn("Accession").getModelIndex()
                        && evt != null && evt.getButton() == MouseEvent.BUTTON1
                        && ((String) proteinTable.getValueAt(row, column)).lastIndexOf("<html>") != -1) {

                    String link = (String) proteinTable.getValueAt(row, column);
                    link = link.substring(link.indexOf("\"") + 1);
                    link = link.substring(0, link.indexOf("\""));

                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));
                    BareBonesBrowserLaunch.openURL(link);
                    this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

                }

                // open the protein inference dialog
                if (column == proteinTable.getColumn("PI").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1) {
                    //new ProteinInferenceDialog(peptideShakerGUI, proteinKey, peptideShakerGUI.getIdentification()); // @TODO: implement me!
                }

                // open the match validation level dialog
//                if (column == proteinTable.getColumn("").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1) { // @TODO: implement me!
//                    PSMaps pSMaps = new PSMaps();
//                    pSMaps = (PSMaps) identification.getUrParam(pSMaps);
//                    try {
//                        MatchValidationDialog matchValidationDialog = new MatchValidationDialog(reporterGUI, reporterGUI.getExceptionHandler(),
//                                identification, identificationFeaturesGenerator, pSMaps.getProteinMap(), proteinKey,
//                                reporterGUI.getShotgunProtocol(), reporterGUI.getIdentificationParameters());
//                        if (matchValidationDialog.isValidationChanged()) {
//                            updateProteinPanelTitle();
//                        }
//                    } catch (Exception e) {
//                        reporterGUI.catchException(e);
//                    }
//                }
            }
        } else if (evt.getButton()
                == MouseEvent.BUTTON3) {
            if (proteinTable.columnAtPoint(evt.getPoint()) == proteinTable.getColumn("  ").getModelIndex()) {
                //selectJPopupMenu.show(proteinTable, evt.getX(), evt.getY()); // @TODO: implement?
            }
        }

    }//GEN-LAST:event_proteinTableMouseReleased

    /**
     * Show a hand cursor if over a column with an HTML link or show the
     * complete protein description if over the protein description column.
     *
     * @param evt
     */
    private void proteinTableMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinTableMouseMoved

        int row = proteinTable.rowAtPoint(evt.getPoint());
        int column = proteinTable.columnAtPoint(evt.getPoint());

        proteinTable.setToolTipText(null);

        if (row != -1 && column != -1
                && column == proteinTable.getColumn("Accession").getModelIndex()
                && proteinTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }

        } else if (column == proteinTable.getColumn("PI").getModelIndex()
                && proteinTable.getValueAt(row, column) != null) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        } else if (column == proteinTable.getColumn("Chr").getModelIndex()
                && proteinTable.getValueAt(row, column) != null) {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));

        } else if (column == proteinTable.getColumn("Description").getModelIndex()
                && proteinTable.getValueAt(row, column) != null) {

            if (GuiUtilities.getPreferredWidthOfCell(proteinTable, row, column) > proteinTable.getColumn("Description").getWidth()) {
                proteinTable.setToolTipText("" + proteinTable.getValueAt(row, column));
            }

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        } else {

            this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));

        }
    }//GEN-LAST:event_proteinTableMouseMoved

    /**
     * Update the protein selection.
     *
     * @param evt
     */
    private void proteinTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_proteinTableKeyReleased

        if (evt.getKeyCode() == KeyEvent.VK_UP
                || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP
                || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            proteinTableMouseReleased(null);

        }

    }//GEN-LAST:event_proteinTableKeyReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void proteinsHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void proteinsHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonMouseExited

    /**
     * Open the protein table help.
     *
     * @param evt
     */
    private void proteinsHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_proteinsHelpJButtonActionPerformed

        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        new HelpDialog(
                reporterGUI,
                getClass().getResource("/helpFiles/OverviewTabReporter.html"),
                null, // @TODO: write help
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                "Proteins Help"
        );

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_proteinsHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportProteinsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportProteinsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportProteinsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportProteinsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportProteinsJButtonMouseExited

    /**
     * Export the protein table to file.
     *
     * @param evt
     */
    private void exportProteinsJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportProteinsJButtonActionPerformed
        //copyTableContentToClipboardOrFile(TableIndex.PROTEIN_TABLE); // @TODO: reimplement me!
    }//GEN-LAST:event_exportProteinsJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void ratioPlotHelpJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratioPlotHelpJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_ratioPlotHelpJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void ratioPlotHelpJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratioPlotHelpJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ratioPlotHelpJButtonMouseExited

    /**
     * Open the ratio plot help.
     *
     * @param evt
     */
    private void ratioPlotHelpJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_ratioPlotHelpJButtonActionPerformed

        setCursor(new java.awt.Cursor(java.awt.Cursor.WAIT_CURSOR));

        new HelpDialog(
                reporterGUI,
                getClass().getResource("/helpFiles/OverviewTabReporter.html"), null, // @TODO: write help
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                "Plot Help"
        );

        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ratioPlotHelpJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void exportRatioPlotContextJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportRatioPlotContextJButtonMouseEntered
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
    }//GEN-LAST:event_exportRatioPlotContextJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void exportRatioPlotContextJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_exportRatioPlotContextJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_exportRatioPlotContextJButtonMouseExited

    /**
     * Export the plot to file.
     *
     * @param evt
     */
    private void exportRatioPlotContextJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_exportRatioPlotContextJButtonActionPerformed

        // @TODO: implement me!
//        try {
//            if (proteinTable.getSelectedRow() != -1) {
//
//                String proteinKey = proteinKeys.get(proteinTable.convertRowIndexToModel(proteinTable.getSelectedRow()));
//                Protein protein = sequenceFactory.getProtein(proteinKey);
//
//                String clipboardString = protein.getSequence();
//                StringSelection stringSelection = new StringSelection(clipboardString);
//                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
//                clipboard.setContents(stringSelection, peptideShakerGUI);
//
//                JOptionPane.showMessageDialog(peptideShakerGUI, "Protein sequence copied to clipboard.", "Copied to Clipboard", JOptionPane.INFORMATION_MESSAGE);
//            }
//        } catch (Exception e) {
//            peptideShakerGUI.catchException(e);
//            e.printStackTrace();
//        }
    }//GEN-LAST:event_exportRatioPlotContextJButtonActionPerformed

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void ratioPlotOptionsJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratioPlotOptionsJButtonMouseEntered

        if (ratioPlotOptionsJButton.isEnabled()) {
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

    }//GEN-LAST:event_ratioPlotOptionsJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void ratioPlotOptionsJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratioPlotOptionsJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_ratioPlotOptionsJButtonMouseExited

    /**
     * Set the optimal size of the components.
     */
    public void autoResizeComponents() {
        formComponentResized(null);
    }

    /**
     * Resize the components of the frame size changes.
     *
     * @param evt
     */
    private void formComponentResized(java.awt.event.ComponentEvent evt) {//GEN-FIRST:event_formComponentResized

        // resize the background panel
        backgroundLayeredPane.getComponent(0).setBounds(
                0,
                0,
                backgroundLayeredPane.getWidth(),
                backgroundLayeredPane.getHeight()
        );

        backgroundLayeredPane.revalidate();
        backgroundLayeredPane.repaint();

        // resize the layered panels
        SwingUtilities.invokeLater(new Runnable() {

            public void run() {

                overviewJSplitPane.setDividerLocation(0.5);

                // move the icons
                proteinsLayeredPane.getComponent(0).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        proteinsLayeredPane.getComponent(0).getWidth(),
                        proteinsLayeredPane.getComponent(0).getHeight()
                );

                proteinsLayeredPane.getComponent(1).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        proteinsLayeredPane.getComponent(1).getWidth(),
                        proteinsLayeredPane.getComponent(1).getHeight()
                );

                proteinsLayeredPane.getComponent(2).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        proteinsLayeredPane.getComponent(2).getWidth(),
                        proteinsLayeredPane.getComponent(2).getHeight()
                );

                // resize the plot area
                proteinsLayeredPane.getComponent(3).setBounds(
                        0,
                        0,
                        proteinsLayeredPane.getWidth(),
                        proteinsLayeredPane.getHeight()
                );

                proteinsLayeredPane.revalidate();
                proteinsLayeredPane.repaint();

                // move the icons
                ratioPlotsMainLayeredPane.getComponent(0).setBounds(
                        ratioPlotsMainLayeredPane.getWidth() - ratioPlotsMainLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        ratioPlotsMainLayeredPane.getComponent(0).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(0).getHeight()
                );

                ratioPlotsMainLayeredPane.getComponent(1).setBounds(
                        ratioPlotsMainLayeredPane.getWidth() - ratioPlotsMainLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        ratioPlotsMainLayeredPane.getComponent(1).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(1).getHeight()
                );

                ratioPlotsMainLayeredPane.getComponent(2).setBounds(
                        ratioPlotsMainLayeredPane.getWidth() - ratioPlotsMainLayeredPane.getComponent(2).getWidth() - 32,
                        0,
                        ratioPlotsMainLayeredPane.getComponent(2).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(2).getHeight()
                );

                ratioPlotsMainLayeredPane.getComponent(3).setBounds(
                        ratioPlotsMainLayeredPane.getWidth() - ratioPlotsMainLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        ratioPlotsMainLayeredPane.getComponent(3).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(3).getHeight()
                );

                ratioPlotsMainLayeredPane.getComponent(4).setBounds(
                        50,
                        50,
                        ratioPlotsMainLayeredPane.getComponent(4).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(4).getHeight()
                );

                // resize the plot area
                ratioPlotsMainLayeredPane.getComponent(5).setBounds(
                        0,
                        0,
                        ratioPlotsMainLayeredPane.getWidth(),
                        ratioPlotsMainLayeredPane.getHeight()
                );

                ratioPlotsMainLayeredPane.revalidate();
                ratioPlotsMainLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_formComponentResized

    /**
     * Let the user select the number of clusters to use.
     *
     * @param evt
     */
    private void numberOfClustersMenuItemActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_numberOfClustersMenuItemActionPerformed

        String value = JOptionPane.showInputDialog(
                this,
                "Number of clusters:",
                reporterGUI.getkMeansClutering().getNumberOfClusters()
        );

        if (value != null) {

            try {

                int numberOfClusters = Integer.parseInt(value);
                reporterGUI.recluster(numberOfClusters, false);

            } catch (NumberFormatException e) {

                JOptionPane.showMessageDialog(
                        this,
                        "The number of cluster has to be an integer value.",
                        "Input Error",
                        JOptionPane.WARNING_MESSAGE
                );

            }

        }

    }//GEN-LAST:event_numberOfClustersMenuItemActionPerformed

    /**
     * Show the contextual options for the ratio plots.
     *
     * @param evt
     */
    private void ratioPlotOptionsJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratioPlotOptionsJButtonMouseReleased

        if (ratioPlotOptionsJButton.isEnabled()) {
            clusterPopupMenu.show(ratioPlotOptionsJButton, evt.getX(), evt.getY());
        }

    }//GEN-LAST:event_ratioPlotOptionsJButtonMouseReleased

    /**
     * Change the cursor to a hand cursor.
     *
     * @param evt
     */
    private void maximizeChartJButtonMouseEntered(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_maximizeChartJButtonMouseEntered

        if (ratioPlotOptionsJButton.isEnabled()) {
            setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        }

    }//GEN-LAST:event_maximizeChartJButtonMouseEntered

    /**
     * Change the cursor back to the default cursor.
     *
     * @param evt
     */
    private void maximizeChartJButtonMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_maximizeChartJButtonMouseExited
        setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
    }//GEN-LAST:event_maximizeChartJButtonMouseExited

    /**
     * Maximize or minimize the given chart.
     *
     * @param evt
     */
    private void maximizeChartJButtonActionPerformed(java.awt.event.ActionEvent evt) {//GEN-FIRST:event_maximizeChartJButtonActionPerformed

        maximizeChartJButton.setVisible(false);

        if (chartMaximized) {
            maximizeChartJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/maximize-grey.png")));
            maximizeChartJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/maximize.png")));
            maximizeChartJButton.setToolTipText("Maximize");
        } else {
            maximizeChartJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/minimize-grey.png")));
            maximizeChartJButton.setRolloverIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/minimize.png")));
            maximizeChartJButton.setToolTipText("Minimize");
        }

        chartMaximized = !chartMaximized;

        CategoryPlot plot = ((CategoryPlot) selectedChartPanel.getChart().getPlot());
        plot.getDomainAxis().setVisible(chartMaximized);
        plot.setDomainGridlinesVisible(chartMaximized);

        if (chartMaximized) {

            boolean chartAlreadySelected = false;

            if (selectedChartPanel != null) {
                chartAlreadySelected = selectedChartPanel.getName().equalsIgnoreCase(maximizeIconChartPanel.getName());
            }

            if (!chartAlreadySelected) {

                reporterGUI.setSelectedProteins(new ArrayList<Long>(), true, true);

                if (selectedChartPanel != null) {
                    selectCluster(selectedChartPanel, true);
                    selectedChartPanel.setBorder(null);
                    selectedChartPanel.getChart().fireChartChanged();
                }

                selectCluster(maximizeIconChartPanel, true);

            }

            selectedChartPanel = maximizeIconChartPanel;
            plot.getRenderer().setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
            plotPanel.removeAll();
            plotPanel.setLayout(new BorderLayout());
            plotPanel.add(selectedChartPanel);

            selectedChartPanel.setBorder(null);
            selectedChartPanel.getChart().fireChartChanged();

        } else {

            plotPanel.removeAll();
            plotPanel.setLayout(new java.awt.GridLayout(3, 4, 15, 15));

            for (ChartPanel tempChartPanel : allChartPanels) {

                tempChartPanel.setBorder(null);
                tempChartPanel.getChart().fireChartChanged();
                ((CategoryPlot) tempChartPanel.getChart().getPlot()).getRenderer().setBaseToolTipGenerator(null);
                plotPanel.add(tempChartPanel);

            }

            if (selectedChartPanel != null) {
                selectedChartPanel.setBorder(new LineBorder(Color.DARK_GRAY));
                selectedChartPanel.getChart().fireChartChanged();
            }
        }

        ratioPlotsMainLayeredPane.revalidate();
        ratioPlotsMainLayeredPane.repaint();

    }//GEN-LAST:event_maximizeChartJButtonActionPerformed

    /**
     * Hide the maximize button.
     *
     * @param evt
     */
    private void plotPanelMouseMoved(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_plotPanelMouseMoved

        Component c = evt.getComponent();

        if (c == null || c instanceof JPanel) {
            maximizeChartJButton.setVisible(false);
        }

    }//GEN-LAST:event_plotPanelMouseMoved

    /**
     * Hide the maximize button.
     *
     * @param evt
     */
    private void plotPanelMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_plotPanelMouseExited
        maximizeChartJButton.setVisible(false);
    }//GEN-LAST:event_plotPanelMouseExited

    /**
     * Hide the maximize button.
     *
     * @param evt
     */
    private void ratioPlotsMainLayeredPaneMouseExited(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratioPlotsMainLayeredPaneMouseExited
        maximizeChartJButton.setVisible(false);
    }//GEN-LAST:event_ratioPlotsMainLayeredPaneMouseExited

    /**
     * Update the peptide selection.
     *
     * @param evt
     */
    private void peptideTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_peptideTableMouseReleased

        if (selectedChartPanel != null) {

            DefaultCategoryDataset oldDataset = (DefaultCategoryDataset) selectedChartPanel.getChart().getCategoryPlot().getDataset();

            // get the list of selected peptides
            ArrayList<Long> selectedPeptides = new ArrayList<>();
            int[] selectedRowIndexes = peptideTable.getSelectedRows();

            for (int i = 0; i < selectedRowIndexes.length; i++) {
                Long peptideKey = peptideKeys[((SelfUpdatingTableModel) peptideTable.getModel()).getViewIndex(selectedRowIndexes[i])];
                selectedPeptides.add(peptideKey);
            }

            reporterGUI.setSelectedProteins(new ArrayList<Long>(), false, false); // @TODO: should be possible to select proteins, peptides and PSMs at the same time
            reporterGUI.setSelectedPeptides(selectedPeptides, false, false);
            reporterGUI.setSelectedPsms(new ArrayList<Long>(), false, false);

            // create the new dataset with the selected peptides in the front
            DefaultCategoryDataset newDataset = new DefaultCategoryDataset();

            for (int i = 0; i < oldDataset.getRowCount(); i++) {

                String peptideKey = (String) oldDataset.getRowKey(i);
                Long peptideKeyAsLong = Long.valueOf(peptideKey);

                if (!selectedPeptides.contains(peptideKeyAsLong)) {

                    for (int j = 0; j < oldDataset.getColumnKeys().size(); j++) {

                        newDataset.addValue(
                                oldDataset.getValue(i, j),
                                peptideKey,
                                (String) oldDataset.getColumnKeys().get(j)
                        );

                    }

                }

            }

            for (int i = 0; i < oldDataset.getRowCount(); i++) {

                String peptideKey = (String) oldDataset.getRowKey(i);
                Long peptideKeyAsLong = Long.valueOf(peptideKey);

                if (selectedPeptides.contains(peptideKeyAsLong)) {

                    for (int j = 0; j < oldDataset.getColumnKeys().size(); j++) {

                        newDataset.addValue(
                                oldDataset.getValue(i, j),
                                peptideKey,
                                (String) oldDataset.getColumnKeys().get(j)
                        );

                    }

                }
            }

            selectedChartPanel.getChart().getCategoryPlot().setDataset(newDataset);

            // update the renderer
            CategoryItemRenderer renderer = selectedChartPanel.getChart().getCategoryPlot().getRenderer();

            ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();
            ClusteringSettings clusteringSettings = reporterGUI.getDisplayParameters().getClusteringSettings();

            for (int i = 0; i < newDataset.getRowCount(); i++) {

                Long key = Long.valueOf((String) newDataset.getRowKey(i));
                boolean isProtein = clusterBuilder.getProteinIndex(key) != null;
                boolean isPeptide = clusterBuilder.getPeptideIndex(key) != null;
                boolean isPsm = clusterBuilder.getPsmIndex(key) != null;

                if (isProtein) {

                    ArrayList<String> classes = clusterBuilder.getProteinClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedPeptides.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else if (isPeptide) {

                    ArrayList<String> classes = clusterBuilder.getPeptideClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedPeptides.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else if (isPsm) {

                    ArrayList<String> classes = clusterBuilder.getPsmClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedPeptides.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else {
                    throw new IllegalArgumentException("No match found for key " + key + ".");
                }
            }

            selectedChartPanel.getChart().fireChartChanged();
        }

    }//GEN-LAST:event_peptideTableMouseReleased

    /**
     * Update the peptide selection.
     *
     * @param evt
     */
    private void peptideTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_peptideTableKeyReleased

        if (evt.getKeyCode() == KeyEvent.VK_UP
                || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP
                || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            peptideTableMouseReleased(null);

        }
    }//GEN-LAST:event_peptideTableKeyReleased

    /**
     * Update the PSM selection.
     *
     * @param evt
     */
    private void psmTableMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_psmTableMouseReleased

        if (selectedChartPanel != null) {

            DefaultCategoryDataset oldDataset = (DefaultCategoryDataset) selectedChartPanel.getChart().getCategoryPlot().getDataset();

            // get the list of selected PSMs
            ArrayList<Long> selectedPsms = new ArrayList<>();
            int[] selectedRowIndexes = psmTable.getSelectedRows();

            for (int i = 0; i < selectedRowIndexes.length; i++) {

                long proteinKey = psmKeys[((SelfUpdatingTableModel) psmTable.getModel()).getViewIndex(selectedRowIndexes[i])];
                selectedPsms.add(proteinKey);

            }

            reporterGUI.setSelectedProteins(new ArrayList<Long>(), false, false); // @TODO: should be possible to select proteins, peptides and PSMs at the same time
            reporterGUI.setSelectedPeptides(new ArrayList<Long>(), false, false);
            reporterGUI.setSelectedPsms(selectedPsms, false, false);

            // create the new dataset with the selected peptides in the front
            DefaultCategoryDataset newDataset = new DefaultCategoryDataset();

            for (int i = 0; i < oldDataset.getRowCount(); i++) {

                String psmKey = (String) oldDataset.getRowKey(i);
                Long psmKeyLong = Long.valueOf(psmKey);

                if (!selectedPsms.contains(psmKeyLong)) {

                    for (int j = 0; j < oldDataset.getColumnKeys().size(); j++) {

                        newDataset.addValue(
                                oldDataset.getValue(i, j),
                                psmKey,
                                (String) oldDataset.getColumnKeys().get(j)
                        );

                    }

                }

            }

            for (int i = 0; i < oldDataset.getRowCount(); i++) {

                String psmKey = (String) oldDataset.getRowKey(i);
                Long psmKeyLong = Long.valueOf(psmKey);

                if (selectedPsms.contains(psmKeyLong)) {

                    for (int j = 0; j < oldDataset.getColumnKeys().size(); j++) {

                        newDataset.addValue(
                                oldDataset.getValue(i, j),
                                psmKey,
                                (String) oldDataset.getColumnKeys().get(j)
                        );
                    }

                }

            }

            selectedChartPanel.getChart().getCategoryPlot().setDataset(newDataset);

            // update the renderer
            CategoryItemRenderer renderer = selectedChartPanel.getChart().getCategoryPlot().getRenderer();

            ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();
            ClusteringSettings clusteringSettings = reporterGUI.getDisplayParameters().getClusteringSettings();

            for (int i = 0; i < newDataset.getRowCount(); i++) {

                Long key = Long.valueOf((String) newDataset.getRowKey(i));
                boolean isProtein = clusterBuilder.getProteinIndex(key) != null;
                boolean isPeptide = clusterBuilder.getPeptideIndex(key) != null;
                boolean isPsm = clusterBuilder.getPsmIndex(key) != null;

                if (isProtein) {

                    ArrayList<String> classes = clusterBuilder.getProteinClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedPsms.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else if (isPeptide) {

                    ArrayList<String> classes = clusterBuilder.getPeptideClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedPsms.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else if (isPsm) {

                    ArrayList<String> classes = clusterBuilder.getPsmClasses(key);
                    String clusterClass = classes.get(0);  // @TODO: what if present in different classes?

                    if (selectedPsms.contains(key)) {
                        Color classColor = clusteringSettings.getColor(clusterClass);
                        renderer.setSeriesPaint(i, classColor);
                    } else {
                        Color nonSelectedColor = clusteringSettings.getNonSelectedColor(clusterClass);
                        renderer.setSeriesPaint(i, nonSelectedColor);
                    }

                } else {
                    throw new IllegalArgumentException("No match found for key " + key + ".");
                }
            }

            selectedChartPanel.getChart().fireChartChanged();
        }

    }//GEN-LAST:event_psmTableMouseReleased

    /**
     * Update the PSM selection.
     *
     * @param evt
     */
    private void psmTableKeyReleased(java.awt.event.KeyEvent evt) {//GEN-FIRST:event_psmTableKeyReleased

        if (evt.getKeyCode() == KeyEvent.VK_UP
                || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP
                || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {

            psmTableMouseReleased(null);

        }

    }//GEN-LAST:event_psmTableKeyReleased

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLayeredPane backgroundLayeredPane;
    private javax.swing.JPopupMenu clusterPopupMenu;
    private javax.swing.JPanel contextMenuProteinsBackgroundPanel;
    private javax.swing.JPanel contextMenuRatioPlotBackgroundPanel;
    private javax.swing.JButton exportProteinsJButton;
    private javax.swing.JButton exportRatioPlotContextJButton;
    private javax.swing.JTabbedPane matchesJTabbedPane;
    private javax.swing.JButton maximizeChartJButton;
    private javax.swing.JMenuItem numberOfClustersMenuItem;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JSplitPane overviewJSplitPane;
    private javax.swing.JScrollPane peptideScrollPane;
    private javax.swing.JTable peptideTable;
    private javax.swing.JPanel plotPanel;
    private javax.swing.JPanel proteinPeptidePsmLayeredPanel;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JButton proteinsHelpJButton;
    private javax.swing.JPanel proteinsJPanel;
    private javax.swing.JLayeredPane proteinsLayeredPane;
    private javax.swing.JScrollPane psmScrollPane;
    private javax.swing.JTable psmTable;
    private javax.swing.JButton ratioPlotHelpJButton;
    private javax.swing.JButton ratioPlotOptionsJButton;
    private javax.swing.JPanel ratioPlotsJPanel;
    private javax.swing.JLayeredPane ratioPlotsMainLayeredPane;
    private javax.swing.JPanel ratioPlotsTitledPanel;
    // End of variables declaration//GEN-END:variables

    /**
     * Deactivates the self updating tables.
     */
    public void deactivateSelfUpdatingTableModels() {

        if (proteinTable.getModel() instanceof SelfUpdatingTableModel) {
            ((SelfUpdatingTableModel) proteinTable.getModel()).setSelfUpdating(false);
        }

    }

    /**
     * Update the protein table cell renderers.
     */
    private void updateProteinTableCellRenderers() {

        if (reporterGUI.getIdentification() != null) {

            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Peptides").getCellRenderer()).setMaxValue(reporterGUI.getMetrics().getMaxNPeptides());
            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(reporterGUI.getMetrics().getMaxNPsms());
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).setMaxValue(reporterGUI.getMetrics().getMaxMW());

            try {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).setMaxValue(100.0);
            } catch (IllegalArgumentException e) {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
            }
        }
    }

    /**
     * Update the peptide table cell renderers.
     */
    private void updatePeptideTableCellRenderers() {

        if (reporterGUI.getIdentification() != null) {

            // set up the peptide inference color map
            HashMap<Integer, Color> peptideInferenceColorMap = new HashMap<>();
            peptideInferenceColorMap.put(PSParameter.NOT_GROUP, reporterGUI.getSparklineColor());
            peptideInferenceColorMap.put(PSParameter.RELATED, Color.YELLOW);
            peptideInferenceColorMap.put(PSParameter.RELATED_AND_UNRELATED, Color.ORANGE);
            peptideInferenceColorMap.put(PSParameter.UNRELATED, Color.RED);

            // set up the peptide inference tooltip map
            HashMap<Integer, String> peptideInferenceTooltipMap = new HashMap<>();
            peptideInferenceTooltipMap.put(PSParameter.NOT_GROUP, "Unique to a single protein");
            peptideInferenceTooltipMap.put(PSParameter.RELATED, "Belongs to a group of related proteins");
            peptideInferenceTooltipMap.put(PSParameter.RELATED_AND_UNRELATED, "Belongs to a group of related and unrelated proteins");
            peptideInferenceTooltipMap.put(PSParameter.UNRELATED, "Belongs to unrelated proteins");

            peptideTable.getColumn("PI").setCellRenderer(
                    new JSparklinesIntegerColorTableCellRenderer(
                            reporterGUI.getSparklineColor(),
                            peptideInferenceColorMap,
                            peptideInferenceTooltipMap
                    )
            );

            // use a gray color for no decoy searches
            Color nonValidatedColor = reporterGUI.getSparklineColorNonValidated();

            if (!reporterGUI.getIdentificationParameters().getFastaParameters().isTargetDecoy()) {
                nonValidatedColor = reporterGUI.getUtilitiesUserParameters().getSparklineColorNotFound();
            }

            ArrayList<Color> sparklineColors = new ArrayList<>();
            sparklineColors.add(reporterGUI.getSparklineColor());
            sparklineColors.add(reporterGUI.getUtilitiesUserParameters().getSparklineColorDoubtful());
            sparklineColors.add(nonValidatedColor);

            peptideTable.getColumn("#Spectra").setCellRenderer(
                    new JSparklinesArrayListBarChartTableCellRenderer(
                            PlotOrientation.HORIZONTAL,
                            10.0, sparklineColors,
                            JSparklinesArrayListBarChartTableCellRenderer.ValueDisplayType.sumOfNumbers
                    )
            );

            ((JSparklinesArrayListBarChartTableCellRenderer) peptideTable.getColumn("#Spectra").getCellRenderer()).showNumberAndChart(true, TableProperties.getLabelWidth(), new DecimalFormat("0"));

            peptideTable.getColumn("").setCellRenderer(
                    new JSparklinesIntegerIconTableCellRenderer(
                            MatchValidationLevel.getIconMap(this.getClass()),
                            MatchValidationLevel.getTooltipMap()
                    )
            );

            try {

                peptideTable.getColumn("Confidence").setCellRenderer(
                        new JSparklinesBarChartTableCellRenderer(
                                PlotOrientation.HORIZONTAL,
                                100.0,
                                reporterGUI.getSparklineColor()
                        )
                );

                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                        true,
                        TableProperties.getLabelWidth() - 20,
                        reporterGUI.getScoreAndConfidenceDecimalFormat()
                );

            } catch (IllegalArgumentException e) {

                peptideTable.getColumn("Score").setCellRenderer(
                        new JSparklinesBarChartTableCellRenderer(
                                PlotOrientation.HORIZONTAL,
                                100.0,
                                reporterGUI.getSparklineColor())
                );

                ((JSparklinesBarChartTableCellRenderer) peptideTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                        true,
                        TableProperties.getLabelWidth() - 20,
                        reporterGUI.getScoreAndConfidenceDecimalFormat()
                );

            }

            // the quant plot column
            ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();
            Double amplitude = clusterBuilder.getRatioAmplitude();

            peptideTable.getColumn("Quant").setCellRenderer(
                    new JSparklinesHeatMapTableCellRenderer(
                            GradientColorCoding.ColorGradient.GreenWhiteRed,
                            amplitude
                    )
            );
        }

    }

    /**
     * Update the PSM table cell renderers.
     */
    private void updatePsmTableCellRenderers() {

        if (reporterGUI.getIdentification() != null) {

            // set up the psm color map
            HashMap<Integer, java.awt.Color> psmColorMap = new HashMap<>();
            psmColorMap.put(AGREEMENT_WITH_MODS, reporterGUI.getSparklineColor()); // id software agree with PTM certainty
            psmColorMap.put(AGREEMENT, java.awt.Color.CYAN); // id software agree on peptide but not ptm certainty
            psmColorMap.put(CONFLICT, java.awt.Color.YELLOW); // id software don't agree
            psmColorMap.put(PARTIALLY_MISSING, java.awt.Color.ORANGE); // some id software id'ed some didn't

            // set up the psm tooltip map
            HashMap<Integer, String> psmTooltipMap = new HashMap<>();
            psmTooltipMap.put(AGREEMENT_WITH_MODS, "ID Software Agree");
            psmTooltipMap.put(AGREEMENT, "ID Software Agree - PTM Certainty Issues");
            psmTooltipMap.put(CONFLICT, "ID Software Disagree");
            psmTooltipMap.put(PARTIALLY_MISSING, "First Hit(s) Missing");

            psmTable.getColumn("ID").setCellRenderer(
                    new JSparklinesIntegerColorTableCellRenderer(
                            Color.lightGray,
                            psmColorMap,
                            psmTooltipMap
                    )
            );

            psmTable.getColumn("m/z Error").setCellRenderer(
                    new JSparklinesBarChartTableCellRenderer(
                            PlotOrientation.HORIZONTAL,
                            -reporterGUI.getIdentificationParameters().getSearchParameters().getPrecursorAccuracy(),
                            reporterGUI.getIdentificationParameters().getSearchParameters().getPrecursorAccuracy(), // @TODO: how to handle negative values..?
                            reporterGUI.getSparklineColor(),
                            reporterGUI.getSparklineColor()
                    )
            );

            ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("m/z Error").getCellRenderer()).showNumberAndChart(
                    true,
                    TableProperties.getLabelWidth()
            );

            psmTable.getColumn("Charge").setCellRenderer(
                    new JSparklinesBarChartTableCellRenderer(
                            PlotOrientation.HORIZONTAL,
                            (double) reporterGUI.getMetrics().getMaxCharge(),
                            reporterGUI.getSparklineColor()
                    )
            );

            ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Charge").getCellRenderer()).showNumberAndChart(
                    true,
                    TableProperties.getLabelWidth() - 30
            );

            psmTable.getColumn("").setCellRenderer(
                    new JSparklinesIntegerIconTableCellRenderer(
                            MatchValidationLevel.getIconMap(this.getClass()),
                            MatchValidationLevel.getTooltipMap()
                    )
            );

            try {

                psmTable.getColumn("Confidence").setCellRenderer(
                        new JSparklinesBarChartTableCellRenderer(
                                PlotOrientation.HORIZONTAL,
                                100.0,
                                reporterGUI.getSparklineColor()
                        )
                );

                ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Confidence").getCellRenderer()).showNumberAndChart(
                        true,
                        TableProperties.getLabelWidth() - 20,
                        reporterGUI.getScoreAndConfidenceDecimalFormat()
                );

            } catch (IllegalArgumentException e) {

                psmTable.getColumn("Score").setCellRenderer(
                        new JSparklinesBarChartTableCellRenderer(
                                PlotOrientation.HORIZONTAL,
                                100.0,
                                reporterGUI.getSparklineColor()
                        )
                );

                ((JSparklinesBarChartTableCellRenderer) psmTable.getColumn("Score").getCellRenderer()).showNumberAndChart(
                        true,
                        TableProperties.getLabelWidth() - 20,
                        reporterGUI.getScoreAndConfidenceDecimalFormat()
                );

            }

            // the quant plot column
            ClusterBuilder clusterBuilder = reporterGUI.getClusterBuilder();
            Double amplitude = clusterBuilder.getRatioAmplitude();

            psmTable.getColumn("Quant").setCellRenderer(
                    new JSparklinesHeatMapTableCellRenderer(
                            GradientColorCoding.ColorGradient.GreenWhiteRed,
                            amplitude
                    )
            );
        }
    }
}
