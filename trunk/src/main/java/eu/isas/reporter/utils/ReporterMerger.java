package eu.isas.reporter.utils;

import com.compomics.util.math.BasicMathFunctions;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import org.apache.commons.math.util.FastMath;

/**
 * Merging Reporter protein reports. For testing purposes only!
 *
 * @author Marc Vaudel
 * @author Harald Barsnes
 */
public class ReporterMerger {

    private static final String separator = "\t";

    public static void main(String[] args) {
        ReporterMerger rm = new ReporterMerger();

        try {
            rm.testReporterGrouping();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void testReporterGrouping() throws FileNotFoundException, IOException {

        try {
            String path = "C:\\Users\\hba041\\Desktop\\reporter_test\\new_tmt_masses\\";////this.getClass().getResource("ReporterGrouping.class").getPath();
//                path = path.substring(1, path.indexOf("/target/"));
//                path += "/src/test/resources/experiment/testMarc";

            int nFiles = 3;
            int nProteins = 20000;
            int nRatios = 6;
            int ratioIndex = 15;
            int referenceChannel = 2;

            int minValidatedPeptideCount = 2;
            int minValidatedSpectrumCount = 2;

            //String[] orderedExperiments = {"Exp 1", "Exp 2", "Exp 3", "Exp 4"};
            String[] orderedExperiments = {"Mix 1", "Mix 2", "Mix 3"};
            HashMap<String, String> fileMap = new HashMap<String, String>(nFiles);
            fileMap.put(orderedExperiments[0], "Mix1.txt");
            fileMap.put(orderedExperiments[1], "Mix2.txt");
            fileMap.put(orderedExperiments[2], "Mix3.txt");

            HashMap<String, ArrayList<String>> ratios = new HashMap<String, ArrayList<String>>(nFiles);
            HashMap<String, String> referenceChannels = new HashMap<String, String>();

            HashMap<String, String> keyToMainMatchMap = new HashMap<String, String>(nProteins);
            HashMap<String, String> keyToOtherMatchesMap = new HashMap<String, String>(nProteins);
            HashMap<String, String> groupClassMap = new HashMap<String, String>(nProteins);
            HashMap<String, String> mwMap = new HashMap<String, String>(nProteins);
            HashMap<String, String> descriptionMap = new HashMap<String, String>(nProteins);
            HashMap<String, Integer> decoyMap = new HashMap<String, Integer>(nProteins);
            HashMap<String, Boolean> proteinValidatedMap = new HashMap<String, Boolean>(nProteins);

            HashMap<String, ArrayList<String>> proteinKeysMap = new HashMap<String, ArrayList<String>>(nFiles);
            HashMap<String, HashMap<String, Integer>> peptidesMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, Integer>> spectraMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, Integer>> validatedPeptidesMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, Integer>> validatedSpectraMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, Double>> pepMap = new HashMap<String, HashMap<String, Double>>(nFiles);
            HashMap<String, HashMap<String, Double>> scoreMap = new HashMap<String, HashMap<String, Double>>(nFiles);
            HashMap<String, HashMap<String, Integer>> validatedMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, HashMap<String, Double>>> ratiosMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>(nFiles);
            HashMap<String, HashMap<String, HashMap<String, Double>>> ratiosMapNormalized = new HashMap<String, HashMap<String, HashMap<String, Double>>>(nFiles);

            for (String experiment : orderedExperiments) {

                ratios.put(experiment, new ArrayList<String>(nRatios));
                proteinKeysMap.put(experiment, new ArrayList<String>(nProteins));
                peptidesMap.put(experiment, new HashMap<String, Integer>(nProteins));
                spectraMap.put(experiment, new HashMap<String, Integer>(nProteins));
                validatedPeptidesMap.put(experiment, new HashMap<String, Integer>(nProteins));
                validatedSpectraMap.put(experiment, new HashMap<String, Integer>(nProteins));
                pepMap.put(experiment, new HashMap<String, Double>(nProteins));
                scoreMap.put(experiment, new HashMap<String, Double>(nProteins));
                validatedMap.put(experiment, new HashMap<String, Integer>(nProteins));
                ratiosMap.put(experiment, new HashMap<String, HashMap<String, Double>>(nProteins));
                ratiosMapNormalized.put(experiment, new HashMap<String, HashMap<String, Double>>(nProteins));

                String fileName = fileMap.get(experiment);
                File myFile = new File(path, fileName);
                BufferedReader br = new BufferedReader(new FileReader(myFile));

                String line = br.readLine();
                String[] split = line.split(separator);
                for (int i = ratioIndex; i < split.length; i++) {
                    String value = split[i];
                    ratios.get(experiment).add(value);
                    ratiosMap.get(experiment).put(value, new HashMap<String, Double>());
                    ratiosMapNormalized.get(experiment).put(value, new HashMap<String, Double>());
                }

                // read the data from the input file
                while ((line = br.readLine()) != null && !line.equals("")) {
                    split = line.split(separator);
                    String completeProteinGroup = split[2];

                    Integer numberOfValidatedPeptides = new Integer(split[6]);
                    Integer numberOfValidatedSpectra = new Integer(split[7]);
                    boolean peptideAndSpectrumFilter = false;

                    // check validated peptide and spectrum counts
                    if (numberOfValidatedPeptides >= minValidatedPeptideCount
                            && numberOfValidatedSpectra >= minValidatedSpectrumCount) {
                        peptideAndSpectrumFilter = true;
                    }

                    Integer decoyIntegerValue = new Integer(split[12]);
                    decoyMap.put(completeProteinGroup, decoyIntegerValue);
                    keyToMainMatchMap.put(completeProteinGroup, split[0]);
                    keyToOtherMatchesMap.put(completeProteinGroup, split[1]);
                    groupClassMap.put(completeProteinGroup, split[3]);
                    mwMap.put(completeProteinGroup, split[8]);
                    descriptionMap.put(completeProteinGroup, split[14]);
                    Integer numberOfPeptides = new Integer(split[4]);
                    peptidesMap.get(experiment).put(completeProteinGroup, numberOfPeptides);
                    Integer numberOfSpectra = new Integer(split[5]);
                    spectraMap.get(experiment).put(completeProteinGroup, numberOfSpectra);
                    validatedPeptidesMap.get(experiment).put(completeProteinGroup, numberOfValidatedPeptides);
                    validatedSpectraMap.get(experiment).put(completeProteinGroup, numberOfValidatedSpectra);
                    Double pScore = new Double(split[10]);
                    scoreMap.get(experiment).put(completeProteinGroup, pScore);
                    Double p = new Double(split[11]);
                    pepMap.get(experiment).put(completeProteinGroup, p);
                    Integer validatedIntegerValue = new Integer(split[13]);
                    validatedMap.get(experiment).put(completeProteinGroup, validatedIntegerValue);

                    for (int i = ratioIndex; i < split.length; i++) {
                        int index = i - ratioIndex;
                        String ratioTitle = ratios.get(experiment).get(index);

                        // only add ratios passing the peptide and spectrum filter
                        if (peptideAndSpectrumFilter) {
                            Double ratioValue = new Double(split[i]);
                            ratiosMap.get(experiment).get(ratioTitle).put(completeProteinGroup, ratioValue);
                        } else {
                            ratiosMap.get(experiment).get(ratioTitle).put(completeProteinGroup, null);
                        }
                    }
                }

                br.close();
            }

            int nKeys = keyToMainMatchMap.size();
            HashMap<String, Double> keyToScoreMap = new HashMap<String, Double>(nKeys);
            HashMap<Double, Integer> nDecoy = new HashMap<Double, Integer>();
            HashMap<Double, Integer> nTarget = new HashMap<Double, Integer>();

            for (String key : keyToMainMatchMap.keySet()) {
                double score = 1;
                for (String experiment : pepMap.keySet()) {
                    Double pep = pepMap.get(experiment).get(key);
                    if (pep != null) {
                        score *= pep;
                    }
                }
                keyToScoreMap.put(key, score);
                if (!nDecoy.containsKey(score)) {
                    nDecoy.put(score, 0);
                    nTarget.put(score, 0);
                }
                if (decoyMap.get(key) == 1) {
                    nDecoy.put(score, nDecoy.get(score) + 1);
                } else {
                    nTarget.put(score, nTarget.get(score) + 1);
                }
            }

            ArrayList<Double> scores = new ArrayList<Double>(nDecoy.keySet());
            Collections.sort(scores);
            int nMax = 0, cpt = -1;
            for (Double score : scores) {
                if (nDecoy.get(score) > 0) {
                    if (cpt > nMax) {
                        nMax = cpt;
                    }
                    if (cpt == -1) {
                        cpt = 0;
                    }
                }
                if (cpt != -1) {
                    cpt += nTarget.get(score);
                }
            }

            HashMap<Double, Double> peps = new HashMap<Double, Double>();
            for (int i = 0; i < scores.size(); i++) {
                double tempScore, score = scores.get(i);
                double nt = nTarget.get(score);
                double nd = nDecoy.get(score);
                int j = i;
                double limit = ((double) (nMax - nt)) / 2;
                while (--j >= 0 && nt < limit) {
                    tempScore = scores.get(j);
                    nt += nTarget.get(tempScore);
                    nd += nDecoy.get(tempScore);
                }
                j = i;
                while (++j < scores.size() && nt < limit) {
                    tempScore = scores.get(j);
                    nt += nTarget.get(tempScore);
                    nd += nDecoy.get(tempScore);
                }
                double pep = nd / nt;
                peps.put(score, pep);
            }

            // find validated proteins
            for (String key : keyToMainMatchMap.keySet()) {
                
                int validationCount = 0;
                
                for (String experiment : orderedExperiments) {
                    if (validatedMap.get(experiment).containsKey(key)) {
                        validationCount += validatedMap.get(experiment).get(key);
                    }
                }
                
                proteinValidatedMap.put(key, validationCount > 0); // @TODO: make this more advanced!!!
            }

            // find the median values
            HashMap<String, HashMap<String, Double>> medians = new HashMap<String, HashMap<String, Double>>(nFiles);

            for (String experiment : orderedExperiments) {

                medians.put(experiment, new HashMap<String, Double>(nRatios));

                for (String ratio : ratios.get(experiment)) {

                    ArrayList<Double> tempValues = new ArrayList<Double>();

                    for (String key : keyToMainMatchMap.keySet()) {

                        if (decoyMap.get(key) == 0 && proteinValidatedMap.get(key)) { // filter out decoys and non validated proteins
                            Double value = ratiosMap.get(experiment).get(ratio).get(key);
                            if (value != null && !value.isNaN()) {
                                tempValues.add(value);
                            }
                        }
                    }

                    double median = BasicMathFunctions.median(tempValues);
                    medians.get(experiment).put(ratio, median);
                }
            }

            // find the normalized ratios
            for (String key : keyToMainMatchMap.keySet()) {
                if (decoyMap.get(key) == 0 && proteinValidatedMap.get(key)) { // filter out decoys and non validated proteins
                    for (String experiment : orderedExperiments) {
                        for (String ratio : ratios.get(experiment)) {
                            Double value = ratiosMap.get(experiment).get(ratio).get(key);
                            if (value != null && !value.isNaN()) {
                                ratiosMapNormalized.get(experiment).get(ratio).put(key, value / medians.get(experiment).get(ratio));
                            }
                        }
                    }
                }
            }
            
            // find the outliers
            // @TODO: implement me!!

            // print the results to file
            File outputFile = new File(path, "summary.txt");
            BufferedWriter writer = new BufferedWriter(new FileWriter(outputFile));

            // first header row
            writer.write("Protein" + separator + "Description" + separator + "MW" + separator + "Other Protein(s) (alphabetical order)"
                    + separator + "Complete Protein Group (alphabetical order)" + separator + "Group Class" + separator + "#Peptides");
            for (int i = 0; i < nFiles; i++) {
                writer.write(separator);
            }
            writer.write("#Spectra");
            for (int i = 0; i < nFiles; i++) {
                writer.write(separator);
            }
            writer.write("#Validated Peptides");
            for (int i = 0; i < nFiles; i++) {
                writer.write(separator);
            }
            writer.write("#Validated Spectra");
            for (int i = 0; i < nFiles; i++) {
                writer.write(separator);
            }
            writer.write("Confidence");
            for (int i = 0; i < nFiles; i++) {
                writer.write(separator);
            }
            writer.write("score" + separator + "Overall Confidence" + separator + "decoy" + separator + "Validated");
            for (int i = 0; i < nFiles; i++) {
                writer.write(separator);
            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment);
                for (int i = 0; i < nRatios; i++) {
                    writer.write(separator);
                }
            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment + " normalized");
                for (int i = 0; i < nRatios; i++) {
                    writer.write(separator);
                }
            }
//            for (String experiment : orderedExperiments) {
//                writer.write(experiment + " normalized / ref");
//                for (int i = 0; i < nRatios - 1; i++) {
//                    writer.write(separator);
//                }
//            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment + " normalized / ref (log2)");
                for (int i = 0; i < nRatios - 1; i++) {
                    writer.write(separator);
                }
            }
            writer.newLine();

            // second header row
            writer.write(separator + separator + separator + separator + separator + separator);
            for (String experiment : orderedExperiments) {
                writer.write(experiment + separator);
            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment + separator);
            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment + separator);
            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment + separator);
            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment + separator);
            }
            writer.write(separator + separator + separator);
            for (String experiment : orderedExperiments) {
                writer.write(experiment + separator);
            }
            for (String experiment : orderedExperiments) {
                for (String ratioName : ratios.get(experiment)) {
                    writer.write(ratioName + separator);
                }
            }
            for (String experiment : orderedExperiments) {
                for (String ratioName : ratios.get(experiment)) {
                    writer.write(ratioName + separator);
                }
            }
//            for (String experiment : orderedExperiments) {
//                for (int i = 0; i < ratios.get(experiment).size(); i++) {
//                    if (i + 1 != referenceChannel) {
//                        writer.write(ratios.get(experiment).get(i) + separator);
//                    } else {
//                        referenceChannels.put(experiment, ratios.get(experiment).get(i));
//                    }
//                }
//            }
            for (String experiment : orderedExperiments) {
                for (int i = 0; i < ratios.get(experiment).size(); i++) {
                    if (i + 1 != referenceChannel) {
                        writer.write(ratios.get(experiment).get(i) + separator);
                    } else {
                        referenceChannels.put(experiment, ratios.get(experiment).get(i));
                    }
                }
            }
            writer.newLine();

            // print the protein details
            for (String key : keyToMainMatchMap.keySet()) {

                if (decoyMap.get(key) == 0 && proteinValidatedMap.get(key)) { // filter out decoys and non validated proteins

                    writer.write(keyToMainMatchMap.get(key) + separator);
                    writer.write(descriptionMap.get(key) + separator);
                    writer.write(mwMap.get(key) + separator);
                    writer.write(keyToOtherMatchesMap.get(key) + separator);
                    writer.write(key + separator);
                    writer.write(groupClassMap.get(key) + separator);
                    for (String experiment : orderedExperiments) {
                        Object value = peptidesMap.get(experiment).get(key);
                        if (value != null) {
                            writer.write(value.toString());
                        }
                        writer.write(separator);
                    }
                    for (String experiment : orderedExperiments) {
                        Object value = spectraMap.get(experiment).get(key);
                        if (value != null) {
                            writer.write(value.toString());
                        }
                        writer.write(separator);
                    }
                    for (String experiment : orderedExperiments) {
                        Object value = validatedPeptidesMap.get(experiment).get(key);
                        if (value != null) {
                            writer.write(value.toString());
                        }
                        writer.write(separator);
                    }
                    for (String experiment : orderedExperiments) {
                        Object value = validatedSpectraMap.get(experiment).get(key);
                        if (value != null) {
                            writer.write(value.toString());
                        }
                        writer.write(separator);
                    }
                    for (String experiment : orderedExperiments) {
                        Double value = pepMap.get(experiment).get(key);
                        if (value != null) {
                            value = 1 - value;
                            writer.write(value.toString());
                        }
                        writer.write(separator);
                    }
                    double score = keyToScoreMap.get(key);
                    writer.write(score + separator);
                    double pep = peps.get(score);
                    writer.write(pep + separator);
                    writer.write(decoyMap.get(key) + separator);
                    for (String experiment : orderedExperiments) {
                        Object value = validatedMap.get(experiment).get(key);
                        if (value != null) {
                            writer.write(value.toString());
                        }
                        writer.write(separator);
                    }

                    // write the raw ratios
                    for (String experiment : orderedExperiments) {
                        for (String ratio : ratios.get(experiment)) {
                            Double value = ratiosMap.get(experiment).get(ratio).get(key);
                            if (value != null && !value.isNaN()) {
                                writer.write(value.toString());
                            }
                            writer.write(separator);
                        }
                    }

                    // write the normalized ratios
                    for (String experiment : orderedExperiments) {
                        for (String ratio : ratios.get(experiment)) {
                            Double value = ratiosMapNormalized.get(experiment).get(ratio).get(key);
                            if (value != null && !value.isNaN()) {
                                writer.write("" + value);
                            }
                            writer.write(separator);
                        }
                    }

                    // write the normalized ratios divided by the reference
//                    for (String experiment : orderedExperiments) {
//                        for (String ratio : ratios.get(experiment)) {
//
//                            if (!ratio.equalsIgnoreCase(referenceChannels.get(experiment))) {
//                                Double value = ratiosMapNormalized.get(experiment).get(ratio).get(key);
//                                Double referenceValue = ratiosMapNormalized.get(experiment).get(referenceChannels.get(experiment)).get(key);
//
//                                if (value != null && !value.isNaN()
//                                        && referenceValue != null && !referenceValue.isNaN() && referenceValue != 0) {
//                                    writer.write("" + (value / referenceValue)); // @TODO: store these values?
//                                }
//
//                                writer.write(separator);
//                            }
//                        }
//                    }

                    // write the log2 normalized ratios divided by the reference
                    for (String experiment : orderedExperiments) {
                        for (String ratio : ratios.get(experiment)) {

                            if (!ratio.equalsIgnoreCase(referenceChannels.get(experiment))) {
                                Double value = ratiosMapNormalized.get(experiment).get(ratio).get(key);
                                Double referenceValue = ratiosMapNormalized.get(experiment).get(referenceChannels.get(experiment)).get(key);

                                if (value != null && !value.isNaN()
                                        && referenceValue != null && !referenceValue.isNaN() && referenceValue != 0) {
                                    double tempValue = value / referenceValue;
                                    writer.write("" + (FastMath.log(tempValue) / FastMath.log(2))); // @TODO: store these values?
                                }

                                writer.write(separator);
                            }
                        }
                    }

                    writer.newLine();
                }
            }

            writer.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
