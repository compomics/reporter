package eu.isas.reporter.utils;

import com.compomics.util.experiment.mass_spectrometry.spectra.Peak;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import uk.ac.ebi.jmzml.model.mzml.BinaryDataArray;
import uk.ac.ebi.jmzml.model.mzml.CVParam;
import uk.ac.ebi.jmzml.model.mzml.Precursor;
import uk.ac.ebi.jmzml.model.mzml.SelectedIonList;
import uk.ac.ebi.jmzml.model.mzml.Spectrum;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshaller;

/**
 * Merging of MS2 and MS3 data from Fusion instruments to merge the reporters
 * from HCD MS3 with the identification from CID MS2. Work in progress...
 *
 * @author Harald Barsnes
 */
public class SpectrumMerger {

    private double upperReporterRange = 135;
    private File mzmlFileFolder = new File("C:\\Users\\hba041\\Desktop\\heidrun\\data\\mzml");
    private File mgfFileFolder = new File("C:\\Users\\hba041\\Desktop\\heidrun\\data\\mgf");

    /**
     * Main method for testing purposes.
     * 
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        SpectrumMerger spectrumMerge = new SpectrumMerger();
        spectrumMerge.mergeData();
        System.exit(0);
    }
    
    /**
     * Merge the MS2 and MS3 for all mzML files in the mzML folder.
     */
    private void mergeData() {
        for (File tempMzmlFile : mzmlFileFolder.listFiles()) {
            mergeData(tempMzmlFile);
            System.out.println();
        }
    }

    /**
     * Merge the MS2 and MS3 for the given mzML file.
     * 
     * @param mzMlFile the mzML file
     */
    private void mergeData(File mzMlFile) {

        String mzmlFileName = mzMlFile.getName();
        String mgfFileName = mzmlFileName.substring(0, mzmlFileName.length() - 5) + ".mgf";
        File mgfFile = new File(mgfFileFolder, mgfFileName);
        
        System.out.println("Processing: " + mzMlFile.getAbsolutePath());
        
        try {
            FileWriter f = new FileWriter(mgfFile);
            BufferedWriter bw = new BufferedWriter(f);

            MzMLUnmarshaller unmarshaller = new MzMLUnmarshaller(mzMlFile);
            Set<String> spectrumIds = unmarshaller.getSpectrumIDs();
            //System.out.println("#spectra: " + spectrumIds.size());

            ArrayList<String> ms2Spectra = new ArrayList<String>();

            for (String tempId : spectrumIds) {

                Spectrum currentSpectrum = unmarshaller.getSpectrumById(tempId);

                int msLevel = -1;
                List<CVParam> specParams = currentSpectrum.getCvParam();

                for (Iterator lCVParamIterator = specParams.iterator(); lCVParamIterator.hasNext() && msLevel == -1;) {
                    CVParam lCVParam = (CVParam) lCVParamIterator.next();
                    if (lCVParam.getAccession().equals("MS:1000511")) {
                        msLevel = Integer.parseInt(lCVParam.getValue().trim());
                    }
                }

                if (msLevel == 3) {
                    Precursor tempMs3Precursor = currentSpectrum.getPrecursorList().getPrecursor().get(0);
                    String precursorSpectrumRef = tempMs3Precursor.getSpectrumRef();
                    Spectrum ms2Spectrum = unmarshaller.getSpectrumById(precursorSpectrumRef);

                    if (ms2Spectrum != null) {

                        if (ms2Spectra.contains(precursorSpectrumRef)) { // @TODO: how to handle multiple MS3 for the same MS2..?
                            System.out.println(precursorSpectrumRef);
                        } else {
                            ms2Spectra.add(precursorSpectrumRef);
                        }

                        // get the ms2 precuror details
                        double precursorMz = 0.0;
                        int precursorCharge = 0;
                        double precursorIntensity = 0.0;
                        Precursor tempMs2Precursor = ms2Spectrum.getPrecursorList().getPrecursor().get(0);
                        SelectedIonList sIonList = tempMs2Precursor.getSelectedIonList();

                        List<CVParam> cvParams = sIonList.getSelectedIon().get(0).getCvParam();

                        for (Object cvParam : cvParams) {
                            CVParam lCVParam = (CVParam) cvParam;
                            if (lCVParam.getAccession().equals("MS:1000744")) {
                                precursorMz = Double.parseDouble(lCVParam.getValue().trim());
                            } else if (lCVParam.getAccession().equals("MS:1000041")) {
                                precursorCharge = Integer.parseInt(lCVParam.getValue());
                            } else if (lCVParam.getAccession().equals("MS:1000042")) {
                                precursorIntensity = Double.parseDouble(lCVParam.getValue()); // @TODO: extract retention time..?
                            }
                        }

                        // get the ms2 data and remove peaks in the reporter range
                        List<BinaryDataArray> bdal = ms2Spectrum.getBinaryDataArrayList().getBinaryDataArray();
                        BinaryDataArray mzBinaryDataArray = (BinaryDataArray) bdal.get(0);
                        if (mzBinaryDataArray.getEncodedLength() == 0) {
                            throw new Exception("mzBinaryDataArray empty!");
                        }

                        Number[] mzNumbers = mzBinaryDataArray.getBinaryDataAsNumberArray();
                        if (mzNumbers.length < 1) {
                            throw new Exception("mzBinaryDataArray empty!");
                        }

                        BinaryDataArray intBinaryDataArray = (BinaryDataArray) bdal.get(1);
                        Number[] intNumbers = intBinaryDataArray.getBinaryDataAsNumberArray();

                        ArrayList<Double> ms2MzValues = new ArrayList<Double>();
                        ArrayList<Double> ms2IntensityValues = new ArrayList<Double>();
                        double ms2MaxIntensity = 0.0;

                        for (int i = 0; i < mzNumbers.length; i++) {
                            if (mzNumbers[i].doubleValue() >= upperReporterRange) {
                                ms2MzValues.add(mzNumbers[i].doubleValue());
                                ms2IntensityValues.add(intNumbers[i].doubleValue());
                            }

                            if (intNumbers[i].doubleValue() > ms2MaxIntensity) {
                                ms2MaxIntensity = intNumbers[i].doubleValue();
                            }
                        }

                        // get the ms3 data and find the peaks in the reporter range
                        Spectrum ms3Spectrum = currentSpectrum;
                        bdal = ms3Spectrum.getBinaryDataArrayList().getBinaryDataArray();
                        mzBinaryDataArray = (BinaryDataArray) bdal.get(0);
                        if (mzBinaryDataArray.getEncodedLength() == 0) {
                            throw new Exception("mzBinaryDataArray empty!");
                        }

                        mzNumbers = mzBinaryDataArray.getBinaryDataAsNumberArray();
                        if (mzNumbers.length < 1) {
                            throw new Exception("mzBinaryDataArray empty!");
                        }

                        intBinaryDataArray = (BinaryDataArray) bdal.get(1);
                        intNumbers = intBinaryDataArray.getBinaryDataAsNumberArray();

                        double ms3MaxIntensity = 0.0;
                        ArrayList<Double> ms3MzValues = new ArrayList<Double>();
                        ArrayList<Double> ms3IntensityValues = new ArrayList<Double>();

                        for (int i = 0; i < mzNumbers.length; i++) {
                            if (mzNumbers[i].doubleValue() < upperReporterRange) {
                                ms3MzValues.add(mzNumbers[i].doubleValue());
                                ms3IntensityValues.add(intNumbers[i].doubleValue());

                                if (intNumbers[i].doubleValue() > ms3MaxIntensity) {
                                    ms3MaxIntensity = intNumbers[i].doubleValue();
                                }
                            }
                        }

                        // normalize the ms3 intensities relative to the average ms2 intensity
                        double normalizationFactor = (ms2MaxIntensity / ms3MaxIntensity) / 2; // @TODO: verify the normalization
                        ArrayList< Double> ms3IntensityValuesNormalized = new ArrayList<Double>();
                        for (Double tempIntensityValue : ms3IntensityValues) {
                            ms3IntensityValuesNormalized.add(tempIntensityValue * normalizationFactor);
                        }

                        // create the new mgf spectrum
                        HashMap<Double, Peak> peakMap = new HashMap<Double, Peak>();
                        for (int i = 0; i < ms3MzValues.size(); i++) {
                            peakMap.put(ms3MzValues.get(i), new Peak(ms3MzValues.get(i), ms3IntensityValuesNormalized.get(i)));
                        }
                        for (int i = 0; i < ms2MzValues.size(); i++) {
                            peakMap.put(ms2MzValues.get(i), new Peak(ms2MzValues.get(i), ms2IntensityValues.get(i)));
                        }

                        ArrayList<Integer> possibleCharges = new ArrayList<>();
                        possibleCharges.add(precursorCharge);
                        com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum mgfSpectrum = new com.compomics.util.experiment.mass_spectrometry.spectra.Spectrum(2,
                                new com.compomics.util.experiment.mass_spectrometry.spectra.Precursor(-1, precursorMz, precursorIntensity, possibleCharges),
                                precursorSpectrumRef + " (MS2) and " + tempId + " (MS3)", peakMap, mgfFileName);

                        bw.write(mgfSpectrum.asMgf());
                    } else {
                        throw new Exception(precursorSpectrumRef + " not found!");
                    }
                }
            }

            bw.close();
            f.close();
            
            System.out.println("Created: " + mgfFile.getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
