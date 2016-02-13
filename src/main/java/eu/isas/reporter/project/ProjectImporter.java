package eu.isas.reporter.project;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.identification_parameters.PtmSettings;
import com.compomics.util.preferences.IdentificationParameters;
import eu.isas.reporter.calculation.clustering.keys.PeptideClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.ProteinClusterClassKey;
import eu.isas.reporter.calculation.clustering.keys.PsmClusterClassKey;
import eu.isas.reporter.project.attributes.ClusterMetrics;
import java.util.ArrayList;

/**
 *
 * @author Marc Vaudel
 */
public class ProjectImporter {

    /**
     * Constructor.
     */
    public ProjectImporter() {

    }

    /**
     * Returns the cluster metrics for a given project.
     *
     * @param identificationParameters the identification parameters
     * @param identification the identification
     *
     * @return the cluster metrics for a given project
     */
    public ClusterMetrics getClusterMetrics(IdentificationParameters identificationParameters, Identification identification) {

        ArrayList<ProteinClusterClassKey> proteinClasses = new ArrayList<ProteinClusterClassKey>(1);
        ProteinClusterClassKey proteinClusterClassKey = new ProteinClusterClassKey();
        proteinClasses.add(proteinClusterClassKey);
        proteinClusterClassKey = new ProteinClusterClassKey();
        proteinClusterClassKey.setStarred(Boolean.TRUE);
        proteinClasses.add(proteinClusterClassKey);

        PtmSettings ptmSettings = identificationParameters.getSearchParameters().getPtmSettings();
        ArrayList<PeptideClusterClassKey> peptideClasses = new ArrayList<PeptideClusterClassKey>(4);
        PeptideClusterClassKey peptidelusterClassKey = new PeptideClusterClassKey();
        peptideClasses.add(peptidelusterClassKey);
        peptidelusterClassKey = new PeptideClusterClassKey();
        peptidelusterClassKey.setStarred(Boolean.TRUE);
        peptideClasses.add(peptidelusterClassKey);
        peptidelusterClassKey = new PeptideClusterClassKey();
        peptidelusterClassKey.setnTerm(Boolean.TRUE);
        peptideClasses.add(peptidelusterClassKey);
        peptidelusterClassKey = new PeptideClusterClassKey();
        peptidelusterClassKey.setcTerm(Boolean.TRUE);
        peptideClasses.add(peptidelusterClassKey);
        for (String ptm : ptmSettings.getAllNotFixedModifications()) {
            peptidelusterClassKey = new PeptideClusterClassKey();
            peptidelusterClassKey.setPtm(ptm);
            peptideClasses.add(peptidelusterClassKey);
        }

        ArrayList<PsmClusterClassKey> psmClasses = new ArrayList<PsmClusterClassKey>(1);
        PsmClusterClassKey psmClusterClassKey = new PsmClusterClassKey();
        psmClasses.add(psmClusterClassKey);
        psmClusterClassKey = new PsmClusterClassKey();
        psmClusterClassKey.setStarred(Boolean.TRUE);
        psmClasses.add(psmClusterClassKey);
        ArrayList<String> spectrumFiles = identification.getOrderedSpectrumFileNames();
        if (spectrumFiles.size() > 1) {
            for (String spectrumFile : spectrumFiles) {
                psmClusterClassKey = new PsmClusterClassKey();
                psmClusterClassKey.setFile(spectrumFile);
                psmClasses.add(psmClusterClassKey);
            }
        }

        ClusterMetrics clusterMetrics = new ClusterMetrics();
        clusterMetrics.setProteinClassKeys(proteinClasses);
        clusterMetrics.setPeptideClassKeys(peptideClasses);
        clusterMetrics.setPsmClassKeys(psmClasses);
        return clusterMetrics;
    }

}
