package me.soels.thesis.analysis.statik;

import me.soels.thesis.model.EvaluationInputBuilder;

import java.nio.file.Path;

/**
 * Context holder for static analysis.
 * <p>
 * This context holder will hold the resulting analysis results and populates that in the {@link EvaluationInputBuilder}.
 * It furthermore contains data needed to share between stages of the static analysis, utility functions on the data
 * stored within this context, and additional data in favor of debugging such as counters.
 */
class StaticAnalysisContext {
    private final Path projectLocation;
    private final StaticAnalysisInput input;
    private final EvaluationInputBuilder resultBuilder;
    private final Counters counters = new Counters();

    public StaticAnalysisContext(Path projectLocation,
                                 StaticAnalysisInput input,
                                 EvaluationInputBuilder resultBuilder) {
        this.projectLocation = projectLocation;
        this.input = input;
        this.resultBuilder = resultBuilder;
    }

    public Path getProjectLocation() {
        return projectLocation;
    }

    public StaticAnalysisInput getInput() {
        return input;
    }

    public EvaluationInputBuilder getResultBuilder() {
        return resultBuilder;
    }

    public Counters getCounters() {
        return counters;
    }

    static class Counters {
        int unresolvedNodes = 0;
        int relevantConstructorCalls = 0;
        int matchingMethodReferences = 0;
        int relevantMethodReferences = 0;
        int matchingMethodCalls = 0;
        int relevantMethodCalls;
    }
}
