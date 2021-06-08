package me.soels.thesis.analysis;

import com.github.javaparser.ParserConfiguration;

import java.nio.file.Path;
import java.util.UUID;

/**
 * The input required to perform static analysis
 */
public final class StaticAnalysisInput {
    private final UUID evaluationId;
    private final Path pathToZip;
    private final ParserConfiguration.LanguageLevel languageLevel;
    private final String customDataAnnotation;

    /**
     * Creates the input required to perform static analysis.
     *
     * @param evaluationId         the ID of the evaluation which we are performing analysis for
     * @param pathToZip            the path to the .zip file to analyze containing (generated) source code
     * @param languageLevel        the Java language to parse the project with
     * @param customDataAnnotation the custom annotation to apply on classes identifying data
     */
    public StaticAnalysisInput(UUID evaluationId, Path pathToZip,
                               ParserConfiguration.LanguageLevel languageLevel,
                               String customDataAnnotation) {
        this.evaluationId = evaluationId;
        this.pathToZip = pathToZip;
        this.languageLevel = languageLevel;
        this.customDataAnnotation = customDataAnnotation;
    }

    public UUID getEvaluationId() {
        return evaluationId;
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
