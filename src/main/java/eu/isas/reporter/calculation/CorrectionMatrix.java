package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.Atom;
import java.util.HashMap;

/**
 * A correction matrix contains the deisotoping coefficients as well as the
 * labels to be corrected.
 *
 * @author Marc Vaudel
 */
public class CorrectionMatrix {

    /**
     * The correction matrix.
     */
    private double[][] correctionMatrix;
    /**
     * The name of the reagents affected by this matrix at their index. 0 is the
     * first index.
     */
    private HashMap<Integer, String> reagentNames;
    /**
     * The mass affected by the first column of the matrix.
     */
    private double refMass;

    /**
     * Constructor.
     *
     * @param correctionMatrix the correction matrix
     * @param reagentNames he name of the reagents affected by this matrix
     * @param refMass the mass affected by the first column of the matrix
     */
    public CorrectionMatrix(double[][] correctionMatrix, HashMap<Integer, String> reagentNames, double refMass) {
        this.correctionMatrix = correctionMatrix;
        this.reagentNames = reagentNames;
        this.refMass = refMass;
    }

    /**
     * Returns the mass affected by the given column of the matrix.
     *
     * @param isotope the column of the matrix
     *
     * @return the mass affected by the given column of the matrix
     */
    public double getReagentMass(int isotope) {
        return refMass + isotope * Atom.C.getDifferenceToMonoisotopic(1);
    }

    /**
     * Returns the names of the reagents involved in the deisotoping by this
     * matrix indexed by their isotopic number.
     *
     * @return the names of the reagents involved in the deisotoping by this
     * matrix
     */
    public HashMap<Integer, String> getReagentsNames() {
        return reagentNames;
    }

    /**
     * Returns the dimension of the correction matrix.
     *
     * @return the dimension of the correction matrix
     */
    public int getDimension() {
        return correctionMatrix.length;
    }

    /**
     * Returns the value of the matrix at row i and column j.
     *
     * @param i the row number
     * @param j the column number
     *
     * @return the value of the matrix at row i and column j
     */
    public double getValueAt(int i, int j) {
        return correctionMatrix[i][j];
    }
}
