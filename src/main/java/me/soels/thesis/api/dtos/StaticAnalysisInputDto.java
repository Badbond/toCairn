package me.soels.thesis.api.dtos;

import com.github.javaparser.ParserConfiguration;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.analysis.statik.StaticAnalysisInput;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import java.nio.file.Path;

@Getter
@AllArgsConstructor
public final class StaticAnalysisInputDto {
    @NotNull
    private final Path pathToProjectZip;
    @NotNull
    private final ParserConfiguration.LanguageLevel languageLevel;
    @Pattern(regexp = ".*\\S+.*") // Don't allow empty. @Pattern allows null.
    private final String customDataAnnotation;

    public StaticAnalysisInput toDao() {
        return new StaticAnalysisInput(pathToProjectZip, languageLevel, customDataAnnotation);
    }
}
