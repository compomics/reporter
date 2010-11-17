package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.quantification.reporterion.CorrectionFactor;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import java.util.ArrayList;
import java.util.HashMap;
import org.ujmp.core.doublematrix.calculation.general.decomposition.Ginv;

/**
 * @TODO: JavaDoc missing
 *
 * @author Marc Vaudel
 */
public class Deisotoper {

    private double[][] correctionMatrix;

    private ReporterMethod method;

    /**
     * @TODO: JavaDoc missing
     *
     * @param method
     */
    public Deisotoper(ReporterMethod method) {
        this.method = method;
        estimateCorrectionMatrix();
    }

    /**
     * @TODO: JavaDoc missing
     */
    private void estimateCorrectionMatrix() {
        ArrayList<CorrectionFactor> correctionFactors = method.getCorrectionFactors();
        ArrayList<ReporterIon> reporterIons = method.getReporterIons();
        int dimension = correctionFactors.size();
        double[][] coefficients = new double[dimension][dimension];
        for (int i = 0 ; i < dimension ; i++) {
            for (int j = 0 ; j < dimension ; j++) {
                if (reporterIons.get(i).getIndex() - correctionFactors.get(j).getIonId() == -2) {
                    coefficients[i][j] = correctionFactors.get(j).getMinus2() / 100.0;
                } else if (reporterIons.get(i).getIndex() - correctionFactors.get(j).getIonId() == -1) {
                    coefficients[i][j] = correctionFactors.get(j).getMinus1() / 100.0;
                } else if (reporterIons.get(i).getIndex() - correctionFactors.get(j).getIonId() == 0) {
                    coefficients[i][j] = 1 -correctionFactors.get(j).getMinus2() / 100.0
                            -correctionFactors.get(j).getMinus1() / 100.0
                            -correctionFactors.get(j).getPlus1() / 100.0
                            -correctionFactors.get(j).getPlus2() / 100.0;
                } else if (reporterIons.get(i).getIndex() - correctionFactors.get(j).getIonId() == 1) {
                    coefficients[i][j] = correctionFactors.get(j).getPlus1() / 100.0;
                } else if (reporterIons.get(i).getIndex() - correctionFactors.get(j).getIonId() == 2) {
                    coefficients[i][j] = correctionFactors.get(j).getPlus2() / 100.0;
                } else {
                    coefficients[i][j] = 0.0;
                }
            }
        }
        correctionMatrix = Ginv.inverse(coefficients).toDoubleArray();
    }

    /**
     * @TODO: JavaDoc missing
     *
     * @param ionMatches
     * @return
     */
    public HashMap<Integer, Double> deisotope(HashMap<Integer, IonMatch> ionMatches) {
        ArrayList<ReporterIon> reporterIons = method.getReporterIons();
        double[] intensities = new double[reporterIons.size()];
        for (int i = 0 ; i < reporterIons.size() ; i++) {
            if (ionMatches.get(reporterIons.get(i).getIndex()) != null) {
                if (ionMatches.get(reporterIons.get(i).getIndex()).peak != null) {
                intensities[i] = ionMatches.get(reporterIons.get(i).getIndex()).peak.intensity;
                }
            }
        }
        double resultInt;
        HashMap<Integer, Double> result = new HashMap<Integer, Double>();
        for(int i = 0 ; i < correctionMatrix.length ; i++) {
            resultInt = 0;
            for(int j=0 ; j < intensities.length ; j++) {
                resultInt += intensities[j] * correctionMatrix[i][j];
            }
            if (resultInt < 0) {
                resultInt = 0;
            }
            result.put(reporterIons.get(i).getIndex(), resultInt);
        }
        return result;
    }
}
