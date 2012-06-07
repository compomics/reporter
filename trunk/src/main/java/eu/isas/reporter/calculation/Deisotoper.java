package eu.isas.reporter.calculation;

import com.compomics.util.experiment.biology.ions.ReporterIon;
import com.compomics.util.experiment.identification.matches.IonMatch;
import com.compomics.util.experiment.quantification.reporterion.CorrectionFactor;
import com.compomics.util.experiment.quantification.reporterion.ReporterMethod;
import java.util.ArrayList;
import java.util.HashMap;
import org.ujmp.core.doublematrix.calculation.general.decomposition.Ginv;

/**
 * This class takes care of the deisotoping of reporter ion intensities.
 *
 * @author Marc Vaudel
 */
public class Deisotoper {

    /**
     * The correction matrix (see PMID: 19953549).
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
        ArrayList<ReporterIon> reporterIons = method.getReporterIons();
        int dimension = correctionFactors.size();
        double[][] coefficients = new double[dimension][dimension];
        
        for (int i = 0; i < dimension; i++) {
            for (int j = 0; j < dimension; j++) {
                if (reporterIons.get(i).getIndex() - correctionFactors.get(j).getIonId() == -2) {
                    coefficients[i][j] = correctionFactors.get(j).getMinus2() / 100.0;
                } else if (reporterIons.get(i).getIndex() - correctionFactors.get(j).getIonId() == -1) {
                    coefficients[i][j] = correctionFactors.get(j).getMinus1() / 100.0;
                } else if (reporterIons.get(i).getIndex() - correctionFactors.get(j).getIonId() == 0) {
                    coefficients[i][j] = 1 - correctionFactors.get(j).getMinus2() / 100.0
                            - correctionFactors.get(j).getMinus1() / 100.0
                            - correctionFactors.get(j).getPlus1() / 100.0
                            - correctionFactors.get(j).getPlus2() / 100.0;
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
     * This method returns deisotoped intensities.
     *
     * @param ionMatches the ion matches to deisotope
     * @return a map of the deisotoped intensities (ion index -> intensity)
     */
    public HashMap<Integer, Double> deisotope(HashMap<Integer, IonMatch> ionMatches) {
        
        ArrayList<ReporterIon> reporterIons = method.getReporterIons();
        double[] intensities = new double[reporterIons.size()];
        
        for (int i = 0; i < reporterIons.size(); i++) {
            if (ionMatches.get(reporterIons.get(i).getIndex()) != null) {
                if (ionMatches.get(reporterIons.get(i).getIndex()).peak != null) {
                    intensities[i] = ionMatches.get(reporterIons.get(i).getIndex()).peak.intensity;
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
            result.put(reporterIons.get(i).getIndex(), resultInt);
        }
        return result;
    }

    /**
     * User dependant code! Do not commit!
     * 
     * @param ionMatches
     * @param spectrumName
     * @return  
     */
    public HashMap<Integer, Double> deisotopeSwitch(HashMap<Integer, IonMatch> ionMatches, String spectrumName) {

        // @TODO: i guess this code should not be in svn then..?
        
        ArrayList<String> cofradicCys = new ArrayList<String>();
        cofradicCys.add("orbitrap001989.mgf");
        cofradicCys.add("orbitrap001990.mgf");
        cofradicCys.add("orbitrap001991.mgf");
        cofradicCys.add("orbitrap001992.mgf");
        cofradicCys.add("orbitrap001993.mgf");
        cofradicCys.add("orbitrap002013.mgf");
        cofradicCys.add("orbitrap002014.mgf");
        cofradicCys.add("orbitrap002015.mgf");
        cofradicCys.add("orbitrap002016.mgf");
        cofradicCys.add("orbitrap002017.mgf");
        cofradicCys.add("orbitrap002018.mgf");
        cofradicCys.add("orbitrap002019.mgf");
        cofradicCys.add("QstarE04564.mgf");
        cofradicCys.add("QstarE04565.mgf");
        cofradicCys.add("QstarE04567.mgf");
        cofradicCys.add("QstarE04568.mgf");
        cofradicCys.add("QstarE04570.mgf");
        cofradicCys.add("QstarE04571 (recalibrated).mgf");
        cofradicCys.add("QstarE04573 (recalibrated).mgf");
        cofradicCys.add("QstarE04574 (recalibrated).mgf");
        cofradicCys.add("QstarE04576.mgf");
        cofradicCys.add("QstarE04577 (recalibrated).mgf");
        cofradicCys.add("QstarE04579 (recalibrated).mgf");
        cofradicCys.add("QstarE04580 (recalibrated).mgf");
        ArrayList<String> cofradicMet = new ArrayList<String>();
        cofradicMet.add("orbitrap001979.mgf");
        cofradicMet.add("orbitrap001980.mgf");
        cofradicMet.add("orbitrap001981.mgf");
        cofradicMet.add("orbitrap001982.mgf");
        cofradicMet.add("orbitrap001983.mgf");
        cofradicMet.add("orbitrap001984.mgf");
        cofradicMet.add("orbitrap001985.mgf");
        cofradicMet.add("orbitrap001986.mgf");
        cofradicMet.add("orbitrap001987.mgf");
        cofradicMet.add("QstarE04494.mgf");
        cofradicMet.add("QstarE04496.mgf");
        cofradicMet.add("QstarE04498.mgf");
        cofradicMet.add("QstarE04500.mgf");
        cofradicMet.add("QstarE04502.mgf");
        cofradicMet.add("QstarE04504.mgf");
        cofradicMet.add("QstarE04506.mgf");
        cofradicMet.add("QstarE04508.mgf");
        cofradicMet.add("QstarE04510.mgf");
        ArrayList<String> hilic = new ArrayList<String>();
        hilic.add("orbitrap001935.mgf");
        hilic.add("orbitrap001936.mgf");
        hilic.add("orbitrap001937.mgf");
        hilic.add("orbitrap001938.mgf");
        hilic.add("orbitrap001939.mgf");
        hilic.add("orbitrap001940.mgf");
        hilic.add("orbitrap001941.mgf");
        hilic.add("orbitrap001942.mgf");
        hilic.add("orbitrap001944.mgf");
        hilic.add("orbitrap001945.mgf");
        hilic.add("orbitrap001946.mgf");
        hilic.add("orbitrap001947.mgf");
        hilic.add("orbitrap001948.mgf");
        hilic.add("orbitrap001949.mgf");
        hilic.add("orbitrap001950.mgf");
        hilic.add("orbitrap001951.mgf");
        hilic.add("orbitrap001952.mgf");
        hilic.add("orbitrap001954.mgf");
        hilic.add("orbitrap001955.mgf");
        hilic.add("orbitrap001956.mgf");
        hilic.add("orbitrap001957.mgf");
        hilic.add("orbitrap001958.mgf");
        hilic.add("orbitrap001959.mgf");
        hilic.add("orbitrap001960.mgf");
        hilic.add("orbitrap001961.mgf");
        hilic.add("orbitrap001962.mgf");
        hilic.add("orbitrap001964.mgf");
        hilic.add("orbitrap001965.mgf");
        hilic.add("orbitrap001966.mgf");
        hilic.add("orbitrap001967.mgf");
        hilic.add("orbitrap001968.mgf");
        hilic.add("orbitrap001969.mgf");
        hilic.add("orbitrap001970.mgf");
        hilic.add("orbitrap001971.mgf");
        hilic.add("orbitrap001972.mgf");
        hilic.add("orbitrap001974.mgf");
        hilic.add("orbitrap001975.mgf");
        hilic.add("orbitrap001976.mgf");
        hilic.add("QstarE04512.mgf");
        hilic.add("QstarE04513.mgf");
        hilic.add("QstarE04515.mgf");
        hilic.add("QstarE04516.mgf");
        hilic.add("QstarE04518.mgf");
        hilic.add("QstarE04520.mgf");
        hilic.add("QstarE04523.mgf");
        hilic.add("QstarE04524.mgf");
        hilic.add("QstarE04526.mgf");
        hilic.add("QstarE04527.mgf");
        hilic.add("QstarE04529.mgf");
        hilic.add("QstarE04530.mgf");
        hilic.add("QstarE04537.mgf");
        hilic.add("QstarE04538 (recalibrated).mgf");
        hilic.add("QstarE04540.mgf");
        hilic.add("QstarE04541.mgf");
        hilic.add("QstarE04543.mgf");
        hilic.add("QstarE04544.mgf");
        hilic.add("QstarE04546.mgf");
        hilic.add("QstarE04547.mgf");
        hilic.add("QstarE04549.mgf");
        hilic.add("QstarE04550.mgf");
        hilic.add("QstarE04552.mgf");
        hilic.add("QstarE04553.mgf");
        hilic.add("QstarE04555.mgf");
        hilic.add("QstarE04556.mgf");
        hilic.add("QstarE04558 (recalibrated).mgf");
        hilic.add("QstarE04559 (recalibrated).mgf");
        hilic.add("QstarE04561 (recalibrated).mgf");
        hilic.add("QstarE04562 (recalibrated2).mgf");
        hilic.add("QstarE04610.mgf");
        hilic.add("QstarE04611.mgf");
        hilic.add("QstarE04613 (recalibrated).mgf");
        hilic.add("QstarE04614 (recalibrated).mgf");
        hilic.add("QstarE04616 (recalibrated).mgf");
        ArrayList<String> ief = new ArrayList<String>();
        ief.add("orbitrap001756.mgf");
        ief.add("orbitrap001757.mgf");
        ief.add("orbitrap001758.mgf");
        ief.add("orbitrap001761.mgf");
        ief.add("orbitrap001762.mgf");
        ief.add("orbitrap001763.mgf");
        ief.add("orbitrap001764.mgf");
        ief.add("orbitrap001765.mgf");
        ief.add("orbitrap001766.mgf");
        ief.add("orbitrap001769.mgf");
        ief.add("orbitrap001770.mgf");
        ief.add("orbitrap001873.mgf");
        ief.add("orbitrap001874.mgf");
        ief.add("orbitrap001875.mgf");
        ief.add("orbitrap001876.mgf");
        ief.add("orbitrap001877.mgf");
        ief.add("orbitrap001878.mgf");
        ief.add("orbitrap001879.mgf");
        ief.add("orbitrap001881.mgf");
        ief.add("orbitrap001883.mgf");
        ief.add("orbitrap001884.mgf");
        ief.add("orbitrap001885.mgf");
        ief.add("orbitrap001904.mgf");
        ief.add("orbitrap006549.mgf");
        ief.add("QstarE04582.mgf");
        ief.add("QstarE04583 (recalibrated).mgf");
        ief.add("QstarE04585 (recalibrated)2.mgf");
        ief.add("QstarE04586.mgf");
        ief.add("QstarE04588.mgf");
        ief.add("QstarE04589.mgf");
        ief.add("QstarE04591.mgf");
        ief.add("QstarE04592 (recalibrated).mgf");
        ief.add("QstarE04594 (recalibrated).mgf");
        ief.add("QstarE04595 (recalibrated).mgf");
        ief.add("QstarE04597 (recalibrated).mgf");
        ief.add("QstarE04595 (recalibrated).mgf");
        ief.add("QstarE04599.mgf");
        ief.add("QstarE04595 (recalibrated).mgf");
        ief.add("QstarE04600 (recalibrated).mgf");
        ief.add("QstarE04602 (recalibrated).mgf");
        ief.add("QstarE04603.mgf");
        ief.add("QstarE04605 (recalibrated).mgf");
        ief.add("QstarE04606.mgf");
        ief.add("QstarE04636.mgf");
        ief.add("QstarE04637 (recalibrated).mgf");
        ief.add("QstarE04639.mgf");
        ief.add("QstarE04640 (recalibrated).mgf");
        ief.add("QstarE04642.mgf");
        ief.add("QstarE04643.mgf");
        ief.add("QstarE04647.mgf");
        ief.add("QstarE04648.mgf");
        ief.add("QstarE04650.mgf");
        ief.add("QstarE04651.mgf");
        ief.add("QstarE04653.mgf");
        ief.add("QstarE04655 (recalibrated).mgf");
        ief.add("QstarE04656 (recalibrated) (recalibrated).mgf");
        ief.add("QstarE04658 (recalibrated).mgf");
        ief.add("QstarE04659 (recalibrated).mgf");
        ief.add("QstarE04661 (recalibrated).mgf");
        ief.add("QstarE04663.mgf");
        ief.add("QstarE04664.mgf");
        ief.add("QstarE04666.mgf");
        ief.add("QstarE04667.mgf");
        ief.add("QstarE04669.mgf");
        ief.add("QstarE04670.mgf");
        ArrayList<String> scx = new ArrayList<String>();
        scx.add("orbitrap001887.mgf");
        scx.add("orbitrap001888.mgf");
        scx.add("orbitrap001889.mgf");
        scx.add("orbitrap001890.mgf");
        scx.add("orbitrap001891.mgf");
        scx.add("orbitrap001892.mgf");
        scx.add("orbitrap001899.mgf");
        scx.add("orbitrap001900.mgf");
        scx.add("orbitrap001901.mgf");
        scx.add("orbitrap001902.mgf");
        scx.add("orbitrap001903.mgf");
        scx.add("orbitrap001906.mgf");
        scx.add("orbitrap001907.mgf");
        scx.add("orbitrap001908.mgf");
        scx.add("orbitrap001909.mgf");
        scx.add("orbitrap001910.mgf");
        scx.add("orbitrap001911.mgf");
        scx.add("orbitrap001912.mgf");
        scx.add("orbitrap001918.mgf");
        scx.add("orbitrap001919.mgf");
        scx.add("orbitrap001920.mgf");
        scx.add("orbitrap001921.mgf");
        scx.add("orbitrap001923.mgf");
        scx.add("orbitrap001924.mgf");
        scx.add("orbitrap001925.mgf");
        scx.add("orbitrap001926.mgf");
        scx.add("orbitrap001927.mgf");
        scx.add("orbitrap001928.mgf");
        scx.add("orbitrap001929.mgf");
        scx.add("orbitrap001930.mgf");
        scx.add("orbitrap001931.mgf");
        scx.add("orbitrap001932.mgf");
        scx.add("orbitrap001933.mgf");
        scx.add("QstarE04397.mgf");
        scx.add("QstarE04399.mgf");
        scx.add("QstarE04401 (recalibrated) (recalibrated).mgf");
        scx.add("QstarE04403.mgf");
        scx.add("QstarE04405.mgf");
        scx.add("QstarE04407.mgf");
        scx.add("QstarE04409.mgf");
        scx.add("QstarE04411 (recalibrated)3.mgf");
        scx.add("QstarE04413.mgf");
        scx.add("QstarE04416.mgf");
        scx.add("QstarE04420.mgf");
        scx.add("QstarE04422 (recalibrated).mgf");
        scx.add("QstarE04424.mgf");
        scx.add("QstarE04426.mgf");
        scx.add("QstarE04428 (recalibrated).mgf");
        scx.add("QstarE04429.mgf");
        scx.add("QstarE04431 (recalibrated).mgf");
        scx.add("QstarE04432.mgf");
        scx.add("QstarE04434 (recalibrated).mgf");
        scx.add("QstarE04435.mgf");
        scx.add("QstarE04437.mgf");
        scx.add("QstarE04438.mgf");
        scx.add("QstarE04442.mgf");
        scx.add("QstarE04444.mgf");
        scx.add("QstarE04446 (recalibrated).mgf");
        scx.add("QstarE04451 (recalibrated).mgf");
        scx.add("QstarE04453 (recalibrated).mgf");
        scx.add("QstarE04455 (recalibrated).mgf");
        scx.add("QstarE04459.mgf");
        scx.add("QstarE04461.mgf");
        scx.add("QstarE04463.mgf");
        scx.add("QstarE04465.mgf");
        scx.add("QstarE04470.mgf");
        scx.add("QstarE04473.mgf");
        scx.add("QstarE04474.mgf");
        scx.add("QstarE04476.mgf");
        scx.add("QstarE04477.mgf");
        scx.add("QstarE04480.mgf");
        scx.add("QstarE04482.mgf");
        scx.add("QstarE04484.mgf");
        scx.add("QstarE04486 (recalibrated) (recalibrated).mgf");
        scx.add("QstarE04492.mgf");
        HashMap<Integer, Integer> scxMap = new HashMap<Integer, Integer>();
        scxMap.put(114, 114);
        scxMap.put(115, 115);
        scxMap.put(116, 116);
        scxMap.put(117, 117);
        HashMap<Integer, Integer> iefMap = new HashMap<Integer, Integer>();
        iefMap.put(114, 117);
        iefMap.put(115, 114);
        iefMap.put(116, 115);
        iefMap.put(117, 116);
        HashMap<Integer, Integer> cofradicMetMap = new HashMap<Integer, Integer>();
        cofradicMetMap.put(114, 116);
        cofradicMetMap.put(115, 117);
        cofradicMetMap.put(116, 114);
        cofradicMetMap.put(117, 115);
        HashMap<Integer, Integer> hilicMap = new HashMap<Integer, Integer>();
        hilicMap.put(114, 117);
        hilicMap.put(115, 114);
        hilicMap.put(116, 115);
        hilicMap.put(117, 116);
        HashMap<Integer, Integer> cofradicCysMap = new HashMap<Integer, Integer>();
        cofradicCysMap.put(114, 117);
        cofradicCysMap.put(115, 114);
        cofradicCysMap.put(116, 115);
        cofradicCysMap.put(117, 116);

        ArrayList<ReporterIon> reporterIons = method.getReporterIons();
        double[] intensities = new double[reporterIons.size()];
        
        for (int i = 0; i < reporterIons.size(); i++) {
            if (ionMatches.get(reporterIons.get(i).getIndex()) != null) {
                if (ionMatches.get(reporterIons.get(i).getIndex()).peak != null) {
                    intensities[i] = ionMatches.get(reporterIons.get(i).getIndex()).peak.intensity;
                }
            }
        }
        
        HashMap<Integer, Double> result = new HashMap<Integer, Double>();
        HashMap<Integer, Integer> switchMap = new HashMap<Integer, Integer>();
        
        if (cofradicCys.contains(spectrumName)) {
            switchMap = cofradicCysMap;
        } else if (cofradicMet.contains(spectrumName)) {
            switchMap = cofradicMetMap;
        } else if (scx.contains(spectrumName)) {
            switchMap = scxMap;
        } else if (ief.contains(spectrumName)) {
            switchMap = iefMap;
        } else if (hilic.contains(spectrumName)) {
            switchMap = hilicMap;
        }
        
        for (int i = 0; i < correctionMatrix.length; i++) {
            double resultInt = 0;
            for (int j = 0; j < intensities.length; j++) {
                resultInt += intensities[j] * correctionMatrix[i][j];
            }
            if (resultInt < 0) {
                resultInt = 0;
            }
            result.put(switchMap.get(reporterIons.get(i).getIndex()), resultInt);
        }
        
        return result;
    }
}
