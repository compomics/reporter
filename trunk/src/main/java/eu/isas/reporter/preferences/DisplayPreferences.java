/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package eu.isas.reporter.preferences;

/**
 * Reporter display preferences
 *
 * @author Marc
 */
public class DisplayPreferences {
    
    /**
     * Indicates whether scores should be displayed in the tables
     */
    private boolean showScores = false;

    /**
     * Indicates whether scores should be displayed in the tables.
     * 
     * @return a boolean indicating whether scores should be displayed
     */
    public boolean showScores() {
        return showScores;
    }

    /**
     * Sets whether scores should be displayed in the tables.
     * 
     * @param showScores a boolean indicating whether scores should be displayed
     */
    public void setShowScores(boolean showScores) {
        this.showScores = showScores;
    }
    
    
    
}
