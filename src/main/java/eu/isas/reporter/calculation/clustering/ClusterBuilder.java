package eu.isas.reporter.calculation.clustering;

import com.compomics.util.experiment.biology.Peptide;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.ModificationMatch;
import com.compomics.util.experiment.identification.matches.PeptideMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.identification.matches_iterators.PeptideMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.ProteinMatchesIterator;
import com.compomics.util.experiment.identification.matches_iterators.PsmIterator;
import com.compomics.util.experiment.personalization.UrParameter;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.math.clustering.KMeansClustering;
import com.compomics.util.preferences.IdentificationParameters;
import com.compomics.util.waiting.WaitingHandler;
import eu.isas.peptideshaker.parameters.PSParameter;
import eu.isas.peptideshaker.utils.Metrics;
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
    private HashMap<String, ArrayList<String>> filteredProteinKeys;
    /**
     * The clusters corresponding to every protein.
     */
    private HashMap<String, ArrayList<String>> proteinClusters;
    /**
     * The index of the protein keys in clusterKeys.
     */
    private HashMap<String, Integer> proteinKeysIndexes;
    /**
     * The filtered peptide keys indexed by cluster class key.
     */
    private HashMap<String, ArrayList<String>> filteredPeptideKeys;
    /**
     * The clusters corresponding to every peptide.
     */
    private HashMap<String, ArrayList<String>> peptideClusters;
    /**
     * The index of the peptide keys in clusterKeys.
     */
    private HashMap<String, Integer> peptideKeysIndexes;
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
    public KMeansClustering clusterProfiles(Identification identification, IdentificationParameters identificationParameters, Metrics metrics,
            ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator,
            DisplayPreferences displayPreferences, boolean loadData, WaitingHandler waitingHandler)
            throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        waitingHandler.setSecondaryProgressCounterIndeterminate(true);

        // Load data if needed
        if (loadData) {
            waitingHandler.setWaitingText("Loading data (1/2). Please Wait...");
            loadData(identification, identificationParameters, metrics, displayPreferences, reporterIonQuantification, quantificationFeaturesGenerator, waitingHandler);
            waitingHandler.setSecondaryProgressCounterIndeterminate(true);
            waitingHandler.setWaitingText("Clustering Data (2/2). Please Wait...");
        } else {
            waitingHandler.setWaitingText("Clustering Data. Please Wait...");
        }

        // Perform the clustering
        String[] keysArray = clusterKeys.toArray(new String[clusterKeys.size()]);
        int numClusters = displayPreferences.getClusteringSettings().getKMeansClusteringSettings().getnClusters();
        if (ratios.length < numClusters) {
            displayPreferences.getClusteringSettings().getKMeansClusteringSettings().setnClusters(ratios.length);
        }
        KMeansClustering kMeansClutering = new KMeansClustering(ratios, keysArray, displayPreferences.getClusteringSettings().getKMeansClusteringSettings().getnClusters());
        kMeansClutering.kMeanCluster(waitingHandler);

        return kMeansClutering;
    }

    /**
     * Filters the proteins and indexes them according to the clustering
     * settings and stores the result in the attribute maps.
     *
     * @param identification the identification
     * @param identificationParameters the identification parameters
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
    public void loadData(Identification identification, IdentificationParameters identificationParameters, Metrics metrics, DisplayPreferences displayPreferences, ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator, WaitingHandler waitingHandler) throws SQLException, IOException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        ClusteringSettings clusteringSettings = displayPreferences.getClusteringSettings();

        HashSet<String> proteinKeys = identification.getProteinIdentification();
        HashSet<String> peptideKeys = identification.getPeptideIdentification();

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

        proteinClusters = new HashMap<String, ArrayList<String>>(nProteinClusters);
        proteinKeysIndexes = new HashMap<String, Integer>(metrics.getnValidatedProteins());
        filteredProteinKeys = new HashMap<String, ArrayList<String>>(metrics.getnValidatedProteins());

        if (nProteinClusters > 0) {

            int selectedRatioType = displayPreferences.getProteinRatioType();
            ProteinRatioType proteinRatioType = ProteinRatioType.getProteinRatioType(selectedRatioType);
            if (proteinRatioType == null) {
                throw new IllegalArgumentException("Ratio type of index " + selectedRatioType + " not recognized.");
            }

            ProteinMatchesIterator proteinMatchesIterator;
            if (quantificationFeaturesGenerator.getQuantificationFeaturesCache().memoryCheck()) {
                proteinMatchesIterator = identification.getProteinMatchesIterator(metrics.getProteinKeys(), parameters, false, null, false, null, waitingHandler);
            } else {
                proteinMatchesIterator = identification.getProteinMatchesIterator(metrics.getProteinKeys(), parameters, true, parameters, true, parameters, waitingHandler);
            }

            while (proteinMatchesIterator.hasNext() && !waitingHandler.isRunCanceled()) {

                ProteinMatch proteinMatch = proteinMatchesIterator.next();
                String proteinKey = proteinMatch.getKey();
                psParameter = (PSParameter) identification.getProteinMatchParameter(proteinKey, psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {
                    boolean found = false;
                    for (String keyName : clusteringSettings.getSelectedProteinClasses()) {
                        boolean inCluster = true;
                        ProteinClusterClassKey proteinClusterClassKey = clusteringSettings.getProteinClassKey(keyName);
                        if (proteinClusterClassKey.isStarred() && !psParameter.isStarred()) {
                            inCluster = false;
                        }
                        if (inCluster) {
                            ArrayList<String> tempClusterKeys = filteredProteinKeys.get(keyName);
                            if (tempClusterKeys == null) {
                                tempClusterKeys = new ArrayList<String>();
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

        filteredPeptideKeys = new HashMap<String, ArrayList<String>>(metrics.getnValidatedProteins());
        peptideKeysIndexes = new HashMap<String, Integer>(metrics.getnValidatedProteins());
        peptideClusters = new HashMap<String, ArrayList<String>>(nPeptideClusters);

        if (nPeptideClusters > 0) {

            PeptideMatchesIterator peptideMatchesIterator; //@TODO: sort the peptides in some way?
            if (quantificationFeaturesGenerator.getQuantificationFeaturesCache().memoryCheck()) {
                peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, false, null, waitingHandler);
            } else {
                peptideMatchesIterator = identification.getPeptideMatchesIterator(parameters, true, parameters, waitingHandler);
            }

            while (peptideMatchesIterator.hasNext() && !waitingHandler.isRunCanceled()) {

                PeptideMatch peptideMatch = peptideMatchesIterator.next();
                Peptide peptide = peptideMatch.getTheoreticPeptide();
                String peptideKey = peptideMatch.getKey();
                psParameter = (PSParameter) identification.getPeptideMatchParameter(peptideKey, psParameter);

                if (psParameter.getMatchValidationLevel().isValidated()) {
                    boolean found = false;
                    for (String keyName : clusteringSettings.getSelectedPeptideClasses()) {
                        boolean inCluster = true;
                        PeptideClusterClassKey peptideClusterClassKey = clusteringSettings.getPeptideClassKey(keyName);
                        if (peptideClusterClassKey.isStarred() && !psParameter.isStarred()) {
                            inCluster = false;
                        }
                        if (inCluster && peptideClusterClassKey.isNotModified()) {
                            if (peptide.getModificationMatches() != null) {
                                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                    if (modificationMatch.isVariable()) {
                                        inCluster = false;
                                        break;
                                    }
                                }
                            }
                        }
                        if (inCluster && peptideClusterClassKey.getPossiblePtms() != null) {
                            boolean possiblePtms = false;
                            if (peptide.getModificationMatches() != null) {
                                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                    if (modificationMatch.isVariable()) {
                                        if (peptideClusterClassKey.getPossiblePtmsAsSet().contains(modificationMatch.getTheoreticPtm())) {
                                            possiblePtms = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (!possiblePtms) {
                                inCluster = false;
                            }
                        }
                        if (inCluster && peptideClusterClassKey.getForbiddenPtms() != null) {
                            boolean forbiddenPtms = false;
                            if (peptide.getModificationMatches() != null) {
                                for (ModificationMatch modificationMatch : peptide.getModificationMatches()) {
                                    if (modificationMatch.isVariable()) {
                                        if (peptideClusterClassKey.getForbiddenPtmsAsSet().contains(modificationMatch.getTheoreticPtm())) {
                                            forbiddenPtms = true;
                                            break;
                                        }
                                    }
                                }
                            }
                            if (forbiddenPtms) {
                                inCluster = false;
                            }
                        }
                        if (inCluster && peptideClusterClassKey.isNTerm() && peptide.isNterm(identificationParameters.getSequenceMatchingPreferences()).isEmpty()) {
                            inCluster = false;
                        }
                        if (inCluster && peptideClusterClassKey.isCTerm() && peptide.isCterm(identificationParameters.getSequenceMatchingPreferences()).isEmpty()) {
                            inCluster = false;
                        }
                        if (inCluster) {
                            ArrayList<String> tempClusterKeys = filteredPeptideKeys.get(keyName);
                            if (tempClusterKeys == null) {
                                tempClusterKeys = new ArrayList<String>();
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

                PsmIterator psmMatchesIterator = identification.getPsmIterator(spectrumFile, parameters, false, waitingHandler); //@TODO: sort the PSMs in some way?

                while (psmMatchesIterator.hasNext() && !waitingHandler.isRunCanceled()) {

                    SpectrumMatch spectrumMatch = psmMatchesIterator.next();
                    String spectrumKey = spectrumMatch.getKey();
                    psParameter = (PSParameter) identification.getSpectrumMatchParameter(spectrumKey, psParameter);

                    if (psParameter.getMatchValidationLevel().isValidated()) {
                        boolean found = false;
                        for (String keyName : clusteringSettings.getSelectedPsmClasses()) {
                            boolean inCluster = true;
                            PsmClusterClassKey psmClusterClassKey = clusteringSettings.getPsmClassKey(keyName);
                            if (psmClusterClassKey.getFile() != null && !spectrumFile.equals(psmClusterClassKey.getFile())) {
                                inCluster = false;
                            }
                            if (inCluster && psmClusterClassKey.isStarred() && !psParameter.isStarred()) {
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
     * Returns the index of the cluster of the given PSM accession. Null if not
     * found or not a PSM.
     *
     * @param accession the PSM accession
     *
     * @return the index in the cluster
     */
    public Integer getPsmIndex(String accession) {
        return psmKeysIndexes.get(accession);
    }

    /**
     * Returns the index of the cluster of the given peptide accession. Null if
     * not found or not a peptide.
     *
     * @param accession the peptide accession
     *
     * @return the index in the cluster
     */
    public Integer getPeptideIndex(String accession) {
        return peptideKeysIndexes.get(accession);
    }

    /**
     * Returns the index of the cluster of the given protein accession. Null if
     * not found or not a protein.
     *
     * @param accession the protein accession
     *
     * @return the index in the cluster
     */
    public Integer getProteinIndex(String accession) {
        return proteinKeysIndexes.get(accession);
    }
    
    /**
     * Returns the cluster classes corresponding to a protein match.
     * 
     * @param key the match key
     * 
     * @return the cluster classes corresponding to this protein
     */
    public ArrayList<String> getProteinClasses(String key) {
        return proteinClusters.get(key);
    }
    
    /**
     * Returns the cluster classes corresponding to a peptide match.
     * 
     * @param key the match key
     * 
     * @return the cluster classes corresponding to this peptide
     */
    public ArrayList<String> getPeptideClasses(String key) {
        return peptideClusters.get(key);
    }
    
    /**
     * Returns the cluster classes corresponding to a PSM.
     * 
     * @param key the match key
     * 
     * @return the cluster classes corresponding to this PSM
     */
    public ArrayList<String> getPsmClasses(String key) {
        return psmClusters.get(key);
    }
}
