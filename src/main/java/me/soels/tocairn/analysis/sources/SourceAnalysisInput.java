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
    private final String topPackageRegex;
    private final String customDataAnnotation;
    private final List<String> pathIncludeRegexes;
    private final List<String> fqnExcludeRegexes;
    private final List<String> dataClassFqnRegexes;

    /**
     * Creates the input required to perform source analysis.
     *
     * @param pathToZip            the path to the .zip file to analyze containing (generated) source code
     * @param pathToJaCoCoXml      the path to the jacoco.xml report
     * @param topPackageRegex      the top-level package definition regex for feature extraction
     * @param languageLevel        the Java language to parse the project with
     * @param customDataAnnotation the custom annotation to apply on classes identifying data
     * @param pathIncludeRegexes   a list of regexes targeting a class' storage path to include in analysis
     * @param fqnExcludeRegexes    a list of regexes targeting a class' FQN to exclude in analysis
     * @param dataClassFqnRegexes  a list of regex to match classes with to mark them as data
     */
    public SourceAnalysisInput(Path pathToZip,
                               @Nullable Path pathToJaCoCoXml,
                               String topPackageRegex,
                               ParserConfiguration.LanguageLevel languageLevel,
                               String customDataAnnotation,
                               @Nullable List<String> pathIncludeRegexes,
                               @Nullable List<String> fqnExcludeRegexes,
                               @Nullable List<String> dataClassFqnRegexes) {
        this.pathToZip = pathToZip;
        this.pathToJaCoCoXml = pathToJaCoCoXml;
        this.languageLevel = languageLevel;
        this.topPackageRegex = topPackageRegex;
        this.customDataAnnotation = customDataAnnotation;
        this.pathIncludeRegexes = pathIncludeRegexes == null ? new ArrayList<>() : pathIncludeRegexes;
        this.fqnExcludeRegexes = fqnExcludeRegexes == null ? new ArrayList<>() : fqnExcludeRegexes;
        this.dataClassFqnRegexes = dataClassFqnRegexes == null ? new ArrayList<>() : dataClassFqnRegexes;
    }

    public Optional<Path> getPathToJaCoCoXml() {
        return Optional.ofNullable(pathToJaCoCoXml);
    }
}
