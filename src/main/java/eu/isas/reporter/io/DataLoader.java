package eu.isas.reporter.io;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.massspectrometry.Charge;
import com.compomics.util.experiment.massspectrometry.Peak;
import com.compomics.util.experiment.massspectrometry.Precursor;
import com.compomics.util.experiment.massspectrometry.Spectrum;
import com.compomics.util.experiment.massspectrometry.SpectrumFactory;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import com.compomics.util.experiment.quantification.matches.PsmQuantification;
import com.compomics.util.gui.waiting.WaitingHandler;
import eu.isas.reporter.myparameters.QuantificationPreferences;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

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
    private WaitingHandler waitingHandler;
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
     * The precursor m/z values.
     */
    HashMap<String, Double> precursorMzValues = new HashMap<String, Double>();
    /**
     * The precursor retention time values.
     */
    HashMap<String, Double> precursorRtValues = new HashMap<String, Double>();

    /**
     * Constructor.
     *
     * @param quantificationPreferences The quantification preferences
     * @param waitingHandler A waiting handler to display feedback to the user
     * @param identification the identification
     */
    public DataLoader(QuantificationPreferences quantificationPreferences, WaitingHandler waitingHandler, Identification identification) {
        this.identification = identification;
        this.quantificationPreferences = quantificationPreferences;
        this.waitingHandler = waitingHandler;
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

            double rtTol = quantificationPreferences.getPrecursorRTTolerance();
            double mzTol = quantificationPreferences.getPrecursorMzTolerance();
            double mzRef = precursorMzValues.get(spectrumKey);
            double rtRef = precursorRtValues.get(spectrumKey);

            String spectrumFile = Spectrum.getSpectrumFile(spectrumKey);

            for (String spectrumTitle : spectrumFactory.getSpectrumTitles(spectrumFile)) {

                String newKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                double precursorMz = precursorMzValues.get(newKey);
                double precursorRt = precursorRtValues.get(newKey);

                double errPpm = Math.abs((mzRef - precursorMz) / mzRef * 1000000);

                if (errPpm < mzTol && Math.abs(precursorRt - rtRef) < rtTol) {
                    result.add(newKey);
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
            precursorMzValues = new HashMap<String, Double>();
            precursorRtValues = new HashMap<String, Double>();

            // @TODO: The code should (if possible) be refactored to not have to temporary store the precursor mz and rt values.
            //        This has to be done without reading the spectra more than once!


            // see if we need to load the precuror rt and m/z values
            if (!quantificationPreferences.isSameSpectra()) {

                waitingHandler.appendReport("Merging HCD and CID spectra.", true, true);
                waitingHandler.resetSecondaryProgressBar();
                waitingHandler.setMaxSecondaryProgressValue(spectrumFactory.getNSpectra());

                for (String spectrumFile : spectrumFactory.getMgfFileNames()) {
                    for (String spectrumTitle : spectrumFactory.getSpectrumTitles(spectrumFile)) {
                        String newKey = Spectrum.getSpectrumKey(spectrumFile, spectrumTitle);
                        Precursor precursor = spectrumFactory.getPrecursor(newKey); // @TODO: replace by batch selection?
                        precursorMzValues.put(newKey, precursor.getMz());
                        precursorRtValues.put(newKey, precursor.getRt());
                        waitingHandler.increaseSecondaryProgressValue();

                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                    }
                }
            }

            waitingHandler.appendReport("PSM quantification.", true, true);
            waitingHandler.increaseProgressValue();
            waitingHandler.resetSecondaryProgressBar();

            int fileCounter = 0;

            for (String spectrumFile : spectrumFactory.getMgfFileNames()) {

                waitingHandler.appendReport("Quantifying file: " + spectrumFile + " (" + ++fileCounter + "/" + spectrumFactory.getMgfFileNames().size() + ")", true, true);
                waitingHandler.resetSecondaryProgressBar();
                waitingHandler.setMaxSecondaryProgressValue(identification.getSpectrumIdentification(spectrumFile).size());

                //identification.loadSpectrumMatches(spectrumFile, null); // this should take in a waiting handler!!
                //identification.loadSpectrumMatchParameters(spectrumFile, ?, null); // this should take in a waiting handler!!

                for (String matchKey : identification.getSpectrumIdentification(spectrumFile)) {
                    for (String spectrumKey : getSpectrumIds(matchKey)) {
                        PsmQuantification spectrumQuantification = new PsmQuantification(spectrumKey, matchKey);
                        HashMap<ReporterIon, IonMatch> ionMatches = matchIons(spectrumKey, quantification.getReporterMethod().getReporterIons());

                        for (ReporterIon ion : ionMatches.keySet()) {
                            spectrumQuantification.addIonMatch(ion.getIndex(), ionMatches.get(ion));
                        }

                        quantification.addPsmQuantification(spectrumQuantification);

                        if (waitingHandler.isRunCanceled()) {
                            return;
                        }
                    }

                    waitingHandler.increaseSecondaryProgressValue();
                }
            }

            // empty the mz and rt maps to free memory
            precursorMzValues.clear();
            precursorRtValues.clear();

        } catch (Exception e) {
            e.printStackTrace();
            waitingHandler.appendReport("An error occurred while quantifying PSMs:", true, true);
            waitingHandler.appendReport(e.getLocalizedMessage(), true, true);
            waitingHandler.setRunCanceled();
        }
    }
}
