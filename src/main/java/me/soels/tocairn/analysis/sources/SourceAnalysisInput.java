package me.soels.tocairn.analysis.sources;

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
    private final Path pathToJaCoCoXml;
    private final ParserConfiguration.LanguageLevel languageLevel;
    private final String customDataAnnotation;
    private final List<String> fnqExcludeRegexes;
    private final List<String> dataClassFqnRegexes;

    /**
     * Creates the input required to perform source analysis.
     *
     * @param pathToZip            the path to the .zip file to analyze containing (generated) source code
     * @param pathToJaCoCoXml      the path to the jacoco.xml report
     * @param languageLevel        the Java language to parse the project with
     * @param customDataAnnotation the custom annotation to apply on classes identifying data
     * @param fnqExcludeRegexes    a list of regexes to exclude classes in analysis for
     * @param dataClassFqnRegexes  a list of regex to match classes with to mark them as data
     */
    public SourceAnalysisInput(Path pathToZip,
                               @Nullable Path pathToJaCoCoXml,
                               ParserConfiguration.LanguageLevel languageLevel,
                               String customDataAnnotation,
                               @Nullable List<String> fnqExcludeRegexes,
                               @Nullable List<String> dataClassFqnRegexes) {
        this.pathToZip = pathToZip;
        this.pathToJaCoCoXml = pathToJaCoCoXml;
        this.languageLevel = languageLevel;
        this.customDataAnnotation = customDataAnnotation;
        this.fnqExcludeRegexes = fnqExcludeRegexes == null ? new ArrayList<>() : fnqExcludeRegexes;
        this.dataClassFqnRegexes = dataClassFqnRegexes == null ? new ArrayList<>() : dataClassFqnRegexes;
    }

    public Optional<Path> getPathToJaCoCoXml() {
        return Optional.ofNullable(pathToJaCoCoXml);
    }
}
