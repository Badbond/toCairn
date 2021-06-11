package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.analysis.evolutionary.EvolutionaryAnalysisInput;

import java.nio.file.Path;

@Getter
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public final class EvolutionaryAnalysisInputDto {
    private final Path pathToGitLog;

    public EvolutionaryAnalysisInput toDao() {
        return new EvolutionaryAnalysisInput(pathToGitLog);
    }
}
