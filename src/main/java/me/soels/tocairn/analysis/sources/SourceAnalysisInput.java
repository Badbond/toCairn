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
    private final List<String> dataClassFqnRegexes;
    private final List<String> dataClassAnnotationRegexes;
    private final List<String> pathIncludeRegexes;
    private final List<String> fqnExcludeRegexes;
    private final boolean includeInnerClasses;
    private final boolean removeIsolatedClasses;

    /**
     * Creates the input required to perform source analysis.
     *
     * @param pathToZip                  the path to the .zip file to analyze containing (generated) source code
     * @param pathToJaCoCoXml            the path to the jacoco.xml report
     * @param topPackageRegex            the top-level package definition regex for feature extraction
     * @param languageLevel              the Java language to parse the project with
     * @param dataClassFqnRegexes        a list of regexes to match classes' FQN with to mark them as data
     * @param dataClassAnnotationRegexes a list of regexes to match classes' annotations with to mark them as data
     * @param pathIncludeRegexes         a list of regexes targeting a class' storage path to include in analysis
     * @param fqnExcludeRegexes          a list of regexes targeting a class' FQN to exclude in analysis
     * @param includeInnerClasses        whether to include inner classes or not, default true
     * @param removeIsolatedClasses      whether to remove isolated classes after analysis is completed
     */
    public SourceAnalysisInput(Path pathToZip,
                               @Nullable Path pathToJaCoCoXml,
                               String topPackageRegex,
                               ParserConfiguration.LanguageLevel languageLevel,
                               @Nullable List<String> dataClassFqnRegexes,
                               @Nullable List<String> dataClassAnnotationRegexes,
                               @Nullable List<String> pathIncludeRegexes,
                               @Nullable List<String> fqnExcludeRegexes,
                               @Nullable Boolean includeInnerClasses,
                               @Nullable Boolean removeIsolatedClasses) {
        this.pathToZip = pathToZip;
        this.pathToJaCoCoXml = pathToJaCoCoXml;
        this.languageLevel = languageLevel;
        this.topPackageRegex = topPackageRegex;
        this.dataClassFqnRegexes = dataClassFqnRegexes == null ? new ArrayList<>() : dataClassFqnRegexes;
        this.dataClassAnnotationRegexes = dataClassAnnotationRegexes == null ? new ArrayList<>() : dataClassAnnotationRegexes;
        this.pathIncludeRegexes = pathIncludeRegexes == null ? new ArrayList<>() : pathIncludeRegexes;
        this.fqnExcludeRegexes = fqnExcludeRegexes == null ? new ArrayList<>() : fqnExcludeRegexes;
        this.includeInnerClasses = includeInnerClasses == null || includeInnerClasses;
        this.removeIsolatedClasses = removeIsolatedClasses != null && removeIsolatedClasses;
    }

    public Optional<Path> getPathToJaCoCoXml() {
        return Optional.ofNullable(pathToJaCoCoXml);
    }

    public boolean includeInnerClasses() {
        return includeInnerClasses;
    }

    public boolean removeInnerClasses() {
        return removeIsolatedClasses;
    }
}
