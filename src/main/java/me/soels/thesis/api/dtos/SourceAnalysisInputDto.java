package me.soels.thesis.api.dtos;

import com.github.javaparser.ParserConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.analysis.sources.SourceAnalysisInput;

import javax.annotation.Nullable;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
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
    @Pattern(regexp = ".*\\S+.*") // Don't allow empty. @Pattern allows null.
    private final String customDataAnnotation;
    @Nullable
    private final List<String> fnqExcludeRegexes;

    public SourceAnalysisInput toDao() {
        return new SourceAnalysisInput(pathToProjectZip, pathToJaCoCoXml, languageLevel, customDataAnnotation, fnqExcludeRegexes);
    }
}
