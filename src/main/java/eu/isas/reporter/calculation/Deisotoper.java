package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.atoms.Atom;
import com.compomics.util.experiment.biology.ions.impl.ReporterIon;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum;
import com.compomics.util.experiment.quantification.reporterion.Reagent;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import eu.isas.reporter.Reporter;
import java.util.ArrayList;
import java.util.HashMap;
import org.ujmp.core.doublematrix.calculation.general.decomposition.Ginv;

/**
 * This class takes care of the deisotoping of reporter ion intensities.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class Deisotoper {

    /**
     * The correction matrix corresponding to every label.
     */
    private HashMap<String, CorrectionMatrix> correctionMatrices;
    /**
     * The reporter method used.
     */
    private ReporterMethod method;

    /**
     * Constructor.
     *
     * @param method the reporter method used
     * @param tolerance the tolerance to use for reporter ions detection
     */
    public Deisotoper(
            ReporterMethod method,
            double tolerance
    ) {
        this.method = method;
        estimateCorrectionFactors(tolerance);
    }

    /**
     * Estimates the correction factors to be applied to every label.
     */
    private void estimateCorrectionFactors(double tolerance) {

        correctionMatrices = new HashMap<>();
        ArrayList<String> labels = new ArrayList<>(method.getReagentNames());
        HashMap<Double, String> massesToLabelMap = new HashMap<>(labels.size());

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

            HashMap<Integer, String> isotopes = new HashMap<>();
            int isotopeCount = 2, isotopeMax = 2;
            double isotopeMass = minMass;

            while (isotopeMass <= maxMass + tolerance) {

                for (Double mass : massesToLabelMap.keySet()) {

                    if (labels.contains(massesToLabelMap.get(mass))
                            && Math.abs(mass - isotopeMass) < tolerance) {

                        if (isotopes.containsKey(isotopeCount)) {
                            throw new IllegalArgumentException(
                                    "More than one reagent correspond to the mass "
                                    + isotopeMass
                                    + " using the given tolerance ("
                                    + tolerance
                                    + ")"
                            );
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

            isotopeCount += 2;

            double refMass = method.getReagent(
                    isotopes.get(2)).getReporterIon().getTheoreticMass()
                    - 2 * Atom.C.getDifferenceToMonoisotopic(1);

            double[][] coefficients = new double[isotopeCount][isotopeCount];

            ArrayList<String> matrixLabels = new ArrayList<>();

            for (int i = 2; i <= isotopeMax; i++) {

                String label = isotopes.get(i);

                if (label != null) {

                    matrixLabels.add(label);
                    Reagent reagent = method.getReagent(label);

                    double totalIntensity = reagent.getMinus2()
                            + reagent.getMinus1()
                            + reagent.getRef()
                            + reagent.getPlus1()
                            + reagent.getPlus2();

                    coefficients[i - 2][i] = reagent.getMinus2() / totalIntensity;
                    coefficients[i - 1][i] = reagent.getMinus1() / totalIntensity;
                    coefficients[i][i] = reagent.getRef() / totalIntensity;
                    coefficients[i + 1][i] = reagent.getPlus1() / totalIntensity;
                    coefficients[i + 2][i] = reagent.getPlus2() / totalIntensity;

                }

            }

            coefficients = Ginv.inverse(coefficients).toDoubleArray();
            CorrectionMatrix matrix = new CorrectionMatrix(coefficients, isotopes, refMass);

            for (String label : matrixLabels) {
                correctionMatrices.put(label, matrix);
            }
        }
    }

    /**
     * This method returns deisotoped intensities.
     *
     * @param ionMatches the ion matches to deisotope
     * @param spectrum the spectrum to search the isotopic intensities in
     * @param mzTolerance the MS2 m/z tolerance
     * @param mostAccurate boolean indicating whether the most accurate ion
     * should be selected
     *
     * @return a map of the deisotoped intensities (ion index &gt; intensity)
     */
    public HashMap<String, Double> deisotope(
            HashMap<String, IonMatch> ionMatches,
            Spectrum spectrum,
            double mzTolerance,
            boolean mostAccurate
    ) {

        HashMap<String, Double> result = new HashMap<>();

        for (String label : method.getReagentNames()) {

            IonMatch refMatch = ionMatches.get(label);

            if (refMatch != null && refMatch.peakIntensity > 0) {

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

                    }

                    double reagentMass = correctionMatrix.getReagentMass(i);
                    ReporterIon tempIon = new ReporterIon("tempIon", reagentMass, false);
                    IonMatch ionMatch = Reporter.getBestReporterIonMatch(
                            tempIon,
                            1,
                            spectrum,
                            mzTolerance,
                            mostAccurate
                    );

                    if (ionMatch != null) {
                        intensities[i] = ionMatch.peakIntensity;
                    }
                }
                if (lineNumber == -1) {

                    throw new IllegalArgumentException(
                            "Index of reagent "
                            + label
                            + " not found in the isotope correction matrix."
                    );

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
