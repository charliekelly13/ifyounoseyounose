package org.ifyounoseyounose.backend.smelldetectors;

import org.ifyounoseyounose.backend.ManualParserSmellDetector;
import org.ifyounoseyounose.backend.SmellDetector;
import org.ifyounoseyounose.backend.SmellReport;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class MessageChainingSmellDetector extends SmellDetector implements ManualParserSmellDetector {
    @Override
    public SmellReport detectSmell(List<File> sourceCode) {
        SmellReport smells = new SmellReport();// To be returned
        int count = 0;// Line number
        for(File f : sourceCode) {// Iterates through files
            List<Integer> current = new ArrayList<>();// Smelly line numbers
            String line = null;
            count=0;
            try {
                FileReader targetStream = new FileReader(f);
                BufferedReader bufferedReader =
                        new BufferedReader(targetStream);
                while((line = bufferedReader.readLine()) != null) {
                    if(line.matches(".*(\\..*\\(.*\\)){3,}"))// Regular expression check
                    {
                        System.out.println("here");
                        current.add(count);
                    }
                    count++;
                }
                    smells.addToReport(f, current);

            }
            catch(Exception e)
            {
                System.out.println("Invalid file");
            }

        }
        return smells;
    }
}
