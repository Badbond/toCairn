package me.soels.thesis.analysis.sources;

import com.github.javaparser.ParserConfiguration;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.Optional;

/**
 * The input required to perform source analysis
 */
public final class SourceAnalysisInput {
    private final Path pathToZip;
    private final Path pathToJacocoXml;
    private final ParserConfiguration.LanguageLevel languageLevel;
    private final String customDataAnnotation;

    /**
     * Creates the input required to perform source analysis.
     *
     * @param pathToZip            the path to the .zip file to analyze containing (generated) source code
     * @param pathToJacocoXml      the path to the jacoco.xml report
     * @param languageLevel        the Java language to parse the project with
     * @param customDataAnnotation the custom annotation to apply on classes identifying data
     */
    public SourceAnalysisInput(Path pathToZip,
                               @Nullable Path pathToJacocoXml,
                               ParserConfiguration.LanguageLevel languageLevel,
                               String customDataAnnotation) {
        this.pathToZip = pathToZip;
        this.pathToJacocoXml = pathToJacocoXml;
        this.languageLevel = languageLevel;
        this.customDataAnnotation = customDataAnnotation;
    }

    public Path getPathToZip() {
        return pathToZip;
    }

    public Optional<Path> getPathToJacocoXml() {
        return Optional.ofNullable(pathToJacocoXml);
    }

    public ParserConfiguration.LanguageLevel getLanguageLevel() {
        return languageLevel;
    }

    public String getCustomDataAnnotation() {
        return customDataAnnotation;
    }
}
