package eu.isas.reporter.gui.resultpanels;

import com.compomics.util.examples.BareBonesBrowserLaunch;
import com.compomics.util.experiment.annotation.gene.GeneFactory;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.gui.GeneDetailsDialog;
import com.compomics.util.gui.GuiUtilities;
import com.compomics.util.gui.error_handlers.HelpDialog;
import com.compomics.util.gui.tablemodels.SelfUpdatingTableModel;
import com.compomics.util.gui.waiting.waitinghandlers.ProgressDialogX;
import com.compomics.util.math.clustering.KMeansClustering;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.utils.IdentificationFeaturesGenerator;
import eu.isas.reporter.gui.ReporterGUI;
import eu.isas.reporter.gui.tablemodels.ProteinTableModel;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
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
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
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
    private ArrayList<String> proteinKeys = new ArrayList<String>();
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
     * The protein ratio distributions.
     */
    private HashMap<String, Double[]> proteinRatioDistributions;
    /**
     * The max protein ratio (in log2).
     */
    private double maxProteinRatio;
    /**
     * The min protein ratio (in log2).
     */
    private double minProteinRatio;
    /**
     * The currently selected chart panel.
     */
    private ChartPanel selectedChartPanel = null;
    /**
     * The color used for the not selected protein profiles.
     */
    private Color notSelectedProteinProfileColor = new Color(200, 200, 200, 100);
    /**
     * The gene factory.
     */
    private GeneFactory geneFactory = GeneFactory.getInstance();

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
        // set main table properties
        proteinTable.getTableHeader().setReorderingAllowed(false);

        // correct the color for the upper right corner
        JPanel proteinCorner = new JPanel();
        proteinCorner.setBackground(proteinTable.getTableHeader().getBackground());
        proteinScrollPane.setCorner(ScrollPaneConstants.UPPER_RIGHT_CORNER, proteinCorner);

        // add table sorting listeners
        SelfUpdatingTableModel.addSortListener(proteinTable, new ProgressDialogX(reporterGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true));

        // add table scrolling listeners
        SelfUpdatingTableModel.addScrollListeners(proteinTable, proteinScrollPane, proteinScrollPane.getVerticalScrollBar());

        // make sure that the scroll panes are see-through
        proteinScrollPane.getViewport().setOpaque(false);

        setUpTableHeaderToolTips();
    }

    /**
     * Updates the display with the underlying data.
     */
    public void updateDisplay() {

        identification = reporterGUI.getIdentification();
        identificationFeaturesGenerator = reporterGUI.getIdentificationFeaturesGenerator();

        progressDialog = new ProgressDialogX(reporterGUI,
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter-orange.gif")),
                true);
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
                    proteinKeys = reporterGUI.getIdentificationFeaturesGenerator().getProcessedProteinKeys(progressDialog, reporterGUI.getFilterPreferences());
                    identification.loadProteinMatches(proteinKeys, progressDialog, false);
                    identification.loadProteinMatchParameters(proteinKeys, new PSParameter(), progressDialog, false);

                    // get the protein ratio distributions
                    getProteinRatioDistributions(progressDialog);

                    // display the clusters
                    displayClusters(progressDialog);

                    if (reporterGUI.getIdentificationDisplayPreferences().showScores()) {
                        proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Score");
                    } else {
                        proteinTableToolTips.set(proteinTable.getColumnCount() - 2, "Protein Confidence");
                    }

                    // enable the contextual export options
                    exportProteinsJButton.setEnabled(true);

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
     * Get the protein ratio distributions.
     *
     * @param waitingHandler waiting handler displaying progress to the user and
     * allowing canceling the process
     */
    private void getProteinRatioDistributions(WaitingHandler waitingHandler) {

        ArrayList<String> sampleIndexes = new ArrayList<String>(reporterGUI.getReporterIonQuantification().getSampleIndexes());
        Collections.sort(sampleIndexes);

        proteinRatioDistributions = new HashMap<String, Double[]>();
        maxProteinRatio = Double.MIN_VALUE;
        minProteinRatio = Double.MAX_VALUE;

        try {
            for (String sampleIndex : sampleIndexes) {

                ArrayList<Double> data = new ArrayList<Double>();

                for (String proteinKey : proteinKeys) {

                    PSParameter psParameter = (PSParameter) reporterGUI.getIdentification().getProteinMatchParameter(proteinKey, new PSParameter());

                    if (psParameter.getMatchValidationLevel().isValidated()) {
                        ProteinQuantificationDetails quantificationDetails = reporterGUI.getQuantificationFeaturesGenerator().getProteinMatchQuantificationDetails(proteinKey, waitingHandler);
                        Double ratio = quantificationDetails.getRatio(sampleIndex);

                        if (ratio != null) {
                            if (ratio != 0) {
                                ratio = Math.log(ratio) / Math.log(2);
                            }
                            data.add(ratio);

                            if (ratio > maxProteinRatio) {
                                maxProteinRatio = ratio;
                            }
                            if (ratio < minProteinRatio) {
                                minProteinRatio = ratio;
                            }
                        }
                    }
                }

                Double[] values = new Double[data.size()];
                for (int i = 0; i < data.size(); i++) {
                    values[i] = data.get(i);
                }

                proteinRatioDistributions.put(sampleIndex, values);
            }
        } catch (Exception e) {
            reporterGUI.catchException(e);
        }
    }

    /**
     * Display the clusters.
     *
     * @param waitingHandler the waiting handler
     */
    private void displayClusters(WaitingHandler waitingHandler) {

        plotPanel.removeAll();

        ArrayList<String> sampleIndexes = new ArrayList<String>(reporterGUI.getReporterIonQuantification().getSampleIndexes());
        Collections.sort(sampleIndexes);

        KMeansClustering kMeansClustering = reporterGUI.getkMeansClutering();

        for (int clusterIndex = 0; clusterIndex < kMeansClustering.getNumberOfClusters() && !waitingHandler.isRunCanceled(); clusterIndex++) {

            DefaultCategoryDataset chartDataset = new DefaultCategoryDataset();
            ArrayList<String> clusterMembers = kMeansClustering.getClusterMembers(clusterIndex);
            HashMap<String, ArrayList<Double>> allValues = kMeansClustering.getClusterMembersData(clusterIndex);

            for (String tempProteinKey : clusterMembers) {

                if (waitingHandler.isRunCanceled()) {
                    break;
                }

                ArrayList<Double> tempValues = allValues.get(tempProteinKey);

                for (int sampleIndex = 0; sampleIndex < sampleIndexes.size() && !waitingHandler.isRunCanceled(); sampleIndex++) {
                    chartDataset.addValue(tempValues.get(sampleIndex), tempProteinKey, sampleIndexes.get(sampleIndex));
                }
            }

            addClusterChart(chartDataset);
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
                false); // urls

        // set the background and gridline colors
        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(Color.WHITE);
        plot.setRangeGridlinePaint(Color.LIGHT_GRAY);

        // set the renderer
        CategoryItemRenderer renderer = new LineAndShapeRenderer(true, false);
        for (int i = 0; i < dataset.getRowCount(); i++) {
            renderer.setSeriesStroke(i, new BasicStroke(LINE_WIDTH, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            renderer.setSeriesPaint(i, notSelectedProteinProfileColor);
        }

        plot.setRenderer(renderer);

        // change the margin at the top and bottom of the range axis
        final ValueAxis rangeAxis = plot.getRangeAxis();
        rangeAxis.setLowerMargin(0.15);
        rangeAxis.setUpperMargin(0.15);

        // make sure that the chart has a symmetrical y-axis
        double maxAbsRatioValue = Math.max(Math.abs(maxProteinRatio), Math.abs(minProteinRatio));
        rangeAxis.setUpperBound(maxAbsRatioValue);
        rangeAxis.setLowerBound(-maxAbsRatioValue);

        // create the chart panel
        lineChartChartPanel = new ChartPanel(chart);

        // add tooltip
        lineChartChartPanel.setToolTipText("#Proteins: " + dataset.getRowCount());

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

        lineChartChartPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseReleased(MouseEvent e) {
                Component c = e.getComponent();
                if (c instanceof ChartPanel) {

                    if (selectedChartPanel != null) {
                        selectedChartPanel.setBorder(null);

                        // reset the selection
                        CategoryItemRenderer renderer = selectedChartPanel.getChart().getCategoryPlot().getRenderer();
                        DefaultCategoryDataset dataset = (DefaultCategoryDataset) selectedChartPanel.getChart().getCategoryPlot().getDataset();

                        for (int i = 0; i < dataset.getRowCount(); i++) {
                            renderer.setSeriesPaint(i, notSelectedProteinProfileColor);
                        }

                        selectedChartPanel.getChart().fireChartChanged();
                    }

                    ChartPanel chartPanel = (ChartPanel) c;
                    chartPanel.setBorder(new LineBorder(Color.DARK_GRAY));
                    chartPanel.getChart().fireChartChanged();

                    selectedChartPanel = chartPanel;

                    // get the current protein keys
                    DefaultCategoryDataset dataset = (DefaultCategoryDataset) chartPanel.getChart().getCategoryPlot().getDataset();

                    List rowKeys = dataset.getRowKeys();
                    proteinKeys = new ArrayList<String>();

                    for (Object proteinKey : rowKeys) {
                        proteinKeys.add((String) proteinKey);
                    }

                    // update the table model
                    if (proteinTable.getRowCount() > 0) {
                        ((ProteinTableModel) proteinTable.getModel()).updateDataModel(identification, identificationFeaturesGenerator,
                                reporterGUI.getReporterIonQuantification(), reporterGUI.getQuantificationFeaturesGenerator(),
                                reporterGUI.getDisplayFeaturesGenerator(), reporterGUI.getExceptionHandler(), proteinKeys);
                    } else {
                        ProteinTableModel proteinTableModel = new ProteinTableModel(identification, identificationFeaturesGenerator,
                                reporterGUI.getReporterIonQuantification(), reporterGUI.getQuantificationFeaturesGenerator(),
                                reporterGUI.getDisplayFeaturesGenerator(), reporterGUI.getExceptionHandler(), proteinKeys);
                        proteinTable.setModel(proteinTableModel);
                    }

                    setProteinTableProperties();

                    ((DefaultTableModel) proteinTable.getModel()).fireTableDataChanged();
                    updateProteinTableCellRenderers();

                    String title = ReporterGUI.TITLED_BORDER_HORIZONTAL_PADDING + "Proteins (" + proteinTable.getRowCount() + ")";
                    ((TitledBorder) proteinsLayeredPanel.getBorder()).setTitle(title);
                    proteinsLayeredPanel.repaint();
                }
            }

            @Override
            public void mouseDragged(MouseEvent e) {
//                plotPanel.revalidate();
//                plotPanel.repaint();
            }
        });

        plotPanel.add(lineChartChartPanel);
        plotPanel.validate();
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
        ProteinTableModel.setProteinTableProperties(proteinTable, reporterGUI.getSparklineColor(), reporterGUI.getSparklineColorNonValidated(),
                reporterGUI.getSparklineColorNotFound(), reporterGUI.getSparklineColorDoubtful(), reporterGUI.getScoreAndConfidenceDecimalFormat(),
                this.getClass(), reporterGUI.getMetrics().getMaxProteinKeyLength(), Math.max(Math.abs(minProteinRatio), Math.abs(maxProteinRatio)));

        proteinTable.getModel().addTableModelListener(new TableModelListener() {
            public void tableChanged(TableModelEvent e) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        reselect();
                    }
                });
            }
        });
    }

    /**
     * Reselect the proteins.
     */
    private void reselect() {

        ArrayList<String> selectedProteins = reporterGUI.getSelectedProteins();

        for (String tempProteinKey : selectedProteins) {
            int proteinRow = getProteinRow(tempProteinKey);
            if (proteinRow != -1) {
                proteinTable.addRowSelectionInterval(proteinRow, proteinRow);
            }
        }
    }

    /**
     * Returns the row of a desired protein.
     *
     * @param proteinKey the key of the protein
     * @return the row of the desired protein
     */
    private int getProteinRow(String proteinKey) {
        int modelIndex = proteinKeys.indexOf(proteinKey);
        if (modelIndex >= 0) {
            SelfUpdatingTableModel tableModel = (SelfUpdatingTableModel) proteinTable.getModel();
            return tableModel.getRowNumber(modelIndex);
        } else {
            return -1;
        }
    }

    /**
     * This method is called from within the constructor to initialize the form.
     * WARNING: Do NOT modify this code. The content of this method is always
     * regenerated by the Form Editor.
     */
    @SuppressWarnings("unchecked")
    // <editor-fold defaultstate="collapsed" desc="Generated Code">//GEN-BEGIN:initComponents
    private void initComponents() {

        backgroundLayeredPane = new javax.swing.JLayeredPane();
        overviewJPanel = new javax.swing.JPanel();
        overviewJSplitPane = new javax.swing.JSplitPane();
        ratioPlotsJPanel = new javax.swing.JPanel();
        ratioPlotsMainLayeredPane = new javax.swing.JLayeredPane();
        ratioPlotHelpJButton = new javax.swing.JButton();
        exportRatioPlotContextJButton = new javax.swing.JButton();
        ratioPlotOptionsJButton = new javax.swing.JButton();
        contextMenuRatioPlotBackgroundPanel = new javax.swing.JPanel();
        ratioPlotsTitledPanel = new javax.swing.JPanel();
        plotPanel = new javax.swing.JPanel();
        proteinsJPanel = new javax.swing.JPanel();
        proteinsLayeredPane = new javax.swing.JLayeredPane();
        proteinsHelpJButton = new javax.swing.JButton();
        exportProteinsJButton = new javax.swing.JButton();
        contextMenuProteinsBackgroundPanel = new javax.swing.JPanel();
        proteinsLayeredPanel = new javax.swing.JPanel();
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
        overviewJSplitPane.setOpaque(false);

        ratioPlotsJPanel.setOpaque(false);

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
        ratioPlotsMainLayeredPane.add(ratioPlotHelpJButton);
        ratioPlotHelpJButton.setBounds(930, 0, 10, 19);
        ratioPlotsMainLayeredPane.setLayer(ratioPlotHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        ratioPlotsMainLayeredPane.add(exportRatioPlotContextJButton);
        exportRatioPlotContextJButton.setBounds(920, 0, 10, 19);
        ratioPlotsMainLayeredPane.setLayer(exportRatioPlotContextJButton, javax.swing.JLayeredPane.POPUP_LAYER);

        ratioPlotOptionsJButton.setIcon(new javax.swing.ImageIcon(getClass().getResource("/icons/contextual_menu_gray.png"))); // NOI18N
        ratioPlotOptionsJButton.setToolTipText("Plot Options");
        ratioPlotOptionsJButton.setBorder(null);
        ratioPlotOptionsJButton.setBorderPainted(false);
        ratioPlotOptionsJButton.setContentAreaFilled(false);
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
        ratioPlotsMainLayeredPane.add(ratioPlotOptionsJButton);
        ratioPlotOptionsJButton.setBounds(905, 5, 10, 19);
        ratioPlotsMainLayeredPane.setLayer(ratioPlotOptionsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        ratioPlotsMainLayeredPane.add(contextMenuRatioPlotBackgroundPanel);
        contextMenuRatioPlotBackgroundPanel.setBounds(890, 0, 50, 19);
        ratioPlotsMainLayeredPane.setLayer(contextMenuRatioPlotBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        ratioPlotsTitledPanel.setBackground(new java.awt.Color(255, 255, 255));
        ratioPlotsTitledPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Protein Profile Clusters"));
        ratioPlotsTitledPanel.setOpaque(false);

        plotPanel.setOpaque(false);
        plotPanel.setLayout(new java.awt.GridLayout(3, 4, 25, 25));

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
        proteinsLayeredPane.add(proteinsHelpJButton);
        proteinsHelpJButton.setBounds(930, 0, 10, 19);
        proteinsLayeredPane.setLayer(proteinsHelpJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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
        proteinsLayeredPane.add(exportProteinsJButton);
        exportProteinsJButton.setBounds(920, 0, 10, 19);
        proteinsLayeredPane.setLayer(exportProteinsJButton, javax.swing.JLayeredPane.POPUP_LAYER);

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

        proteinsLayeredPane.add(contextMenuProteinsBackgroundPanel);
        contextMenuProteinsBackgroundPanel.setBounds(910, 0, 40, 19);
        proteinsLayeredPane.setLayer(contextMenuProteinsBackgroundPanel, javax.swing.JLayeredPane.POPUP_LAYER);

        proteinsLayeredPanel.setBorder(javax.swing.BorderFactory.createTitledBorder("Proteins"));
        proteinsLayeredPanel.setOpaque(false);

        proteinScrollPane.setOpaque(false);

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

        javax.swing.GroupLayout proteinsLayeredPanelLayout = new javax.swing.GroupLayout(proteinsLayeredPanel);
        proteinsLayeredPanel.setLayout(proteinsLayeredPanelLayout);
        proteinsLayeredPanelLayout.setHorizontalGroup(
            proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 938, Short.MAX_VALUE)
            .addGroup(proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 918, Short.MAX_VALUE)
                    .addContainerGap()))
        );
        proteinsLayeredPanelLayout.setVerticalGroup(
            proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
            .addGap(0, 327, Short.MAX_VALUE)
            .addGroup(proteinsLayeredPanelLayout.createParallelGroup(javax.swing.GroupLayout.Alignment.LEADING)
                .addGroup(proteinsLayeredPanelLayout.createSequentialGroup()
                    .addContainerGap()
                    .addComponent(proteinScrollPane, javax.swing.GroupLayout.DEFAULT_SIZE, 305, Short.MAX_VALUE)
                    .addContainerGap()))
        );

        proteinsLayeredPane.add(proteinsLayeredPanel);
        proteinsLayeredPanel.setBounds(0, 0, 950, 350);

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
            ArrayList<String> selectedProteins = new ArrayList<String>();
            int[] selectedRowIndexes = proteinTable.getSelectedRows();

            for (int i = 0; i < selectedRowIndexes.length; i++) {
                String proteinKey = proteinKeys.get(((SelfUpdatingTableModel) proteinTable.getModel()).getViewIndex(selectedRowIndexes[i]));
                selectedProteins.add(proteinKey);
            }

            reporterGUI.setSelectedProteins(selectedProteins);

            // create the new dataset with the selected proteins in the front
            DefaultCategoryDataset newDataset = new DefaultCategoryDataset();

            for (int i = 0; i < oldDataset.getRowCount(); i++) {
                String proteinKey = (String) oldDataset.getRowKey(i);
                if (!selectedProteins.contains(proteinKey)) {
                    for (int j = 0; j < oldDataset.getColumnKeys().size(); j++) {
                        newDataset.addValue(oldDataset.getValue(i, j), proteinKey, (String) oldDataset.getColumnKeys().get(j));
                    }
                }
            }

            for (int i = 0; i < oldDataset.getRowCount(); i++) {
                String proteinKey = (String) oldDataset.getRowKey(i);
                if (selectedProteins.contains(proteinKey)) {
                    for (int j = 0; j < oldDataset.getColumnKeys().size(); j++) {
                        newDataset.addValue(oldDataset.getValue(i, j), proteinKey, (String) oldDataset.getColumnKeys().get(j));
                    }
                }
            }

            selectedChartPanel.getChart().getCategoryPlot().setDataset(newDataset);

            // update the renderer
            CategoryItemRenderer renderer = selectedChartPanel.getChart().getCategoryPlot().getRenderer();

            for (int i = 0; i < newDataset.getRowCount(); i++) {
                String proteinKey = (String) newDataset.getRowKey(i);
                if (selectedProteins.contains(proteinKey)) {
                    renderer.setSeriesPaint(i, new Color(255, 0, 0, 255)); // @TODO: have separate colors for the selected proteins?
                } else {
                    renderer.setSeriesPaint(i, notSelectedProteinProfileColor);
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

                String proteinKey = proteinKeys.get(proteinIndex);

                // open the gene details dialog
                if (column == proteinTable.getColumn("Chr").getModelIndex() && evt != null
                        && evt.getButton() == MouseEvent.BUTTON1 && geneFactory.isMappingFileOpen()) {
                    try {
                        new GeneDetailsDialog(reporterGUI, proteinKey);
                    } catch (Exception ex) {
                        reporterGUI.catchException(ex);
                    }
                }

                // open protein link in web browser
                if (column == proteinTable.getColumn("Accession").getModelIndex() && evt != null && evt.getButton() == MouseEvent.BUTTON1
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
        } else if (evt.getButton() == MouseEvent.BUTTON3) {
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

        if (row != -1 && column != -1 && column == proteinTable.getColumn("Accession").getModelIndex() && proteinTable.getValueAt(row, column) != null) {

            String tempValue = (String) proteinTable.getValueAt(row, column);

            if (tempValue.lastIndexOf("<html>") != -1) {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
            } else {
                this.setCursor(new java.awt.Cursor(java.awt.Cursor.DEFAULT_CURSOR));
            }
        } else if (column == proteinTable.getColumn("PI").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (column == proteinTable.getColumn("Chr").getModelIndex() && proteinTable.getValueAt(row, column) != null && geneFactory.isMappingFileOpen()) {
            this.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        } else if (column == proteinTable.getColumn("Description").getModelIndex() && proteinTable.getValueAt(row, column) != null) {
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
        if (evt.getKeyCode() == KeyEvent.VK_UP || evt.getKeyCode() == KeyEvent.VK_DOWN
                || evt.getKeyCode() == KeyEvent.VK_PAGE_UP || evt.getKeyCode() == KeyEvent.VK_PAGE_DOWN) {
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
        new HelpDialog(reporterGUI, getClass().getResource("/helpFiles/OverviewTabReporter.html"), null, // @TODO: write help
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                "Proteins Help");

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
        new HelpDialog(reporterGUI, getClass().getResource("/helpFiles/OverviewTabReporter.html"), null, // @TODO: write help
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/help.GIF")),
                Toolkit.getDefaultToolkit().getImage(getClass().getResource("/icons/reporter.gif")),
                "Plot Help");
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
        setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
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
     * Show the contextual options for the ratio plots.
     *
     * @param evt
     */
    private void ratioPlotOptionsJButtonMouseReleased(java.awt.event.MouseEvent evt) {//GEN-FIRST:event_ratioPlotOptionsJButtonMouseReleased
        //plotPopupMenu.show(ratioPlotOptionsJButton, evt.getX(), evt.getY()); // @TODO: implement?
    }//GEN-LAST:event_ratioPlotOptionsJButtonMouseReleased

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
        backgroundLayeredPane.getComponent(0).setBounds(0, 0, backgroundLayeredPane.getWidth(), backgroundLayeredPane.getHeight());
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
                        proteinsLayeredPane.getComponent(0).getHeight());

                proteinsLayeredPane.getComponent(1).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        proteinsLayeredPane.getComponent(1).getWidth(),
                        proteinsLayeredPane.getComponent(1).getHeight());

                proteinsLayeredPane.getComponent(2).setBounds(
                        proteinsLayeredPane.getWidth() - proteinsLayeredPane.getComponent(2).getWidth() - 5,
                        -3,
                        proteinsLayeredPane.getComponent(2).getWidth(),
                        proteinsLayeredPane.getComponent(2).getHeight());

                // resize the plot area
                proteinsLayeredPane.getComponent(3).setBounds(0, 0, proteinsLayeredPane.getWidth(), proteinsLayeredPane.getHeight());
                proteinsLayeredPane.revalidate();
                proteinsLayeredPane.repaint();

                // move the icons
                ratioPlotsMainLayeredPane.getComponent(0).setBounds(
                        ratioPlotsMainLayeredPane.getWidth() - ratioPlotsMainLayeredPane.getComponent(0).getWidth() - 10,
                        -3,
                        ratioPlotsMainLayeredPane.getComponent(0).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(0).getHeight());

                ratioPlotsMainLayeredPane.getComponent(1).setBounds(
                        ratioPlotsMainLayeredPane.getWidth() - ratioPlotsMainLayeredPane.getComponent(1).getWidth() - 20,
                        -3,
                        ratioPlotsMainLayeredPane.getComponent(1).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(1).getHeight());

                ratioPlotsMainLayeredPane.getComponent(2).setBounds(
                        ratioPlotsMainLayeredPane.getWidth() - ratioPlotsMainLayeredPane.getComponent(2).getWidth() - 32,
                        0,
                        ratioPlotsMainLayeredPane.getComponent(2).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(2).getHeight());

                ratioPlotsMainLayeredPane.getComponent(3).setBounds(
                        ratioPlotsMainLayeredPane.getWidth() - ratioPlotsMainLayeredPane.getComponent(3).getWidth() - 5,
                        -3,
                        ratioPlotsMainLayeredPane.getComponent(3).getWidth(),
                        ratioPlotsMainLayeredPane.getComponent(3).getHeight());

                // resize the plot area
                ratioPlotsMainLayeredPane.getComponent(4).setBounds(0, 0, ratioPlotsMainLayeredPane.getWidth(), ratioPlotsMainLayeredPane.getHeight());
                ratioPlotsMainLayeredPane.revalidate();
                ratioPlotsMainLayeredPane.repaint();
            }
        });
    }//GEN-LAST:event_formComponentResized

    // Variables declaration - do not modify//GEN-BEGIN:variables
    private javax.swing.JLayeredPane backgroundLayeredPane;
    private javax.swing.JPanel contextMenuProteinsBackgroundPanel;
    private javax.swing.JPanel contextMenuRatioPlotBackgroundPanel;
    private javax.swing.JButton exportProteinsJButton;
    private javax.swing.JButton exportRatioPlotContextJButton;
    private javax.swing.JPanel overviewJPanel;
    private javax.swing.JSplitPane overviewJSplitPane;
    private javax.swing.JPanel plotPanel;
    private javax.swing.JScrollPane proteinScrollPane;
    private javax.swing.JTable proteinTable;
    private javax.swing.JButton proteinsHelpJButton;
    private javax.swing.JPanel proteinsJPanel;
    private javax.swing.JLayeredPane proteinsLayeredPane;
    private javax.swing.JPanel proteinsLayeredPanel;
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
            ((JSparklinesArrayListBarChartTableCellRenderer) proteinTable.getColumn("#Spectra").getCellRenderer()).setMaxValue(reporterGUI.getMetrics().getMaxNSpectra());
            ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("MW").getCellRenderer()).setMaxValue(reporterGUI.getMetrics().getMaxMW());

            try {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Confidence").getCellRenderer()).setMaxValue(100.0);
            } catch (IllegalArgumentException e) {
                ((JSparklinesBarChartTableCellRenderer) proteinTable.getColumn("Score").getCellRenderer()).setMaxValue(100.0);
            }
        }
    }
}
