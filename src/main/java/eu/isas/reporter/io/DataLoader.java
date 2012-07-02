package eu.isas.reporter.io;

import com.compomics.util.experiment.biology.PTMFactory;
import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.experiment.identification.matches.SpectrumMatch;
import com.compomics.util.experiment.io.identifications.IdfileReader;
import com.compomics.util.experiment.io.identifications.IdfileReaderFactory;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.MSnSpectrum;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.matches.PsmQuantification;
import com.compomics.util.gui.waiting.waitinghandlers.WaitingDialog;
import eu.isas.peptideshaker.fileimport.IdFilter;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JOptionPane;

/**
 * This class will load information from spectra and identifications files.
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class DataLoader {

    /**
     * The waiting dialog will display feedback to the user.
     */
    private WaitingDialog waitingDialog;
    /**
     * The quantification preferences.
     */
    private QuantificationPreferences quantificationPreferences;
    /**
     * The identification.
     */
    private Identification identification;
    /**
     * The spectrum factory.
     */
    private SpectrumFactory spectrumFactory = SpectrumFactory.getInstance();

    /**
     * Constructor.
     *
     * @param quantificationPreferences The quantification preferences
     * @param waitingDialog A waiting dialog to display feedback to the user
     * @param identification the identification
     */
    public DataLoader(QuantificationPreferences quantificationPreferences, WaitingDialog waitingDialog, Identification identification) {
        this.identification = identification;
        this.quantificationPreferences = quantificationPreferences;
        this.waitingDialog = waitingDialog;
    }

    /**
     * Method which returns the keys of the spectra potentially containing the
     * quantitative information of a desired psm.
     *
     * @param spectrumKey the inspected psm key
     * @return the spectrum references of the corresponding spectra
     */
    private ArrayList<String> getSpectrumIds(String spectrumKey) throws Exception {
        
        ArrayList<String> result = new ArrayList<String>();
        
        if (quantificationPreferences.isSameSpectra()) {
            result.add(spectrumKey);
        } else {
            Precursor precursor, referencePrecursor = spectrumFactory.getPrecursor(spectrumKey);
            double rtTol = quantificationPreferences.getPrecursorRTTolerance();
            double mzTol = quantificationPreferences.getPrecursorMzTolerance();
            double mzRef = referencePrecursor.getMz();
            double rtRef = referencePrecursor.getRt();
            
            for (String spectrumFile : spectrumFactory.getMgfFileNames()) {
                for (String spectrumTitle : spectrumFactory.getSpectrumTitles(spectrumFile)) {
                    
                    String newKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                    precursor = spectrumFactory.getPrecursor(newKey);
                    double errPpm = Math.abs((mzRef - precursor.getMz()) / mzRef * 1000000);
                    
                    if (errPpm < mzTol && Math.abs(precursor.getRt() - rtRef) < rtTol) {
                        result.add(newKey);
                    }
                }
            }
        }
        
        return result;
    }

    /**
     * Method which matches reporter ions in a spectrum.
     *
     * @param reporterIons The ions to look for
     * @param spectrum The spectrum analyzed
     * @return the corresponding ion matches. If no peak was found the peak will
     * be null.
     */
    private HashMap<ReporterIon, IonMatch> matchIons(String spectrumKey, ArrayList<ReporterIon> reporterIons) throws Exception {
        
        HashMap<Double, Peak> peakMap = spectrumFactory.getSpectrum(spectrumKey).getPeakMap();
        ArrayList<Double> mzArray = new ArrayList(peakMap.keySet());
        Collections.sort(mzArray);
        HashMap<ReporterIon, Peak> peaks = new HashMap<ReporterIon, Peak>();
        double ionTolerance = quantificationPreferences.getReporterIonsMzTolerance();
        
        for (Double mz : mzArray) {
            
            boolean over = true;
            
            for (ReporterIon ion : reporterIons) {
                if (mz > ion.getTheoreticMass() - ionTolerance && mz < ion.getTheoreticMass() + ionTolerance) {
                    if (peaks.get(ion) == null) {
                        peaks.put(ion, peakMap.get(mz));
                    } else if (Math.abs(mz - ion.getTheoreticMass()) < peaks.get(ion).mz - ion.getTheoreticMass()) {
                        peaks.put(ion, peakMap.get(mz));
                    }
                    over = false;
                } else if (mz <= ion.getTheoreticMass() + ionTolerance) {
                    over = false;
                }
            }
            
            if (over) {
                break;
            }
        }
        
        HashMap<ReporterIon, IonMatch> result = new HashMap<ReporterIon, IonMatch>();
        
        for (ReporterIon ion : peaks.keySet()) {
            result.put(ion, new IonMatch(peaks.get(ion), ion, new Charge(Charge.PLUS, 1)));
        }
        
        return result;
    }

    /**
     * Load quantifications.
     * 
     * @param quantification
     * @param mgfFiles 
     */
    public void loadQuantification(ReporterIonQuantification quantification, ArrayList<File> mgfFiles) {
        try {
            waitingDialog.appendReport("PSM quantification.");
            waitingDialog.increaseProgressValue();
            waitingDialog.setMaxSecondaryProgressValue(identification.getSpectrumIdentification().size());
            for (String matchKey : identification.getSpectrumIdentification()) {
                
                if (waitingDialog.isRunCanceled()) {
                    return;
                }
                
                for (String spectrumKey : getSpectrumIds(matchKey)) {
                    PsmQuantification spectrumQuantification = new PsmQuantification(spectrumKey, matchKey);
                    HashMap<ReporterIon, IonMatch> ionMatches = matchIons(spectrumKey, quantification.getReporterMethod().getReporterIons());
                    
                    for (ReporterIon ion : ionMatches.keySet()) {
                        spectrumQuantification.addIonMatch(ion.getIndex(), ionMatches.get(ion));
                    }
                    
                    quantification.addPsmQuantification(spectrumQuantification);
                    
                    if (waitingDialog.isRunCanceled()) {
                        return;
                    }
                }
                
                waitingDialog.increaseSecondaryProgressValue();
            }
            
        } catch (Exception e) {
            e.printStackTrace();
            waitingDialog.appendReport("An error occurred while quantifying PSMs:");
            waitingDialog.appendReport(e.getLocalizedMessage());
            waitingDialog.setRunCanceled();
        }
    }
}
