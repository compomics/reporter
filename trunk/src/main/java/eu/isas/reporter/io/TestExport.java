/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.isas.reporter.io;

import com.compomics.util.experiment.identification.Identification;
import com.compomics.util.experiment.quantification.reporterion.ReporterIonQuantification;
import eu.isas.peptideshaker.myparameters.PSParameter;
import eu.isas.peptideshaker.scoring.MatchValidationLevel;
import eu.isas.reporter.calculation.MatchQuantificationDetails;
import eu.isas.reporter.calculation.QuantificationFeaturesGenerator;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.SQLException;
import uk.ac.ebi.jmzml.xml.io.MzMLUnmarshallerException;

/**
 * Test for the export
 *
 * @author Marc
 */
public class TestExport {

    private static String separator = "\t";

    public static void testExport(Identification identification, ReporterIonQuantification reporterIonQuantification, QuantificationFeaturesGenerator quantificationFeaturesGenerator) throws IOException, SQLException, ClassNotFoundException, InterruptedException, MzMLUnmarshallerException {

        File newFile = new File("D:\\projects\\reporter\\testExport.txt");
        BufferedWriter bw = new BufferedWriter(new FileWriter(newFile));

        bw.write("Key");
        for (int index : reporterIonQuantification.getSampleIndexes()) {
            bw.write(separator + reporterIonQuantification.getSample(index).getReference());
        }
        bw.newLine();
        try {
            int cpt = 0, total = identification.getProteinIdentification().size();
            PSParameter psParameter = new PSParameter();
            identification.loadProteinMatchParameters(psParameter, null);
            for (String protein : identification.getProteinIdentification()) {
                psParameter = (PSParameter) identification.getProteinMatchParameter(protein, psParameter);
                if (psParameter.getMatchValidationLevel() == MatchValidationLevel.confident) {
                    MatchQuantificationDetails matchQuantificationDetails = quantificationFeaturesGenerator.getProteinMatchQuantificationDetails(protein);
                    for (int index : reporterIonQuantification.getSampleIndexes()) {
                        bw.write(separator + matchQuantificationDetails.getRatio(index));
                    }
                }
                System.out.println("export " + cpt + "/" + total);
            }
        } finally {
            bw.close();
        }
    }

}
