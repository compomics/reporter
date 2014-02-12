package eu.isas.reporter.utils;

import com.compomics.util.experiment.identification.matches.ProteinMatch;
import com.compomics.util.math.BasicMathFunctions;
import com.compomics.util.math.statistics.Distribution;
import com.compomics.util.math.statistics.ditributions.NonSymmetricalNormalDistribution;
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
            String path = "D:\\projects\\Yngvild TMT\\reporter";

            int nFiles = 3;
            int nProteins = 20000;
            int nRatios = 6;
            int ratioIndex = 15;

            String[] orderedExperiments = {"Mix 1", "Mix 2", "Mix 3"};
            HashMap<String, String> fileMap = new HashMap<String, String>(nFiles);
            fileMap.put(orderedExperiments[0], "proteins mix1.txt");
            fileMap.put(orderedExperiments[1], "proteins mix2.txt");
            fileMap.put(orderedExperiments[2], "proteins mix3.txt");

            HashMap<String, ArrayList<String>> ratios = new HashMap<String, ArrayList<String>>(nFiles);

            HashMap<String, String> keyToMainMatchMap = new HashMap<String, String>(nProteins);
            HashMap<String, String> keyToOtherMatchesMap = new HashMap<String, String>(nProteins);
            HashMap<String, String> groupClassMap = new HashMap<String, String>(nProteins);
            HashMap<String, String> mwMap = new HashMap<String, String>(nProteins);
            HashMap<String, String> descriptionMap = new HashMap<String, String>(nProteins);
            HashMap<String, Integer> decoyMap = new HashMap<String, Integer>(nProteins);

            HashMap<String, ArrayList<String>> proteinKeysMap = new HashMap<String, ArrayList<String>>(nFiles);
            HashMap<String, HashMap<String, Integer>> peptidesMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, Integer>> spectraMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, Integer>> validatedPeptidesMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, Integer>> validatedSpectraMap = new HashMap<String, HashMap<String, Integer>>(nFiles);
            HashMap<String, HashMap<String, Double>> pepMap = new HashMap<String, HashMap<String, Double>>(nFiles);
            HashMap<String, HashMap<String, Double>> scoreMap = new HashMap<String, HashMap<String, Double>>(nFiles);
            HashMap<String, HashMap<String, String>> validatedMap = new HashMap<String, HashMap<String, String>>(nFiles);
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
                validatedMap.put(experiment, new HashMap<String, String>(nProteins));
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
                    String validatedStatus = split[13];
                    validatedMap.get(experiment).put(completeProteinGroup, validatedStatus);

                    for (int i = ratioIndex; i < split.length; i++) {
                        int index = i - ratioIndex;
                        String ratioTitle = ratios.get(experiment).get(index);
                        Double ratioValue = new Double(split[i]);
                        ratiosMap.get(experiment).get(ratioTitle).put(completeProteinGroup, ratioValue);
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
            HashMap<Double, Double> fdr = new HashMap<Double, Double>();
            double totalDecoy = 0, lastValidatedScore = 0.0;
            int nValidated = 0, totalTarget = 0;
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

                totalDecoy += nd;
                totalTarget += nt;
                double tempFdr = totalDecoy / totalTarget;
                if (tempFdr <= 0.01) {
                    lastValidatedScore = score;
                    nValidated = totalTarget;
                }
                fdr.put(score, tempFdr);
            }

            System.out.println("Score threshold: " + lastValidatedScore + ", # validated: " + nValidated);

            // Output results
            File outputFile = new File(path, "quanification_raw_results.txt");
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
            writer.write("Validated in mix");
            for (int i = 0; i < nFiles; i++) {
                writer.write(separator);
            }
            writer.write("score" + separator + "Overall Confidence" + separator + "decoy" + separator + "FDR" + separator + "Validated" + separator);
            for (String experiment : orderedExperiments) {
                writer.write(experiment + " ratios");
                for (int i = 0; i < nRatios; i++) {
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
            for (String experiment : orderedExperiments) {
                writer.write(experiment + separator);
            }
            writer.write(separator + separator + separator + separator + separator);
            for (String experiment : orderedExperiments) {
                for (String ratioName : ratios.get(experiment)) {
                    writer.write(ratioName + separator);
                }
            }
            writer.newLine();

            // print the protein details
            for (String key : keyToMainMatchMap.keySet()) {

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
                for (String experiment : orderedExperiments) {
                    String value = validatedMap.get(experiment).get(key);
                    if (value != null) {
                        writer.write(value);
                    }
                    writer.write(separator);
                }
                double score = keyToScoreMap.get(key);
                writer.write(score + separator);
                double pep = peps.get(score);
                double confidence = 1 - pep;
                writer.write(confidence + separator);
                writer.write(decoyMap.get(key) + separator);
                writer.write(fdr.get(score) + separator);
                if (score <= lastValidatedScore) {
                    writer.write(1 + separator);
                } else {
                    writer.write(0 + separator);
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

                writer.newLine();
            }
            writer.close();

            // Import pathway proteins
            String[] pathwayFiles = {"jak-stat", "stem cell maintenance"};
            HashMap<String, ArrayList<String>> pathways = new HashMap<String, ArrayList<String>>(nFiles);
            for (String pathway : pathwayFiles) {
                ArrayList<String> accessions = new ArrayList<String>();
                File pathwayFile = new File(path, pathway + ".txt");
                BufferedReader reader = new BufferedReader(new FileReader(pathwayFile));
                String line = reader.readLine();
                while ((line = reader.readLine()) != null) {
                    String[] content = line.split("\t");
                    accessions.add(content[1]);
                }
                pathways.put(pathway, accessions);
                reader.close();
            }

            // Get a list of protein matches amendable for quantification, create list of ratios
            ArrayList<String> quantificationMatches = new ArrayList<String>();
            HashMap<String, HashMap<String, ArrayList<Double>>> validatedRatios = new HashMap<String, HashMap<String, ArrayList<Double>>>();
            HashMap<String, HashMap<String, HashMap<String, HashMap<String, Double>>>> pathwaysRatios = new HashMap<String, HashMap<String, HashMap<String, HashMap<String, Double>>>>(); //Pathway > protein key > experiment > channel > ratio
            for (String key : keyToMainMatchMap.keySet()) {
                int decoy = decoyMap.get(key);
                if (decoy == 0) {
                    double score = keyToScoreMap.get(key);
                    if (score < lastValidatedScore) {
                        for (String experiment : orderedExperiments) {
                            Integer nValidatedPeptides = validatedPeptidesMap.get(experiment).get(key);
                            if (nValidatedPeptides != null && nValidatedPeptides > 1) {
                                if (!quantificationMatches.contains(key)) {
                                    quantificationMatches.add(key);
                                }
                                HashMap<String, ArrayList<Double>> tempRatiosMap = validatedRatios.get(experiment);
                                if (tempRatiosMap == null) {
                                    tempRatiosMap = new HashMap<String, ArrayList<Double>>();
                                    validatedRatios.put(experiment, tempRatiosMap);
                                }
                                for (String ratio : ratios.get(experiment)) {
                                    Double value = ratiosMap.get(experiment).get(ratio).get(key);
                                    if (value != null && !value.isNaN()) {
                                        ArrayList<Double> tempRatios = tempRatiosMap.get(ratio);
                                        if (tempRatios == null) {
                                            tempRatios = new ArrayList<Double>();
                                            tempRatiosMap.put(ratio, tempRatios);
                                        }
                                        tempRatios.add(value);
                                    }
                                }
                            }
                        }
                    }
                    for (String accession : ProteinMatch.getAccessions(key)) {
                        for (String pathway : pathways.keySet()) {
                            if (pathways.get(pathway).contains(accession)) {
                                for (String experiment : orderedExperiments) {
                                    HashMap<String, HashMap<String, HashMap<String, Double>>> pathwayRatios = pathwaysRatios.get(pathway);
                                    if (pathwayRatios == null) {
                                        pathwayRatios = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
                                        pathwaysRatios.put(pathway, pathwayRatios);
                                    }
                                    HashMap<String, HashMap<String, Double>> proteinRatios = pathwayRatios.get(key);
                                    if (proteinRatios == null) {
                                        proteinRatios = new HashMap<String, HashMap<String, Double>>();
                                        pathwayRatios.put(key, proteinRatios);
                                    }
                                    HashMap<String, Double> tempRatiosMap = proteinRatios.get(experiment);
                                    if (tempRatiosMap == null) {
                                        tempRatiosMap = new HashMap<String, Double>();
                                        proteinRatios.put(experiment, tempRatiosMap);
                                    }
                                    for (String ratio : ratios.get(experiment)) {
                                        Double value = ratiosMap.get(experiment).get(ratio).get(key);
                                        if (value != null && !value.isNaN()) {
                                            tempRatiosMap.put(ratio, value);
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Output medianes
            outputFile = new File(path, "medianes.txt");
            writer = new BufferedWriter(new FileWriter(outputFile));
            writer.write(separator);
            for (String experiment : orderedExperiments) {
                writer.write(experiment);
                for (String ratio : ratios.get(experiment)) {
                    writer.write(separator);
                }
            }
            writer.newLine();
            writer.write(separator);
            for (String experiment : orderedExperiments) {
                for (String ratio : ratios.get(experiment)) {
                    writer.write(ratio + separator);
                }
            }
            writer.newLine();
            writer.write("Median" + separator);
            HashMap<String, HashMap<String, Double>> medianes = new HashMap<String, HashMap<String, Double>>();
            for (String experiment : orderedExperiments) {
                medianes.put(experiment, new HashMap<String, Double>());
                for (String ratio : ratios.get(experiment)) {
                    double median = BasicMathFunctions.median(validatedRatios.get(experiment).get(ratio));
                    writer.write(median + separator);
                    medianes.get(experiment).put(ratio, median);
                }
            }
            writer.close();

            // Normalize every column by the median
            HashMap<String, HashMap<String, HashMap<String, Double>>> medianNormalizedValidatedRatiosMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>(nFiles);
            for (String key : quantificationMatches) {
                HashMap<String, HashMap<String, Double>> proteinRatios = new HashMap<String, HashMap<String, Double>>();
                medianNormalizedValidatedRatiosMap.put(key, proteinRatios);
                for (String experiment : orderedExperiments) {
                    HashMap<String, Double> experimentRatios = new HashMap<String, Double>();
                    proteinRatios.put(experiment, experimentRatios);
                    for (String ratio : ratios.get(experiment)) {
                        Double value = ratiosMap.get(experiment).get(ratio).get(key);
                        if (value != null && !value.isNaN()) {
                            double median = medianes.get(experiment).get(ratio);
                            double normalizedValue = value / median;
                            experimentRatios.put(ratio, normalizedValue);
                        }
                    }
                }
            }

            // Normalize every line by channels 126 and 127
            HashMap<String, HashMap<String, HashMap<String, Double>>> normalizedValidatedRatiosMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>(nFiles);
            for (String key : quantificationMatches) {
                HashMap<String, HashMap<String, Double>> proteinRatios = new HashMap<String, HashMap<String, Double>>();
                normalizedValidatedRatiosMap.put(key, proteinRatios);
                for (String experiment : orderedExperiments) {
                    ArrayList<Double> controlIntensities = new ArrayList<Double>();
                    for (String ratio : ratios.get(experiment)) {
                        if (ratio.contains("126") || ratio.contains("127")) {
                            Double value = medianNormalizedValidatedRatiosMap.get(key).get(experiment).get(ratio);
                            if (value != null && !value.isNaN()) {
                                controlIntensities.add(value);
                            }
                        }
                    }
                    HashMap<String, Double> experimentRatios = new HashMap<String, Double>();
                    proteinRatios.put(experiment, experimentRatios);
                    if (!controlIntensities.isEmpty()) {
                        double normalizationValue = BasicMathFunctions.median(controlIntensities);
                        for (String ratio : ratios.get(experiment)) {
                            Double value = medianNormalizedValidatedRatiosMap.get(key).get(experiment).get(ratio);
                            if (value != null && !value.isNaN()) {
                                double normalizedValue = value / normalizationValue;
                                experimentRatios.put(ratio, normalizedValue);
                            }
                        }
                    }
                }
            }

            // Normalize the pathway proteins
            HashMap<String, HashMap<String, HashMap<String, HashMap<String, Double>>>> normalizedPathwaysRatiosMap = new HashMap<String, HashMap<String, HashMap<String, HashMap<String, Double>>>>(nFiles);
            for (String pathway : pathwaysRatios.keySet()) {
                for (String key : pathwaysRatios.get(pathway).keySet()) {
                    for (String experiment : pathwaysRatios.get(pathway).get(key).keySet()) {
                        ArrayList<Double> controlIntensities = new ArrayList<Double>();
                        HashMap<String, Double> allIntensities = new HashMap<String, Double>();
                        for (String channel : pathwaysRatios.get(pathway).get(key).get(experiment).keySet()) {
                            Double value = pathwaysRatios.get(pathway).get(key).get(experiment).get(channel);
                            if (value != null && !value.isNaN()) {
                                double median = medianes.get(experiment).get(channel);
                                value /= median;
                                if (channel.contains("126") || channel.contains("127")) {
                                    controlIntensities.add(value);
                                }
                                allIntensities.put(channel, value);
                            }
                        }
                        if (!allIntensities.isEmpty()) {
                            double normalizationIntensity = 1;
                            if (!controlIntensities.isEmpty()) {
                                normalizationIntensity = BasicMathFunctions.median(controlIntensities);
                            } else {
                                ArrayList<Double> intensities = new ArrayList<Double>(allIntensities.values());
                                normalizationIntensity = BasicMathFunctions.median(intensities);
                            }
                            HashMap<String, HashMap<String, HashMap<String, Double>>> pathwayRatios = normalizedPathwaysRatiosMap.get(pathway);
                            if (pathwayRatios == null) {
                                pathwayRatios = new HashMap<String, HashMap<String, HashMap<String, Double>>>();
                                normalizedPathwaysRatiosMap.put(pathway, pathwayRatios);
                            }
                            HashMap<String, HashMap<String, Double>> proteinRatios = pathwayRatios.get(key);
                            if (proteinRatios == null) {
                                proteinRatios = new HashMap<String, HashMap<String, Double>>();
                                pathwayRatios.put(key, proteinRatios);
                            }
                            HashMap<String, Double> experimentRatios = proteinRatios.get(experiment);
                            if (experimentRatios == null) {
                                experimentRatios = new HashMap<String, Double>();
                                proteinRatios.put(experiment, experimentRatios);
                            }
                            for (String channel : allIntensities.keySet()) {
                                double value = allIntensities.get(channel);
                                value /= normalizationIntensity;
                                experimentRatios.put(channel, value);
                            }
                        }
                    }
                }
            }

            HashMap<String, HashMap<String, HashMap<String, Double>>> logNormalizedValidatedRatiosMap = new HashMap<String, HashMap<String, HashMap<String, Double>>>(nFiles);
            HashMap<String, HashMap<String, ArrayList<Double>>> logNormalizedValidatedRatios = new HashMap<String, HashMap<String, ArrayList<Double>>>();
            for (String key : quantificationMatches) {
                HashMap<String, HashMap<String, Double>> proteinRatios = new HashMap<String, HashMap<String, Double>>();
                logNormalizedValidatedRatiosMap.put(key, proteinRatios);
                for (String experiment : orderedExperiments) {
                    HashMap<String, Double> experimentRatios = new HashMap<String, Double>();
                    proteinRatios.put(experiment, experimentRatios);
                    HashMap<String, ArrayList<Double>> experimentRatiosList = logNormalizedValidatedRatios.get(experiment);
                    if (experimentRatiosList == null) {
                        experimentRatiosList = new HashMap<String, ArrayList<Double>>();
                        logNormalizedValidatedRatios.put(experiment, experimentRatiosList);
                    }
                    for (String ratio : ratios.get(experiment)) {
                        Double value = normalizedValidatedRatiosMap.get(key).get(experiment).get(ratio);
                        if (value != null && !value.isNaN()) {
                            double normalizedLogValue = FastMath.log10(value);
                            ArrayList<Double> tempRatios = experimentRatiosList.get(ratio);
                            if (tempRatios == null) {
                                tempRatios = new ArrayList<Double>();
                                experimentRatiosList.put(ratio, tempRatios);
                            }
                            tempRatios.add(normalizedLogValue);
                            experimentRatios.put(ratio, normalizedLogValue);
                        }
                    }
                }
            }

            HashMap<String, HashMap<String, NonSymmetricalNormalDistribution>> distributionsMap = new HashMap<String, HashMap<String, NonSymmetricalNormalDistribution>>();
            for (String experiment : orderedExperiments) {
                distributionsMap.put(experiment, new HashMap<String, NonSymmetricalNormalDistribution>());
                for (String ratio : ratios.get(experiment)) {
                    ArrayList<Double> input = logNormalizedValidatedRatios.get(experiment).get(ratio);
                    distributionsMap.get(experiment).put(ratio, NonSymmetricalNormalDistribution.getRobustNonSymmetricalNormalDistribution(input));
                }
            }

            // Output results
            outputFile = new File(path, "quanification_results.txt");
            writer = new BufferedWriter(new FileWriter(outputFile));

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
            writer.write("Validated in mix");
            for (int i = 0; i < nFiles; i++) {
                writer.write(separator);
            }
            writer.write("score" + separator + "Overall Confidence" + separator + "Validated" + separator);
            for (String experiment : orderedExperiments) {
                writer.write(experiment + " raw ratios");
                for (int i = 0; i < nRatios; i++) {
                    writer.write(separator);
                }
            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment + " normalized ratios");
                for (int i = 0; i < nRatios; i++) {
                    writer.write(separator);
                }
            }
            for (String experiment : orderedExperiments) {
                writer.write(experiment + " p");
                for (int i = 0; i < nRatios; i++) {
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
            for (String experiment : orderedExperiments) {
                writer.write(experiment + separator);
            }
            writer.write(separator + separator + separator);
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
            for (String experiment : orderedExperiments) {
                for (String ratioName : ratios.get(experiment)) {
                    writer.write(ratioName + separator);
                }
            }
            writer.newLine();

            // print the protein details
            for (String key : quantificationMatches) {

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
                for (String experiment : orderedExperiments) {
                    String value = validatedMap.get(experiment).get(key);
                    if (value != null) {
                        writer.write(value);
                    }
                    writer.write(separator);
                }
                double score = keyToScoreMap.get(key);
                writer.write(score + separator);
                double pep = peps.get(score);
                double confidence = 1 - pep;
                writer.write(confidence + separator);
                if (score <= lastValidatedScore) {
                    writer.write(1 + separator);
                } else {
                    writer.write(0 + separator);
                }

                // write the raw ratios
                for (String experiment : orderedExperiments) {
                    Integer nValidatedPeptides = validatedPeptidesMap.get(experiment).get(key);
                    for (String ratio : ratios.get(experiment)) {
                        if (nValidatedPeptides != null && nValidatedPeptides > 1) {
                            Double value = ratiosMap.get(experiment).get(ratio).get(key);
                            if (value != null && !value.isNaN()) {
                                writer.write(value.toString());
                            }
                        }
                        writer.write(separator);
                    }
                }

                // write the normalized ratios
                for (String experiment : orderedExperiments) {
                    Integer nValidatedPeptides = validatedPeptidesMap.get(experiment).get(key);
                    for (String ratio : ratios.get(experiment)) {
                        if (nValidatedPeptides != null && nValidatedPeptides > 1) {
                            Double value = normalizedValidatedRatiosMap.get(key).get(experiment).get(ratio);
                            if (value != null && !value.isNaN()) {
                                writer.write(value.toString());
                            }
                        }
                        writer.write(separator);
                    }
                }

                // write the p
                for (String experiment : orderedExperiments) {
                    Integer nValidatedPeptides = validatedPeptidesMap.get(experiment).get(key);
                    for (String ratio : ratios.get(experiment)) {
                        if (nValidatedPeptides != null && nValidatedPeptides > 1) {
                            Double logValue = logNormalizedValidatedRatiosMap.get(key).get(experiment).get(ratio);
                            if (logValue != null && !logValue.isNaN()) {
                                Distribution distribution = distributionsMap.get(experiment).get(ratio);
                                Double p = distribution.getProbabilityAt(logValue);
                                writer.write(p.toString());
                            }
                            writer.write(separator);
                        }
                    }
                }

                writer.newLine();
            }
            writer.close();

            // Output pathways
            for (String pathway : normalizedPathwaysRatiosMap.keySet()) {

                outputFile = new File(path, pathway + "_quantification.txt");
                writer = new BufferedWriter(new FileWriter(outputFile));

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
                writer.write("Validated in mix");
                for (int i = 0; i < nFiles; i++) {
                    writer.write(separator);
                }
                writer.write("score" + separator + "Overall Confidence" + separator + "Validated" + separator);
                for (String experiment : orderedExperiments) {
                    writer.write(experiment + " raw ratios");
                    for (int i = 0; i < nRatios; i++) {
                        writer.write(separator);
                    }
                }
                for (String experiment : orderedExperiments) {
                    writer.write(experiment + " normalized ratios");
                    for (int i = 0; i < nRatios; i++) {
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
                for (String experiment : orderedExperiments) {
                    writer.write(experiment + separator);
                }
                writer.write(separator + separator + separator);
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
                writer.newLine();

                // print the protein details
                for (String key : normalizedPathwaysRatiosMap.get(pathway).keySet()) {
                    
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
                    for (String experiment : orderedExperiments) {
                        String value = validatedMap.get(experiment).get(key);
                        if (value != null) {
                            writer.write(value);
                        }
                        writer.write(separator);
                    }
                    double score = keyToScoreMap.get(key);
                    writer.write(score + separator);
                    double pep = peps.get(score);
                    double confidence = 1 - pep;
                    writer.write(confidence + separator);
                    if (score <= lastValidatedScore) {
                        writer.write(1 + separator);
                    } else {
                        writer.write(0 + separator);
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
                            if (normalizedPathwaysRatiosMap.get(pathway).get(key).get(experiment) != null) {
                                Double value = normalizedPathwaysRatiosMap.get(pathway).get(key).get(experiment).get(ratio);
                                if (value != null && !value.isNaN()) {
                                    writer.write(value.toString());
                                }
                            }
                            writer.write(separator);
                        }
                    }

                    writer.newLine();
                }
                writer.close();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
