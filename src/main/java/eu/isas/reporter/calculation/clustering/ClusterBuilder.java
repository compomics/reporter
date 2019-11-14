package eu.isas.reporter.calculation.clustering;

import com.compomics.util.experiment.biology.proteins.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.SpectrumMatchesIterator;
import com.compomics.util.experiment.identification.peptide_shaker.Metrics;
import com.compomics.util.experiment.identification.peptide_shaker.PSParameter;
import com.compomics.util.experiment.identification.utils.PeptideUtils;
import com.compomics.util.experiment.io.biology.protein.SequenceProvider;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.math.clustering.KMeansClustering;
import com.compomics.util.parameters.identification.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import eu.isas.reporter.calculation.clustering.keys.PeptideClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.ProteinClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.PsmClusterClassKey;
import eu.isas.reporter.preferences.DisplayPreferences;
import eu.isas.reporter.quantificationdetails.PeptideQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinQuantificationDetails;
import eu.isas.reporter.quantificationdetails.ProteinRatioType;
import eu.isas.reporter.quantificationdetails.PsmQuantificationDetails;
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
    private HashMap<String, ArrayList<Long>> filteredProteinKeys;
    /**
     * The clusters corresponding to every protein.
     */
    private HashMap<Long, ArrayList<String>> proteinClusters;
    /**
     * The index of the protein keys in clusterKeys.
     */
    private HashMap<Long, Integer> proteinKeysIndexes;
    /**
     * The filtered peptide keys indexed by cluster class key.
     */
    private HashMap<String, ArrayList<Long>> filteredPeptideKeys;
    /**
     * The clusters corresponding to every peptide.
     */
    private HashMap<Long, ArrayList<String>> peptideClusters;
    /**
     * The index of the peptide keys in clusterKeys.
     */
    private HashMap<Long, Integer> peptideKeysIndexes;
    /**
     * The filtered PSM keys indexed by cluster class key.
     */
    private HashMap<String, ArrayList<String>> filteredPsmKeys;
    /**
     * The clusters corresponding to every PSM.
     */
    private HashMap<String, ArrayList<String>> psmClusters;
    /**
     * The index of the PSM keys in clusterKeys.
     */
    private HashMap<String, Integer> psmKeysIndexes;
    /**
     * The matches keys of the ratios used for clustering.
     */
    private ArrayList<String> clusterKeys;
    /**
     * The ratios used for clustering.
     */
    private double[][] ratios;
    /**
     * The minimal ratio.
     */
    private Double minRatio = null;
    /**
     * The maximal ratio.
     */
    private Double maxRatio = null;

    /**
     * Constructor.
     */
    public ClusterBuilder() {

    }

    /**
     * Clusters the profiles according to the given parameters.
     *
     * @param identification the identification
     * @param identificationParameters the identification parameters
     * @param sequenceProvider the sequence provider
     * @param metrics the PeptideShaker metrics
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
    public KMeansClustering clusterProfiles(Identification identification, IdentificationParameters identificationParameters, SequenceProvider sequenceProvider, Metrics metrics,
            ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            DisplayPreferences displayPreferences, boolean loadData, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        // Load data if needed
        if (loadData) {
            waitingHandler.setWaitingText("Loading data (1/2). Please Wait...");
            loadData(identification, identificationParameters, sequenceProvider, metrics, displayPreferences, reporterIonQuantification, quantificationFeaturesGenerator, waitingHandler);
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            waitingHandler.setWaitingText("Clustering Data (2/2). Please Wait...");
        } else {
            waitingHandler.setWaitingText("Clustering Data. Please Wait...");
        }

        // Perform the clustering
        KMeansClustering kMeansClutering = null;

        if (ratios.length > 0) {
            String[] keysArray = clusterKeys.toArray(new String[clusterKeys.size()]);
            int numClusters = displayPreferences.getClusteringSettings().getKMeansClusteringSettings().getnClusters();
            if (ratios.length < numClusters) {
                displayPreferences.getClusteringSettings().getKMeansClusteringSettings().setnClusters(ratios.length);
            }

            kMeansClutering = new KMeansClustering(ratios, keysArray, displayPreferences.getClusteringSettings().getKMeansClusteringSettings().getnClusters());
            kMeansClutering.kMeanCluster(waitingHandler);
        }

        return kMeansClutering;
    }

    /**
     * Filters the proteins and indexes them according to the clustering
     * settings and stores the result in the attribute maps.
     *
     * @param identification the identification
     * @param identificationParameters the identification parameters
     * @param sequenceProvider the sequence provider
     * @param metrics the PeptideShaker metrics
     * @param displayPreferences the display preferences
     * @param reporterIonQuantification the reporter ion quantification
     * @param quantificationFeaturesGenerator the quantification features
     * generator
     * @param waitingHandler the waiting handler
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
    public void loadData(Identification identification, IdentificationParameters identificationParameters, SequenceProvider sequenceProvider, Metrics metrics,
            DisplayPreferences displayPreferences, ReporterIonQuantification reporterIonQuantification,
            QuantificationFeaturesGenerator quantificationFeaturesGenerator, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        ClusteringSettings clusteringSettings = displayPreferences.getClusteringSettings();

        HashSet<Long> proteinKeys = identification.getProteinIdentification();
        HashSet<Long> peptideKeys = identification.getPeptideIdentification();

        int nProteinClusters = clusteringSettings.getSelectedProteinClasses().size();
        int nPeptideClusters = clusteringSettings.getSelectedPeptideClasses().size();
        int nPsmClusters = clusteringSettings.getSelectedPsmClasses().size();
        int progressTotal = 1;
        if (nProteinClusters > 0) {
            progressTotal += proteinKeys.size();
        }
        if (nPeptideClusters > 0) {
            progressTotal += peptideKeys.size();
        }
        if (nPsmClusters > 0) {
            progressTotal += identification.getSpectrumIdentificationSize();
        }

        waitingHandler.resetPrimaryProgressCounter();
        waitingHandler.setPrimaryProgressCounterIndeterminate(false);
        waitingHandler.setMaxPrimaryProgressCounter(progressTotal);
        waitingHandler.increasePrimaryProgressCounter();

        PSParameter psParameter = new PSParameter();
        ArrayList<UrParameter> parameters = new ArrayList<UrParameter>(1);
        parameters.add(psParameter);

        ArrayList<String> sampleIndexes = new ArrayList<String>(reporterIonQuantification.getSampleIndexes());
        Collections.sort(sampleIndexes);

        Integer clusteringIndex = 0;
        clusterKeys = new ArrayList<String>(metrics.getnValidatedProteins());
        ArrayList<double[]> ratiosList = new ArrayList<double[]>(metrics.getnValidatedProteins());

        proteinClusters = new HashMap<Long, ArrayList<String>>(nProteinClusters);
        proteinKeysIndexes = new HashMap<Long, Integer>(metrics.getnValidatedProteins());
        filteredProteinKeys = new HashMap<String, ArrayList<Long>>(metrics.getnValidatedProteins());

        if (nProteinClusters > 0) {

            int selectedRatioType = displayPreferences.getProteinRatioType();
            ProteinRatioType proteinRatioType = ProteinRatioType.getProteinRatioType(selectedRatioType);
            if (proteinRatioType == null) {
                throw new IllegalArgumentException("Ratio type of index " + selectedRatioType + " not recognized.");
            }

            ProteinMatchesIterator proteinMatchesIterator;
            if (quantificationFeaturesGenerator.getQuantificationFeaturesCache().memoryCheck()) {
                proteinMatchesIterator = identification.getProteinMatchesIterator(metrics.getProteinKeys(), waitingHandler);
            } else {
                proteinMatchesIterator = identification.getProteinMatchesIterator(metrics.getProteinKeys(), waitingHandler);
            }

            ProteinMatch proteinMatch;

            while ((proteinMatch = proteinMatchesIterator.next()) != null) {

                long proteinKey = proteinMatch.getKey();
                psParameter = (PSParameter) identification.getProteinMatch(proteinKey).getUrParam(psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {
                    boolean found = false;
                    for (String keyName : clusteringSettings.getSelectedProteinClasses()) {
                        boolean inCluster = true;
                        ProteinClusterClassKey proteinClusterClassKey = clusteringSettings.getProteinClassKey(keyName);
                        if (proteinClusterClassKey.isStarred() && !psParameter.getStarred()) {
                            inCluster = false;
                        }
                        if (inCluster) {
                            ArrayList<Long> tempClusterKeys = filteredProteinKeys.get(keyName);
                            if (tempClusterKeys == null) {
                                tempClusterKeys = new ArrayList<Long>();
                                filteredProteinKeys.put(keyName, tempClusterKeys);
                            }
                            tempClusterKeys.add(proteinKey);
                            ArrayList<String> clusters = proteinClusters.get(proteinKey);
                            if (clusters == null) {
                                clusters = new ArrayList<String>(nProteinClusters);
                                proteinClusters.put(proteinKey, clusters);
                            }
                            clusters.add(keyName);
                            found = true;
                        }
                    }
                    if (found) {
                        ProteinQuantificationDetails quantificationDetails = quantificationFeaturesGenerator.getProteinMatchQuantificationDetails(proteinKey, waitingHandler);
                        double[] proteinRatios = new double[sampleIndexes.size()];

                        for (int sampleIndex = 0; sampleIndex < sampleIndexes.size(); sampleIndex++) {
                            Double ratio;
                            switch (proteinRatioType) {
                                case all:
                                    ratio = quantificationDetails.getRatio(sampleIndexes.get(sampleIndex), reporterIonQuantification.getNormalizationFactors());
                                    break;
                                case shared:
                                    ratio = quantificationDetails.getSharedRatio(sampleIndexes.get(sampleIndex), reporterIonQuantification.getNormalizationFactors());
                                    break;
                                case unique:
                                    ratio = quantificationDetails.getUniqueRatio(sampleIndexes.get(sampleIndex), reporterIonQuantification.getNormalizationFactors());
                                    break;
                                default:
                                    throw new IllegalArgumentException("Ratio type " + proteinRatioType + " not supported.");
                            }

                            if (ratio != null) {
                                if (ratio != 0) {
                                    double logRatio = BasicMathFunctions.log(ratio, 2);
                                    proteinRatios[sampleIndex] = logRatio;
                                    if (maxRatio == null || logRatio > maxRatio) {
                                        maxRatio = logRatio;
                                    }
                                    if (minRatio == null || logRatio < minRatio) {
                                        minRatio = logRatio;
                                    }
                                }
                            }
                        }

                        clusterKeys.add(proteinKey);
                        proteinKeysIndexes.put(proteinKey, clusteringIndex);
                        ratiosList.add(proteinRatios);
                        clusteringIndex++;
                    }
                }
                waitingHandler.increasePrimaryProgressCounter();
            }
        }

        filteredPeptideKeys = new HashMap<String, ArrayList<Long>>(metrics.getnValidatedProteins());
        peptideKeysIndexes = new HashMap<Long, Integer>(metrics.getnValidatedProteins());
        peptideClusters = new HashMap<Long, ArrayList<String>>(nPeptideClusters);

        if (nPeptideClusters > 0) {

            PeptideMatchesIterator peptideMatchesIterator = identification.getPeptideMatchesIterator(waitingHandler);

            PeptideMatch peptideMatch;

            while ((peptideMatch = peptideMatchesIterator.next()) != null) {

                Peptide peptide = peptideMatch.getPeptide();
                long peptideKey = peptideMatch.getKey();
                psParameter = (PSParameter) identification.getPeptideMatch(peptideKey).getUrParam(psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {
                    boolean found = false;
                    for (String keyName : clusteringSettings.getSelectedPeptideClasses()) {
                        boolean inCluster = true;
                        PeptideClusterClassKey peptideClusterClassKey = clusteringSettings.getPeptideClassKey(keyName);
                        if (peptideClusterClassKey.isStarred() && !psParameter.getStarred()) {
                            inCluster = false;
                        }
                        if (inCluster && peptideClusterClassKey.isNotModified()) {
                            if (peptide.getNVariableModifications() > 0) {
                                inCluster = false;
                                break;
                            }
                        }
                        if (inCluster && peptideClusterClassKey.getPossiblePtms() != null) {
                            boolean possiblePtms = false;
                            if (peptide.getNVariableModifications() > 0) {
                                for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {
                                    if (peptideClusterClassKey.getPossiblePtmsAsSet().contains(modificationMatch.getModification())) {
                                        possiblePtms = true;
                                        break;
                                    }
                                }
                            }
                            if (!possiblePtms) {
                                inCluster = false;
                            }
                        }
                        if (inCluster && peptideClusterClassKey.getForbiddenPtms() != null) {
                            boolean forbiddenPtms = false;
                            if (peptide.getNVariableModifications() > 0) {
                                for (ModificationMatch modificationMatch : peptide.getVariableModifications()) {
                                    if (peptideClusterClassKey.getForbiddenPtmsAsSet().contains(modificationMatch.getModification())) {
                                        forbiddenPtms = true;
                                        break;
                                    }
                                }
                            }
                            if (forbiddenPtms) {
                                inCluster = false;
                            }
                        }
                        if (inCluster && peptideClusterClassKey.isNTerm() && PeptideUtils.isNterm(peptide, sequenceProvider)) {
                            inCluster = false;
                        }
                        if (inCluster && peptideClusterClassKey.isCTerm() && PeptideUtils.isCterm(peptide, sequenceProvider)) {
                            inCluster = false;
                        }
                        if (inCluster) {
                            ArrayList<Long> tempClusterKeys = filteredPeptideKeys.get(keyName);
                            if (tempClusterKeys == null) {
                                tempClusterKeys = new ArrayList<Long>();
                                filteredPeptideKeys.put(keyName, tempClusterKeys);
                            }
                            tempClusterKeys.add(peptideKey);
                            ArrayList<String> clusters = peptideClusters.get(peptideKey);
                            if (clusters == null) {
                                clusters = new ArrayList<String>(nPeptideClusters);
                                peptideClusters.put(peptideKey, clusters);
                            }
                            clusters.add(keyName);
                            found = true;
                        }
                    }
                    if (found) {
                        PeptideQuantificationDetails quantificationDetails = quantificationFeaturesGenerator.getPeptideMatchQuantificationDetails(peptideMatch, waitingHandler);
                        double[] peptideRatios = new double[sampleIndexes.size()];

                        for (int sampleIndex = 0; sampleIndex < sampleIndexes.size(); sampleIndex++) {
                            Double ratio = quantificationDetails.getRatio(sampleIndexes.get(sampleIndex), reporterIonQuantification.getNormalizationFactors());
                            if (ratio != null) {
                                if (ratio != 0) {
                                    double logRatio = BasicMathFunctions.log(ratio, 2);
                                    peptideRatios[sampleIndex] = logRatio;
                                    if (maxRatio == null || logRatio > maxRatio) {
                                        maxRatio = logRatio;
                                    }
                                    if (minRatio == null || logRatio < minRatio) {
                                        minRatio = logRatio;
                                    }
                                }
                            }
                        }

                        clusterKeys.add(peptideKey);
                        peptideKeysIndexes.put(peptideKey, clusteringIndex);
                        ratiosList.add(peptideRatios);
                        clusteringIndex++;
                    }
                }
                waitingHandler.increasePrimaryProgressCounter();
            }
        }

        filteredPsmKeys = new HashMap<String, ArrayList<String>>(metrics.getnValidatedProteins());
        psmKeysIndexes = new HashMap<String, Integer>(metrics.getnValidatedProteins());
        psmClusters = new HashMap<String, ArrayList<String>>(nPsmClusters);

        if (nPsmClusters > 0) {

            HashSet<String> neededFiles = new HashSet<String>();
            for (String keyName : clusteringSettings.getSelectedPsmClasses()) {
                PsmClusterClassKey psmClusterClassKey = clusteringSettings.getPsmClassKey(keyName);
                if (psmClusterClassKey.getFile() == null) {
                    neededFiles.addAll(identification.getOrderedSpectrumFileNames());
                    break;
                }
                neededFiles.add(psmClusterClassKey.getFile());
            }

            for (String spectrumFile : neededFiles) {

                SpectrumMatchesIterator spectrumMatchesIterator = identification.getSpectrumMatchesIterator(waitingHandler); //@TODO: sort the PSMs in some way?
                SpectrumMatch spectrumMatch;

                while ((spectrumMatch = spectrumMatchesIterator.next()) != null) {

                    String spectrumKey = spectrumMatch.getSpectrumKey();
                    psParameter = (PSParameter) identification.getSpectrumMatch(spectrumMatch.getKey()).getUrParam(psParameter);

                    if (psParameter.getMatchValidationLevel().isValidated()) {
                        boolean found = false;
                        for (String keyName : clusteringSettings.getSelectedPsmClasses()) {
                            boolean inCluster = true;
                            PsmClusterClassKey psmClusterClassKey = clusteringSettings.getPsmClassKey(keyName);
                            if (psmClusterClassKey.getFile() != null && !spectrumFile.equals(psmClusterClassKey.getFile())) {
                                inCluster = false;
                            }
                            if (inCluster && psmClusterClassKey.isStarred() && !psParameter.getStarred()) {
                                inCluster = false;
                            }
                            if (inCluster) {
                                ArrayList<String> tempClusterKeys = filteredPsmKeys.get(keyName);
                                if (tempClusterKeys == null) {
                                    tempClusterKeys = new ArrayList<String>();
                                    filteredPsmKeys.put(keyName, tempClusterKeys);
                                }
                                tempClusterKeys.add(spectrumKey);
                                ArrayList<String> clusters = psmClusters.get(spectrumKey);
                                if (clusters == null) {
                                    clusters = new ArrayList<String>(nPsmClusters);
                                    psmClusters.put(spectrumKey, clusters);
                                }
                                clusters.add(keyName);
                                found = true;
                            }
                        }
                        if (found) {
                            PsmQuantificationDetails quantificationDetails = quantificationFeaturesGenerator.getPSMQuantificationDetails(spectrumKey);
                            double[] psmRatios = new double[sampleIndexes.size()];

                            for (int sampleIndex = 0; sampleIndex < sampleIndexes.size(); sampleIndex++) {
                                Double ratio = quantificationDetails.getRatio(sampleIndexes.get(sampleIndex), reporterIonQuantification.getNormalizationFactors());
                                if (ratio != null) {
                                    if (ratio != 0) {
                                        double logRatio = BasicMathFunctions.log(ratio, 2);
                                        psmRatios[sampleIndex] = logRatio;
                                        if (maxRatio == null || logRatio > maxRatio) {
                                            maxRatio = logRatio;
                                        }
                                        if (minRatio == null || logRatio < minRatio) {
                                            minRatio = logRatio;
                                        }
                                    }
                                }
                            }

                            clusterKeys.add(spectrumKey);
                            psmKeysIndexes.put(spectrumKey, clusteringIndex);
                            ratiosList.add(psmRatios);
                            clusteringIndex++;
                        }
                    }
                    waitingHandler.increasePrimaryProgressCounter();
                }
            }
        }
        ratios = ratiosList.toArray(new double[ratiosList.size()][sampleIndexes.size()]);
    }

    /**
     * Returns the protein keys retained after filtering.
     *
     * @return the protein keys retained after filtering
     */
    public Set<Long> getFilteredProteins() {
        return proteinClusters.keySet();
    }

    /**
     * Returns the peptide keys retained after filtering.
     *
     * @return the peptide keys retained after filtering
     */
    public Set<Long> getFilteredPeptides() {
        return peptideClusters.keySet();
    }

    /**
     * Returns the PSM keys retained after filtering.
     *
     * @return the PSM keys retained after filtering
     */
    public Set<Long> getFilteredPsms() {
        return psmClusters.keySet();
    }

    /**
     * Returns the minimal ratio included in the clusters.
     *
     * @return the minimal ratio included in the clusters
     */
    public Double getMinRatio() {
        return minRatio;
    }

    /**
     * Returns the maximal ratio included in the clusters.
     *
     * @return the maximal ratio included in the clusters
     */
    public Double getMaxRatio() {
        return maxRatio;
    }

    /**
     * Returns the maximal value between the absolute value of the min and max
     * ratios.
     *
     * @return the maximal value between the absolute value of the min and max
     * ratios
     */
    public Double getRatioAmplitude() {
        return Math.max(Math.abs(minRatio), Math.abs(maxRatio));
    }

    /**
     * Returns the index of the cluster of the given PSM key. Null if not
     * found or not a PSM.
     *
     * @param key the PSM key
     *
     * @return the index in the cluster
     */
    public Integer getPsmIndex(Long key) {
        return psmKeysIndexes.get(key);
    }

    /**
     * Returns the index of the cluster of the given peptide key. Null if
     * not found or not a peptide.
     *
     * @param key the peptide key
     *
     * @return the index in the cluster
     */
    public Integer getPeptideIndex(Long key) {
        return peptideKeysIndexes.get(key);
    }

    /**
     * Returns the index of the cluster of the given protein key. Null if
     * not found or not a protein.
     *
     * @param key the protein key
     *
     * @return the index in the cluster
     */
    public Integer getProteinIndex(Long key) {
        return proteinKeysIndexes.get(key);
    }

    /**
     * Returns the cluster classes corresponding to a protein match.
     *
     * @param key the match key
     *
     * @return the cluster classes corresponding to this protein
     */
    public ArrayList<String> getProteinClasses(Long key) {
        return proteinClusters.get(key);
    }

    /**
     * Returns the cluster classes corresponding to a peptide match.
     *
     * @param key the match key
     *
     * @return the cluster classes corresponding to this peptide
     */
    public ArrayList<String> getPeptideClasses(Long key) {
        return peptideClusters.get(key);
    }

    /**
     * Returns the cluster classes corresponding to a PSM.
     *
     * @param key the match key
     *
     * @return the cluster classes corresponding to this PSM
     */
    public ArrayList<String> getPsmClasses(Long key) {
        return psmClusters.get(key);
    }
}
