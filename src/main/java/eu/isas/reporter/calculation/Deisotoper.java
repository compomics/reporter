package eu.isas.reporter.calculation;

import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.quantification.reporterion.CorrectionFactor;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.ujmp.core.doublematrix.calculation.general.decomposition.Ginv;

/**
 * This class takes care of the deisotoping of reporter ion intensities.
 *
 * @author Marc Vaudel
 */
public class Deisotoper {

    /**
     * The correction matrix (see Pubmed ID: 19953549).
     */
    private double[][] correctionMatrix;
    /**
     * the reporter method used.
     */
    private ReporterMethod method;

    /**
     * Constructor.
     *
     * @param method the reporter method used
     */
    public Deisotoper(ReporterMethod method) {
        this.method = method;
        estimateCorrectionMatrix();
    }

    /**
     * Method which estimates the correction factors matrix.
     */
    private void estimateCorrectionMatrix() {

        ArrayList<CorrectionFactor> correctionFactors = method.getCorrectionFactors();
        ArrayList<Integer> reporterIonsIndexes = method.getReporterIonIndexes();
        Collections.sort(reporterIonsIndexes);
        int dimension = correctionFactors.size();
        double[][] coefficients = new double[dimension][dimension];

        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (reporterIonsIndexes.get(i) - correctionFactors.get(j).getIonId() == -2) {
                    coefficients[i][j] = correctionFactors.get(j).getMinus2() / 100.0;
                } else if (reporterIonsIndexes.get(i) - correctionFactors.get(j).getIonId() == -1) {
                    coefficients[i][j] = correctionFactors.get(j).getMinus1() / 100.0;
                } else if (reporterIonsIndexes.get(i) - correctionFactors.get(j).getIonId() == 0) {
                    coefficients[i][j] = 1 - correctionFactors.get(j).getMinus2() / 100.0
                            - correctionFactors.get(j).getMinus1() / 100.0
                            - correctionFactors.get(j).getPlus1() / 100.0
                            - correctionFactors.get(j).getPlus2() / 100.0;
                } else if (reporterIonsIndexes.get(i) - correctionFactors.get(j).getIonId() == 1) {
                    coefficients[i][j] = correctionFactors.get(j).getPlus1() / 100.0;
                } else if (reporterIonsIndexes.get(i) - correctionFactors.get(j).getIonId() == 2) {
                    coefficients[i][j] = correctionFactors.get(j).getPlus2() / 100.0;
                } else {
                    coefficients[i][j] = 0.0;
                }
            }
        }
        correctionMatrix = Ginv.inverse(coefficients).toDoubleArray();
    }

    /**
     * This method returns deisotoped intensities.
     *
     * @param ionMatches the ion matches to deisotope
     * @return a map of the deisotoped intensities (ion index -> intensity)
     */
    public HashMap<Integer, Double> deisotope(HashMap<Integer, IonMatch> ionMatches) {

        ArrayList<Integer> reporterIonsIndexes = method.getReporterIonIndexes();
        Collections.sort(reporterIonsIndexes);
        double[] intensities = new double[reporterIonsIndexes.size()];

        for (int i = 0; i < reporterIonsIndexes.size(); i++) {
            if (ionMatches.get(reporterIonsIndexes.get(i)) != null) {
                if (ionMatches.get(reporterIonsIndexes.get(i)).peak != null) {
                    intensities[i] = ionMatches.get(reporterIonsIndexes.get(i)).peak.intensity;
                }
            }
        }

        HashMap<Integer, Double> result = new HashMap<Integer, Double>();

        for (int i = 0; i < correctionMatrix.length; i++) {
            double resultInt = 0;
            for (int j = 0; j < intensities.length; j++) {
                resultInt += intensities[j] * correctionMatrix[i][j];
            }
            if (resultInt < 0) {
                resultInt = 0;
            }
            result.put(reporterIonsIndexes.get(i), resultInt);
        }
        return result;
    }
}
