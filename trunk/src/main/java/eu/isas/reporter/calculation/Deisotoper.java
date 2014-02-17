package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.Atom;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.quantification.reporterion.Reagent;
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
     * The correction matrix corresponding to every label
     */
    private HashMap<String, CorrectionMatrix> correctionMatrices;
    /**
     * the reporter method used.
     */
    private ReporterMethod method;

    /**
     * Constructor.
     *
     * @param method the reporter method used
     * @param tolerance the tolerance to use for reporter ions detection
     */
    public Deisotoper(ReporterMethod method, double tolerance) {
        this.method = method;
        estimateCorrectionFactors(tolerance);
    }

    /**
     * Estimates the correction factors to be applied to every label.
     */
    private void estimateCorrectionFactors(double tolerance) {

        correctionMatrices = new HashMap<String, CorrectionMatrix>();
        ArrayList<String> labels = new ArrayList<String>(method.getReagentNames());
        HashMap<Double, String> massesToLabelMap = new HashMap<Double, String>(labels.size());
        for (String label : labels) {
            double mass = method.getReporterIon(label).getTheoreticMass();
            if (massesToLabelMap.containsKey(mass)) {
                throw new IllegalArgumentException("Two labels were found at the same mass (" + mass + ").");
            }
            massesToLabelMap.put(mass, label);
        }
        while (!labels.isEmpty()) {
            Double maxMass = null, minMass = null;
            for (Double mass : massesToLabelMap.keySet()) {
                if (labels.contains(massesToLabelMap.get(mass))) {
                    if (maxMass == null || mass > maxMass) {
                        maxMass = mass;
                    }
                    if (minMass == null || mass < minMass) {
                        minMass = mass;
                    }
                }
            }
            HashMap<Integer, String> isotopes = new HashMap<Integer, String>();
            int isotopeCount = 0, isotopeMax = 0;
            double isotopeMass = minMass;
            while (isotopeMass <= maxMass + tolerance) {
                for (Double mass : massesToLabelMap.keySet()) {
                    if (mass - 130 > 0) {
                        int debug = 1;
                    }
                    if (labels.contains(massesToLabelMap.get(mass)) && Math.abs(mass - isotopeMass) < tolerance) {
                        if (isotopes.containsKey(isotopeCount)) {
                            throw new IllegalArgumentException("More than one reagent correspond to the mass " + isotopeMass + " using the given tolerance (" + tolerance + ")");
                        }
                        String label = massesToLabelMap.get(mass);
                        isotopes.put(isotopeCount, label);
                        labels.remove(label);
                        isotopeMax = isotopeCount;
                    }
                }
                isotopeCount++;
                isotopeMass += Atom.C.getDifferenceToMonoisotopic(1);
            }
            double[][] coefficients = new double[isotopeCount][isotopeCount];
            ArrayList<String> matrixLabels = new ArrayList<String>();
            for (int i = 0; i <= isotopeMax; i++) {
                String label = isotopes.get(i);
                if (label != null) {
                    matrixLabels.add(label);
                    Reagent reagent = method.getReagent(label);
                    double totalIntensity = reagent.getMinus2() + reagent.getMinus1() + reagent.getRef() + reagent.getPlus1() + reagent.getPlus2();
                    if (i >= 2) {
                        coefficients[i - 2][i] = reagent.getMinus2() / totalIntensity;
                    }
                    if (i >= 1) {
                        coefficients[i - 1][i] = reagent.getMinus1() / totalIntensity;
                    }
                    coefficients[i][i] = reagent.getRef() / totalIntensity;
                    if (i <= isotopeMax - 1) {
                        coefficients[i + 1][i] = reagent.getPlus1() / totalIntensity;
                    }
                    if (i <= isotopeMax - 2) {
                        coefficients[i + 2][i] = reagent.getPlus2() / totalIntensity;
                    }
                }
            }
            coefficients = Ginv.inverse(coefficients).toDoubleArray();
            CorrectionMatrix matrix = new CorrectionMatrix(coefficients, isotopes);
            for (String label : matrixLabels) {
                correctionMatrices.put(label, matrix);
            }
        }

    }

    /**
     * This method returns deisotoped intensities.
     *
     * @param ionMatches the ion matches to deisotope
     *
     * @return a map of the deisotoped intensities (ion index -> intensity)
     */
    public HashMap<String, Double> deisotope(HashMap<String, IonMatch> ionMatches) {

        HashMap<String, Double> result = new HashMap<String, Double>();
        for (String label : method.getReagentNames()) {
            IonMatch refMatch = ionMatches.get(label);
            if (refMatch != null && refMatch.peak.intensity > 0) {
                CorrectionMatrix correctionMatrix = correctionMatrices.get(label);
                HashMap<Integer, String> involvedReagents = correctionMatrix.getReagentsNames();
                int dimension = correctionMatrix.getDimension();
                double[] intensities = new double[dimension];
                int lineNumber = -1;
                for (int i = 0; i < dimension; i++) {
                    String reagentName = involvedReagents.get(i);
                    if (reagentName != null) {
                        if (reagentName.equals(label)) {
                            lineNumber = i;
                        }
                        IonMatch ionMatch = ionMatches.get(reagentName);
                        if (ionMatch != null) {
                            intensities[i] = ionMatch.peak.intensity;
                        }
                    }
                }
                if (lineNumber == -1) {
                    throw new IllegalArgumentException("Index of reagent " + label + " not found in the correction matrix.");
                }
                double resultInt = 0;
                for (int j = 0; j < intensities.length; j++) {
                    resultInt += intensities[j] * correctionMatrix.getValueAt(lineNumber, j);
                }
                if (resultInt < 0) {
                    resultInt = 0;
                }
                result.put(label, resultInt);
            } else {
                result.put(label, 0.0);
            }
        }
        return result;
    }
}
