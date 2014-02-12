package eu.isas.reporter.calculation;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * A correction matrix contains the deisotoping coefficients as well as the labels to be corrected
 *
 * @author Marc
 */
public class CorrectionMatrix {

    
    /**
     * The correction matrix
     */
    private double[][] correctionMatrix;
    /**
     * The name of the reagents affected by this matrix at their index. 0 is the first index.
     */
    private HashMap<Integer, String> reagentNames;
    
    /**
     * Constructor
     * 
     * @param correctionMatrix the correction matrix
     * @param reagentNames he name of the reagents affected by this matrix 
    */
    public CorrectionMatrix(double[][] correctionMatrix, HashMap<Integer, String> reagentNames) {
        this.correctionMatrix = correctionMatrix;
        this.reagentNames = reagentNames;
        if (correctionMatrix.length != reagentNames.size()) {
            throw new IllegalArgumentException("Correction matrix size (" + correctionMatrix.length + ") does not match the reagent names list size (" + reagentNames.size() + ").");
        }
    }
    
    /**
     * Returns the names of the reagents involved in the deisotoping by this matrix indexed by their isotopic number.
     * 
     * @return the names of the reagents involved in the deisotoping by this matrix
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
