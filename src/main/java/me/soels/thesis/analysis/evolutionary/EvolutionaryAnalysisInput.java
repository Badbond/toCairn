package me.soels.thesis.analysis.evolutionary;

import java.nio.file.Path;

public class EvolutionaryAnalysisInput {
    private final Path pathToGitLog;

    public EvolutionaryAnalysisInput(Path pathToGitLog) {
        this.pathToGitLog = pathToGitLog;
    }

    public Path getPathToGitLog() {
        return pathToGitLog;
    }
}
