package me.soels.thesis.analysis.sources;

import com.github.javaparser.ParserConfiguration;
import lombok.Getter;

import javax.annotation.Nullable;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * The input required to perform source analysis
 */
@Getter
public final class SourceAnalysisInput {
    private final Path pathToZip;
    private final Path pathToJacocoXml;
    private final ParserConfiguration.LanguageLevel languageLevel;
    private final String customDataAnnotation;
    private final List<String> fnqExcludeRegexes;

    /**
     * Creates the input required to perform source analysis.
     *
     * @param pathToZip            the path to the .zip file to analyze containing (generated) source code
     * @param pathToJacocoXml      the path to the jacoco.xml report
     * @param languageLevel        the Java language to parse the project with
     * @param customDataAnnotation the custom annotation to apply on classes identifying data
     * @param fnqExcludeRegexes    a list of regexes to exclude classes in analysis for
     */
    public SourceAnalysisInput(Path pathToZip,
                               @Nullable Path pathToJacocoXml,
                               ParserConfiguration.LanguageLevel languageLevel,
                               String customDataAnnotation,
                               List<String> fnqExcludeRegexes) {
        this.pathToZip = pathToZip;
        this.pathToJacocoXml = pathToJacocoXml;
        this.languageLevel = languageLevel;
        this.customDataAnnotation = customDataAnnotation;
        this.fnqExcludeRegexes = fnqExcludeRegexes == null ? new ArrayList<>() : fnqExcludeRegexes;
    }

    public Optional<Path> getPathToJacocoXml() {
        return Optional.ofNullable(pathToJacocoXml);
    }
}
