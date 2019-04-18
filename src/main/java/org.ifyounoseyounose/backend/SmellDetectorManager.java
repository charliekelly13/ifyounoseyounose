package org.ifyounoseyounose.backend;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import org.ifyounoseyounose.backend.smelldetectors.*;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.io.FileNotFoundException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * SmellDetectorManager - class that asks each SmellDetector to detect code smells in a list of files
 */
public class SmellDetectorManager {

    /**
     * Method called by ReportBuilder which in turn invokes each individual SmellDetector
     * @param smellDetectorStrings The smell detector names we want to analyse the directory with
     * @param files A list of files we want to analyse
     * @return A result containing info about what code smells were detected
     */
    public List<FileReport> detectSmells(HashMap<String,Integer> smellDetectorStrings, List<File> files) {

        List <SmellDetector> smellDetectors = getSmellDetectors(smellDetectorStrings);

        List<SmellReport> results = new ArrayList<>();
        HashMap<CompilationUnit, File> compUnits = new HashMap<>();

        // For each file, we add a CompilationUnit to the compUnits hashmap, with the File as the key
        // This makes creating SmellReports much easier
        for (File f : files) {
            try {
                compUnits.put(StaticJavaParser.parse(f), f);
            } catch (FileNotFoundException e) {
                System.err.println("Cannot find file " + f.getPath());
            }
        }

        // I go through every file, then every SmellDetector and pass each file to the relevant SmellDetector
        for (SmellDetector smellDetector : smellDetectors) {
            // Check what type of smellDetector it is by seeing what interface it inherits
            // and call smellDetector.detectSmell() on it with the parameters corresponding to its interface
            if (smellDetector instanceof JavaParserSmellDetector) {
                SmellReport result = ((JavaParserSmellDetector) smellDetector).detectSmell(compUnits);
                result.setSmellName(smellDetector.getSmellName());
                results.add(result);
            } else if (smellDetector instanceof ManualParserSmellDetector) {
                SmellReport result = ((ManualParserSmellDetector) smellDetector).detectSmell(files);
                result.setSmellName(smellDetector.getSmellName());
                results.add(result);
            } else if (smellDetector instanceof ReflectionSmellDetector) {
                compileJavaFiles(files);

                HashMap<Class, File> classesMap = new HashMap<>();

                File compiledClassesDirectory = new File(".compiled_classes/");
                URL[] urlList = new URL[1];
                URLClassLoader classLoader = null;

                try {
                    urlList[0] = compiledClassesDirectory.toURI().toURL();
                    classLoader = URLClassLoader.newInstance(urlList);
                } catch (Exception e) {
                    System.err.println("Failed to add .class files to the ClassLoader");
                }

                if (classLoader != null) {
                    // Todo I have no idea why this is in a for loop. I'm scared to break it.
                    for (File f : files) {
                        classesMap = getListOfClasses(classLoader, compiledClassesDirectory, files);
                    }

                    SmellReport result = ((ReflectionSmellDetector) smellDetector).detectSmell(classesMap);
                    result.setSmellName(smellDetector.getSmellName());
                    results.add(result);

                }
            }
        }
        System.out.println("****SmellDetectorManager results:****");
        for (SmellReport s : results) {
            System.out.println(s);
        }

        // We take the list of SmellReports and convert them into FileReports and return this
        List<FileReport> fileReports = new ArrayList<>();

        for (File f : files) {
            for (SmellReport smellReport : results) {
                FileReport fileReport = new FileReport();
                fileReport.addSmellDetections(smellReport.getSmellName(),smellReport.getDetectionsByFile(f));
                fileReport.setFile(f);
                fileReports.add(fileReport);
            }
        }

        return fileReports;
    }

    /**
     * Takes a list of .java files, compiles them .class files and puts them in the .compiled_classes folder
     * @param files List of .java files
     */
    private void compileJavaFiles(List<File> files) {
        String[] st = new String[files.size()+2];

        st[0] = "-d";
        st[1] = ".compiled_classes";
        for (int i = 2; i < st.length; i++) {
            st[i] = files.get(i-2).getPath();
        }

        JavaCompiler compile = ToolProvider.getSystemJavaCompiler();
        compile.run(null, null,null, st);
    }

    /**
     * Given a HashMap containing pairs of SmellDetector names and limits, we create the appropriate SmellDetector
     * objects and return a list
     * @param smellDetectorStrings The HashMap containing SmellDetector names and limits
     * @return The list of SmellDetectors we want to use to detect smells (with limits set)
     */
    private List<SmellDetector> getSmellDetectors(HashMap<String,Integer> smellDetectorStrings) {
        List <SmellDetector> smellDetectors = new ArrayList<>();

        for (String smellDetector : smellDetectorStrings.keySet()) {
            SmellDetector currentSmell = null;
            int limit = -1;
            switch(smellDetector) {
                case "ArrowHeaded":
                    currentSmell = new ArrowheadedIndentationSmellDetector();
                    limit = smellDetectorStrings.get("ArrowHeaded");
                    break;
                case "BloatedClass":
                    currentSmell = new BloatedClassSmellDetector();
                    limit = smellDetectorStrings.get("BloatedClass");
                    break;
                case "BloatedMethod":
                    currentSmell = new BloatedMethodCodeSmellDetector();
                    limit = smellDetectorStrings.get("BloatedMethod");
                    break;
                case "BloatedParameter":
                    currentSmell = new BloatedParamSmellDetector();
                    limit = smellDetectorStrings.get("BloatedParameter");
                    break;
                case "DataOnly":
                    currentSmell = new DataOnlyClassesSmellDetector();
                    limit = smellDetectorStrings.get("DataOnly");
                    break;
                case "DataHiding":
                    currentSmell = new ViolationOfDataHidingSmellDetector();
                    limit = smellDetectorStrings.get("DataHiding");
                    break;
                case "DeadCode":
                    currentSmell = new DeadCodeSmellDetector();
                    limit = smellDetectorStrings.get("DeadCode");
                    break;
                case "DuplicateCode":
                    currentSmell = new DuplicateCodeSmellDetector();
                    limit = smellDetectorStrings.get("DuplicateCode");
                    break;
                case "MessageChaining":
                    currentSmell = new MessageChainingSmellDetector();
                    limit = smellDetectorStrings.get("MessageChaining");
                    break;
                case "PrimitiveObsession":
                    currentSmell = new PrimitiveObsessionSmellDetector();
                    limit = smellDetectorStrings.get("PrimitiveObsession");
                    break;
                case "SwitchStatement":
                    currentSmell = new SwitchStatementSmellDetector();
                    limit = smellDetectorStrings.get("SwitchStatement");
                    break;
                case "TooManyLiterals":
                    currentSmell = new TooManyLiteralsSmellDetector();
                    limit = smellDetectorStrings.get("TooManyLiterals");
                    break;
                case "ViolationOfDataHiding":
                    currentSmell = new ViolationOfDataHidingSmellDetector();
                    limit = smellDetectorStrings.get("ViolationOfDataHiding");
                    break;
                default:
                    System.err.println("Smell Detector not found!");
            }

            if (currentSmell instanceof LimitableSmellDetector) {
                ((LimitableSmellDetector) currentSmell).setLimit(limit);
            }

            smellDetectors.add(currentSmell);
        }

        return smellDetectors;
    }

    /**
     * Returns a list of Class objects that have been loaded into the classLoader
     * @param classLoader URLClassLoader, containing the new classes we have imported
     * @param compiledClassesDirectory This is the folder containing the new .compiled_classes
     * @param  files This is the list of files in the folder imported
     * @return A list of Class objects
     */
    private  HashMap<Class, File> getListOfClasses(URLClassLoader classLoader, File compiledClassesDirectory, List<File> files) {

        List<Path> pathsList = new ArrayList<>();
        HashMap<Class, File> classesMap = new HashMap<>();
        try (Stream<Path> paths = Files.walk(Paths.get(compiledClassesDirectory.toString()))) {
            paths.filter(Files::isRegularFile)
                    .forEach(pathsList::add);
        } catch (Exception e) {
            System.err.println("Error searching .compiled_classes for files");
        }

        for (Path p : pathsList) {
            try {
                String fullPath = p.toString();

                // This extracts the class and package name from the full path of the class in /.compiled_clasees/
                String output = fullPath.replaceAll("\\.compiled_classes/", "");
                output = output.replaceAll("\\.class", "");
                output = output.replaceAll("/", ".");

                for(File f:files){
                    // find the file to which the compiled class corresponds
                    if(fullPath.contains(f.getName().replaceAll(".java", ""))){
                        classesMap.put(classLoader.loadClass(output), f); // add the class and file to hashMap to be returned
                    }
                }


            } catch (ClassNotFoundException e) {
                System.err.println("Cannot find class.");
            }
        }


        return classesMap;
    }
}