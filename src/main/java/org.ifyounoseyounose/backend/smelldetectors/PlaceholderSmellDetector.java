package org.ifyounoseyounose.backend.smelldetectors;

import com.github.javaparser.ast.CompilationUnit;
import org.ifyounoseyounose.backend.SmellReport;

import java.io.File;
import java.util.HashMap;

/**
 * A placeholder smell detector with no actual function - only used for testing purposes
 */
public class PlaceholderSmellDetector extends SmellDetector implements JavaParserSmellDetector {
    @Override
    public SmellReport detectSmell(HashMap<CompilationUnit, File> compilationUnits) {

        return null;
    }
}
