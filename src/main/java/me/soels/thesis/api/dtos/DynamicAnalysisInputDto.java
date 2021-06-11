package me.soels.thesis.api.dtos;

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.analysis.dynamic.DynamicAnalysisInput;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

@Getter
@AllArgsConstructor(onConstructor_ = @JsonCreator)
public final class DynamicAnalysisInputDto {
    @NotNull
    private final Path pathToJfrLog;

    public DynamicAnalysisInput toDao() {
        return new DynamicAnalysisInput(pathToJfrLog);
    }
}
