package org.ifyounoseyounose.backend.smelldetectors;

import org.ifyounoseyounose.backend.SmellReport;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class DuplicateCodeSmellDetector extends SmellDetector implements ManualParserSmellDetector {
    @Override
    public SmellReport detectSmell(List<File> sourceCode) {
        SmellReport smells = new SmellReport();// To be returned
        int count = 0;// Line number
        HashMap<String, HashMap<File, List<Integer>>> temp = new HashMap<>();// Key is line contents, value is number of times line has appeared

        for(File f : sourceCode) {// Iterates through files

            String line = null;
            count=0;
            try {

                FileReader targetStream = new FileReader(f);
                BufferedReader bufferedReader =
                        new BufferedReader(targetStream);

                while((line = bufferedReader.readLine()) != null) {
                    line = line.trim();

                    if (!line.equals("}") && !line.equals("{") && !line.equals("")) {// Checks lines are irrelevant
                        HashMap<File, List<Integer>> hold = temp.get(line); //see if you already have a list for current key
                        System.out.println("test4");

                        if (hold == null) { //if not create one and put it in the map
                            hold = new HashMap<File, List<Integer>>();
                            temp.put(line, hold);
                        }
                        List<Integer> l = (temp.get(line)).get(f); //see if you already have a list for current key
                        System.out.println("test4");

                        if (l == null) { //if not create one and put it in the map
                            l = new ArrayList<Integer>();
                            temp.get(line).put(f, l);
                        }
                        System.out.println("test1");
                        temp.get(line).get(f).add(count);
                        System.out.println("test2");
                    }
                    count++;
                }


            }
            catch(IOException z)
            {
                System.out.println("Invalid file");
            }



        }
        if(!temp.isEmpty()) {//Ensures there is a line of code to add
            for(HashMap<File, List<Integer>> x: temp.values()) {//Searches for lines that have occurred more than the limit
                for(File u: x.keySet())
                {
                    System.out.println(u);
                }

            }
            System.out.println("break");
            for(HashMap<File, List<Integer>> x: temp.values()) {//Searches for lines that have occurred more than the limit

                for(File y: sourceCode)
                {
                    System.out.println(y);
                    if(x.get(y).size()>=2)
                    {
                        smells.addToReport(y, x.get(y));
                    }
                }
            }

        }
        return smells;
    }
}