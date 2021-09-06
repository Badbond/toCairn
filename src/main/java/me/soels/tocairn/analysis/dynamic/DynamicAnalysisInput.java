package me.soels.tocairn.analysis.dynamic;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.nio.file.Path;

@Getter
@AllArgsConstructor
public class DynamicAnalysisInput {
    private final Path pathToJfrFile;
    private final String namespace;
}
