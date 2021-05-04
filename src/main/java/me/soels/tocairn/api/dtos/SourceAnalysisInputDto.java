package me.soels.tocairn.api.dtos;

import com.github.javaparser.ParserConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.tocairn.analysis.sources.SourceAnalysisInput;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import java.nio.file.Path;
import java.util.List;

@Getter
@AllArgsConstructor
public final class SourceAnalysisInputDto {
    @NotNull
    private final Path pathToProjectZip;
    @Nullable
    private final Path pathToJaCoCoXml;
    @NotNull
    private final ParserConfiguration.LanguageLevel languageLevel;
    @NotNull
    private final String topPackageRegex;
    @Nullable
    private final List<String> dataClassFqnRegexes;
    @Nullable
    private final List<String> dataClassAnnotationRegexes;
    @Nullable
    private final List<String> pathIncludeRegexes;
    @Nullable
    private final List<String> fqnExcludeRegexes;
    @Nullable
    private final Boolean includeInnerClasses;
    @Nullable
    private final Boolean removeIsolatedClasses;

    public SourceAnalysisInput toDao() {
        return new SourceAnalysisInput(pathToProjectZip, pathToJaCoCoXml, topPackageRegex, languageLevel,
                dataClassFqnRegexes, dataClassAnnotationRegexes, pathIncludeRegexes, fqnExcludeRegexes,
                includeInnerClasses, removeIsolatedClasses);
    }
}
