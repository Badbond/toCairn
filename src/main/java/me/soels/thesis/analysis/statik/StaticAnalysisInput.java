package me.soels.thesis.analysis.statik;

import com.github.javaparser.ParserConfiguration;

import java.nio.file.Path;

/**
 * The input required to perform static analysis
 */
public final class StaticAnalysisInput {
    private final Path pathToZip;
    private final ParserConfiguration.LanguageLevel languageLevel;
    private final String customDataAnnotation;

    /**
     * Creates the input required to perform static analysis.
     *
     * @param pathToZip            the path to the .zip file to analyze containing (generated) source code
     * @param languageLevel        the Java language to parse the project with
     * @param customDataAnnotation the custom annotation to apply on classes identifying data
     */
    public StaticAnalysisInput(Path pathToZip,
                               ParserConfiguration.LanguageLevel languageLevel,
                               String customDataAnnotation) {
        this.pathToZip = pathToZip;
        this.languageLevel = languageLevel;
        this.customDataAnnotation = customDataAnnotation;
    }

    public Path getPathToZip() {
        return pathToZip;
    }

    public ParserConfiguration.LanguageLevel getLanguageLevel() {
        return languageLevel;
    }

    public String getCustomDataAnnotation() {
        return customDataAnnotation;
    }
}