package eu.isas.reporter.calculation.clustering;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.math.clustering.KMeansClustering;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.clustering.keys.ProteinClusterClassKey;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.settings.ClusteringSettings;
import java.io.IOException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Methods for building clusters based on a reporter project.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ClusterBuilder {

    /**
     * The filtered protein keys indexed by cluster class key.
     */
    private HashMap<String, ArrayList<String>> filteredProteinKeys;
    /**
     * The protein ratios indexed by cluster class in an array by protein and
     * sample indexes.
     */
    private HashMap<String, double[][]> proteinClassesRatiosMap;
    /**
     * The clusters corresponding to every protein.
     */
    private HashMap<String, ArrayList<String>> proteinClusters;
    /**
     * The filtered peptide keys indexed by cluster class key.
     */
    private HashMap<String, ArrayList<String>> filteredPeptideKeys;
    /**
     * The peptide ratios indexed by cluster class in an array by protein and
     * sample indexes.
     */
    private HashMap<String, double[][]> peptideClassesRatiosMap;
    /**
     * The clusters corresponding to every peptide.
     */
    private HashMap<String, ArrayList<String>> peptideClusters;
    /**
     * The filtered PSM keys indexed by cluster class key.
     */
    private HashMap<String, ArrayList<String>> filteredPsmKeys;
    /**
     * The PSM ratios indexed by cluster class in an array by protein and
     * sample indexes.
     */
    private HashMap<String, double[][]> psmClassesRatiosMap;
    /**
     * The clusters corresponding to every PSM.
     */
    private HashMap<String, ArrayList<String>> psmClusters;

    /**
     * Constructor.
     */
    public ClusterBuilder() {

    }

    /**
     * Clusters the profiles according to the given parameters.
     *
     * @param identification the identification
     * @param reporterIonQuantification the reporter ion quantification
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param displayPreferences the display preferences
     * @param loadData if true, the data is (re-)loaded
     * @param waitingHandler a waiting handler
     *
     * @return the k-means clustering of every class
     *
     * @throws SQLException if an SQLException occurs
     * @throws IOException if an IOException occurs
     * @throws ClassNotFoundException if a ClassNotFoundException occurs
     * @throws InterruptedException if an InterruptedException occurs
     * @throws MzMLUnmarshallerException if an MzMLUnmarshallerException occurs
     */
    public HashMap<String, KMeansClustering> clusterProfiles(Identification identification, ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, DisplayPreferences displayPreferences, boolean loadData, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        ClusteringSettings clusteringSettings = displayPreferences.getClusteringSettings();

        int progress = 0;
        int nClasses = 0;
        if (!clusteringSettings.getSelectedProteinClasses().isEmpty()) {
            nClasses++;
        }
        if (!clusteringSettings.getSelectedPeptideClasses().isEmpty()) {
            nClasses++;
        }
        if (!clusteringSettings.getSelectedPsmClasses().isEmpty()) {
            nClasses++;
        }
        int totalProgress = 3 * (Math.min(1, nClasses));

        if (loadData) {
            // filter the proteins
            waitingHandler.setWaitingText("Filtering Proteins (" + ++progress + "/" + totalProgress + "). Please Wait...");
            filterProteins(identification, displayPreferences.getClusteringSettings(), waitingHandler);
            //@TODO: peptides and PSMs

            // get the ratios
            waitingHandler.setWaitingText("Extracting Protein Ratios (" + ++progress + "/" + totalProgress + "). Please Wait...");
            getProteinRatios(identification, reporterIonQuantification, quantificationFeaturesGenerator, waitingHandler);
            //@TODO: peptides and PSMs
        }

        HashMap<String, KMeansClustering> clusteringMap = new HashMap<String, KMeansClustering>(clusteringSettings.getSelectedProteinClasses().size() + clusteringSettings.getSelectedPeptideClasses().size() + clusteringSettings.getSelectedPsmClasses().size());

        // Perform the clustering for every class of proteins
        waitingHandler.setSecondaryProgressCounterIndeterminate(true);
        waitingHandler.setWaitingText("Clustering Proteins (" + ++progress + "/" + totalProgress + "). Please Wait...");
        for (String proteinClass : clusteringSettings.getPossibleProteinClasses()) {
            ArrayList<String> proteinsKeys = filteredProteinKeys.get(proteinClass);
            String[] proteinKeysArray = proteinsKeys.toArray(new String[proteinsKeys.size()]);
            double[][] proteinRatios = proteinClassesRatiosMap.get(proteinClass);
            KMeansClustering kMeansClutering = new KMeansClustering(proteinRatios, proteinKeysArray, displayPreferences.getClusteringSettings().getKMeansClusteringSettings().getnClusters());
            kMeansClutering.kMeanCluster(waitingHandler);
            clusteringMap.put(proteinClass, kMeansClutering);
        }

        return clusteringMap;
    }

    /**
     * Filters the proteins and indexes them according to the clustering
     * settings and stores the result in the attribute maps.
     *
     * @param identification the identification
     * @param clusteringSettings the clustering settings
     * @param waitingHandler the waiting handler
     *
     * @throws SQLException if an exception occurs while interacting with the
     * database
     * @throws IOException if an exception occurs while reading or writing a
     * file
     * @throws ClassNotFoundException if a exception occurs while deserializing
     * an object
     * @throws InterruptedException if an threading exception occurs
     */
    public void filterProteins(Identification identification, ClusteringSettings clusteringSettings, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException {

        HashSet<String> proteinKeys = identification.getProteinIdentification();

        waitingHandler.resetPrimaryProgressCounter();
        waitingHandler.setPrimaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxPrimaryProgressCounter(proteinKeys.size() + 1);
        waitingHandler.increasePrimaryProgressCounter();

        filteredProteinKeys = new HashMap<String, ArrayList<String>>(proteinKeys.size());

        int nProteinClusters = clusteringSettings.getSelectedProteinClasses().size();
        if (nProteinClusters > 0) {
            PSParameter psParameter = new PSParameter();
            ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
            parameters.add(psParameter);
            ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(new ArrayList<String>(proteinKeys), parameters, false, null, false, null, waitingHandler);

            while (proteinMatchesIterator.hasNext() && !waitingHandler.isRunCanceled()) {

                ProteinMatch proteinMatch = proteinMatchesIterator.next();
                String proteinKey = proteinMatch.getKey();
                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {
                    for (String keyName : clusteringSettings.getSelectedProteinClasses()) {
                        ProteinClusterClassKey proteinClusterClassKey = clusteringSettings.getProteinClassKey(keyName);
                        if (proteinClusterClassKey.isStarred()) {
                            if (psParameter.isStarred()) {
                                ArrayList<String> clusterKeys = filteredProteinKeys.get(keyName);
                                if (clusterKeys == null) {
                                    clusterKeys = new ArrayList<String>();
                                    filteredProteinKeys.put(keyName, clusterKeys);
                                }
                                clusterKeys.add(proteinKey);
                                ArrayList<String> clusters = proteinClusters.get(proteinKey);
                                if (clusters == null) {
                                    clusters = new ArrayList<String>(nProteinClusters);
                                    proteinClusters.put(proteinKey, clusters);
                                }
                                clusters.add(keyName);
                            }
                        } else {
                            ArrayList<String> clusterKeys = filteredProteinKeys.get(keyName);
                            if (clusterKeys == null) {
                                clusterKeys = new ArrayList<String>();
                                filteredProteinKeys.put(keyName, clusterKeys);
                            }
                            clusterKeys.add(proteinKey);
                            ArrayList<String> clusters = proteinClusters.get(proteinKey);
                            if (clusters == null) {
                                clusters = new ArrayList<String>(nProteinClusters);
                                proteinClusters.put(proteinKey, clusters);
                            }
                            clusters.add(keyName);
                        }
                    }
                }
                waitingHandler.increasePrimaryProgressCounter();
            }
        }
    }

    /**
     * Returns the protein ratios grouped by cluster class in an array by
     * protein and sample index.
     *
     * @param identification the identification
     * @param reporterIonQuantification the reporter ion quantification
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param waitingHandler the waiting handler
     *
     * @return the protein rations
     *
     * @throws SQLException if an exception occurs while interacting with the
     * database
     * @throws IOException if an exception occurs while reading or writing a
     * file
     * @throws ClassNotFoundException if a exception occurs while deserializing
     * an object
     * @throws InterruptedException if an threading exception occurs
     * @throws MzMLUnmarshallerException if an exception occurs while reading an
     * mzML file
     */
    private void getProteinRatios(Identification identification, ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        int totalProgress = proteinClusters.size();
        for (ArrayList<String> keys : filteredProteinKeys.values()) {
            totalProgress += keys.size();
        }

        waitingHandler.resetPrimaryProgressCounter();
        waitingHandler.setPrimaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxPrimaryProgressCounter(totalProgress);
        waitingHandler.increasePrimaryProgressCounter();

        ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

        HashMap<String, double[]> allProteinsRatiosMap = new HashMap<String, double[]>(proteinClusters.size());

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);
        ProteinMatchesIterator proteinMatchesIterator = identification.getProteinMatchesIterator(new ArrayList<String>(proteinClusters.keySet()), parameters, true, parameters, true, parameters, waitingHandler);

        while (proteinMatchesIterator.hasNext() && !waitingHandler.isRunCanceled()) {
            ProteinMatch proteinMatch = proteinMatchesIterator.next();
            String proteinKey = proteinMatch.getKey();
            ProteinQuantificationDetails quantificationDetails = quantificationFeaturesGenerator.getProteinMatchQuantificationDetails(proteinKey, waitingHandler);
            double[] ratios = new double[sampleIndexes.size()];

            for (int sampleIndex = 0; sampleIndex < sampleIndexes.size(); sampleIndex++) {
                Double ratio = quantificationDetails.getRatio(sampleIndexes.get(sampleIndex), reporterIonQuantification.getNormalizationFactors());
                if (ratio != null) {
                    if (ratio != 0) {
                        double logRatio = BasicMathFunctions.log(ratio, 2);
                        ratios[sampleIndex] = logRatio;
                    }
                }
            }
            allProteinsRatiosMap.put(proteinKey, ratios);
            waitingHandler.increasePrimaryProgressCounter();
        }

        proteinClassesRatiosMap = new HashMap<String, double[][]>(filteredProteinKeys.size());
        for (String clusterKey : filteredPeptideKeys.keySet()) {
            int proteinIndex = 0;
            ArrayList<String> proteinKeys = filteredProteinKeys.get(clusterKey);
            double[][] clusterRatios = new double[proteinKeys.size()][sampleIndexes.size()];
            for (String proteinKey : proteinKeys) {
                double[] proteinRatios = allProteinsRatiosMap.get(proteinKey);
                clusterRatios[proteinIndex] = proteinRatios;
                proteinIndex++;
                if (waitingHandler.isRunCanceled()) {
                    break;
                }
            }
            proteinClassesRatiosMap.put(clusterKey, clusterRatios);
            if (waitingHandler.isRunCanceled()) {
                break;
            }
        }
    }
    
    /**
     * Returns the protein keys retained after filtering.
     * 
     * @return the protein keys retained after filtering
     */
    public Set<String> getFilteredProteins() {
        return proteinClusters.keySet();
    }
    
    /**
     * Returns the peptide keys retained after filtering.
     * 
     * @return the peptide keys retained after filtering
     */
    public Set<String> getFilteredPeptides() {
        return peptideClusters.keySet();
    }
    
    /**
     * Returns the PSM keys retained after filtering.
     * 
     * @return the PSM keys retained after filtering
     */
    public Set<String> getFilteredPsms() {
        return psmClusters.keySet();
    }

}
