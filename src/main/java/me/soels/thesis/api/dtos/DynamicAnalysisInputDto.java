package me.soels.thesis.api.dtos;

import lombok.AllArgsConstructor;
import lombok.Getter;
import me.soels.thesis.analysis.dynamic.DynamicAnalysisInput;

import javax.validation.constraints.NotNull;
import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class DynamicAnalysisInputDto {
    @NotNull
    private final Path pathToJfrFile;
    @NotNull
    private final String namespace;

    public DynamicAnalysisInput toDao() {
        return new DynamicAnalysisInput(pathToJfrFile, namespace);
    }
}
