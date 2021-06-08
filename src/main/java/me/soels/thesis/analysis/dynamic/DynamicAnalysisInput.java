package me.soels.thesis.analysis.dynamic;

import java.nio.file.Path;

public class DynamicAnalysisInput {
    private final Path pathToJfrLog;

    public DynamicAnalysisInput(Path pathToJfrLog) {
        this.pathToJfrLog = pathToJfrLog;
    }

    public Path getPathToJfrLog() {
        return pathToJfrLog;
    }
}
